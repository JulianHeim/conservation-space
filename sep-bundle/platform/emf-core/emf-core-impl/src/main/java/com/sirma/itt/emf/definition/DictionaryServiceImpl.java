/**
 *
 */
package com.sirma.itt.emf.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.cache.CacheConfiguration;
import com.sirma.itt.emf.cache.CacheTransactionMode;
import com.sirma.itt.emf.cache.Eviction;
import com.sirma.itt.emf.cache.Expiration;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache.EntityLookupCallbackDAOAdaptor;
import com.sirma.itt.emf.cache.lookup.EntityLookupCacheContext;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.db.DbDao;
import com.sirma.itt.emf.db.EmfQueries;
import com.sirma.itt.emf.definition.dao.DefinitionAccessor;
import com.sirma.itt.emf.definition.model.BaseDefinition;
import com.sirma.itt.emf.definition.model.DataType;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.definition.model.DefinitionEntry;
import com.sirma.itt.emf.definition.model.FieldDefinitionImpl;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinitionProxy;
import com.sirma.itt.emf.definition.model.PrototypeDefinition;
import com.sirma.itt.emf.definition.model.PrototypeDefinitionImpl;
import com.sirma.itt.emf.definition.model.WritablePropertyDefinition;
import com.sirma.itt.emf.domain.DisplayType;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.Quad;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.domain.model.GenericProxy;
import com.sirma.itt.emf.domain.model.PathElement;
import com.sirma.itt.emf.domain.model.PathElementProxy;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.evaluation.ExpressionsManager;
import com.sirma.itt.emf.exceptions.CmfDatabaseException;
import com.sirma.itt.emf.exceptions.DictionaryException;
import com.sirma.itt.emf.exceptions.EmfConfigurationException;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.hash.HashCalculator;
import com.sirma.itt.emf.instance.model.CommonInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.instance.model.OwnedModel;
import com.sirma.itt.emf.label.Displayable;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.security.AuthenticationService;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.util.Documentation;
import com.sirma.itt.emf.util.EqualsHelper;
import com.sirma.itt.emf.util.PathHelper;

/**
 * Default service implementation for working with definitions.
 *
 * @author BBonev
 */
@Stateless
public class DictionaryServiceImpl implements MutableDictionaryService {

	private static final long serialVersionUID = -6013425395204741541L;

	/** The Constant NO_DEFINITION_SELECTOR. */
	private static final String BASE_DEFINITION_SELECTOR = "$DEFAULT_DEFINITION$";

	/** The Constant DEFAULT_PATH_ELEMENT. */
	private static final PathElementProxy DEFAULT_PATH_ELEMENT = new PathElementProxy(
			BASE_DEFINITION_SELECTOR);

	/** The Constant TYPE_DEFINITION_CACHE. */
	@CacheConfiguration(container = "cmf", doc = @Documentation(""
			+ "Cache used to contain the type definitions in the system. For every type defined are used 2 cache entries."
			+ "<br>Minimal value expression: types * 2.1"))
	private static final String TYPE_DEFINITION_CACHE = "TYPE_DEFINITION_CACHE";
	@CacheConfiguration(container = "cmf", doc = @Documentation(""
			+ "Cache used to contain the type definitions in the system mapped by URI. For every type defined are used 2 cache entries."
			+ "<br>Minimal value expression: types * 1.2"))
	private static final String TYPE_DEFINITION_URI_CACHE = "TYPE_DEFINITION_URI_CACHE";
	/** The Constant MAX_REVISIONS_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 20), expiration = @Expiration(maxIdle = 900000, interval = 60000), doc = @Documentation(""
			+ "Cache for the list of all definitions at max revision per type"
			+ "<br>Minimal value expression: types * 1.2"))
	private static final String MAX_REVISIONS_CACHE = "MAX_REVISIONS_CACHE";
	/** The Constant PROTOTYPE_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 1000), expiration = @Expiration(maxIdle = 1800000, interval = 60000), transaction = CacheTransactionMode.FULL_XA, doc = @Documentation(""
			+ "Fully transactional cache used to store the unique prototy entries. For every unique property are used 2 cache entries."
			+ "<br>Minimal value expression: uniqueProperties * 2.2"))
	private static final String PROTOTYPE_CACHE = "PROTOTYPE_CACHE";

	/** The db dao. */
	@Inject
	private DbDao dbDao;

	/** The cache context. */
	@Inject
	private EntityLookupCacheContext cacheContext;

	/** The label provider. */
	@Inject
	private LabelProvider labelProvider;

	/** The logger. */
	@Inject
	private Logger LOGGER;

	/** The evaluator manager. */
	@Inject
	private ExpressionsManager evaluatorManager;

	/** The authentication service instance. */
	@Inject
	private javax.enterprise.inject.Instance<AuthenticationService> authenticationServiceInstance;

	/** The default container. */
	@Inject
	@Config(name = EmfConfigurationProperties.DEFAULT_CONTAINER)
	private String defaultContainer;

	private Map<Class<?>, DefinitionAccessor> accessorMapping;

	/**
	 * The definition type to accessor mapping. A mapping of concrete definition id to specific
	 * accessor. Populated lazily.
	 */
	private Map<String, DefinitionAccessor> definitionTypeToAccessorMapping;

	/** Collect all installed accessors. */
	@Inject
	@Any
	private javax.enterprise.inject.Instance<DefinitionAccessor> accessors;

	@Inject
	private HashCalculator hashCalculator;

	@Inject
	private TypeConverter typeConverter;

	/** The trace. */
	private boolean trace;
	/** The debug. */
	private boolean debug;

	/**
	 * Initialize the instance by collecting information about the accessors and cache instances
	 */
	@PostConstruct
	public void init() {
		debug = LOGGER.isDebugEnabled();
		trace = LOGGER.isTraceEnabled();

		definitionTypeToAccessorMapping = CollectionUtils.createHashMap(250);
		accessorMapping = CollectionUtils.createHashMap(50);
		for (DefinitionAccessor accessor : accessors) {
			// map all supported classes to the same accessor for faster lookup
			Set<Class<?>> supportedObjects = accessor.getSupportedObjects();
			for (Class<?> supportedObjectType : supportedObjects) {
				if (accessorMapping.containsKey(supportedObjectType)) {
					throw new EmfConfigurationException("Ambiguous definition accessor: "
							+ supportedObjectType + " already defined by "
							+ accessorMapping.get(supportedObjectType).getClass());
				}
				if (trace) {
					LOGGER.trace("Registering " + supportedObjectType + " to "
							+ accessor.getClass());
				}
				accessorMapping.put(supportedObjectType, accessor);
			}
		}

		if (!cacheContext.containsCache(TYPE_DEFINITION_CACHE)) {
			cacheContext.createCache(TYPE_DEFINITION_CACHE, new TypeDefinitionLookup());
		}
		if (!cacheContext.containsCache(TYPE_DEFINITION_URI_CACHE)) {
			cacheContext.createCache(TYPE_DEFINITION_URI_CACHE, new TypeDefinitionUriLookup());
		}
		if (!cacheContext.containsCache(MAX_REVISIONS_CACHE)) {
			cacheContext.createCache(MAX_REVISIONS_CACHE, new MaxRevisionsLookup());
		}
		if (!cacheContext.containsCache(PROTOTYPE_CACHE)) {
			cacheContext.createCache(PROTOTYPE_CACHE, new PrototypeDefinitionLookup());
		}
	}

	/**
	 * Getter method for typeDefinitionCache.
	 *
	 * @return the typeDefinitionCache
	 */
	private EntityLookupCache<String, DataTypeDefinition, String> getTypeDefinitionCache() {
		return cacheContext.getCache(TYPE_DEFINITION_CACHE);
	}

	/**
	 * Getter method for typeDefinitionCache.
	 *
	 * @return the typeDefinitionCache
	 */
	private EntityLookupCache<String, String, Serializable> getTypeDefinitionUriCache() {
		return cacheContext.getCache(TYPE_DEFINITION_URI_CACHE);
	}

	/**
	 * Gets the prototype cache.
	 *
	 * @return the prototype cache
	 */
	private EntityLookupCache<Long, PrototypeDefinition, Quad<String, String, Boolean, Long>> getPrototypeCache() {
		return cacheContext.getCache(PROTOTYPE_CACHE);
	}

	/**
	 * Gets the max revisions cache.
	 *
	 * @return the max revisions cache
	 */
	@SuppressWarnings("rawtypes")
	private EntityLookupCache<Pair<Class, String>, List<DefinitionModel>, Serializable> getMaxRevisionsCache() {
		return cacheContext.getCache(MAX_REVISIONS_CACHE);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends DefinitionModel> List<E> getAllDefinitions(Class<E> ref) {
		String currentContainer = getCurrentContainer();
		if (StringUtils.isNullOrEmpty(currentContainer)) {
			return CollectionUtils.emptyList();
		}
		Pair<Pair<Class, String>, List<DefinitionModel>> pair = getMaxRevisionsCache().getByKey(
				new Pair<Class, String>(ref, currentContainer));
		List<DefinitionModel> list = getCacheValue(pair);
		// used linked list: later in the application the returned list is
		// filtered - elements are removed from it so we use faster list for removal
		List<E> result = new LinkedList<>();
		if (list != null) {
			for (DefinitionModel topLevelDefinition : list) {
				result.add((E) topLevelDefinition);
			}
		}
		return result;
	}

	/**
	 * Gets the current container.
	 *
	 * @return the current container
	 */
	private String getCurrentContainer() {
		return SecurityContextManager.getCurrentContainer(authenticationServiceInstance);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends DefinitionModel> E getDefinition(Class<E> ref, String defId) {
		return getDefinitionAccessor(ref).getDefinition(getCurrentContainer(), defId);
	}

	/**
	 * Gets the definition accessor.
	 *
	 * @param <E>
	 *            the element type
	 * @param ref
	 *            the ref
	 * @return the definition accessor
	 */
	private <E extends DefinitionModel> DefinitionAccessor getDefinitionAccessor(Class<E> ref) {
		DefinitionAccessor accessor = accessorMapping.get(ref);
		if (accessor == null) {
			throw new EmfConfigurationException("The requested definition type " + ref
					+ " is not supproted!");
		}
		return accessor;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends DefinitionModel> E getDefinition(Class<E> ref, String defId, Long version) {
		return getDefinitionAccessor(ref).getDefinition(getCurrentContainer(), defId, version);
	}

	@Override
	public <E extends DefinitionModel> List<E> getDefinitionVersions(Class<E> ref, String defId) {
		return getDefinitionAccessor(ref).getDefinitionVersions(getCurrentContainer(), defId);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public PropertyDefinition getProperty(String propertyName, Long revision,
			PathElement pathElement) {
		String container = null;
		if (!RuntimeConfiguration
				.isConfigurationSet(RuntimeConfigurationProperties.DO_NOT_USE_CONTAINER_FILTERING)) {
			container = getCurrentContainer();
		}
		return getPropertyInternal(propertyName, revision, pathElement, container);
	}

	@Override
	public PrototypeDefinition getPrototype(String propertyName, Long revision,
			PathElement pathElement) {
		return getPropertyInternal(propertyName, revision, pathElement, getCurrentContainer());
	}

	/**
	 * Gets the property internal.
	 *
	 * @param propertyName
	 *            the property name
	 * @param revision
	 *            the revision
	 * @param pathElement
	 *            the path element
	 * @param container
	 *            the container
	 * @return the property internal
	 */
	private PropertyDefinition getPropertyInternal(String propertyName, Long revision,
			PathElement pathElement, String container) {
		if (trace) {
			LOGGER.trace("Searching for property (" + propertyName + ", " + revision + ", "
					+ PathHelper.getPath(pathElement) + ")");
		}
		DefinitionModel model = getRootModel(pathElement, revision, container);
		if (model == null) {
			LOGGER.warn("Invalid path. No model found for " + PathHelper.getPath(pathElement));
			return null;
		}
		PropertyDefinition propertyDefinition = PathHelper.findProperty(model, pathElement,
				propertyName);
		if (propertyDefinition != null) {
			return injectLabelProvider(propertyDefinition);
		}
		if (trace) {
			LOGGER.trace("Property was NOT found!");
		}
		return null;
	}

	/**
	 * Gets the root model by the given path. The model is searched in workflow and case definitions
	 * caches.
	 *
	 * @param pathElement
	 *            the path element
	 * @param revision
	 *            the revision
	 * @param container
	 *            the container
	 * @return the root model if found or <code>null</code> if not.
	 */
	@SuppressWarnings("rawtypes")
	private DefinitionModel getRootModel(PathElement pathElement, Long revision, String container) {
		String rootPath = PathHelper.getRootPath(pathElement);
		if (StringUtils.isNullOrEmpty(rootPath)) {
			return null;
		}
		if (EqualsHelper.nullSafeEquals(pathElement, DEFAULT_PATH_ELEMENT)) {
			EntityLookupCache<Pair<Class, String>, List<DefinitionModel>, Serializable> maxRevisionsCache = getMaxRevisionsCache();
			Pair<Pair<Class, String>, List<DefinitionModel>> pair = maxRevisionsCache
					.getByKey(new Pair<Class, String>(BaseDefinition.class,
							SecurityContextManager.NO_CONTAINER));
			List<DefinitionModel> list = getCacheValue(pair);
			if ((list != null) && !list.isEmpty()) {
				DefinitionModel model = list.get(0);
				if (model instanceof DefinitionEntry) {
					return ((DefinitionEntry) model).getTarget();
				}
			}
		}
		// first we try in the mapping for the supported class
		DefinitionAccessor definitionAccessor = accessorMapping.get(pathElement.getClass());
		if (definitionAccessor != null) {
			DefinitionModel model = definitionAccessor.getDefinition(container, rootPath, revision);
			if (model != null) {
				return model;
			}
		} else if (trace) {
			LOGGER.trace("No explicit accessor found for " + pathElement.getClass());
		}

		DefinitionModel model = null;

		// first try to get it from the cached mapping
		definitionAccessor = definitionTypeToAccessorMapping.get(rootPath);
		if (definitionAccessor != null) {
			model = definitionAccessor.getDefinition(container, rootPath, revision);
			if (model != null) {
				return model;
			}
		}
		for (DefinitionAccessor accessor : accessorMapping.values()) {
			model = accessor.getDefinition(container, rootPath, revision);
			if (model != null) {
				// cache for future use
				definitionTypeToAccessorMapping.put(rootPath, accessor);
				break;
			}
		}

		// as last resort will try to fetch the model from the parent if any
		if (model == null) {
			// if not found then we have some proxy that we have to search for
			// if element instance of owned model then we will try to return the parent instead of
			// the current node
			if (pathElement instanceof OwnedModel) {
				InstanceReference reference = ((OwnedModel) pathElement).getOwningReference();
				if ((reference != null) && (reference.getReferenceType() != null)) {
					Class parentModelClass = reference.getReferenceType().getJavaClass();
					definitionAccessor = accessorMapping.get(parentModelClass);
					if (definitionAccessor != null) {
						model = definitionAccessor.getDefinition(container, rootPath, revision);
						if (model != null) {
							return model;
						}
					}
				}
			}
		}
		return model;
	}

	/**
	 * Inject label provider.
	 *
	 * @param <E>
	 *            the element type
	 * @param displayable
	 *            the definition impl
	 * @return the property definition
	 */
	private <E extends Displayable> E injectLabelProvider(E displayable) {
		if ((displayable != null)
				&& ((displayable.getLabelId() != null) || (displayable.getTooltipId() != null))) {
			displayable.setLabelProvider(labelProvider);
		}
		return displayable;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Long getPropertyId(String propertyName, Long revision, PathElement pathElement,
			Serializable value) {
		String container = getCurrentContainer();
		PropertyDefinition propertyDefinition = getPropertyInternal(propertyName, revision,
				pathElement, container);
		if (propertyDefinition != null) {
			return propertyDefinition.getPrototypeId();
		}
		// if we does not have a value we cannot continue due to the fact we cannot determine the
		// type of the fields to look for
		if (value == null) {
			return null;
		}
		DataTypeDefinition detectType = detectType(value);
		if (detectType == null) {
			// we cannot continue if the type is unknown - most likely an empty list
			return null;
		}
		// as last resort we will try to determine the field prototype
		EntityLookupCache<Long, PrototypeDefinition, Quad<String, String, Boolean, Long>> prototypeCache = getPrototypeCache();
		PrototypeDefinition prototype = new PrototypeDefinitionImpl();
		prototype.setIdentifier(propertyName);
		prototype.setContainer(container);
		prototype.setDataType(detectType);
		prototype.setMultiValued(value instanceof Collection);
		Pair<Long, PrototypeDefinition> pair2 = prototypeCache.getByValue(prototype);
		if (pair2 != null) {
			return pair2.getFirst();
		}
		return null;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public String getPropertyById(Long propertyId) {
		if (propertyId == null) {
			return null;
		}
		Pair<Long, PrototypeDefinition> pair = getPrototypeCache().getByKey(propertyId);
		if (pair == null) {
			return null;
		}
		return pair.getSecond().getIdentifier();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public PrototypeDefinition getProperty(Long propertyId) {
		if (propertyId == null) {
			return null;
		}
		Pair<Long, PrototypeDefinition> pair = getPrototypeCache().getByKey(propertyId);
		return getCacheValue(pair);
	}

	@Override
	@SuppressWarnings("unchecked")
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public PropertyDefinition savePropertyIfChanged(PropertyDefinition newProperty,
			PropertyDefinition oldProperty) {
		// if we have old property we compare the two of them and copy the id if needed
		if ((oldProperty != null) && (oldProperty.getPrototypeId() != null)) {
			WritablePropertyDefinition newProp = (WritablePropertyDefinition) newProperty;
			WritablePropertyDefinition oldProp = (WritablePropertyDefinition) oldProperty;
			if (newProperty instanceof GenericProxy) {
				newProp = ((GenericProxy<WritablePropertyDefinition>) newProperty).getTarget();
			}
			if (oldProperty instanceof GenericProxy) {
				oldProp = ((GenericProxy<WritablePropertyDefinition>) oldProperty).getTarget();
			}

			if (hashCalculator.computeHash(newProp).equals(hashCalculator.computeHash(oldProp))) {
				newProp.setPrototypeId(oldProp.getPrototypeId());
				// could call refresh..
				return newProperty;
			}
		}
		if (newProperty instanceof GenericProxy) {
			// fill default container
			if (StringUtils.isNullOrEmpty(newProperty.getContainer())) {
				((WritablePropertyDefinition) newProperty)
						.setContainer(SecurityContextManager.NO_CONTAINER);
			}
			Object target = ((GenericProxy<?>) newProperty).getTarget();
			if (target instanceof WritablePropertyDefinition) {
				WritablePropertyDefinition definition = (WritablePropertyDefinition) target;
				// if the field does not have computed hash then we will compute it and set it
				if (definition.getHash() == null) {
					definition.setHash(hashCalculator.computeHash(definition));
				}
				// fill prototype Id
				createOrUpdatePrototypeDefinition(definition);
			}
		}
		// if not a proxy then save it
		if (!(newProperty instanceof PropertyDefinitionProxy)
				&& (newProperty instanceof WritablePropertyDefinition)
				&& (newProperty.getPrototypeId() == null)) {
			createOrUpdatePrototypeDefinition((WritablePropertyDefinition) newProperty);
			return newProperty;
		}
		return newProperty;
	}

	/**
	 * Creates the or update prototype definition.
	 *
	 * @param propertyDefinition
	 *            the property definition
	 */
	private void createOrUpdatePrototypeDefinition(WritablePropertyDefinition propertyDefinition) {
		if (propertyDefinition.getPrototypeId() != null) {
			return;
		}
		EntityLookupCache<Long, PrototypeDefinition, Quad<String, String, Boolean, Long>> prototypeCache = getPrototypeCache();

		PrototypeDefinition prototype = new PrototypeDefinitionImpl();
		prototype.setIdentifier(propertyDefinition.getName());
		prototype.setContainer(propertyDefinition.getContainer());
		prototype.setDataType(propertyDefinition.getDataType());
		prototype.setMultiValued(propertyDefinition.isMultiValued());
		Pair<Long, PrototypeDefinition> pair = prototypeCache.getOrCreateByValue(prototype);
		if (pair != null) {
			propertyDefinition.setPrototypeId(pair.getFirst());
		} else {
			LOGGER.error("Failed to create prototype definition.");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public DataTypeDefinition getDataTypeDefinition(String name) {
		if (StringUtils.isNullOrEmpty(name)) {
			return null;
		}
		String localName = name;
		EntityLookupCache<String, DataTypeDefinition, String> typeDefinitionCache = getTypeDefinitionCache();
		Pair<String, DataTypeDefinition> pair = null;
		// if the given argument is class name we could probably find it by it
		if (localName.indexOf(":", 1) > 0) {
			Pair<String, String> byKey = getTypeDefinitionUriCache().getByKey(localName);
			if (byKey != null) {
				localName = byKey.getSecond();
			}
		} else if (localName.indexOf(".", 1) > 0) {
			// probably we can cache the data type instance not to create it every time
			DataType dataType = new DataType();
			if (Date.class.toString().equals(localName)) {
				dataType.setName(DataTypeDefinition.DATETIME);
			}
			dataType.setJavaClassName(localName);
			pair = typeDefinitionCache.getByValue(dataType);
		}
		if (pair == null) {
			pair = typeDefinitionCache.getByKey(localName);
		}
		return getCacheValue(pair);
	}

	/**
	 * Initialize base property definitions.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void initializeBasePropertyDefinitions() {
		EntityLookupCache<Pair<Class, String>, List<DefinitionModel>, Serializable> maxRevisionsCache = getMaxRevisionsCache();
		Pair<Class, String> key = new Pair<Class, String>(BaseDefinition.class,
				SecurityContextManager.NO_CONTAINER);
		Pair<Pair<Class, String>, List<DefinitionModel>> pair = maxRevisionsCache.getByKey(key);
		// definition exists we a done
		if ((pair != null) && (getCacheValue(pair) != null) && !getCacheValue(pair).isEmpty()) {
			if (trace) {
				LOGGER.trace("Base definition was initialized before. Nothing to do.");
			}
			return;
		}
		// create new empty definition
		BaseDefinition<BaseDefinition<?>> definition = new BaseDefinition<>();
		definition.setIdentifier(BASE_DEFINITION_SELECTOR);

		DefinitionEntry entry = new DefinitionEntry();
		entry.setContainer(SecurityContextManager.NO_CONTAINER);
		entry.setAbstract(Boolean.FALSE);
		entry.setDmsId(null);
		entry.setHash(-1);
		entry.setRevision(0L);
		entry.setIdentifier(BASE_DEFINITION_SELECTOR);
		entry.setTargetType(getDataTypeDefinition(BaseDefinition.class));
		entry.setTarget(definition);

		DefinitionEntry baseDefinition = dbDao.saveOrUpdate(entry);

		maxRevisionsCache.setValue(key,
				new ArrayList<DefinitionModel>(Arrays.asList(baseDefinition)));
	}

	/**
	 * Gets the data type definition for the given class.
	 *
	 * @param clazz
	 *            the class to get the data type definition
	 * @return the data type definition
	 */
	protected DataTypeDefinition getDataTypeDefinition(Class<?> clazz) {
		if (clazz == null) {
			throw new EmfRuntimeException("Cannot fetch type for null class");
		}
		return getDataTypeDefinition(clazz.getName());
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public PrototypeDefinition getDefinitionByValue(String propertyName, Serializable serializable) {
		DataTypeDefinition detectType = detectType(serializable);
		if (detectType == null) {
			return null;
		}
		PrototypeDefinition prototypeDefinition = createDefaultPropertyDefinitionIfNotExist(
				propertyName, detectType.getName(), serializable instanceof Collection);
		if (!EqualsHelper.nullSafeEquals(prototypeDefinition.getDataType().getName(),
				detectType.getName(), false)) {
			try {
				// we will check if is convertible at all
				if (typeConverter.convert(prototypeDefinition.getDataType(), serializable) != null) {
					if (trace) {
						LOGGER.trace("Returned prototype with type "
								+ prototypeDefinition.getDataType().getName() + " for property "
								+ propertyName + " and type " + detectType.getName());
					}
					return prototypeDefinition;
				}
			} catch (RuntimeException e) {
				if (debug) {
					LOGGER.debug("Failed to convert the " + serializable + " of type "
							+ detectType.getName() + " to "
							+ prototypeDefinition.getDataType().getName() + "due to: "
							+ e.getMessage());
				} else if (trace) {
					LOGGER.trace(
							"Failed to convert the " + serializable + " of type "
									+ detectType.getName() + " to "
									+ prototypeDefinition.getDataType().getName() + "due to: "
									+ e.getMessage(), e);
				}
			}
			LOGGER.error("Trying to persist dynamic data with type " + detectType.getName()
					+ " but there is a property with the same name "
					+ prototypeDefinition.getIdentifier() + " but with type "
					+ prototypeDefinition.getDataType().getName() + ". Data will not be persisted!");
			return null;
		}
		return prototypeDefinition;
	}

	/**
	 * Creates the default property definition if not exist.
	 *
	 * @param name
	 *            the name
	 * @param type
	 *            the type
	 * @param multyValued
	 *            if the field is multi valued
	 * @return the property definition
	 */
	@SuppressWarnings("rawtypes")
	private PrototypeDefinition createDefaultPropertyDefinitionIfNotExist(String name, String type,
			boolean multyValued) {
		String container = getCurrentContainer();
		PropertyDefinition property = getPropertyInternal(name, 0L, DEFAULT_PATH_ELEMENT, container);
		boolean toSave = false;
		if (property == null) {
			property = new PropertyDefinitionProxy();
			WritablePropertyDefinition definition = new FieldDefinitionImpl();
			definition.setType(type);
			definition.setDataType(getDataTypeDefinition(type));
			definition.setDefaultProperties();
			PropertyDefinitionProxy proxy = (PropertyDefinitionProxy) property;
			proxy.setTarget(definition);
			proxy.setParentPath(DEFAULT_PATH_ELEMENT.getPath());
			proxy.setRevision(0L);
			proxy.setName(name);
			proxy.setContainer(container);
			definition.setMultiValued(multyValued);

			toSave = true;
		} else if ((property.isMultiValued() == false) && (multyValued == true)) {
			property.setMultiValued(true);
			toSave = true;
		}

		if (toSave) {
			PropertyDefinition propertyDefinition = savePropertyIfChanged(property, null);
			if (propertyDefinition.getPrototypeId() != null) {
				EntityLookupCache<Pair<Class, String>, List<DefinitionModel>, Serializable> maxRevisionsCache = getMaxRevisionsCache();
				Pair<Class, String> key = new Pair<Class, String>(BaseDefinition.class,
						SecurityContextManager.NO_CONTAINER);
				Pair<Pair<Class, String>, List<DefinitionModel>> pair = maxRevisionsCache
						.getByKey(key);
				List<DefinitionModel> list = getCacheValue(pair);
				if ((list != null) && !list.isEmpty()) {
					DefinitionModel model = list.get(0);
					if (model instanceof DefinitionEntry) {
						DefinitionEntry entry = (DefinitionEntry) model;
						DefinitionModel target = entry.getTarget();
						target.getFields().add(propertyDefinition);
						// update the internal model and serialize back to array
						entry.setTarget(target);
						// save model to DB
						dbDao.saveOrUpdate(entry);
						// and update the stale cache
						maxRevisionsCache.setValue(key,
								new ArrayList<DefinitionModel>(Arrays.asList(entry)));
					}
				}
			}
		}

		return property;
	}

	/**
	 * Detects the argument type.
	 *
	 * @param serializable
	 *            the serializable
	 * @return the data type definition
	 */
	@SuppressWarnings("unchecked")
	protected DataTypeDefinition detectType(Serializable serializable) {
		if (serializable instanceof String) {
			return getDataTypeDefinition(DataTypeDefinition.TEXT);
		} else if (serializable instanceof Number) {
			if (serializable instanceof Long) {
				return getDataTypeDefinition(DataTypeDefinition.LONG);
			} else if (serializable instanceof Integer) {
				return getDataTypeDefinition(DataTypeDefinition.INT);
			} else if (serializable instanceof Double) {
				return getDataTypeDefinition(DataTypeDefinition.DOUBLE);
			} else if (serializable instanceof Float) {
				return getDataTypeDefinition(DataTypeDefinition.FLOAT);
			}
		} else if (serializable instanceof Boolean) {
			return getDataTypeDefinition(DataTypeDefinition.BOOLEAN);
		} else if (serializable instanceof CommonInstance) {
			return getDataTypeDefinition(DataTypeDefinition.INSTANCE);
		} else if (serializable instanceof Date) {
			return getDataTypeDefinition(DataTypeDefinition.DATETIME);
		} else if (serializable instanceof Collection) {
			Collection<Serializable> collection = (Collection<Serializable>) serializable;
			if (!collection.isEmpty()) {
				return detectType(collection.iterator().next());
			}
		} else if (serializable != null) {
			return getDataTypeDefinition(DataTypeDefinition.ANY);
		}
		LOGGER.warn("Not supported type for "
				+ (serializable != null ? serializable.getClass().getName() : "null")
				+ " class with value = " + serializable);
		return null;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public DataTypeDefinition saveDataTypeDefinition(DataTypeDefinition typeDefinition) {
		EntityLookupCache<String, DataTypeDefinition, String> typeCache = getTypeDefinitionCache();
		// clear the cache entry to force fetching of new entry !!!
		typeCache.removeByKey(typeDefinition.getName());
		typeCache.removeByValue(typeDefinition);

		Pair<String, DataTypeDefinition> pair = typeCache.getOrCreateByValue(typeDefinition);
		if (pair == null) {
			throw new CmfDatabaseException("Failed to persist type definition: " + typeDefinition);
		}
		DataTypeDefinition definition = pair.getSecond();
		// check for changes
		if (!hashCalculator.equalsByHash(definition, typeDefinition)) {
			typeDefinition.setId(definition.getId());
			definition = dbDao.saveOrUpdate(typeDefinition);
			typeCache.setValue(definition.getName(), typeDefinition);
		}
		// update the second cache
		for (String string : definition.getUries()) {
			getTypeDefinitionUriCache().setValue(string, definition.getName());
		}
		return pair.getSecond();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends DefinitionModel> boolean isDefinitionEquals(E definition1, E definition2) {
		TimeTracker tracker = null;
		if (trace) {
			hashCalculator.setStatisticsEnabled(true);
			tracker = new TimeTracker().begin();
		}
		int hashCode1 = getDefinitionAccessor(definition1.getClass()).computeHash(definition1);

		List<Pair<String, String>> statistics1 = hashCalculator.getStatistics(true);
		int hashCode2 = getDefinitionAccessor(definition2.getClass()).computeHash(definition2);
		List<Pair<String, String>> statistics2 = hashCalculator.getStatistics(true);
		boolean result = hashCode1 == hashCode2;
		if (trace) {
			hashCalculator.setStatisticsEnabled(false);
			LOGGER.trace("Compared definitions " + definition1.getIdentifier() + " vs "
					+ definition2.getIdentifier() + " with result " + result + ", compare took "
					+ tracker.stopInSeconds() + " s");

			if (!result) {
				StringBuilder builder = new StringBuilder(1000);
				builder.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				List<String> diff = EqualsHelper.diffLists(statistics1, statistics2);
				for (String string : diff) {
					builder.append('\n').append(string);
				}
				builder.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				LOGGER.trace("Definitions not equal! Report: \n" + builder);
			}
		}
		return result;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <E extends TopLevelDefinition> E saveDefinition(E definition) {
		DefinitionAccessor accessor = getDefinitionAccessor(definition.getClass());
		// clear all max revision so they later can reinitialize
		getMaxRevisionsCache().clear();
		// updates the standard typed both caches
		return accessor.saveDefinition(definition);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <E extends TopLevelDefinition> E saveTemplateDefinition(E definition) {
		DefinitionAccessor accessor = getDefinitionAccessor(definition.getClass());
		// updates the standard typed both caches
		return accessor.saveDefinition(definition);
	}

	/**
	 * Gets the cache value.
	 *
	 * @param <E>
	 *            the element type
	 * @param pair
	 *            the pair
	 * @return the cache value
	 */
	private <E> E getCacheValue(Pair<?, E> pair) {
		if (pair == null) {
			return null;
		}
		return pair.getSecond();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public DefinitionModel getInstanceDefinition(Instance instance) {
		if (instance == null) {
			throw new EmfRuntimeException("Instance is required argument!");
		}
		DefinitionAccessor accessor = accessorMapping.get(instance.getClass());
		if (accessor != null) {
			return accessor.getDefinition(instance);
		}
		LOGGER.warn("Trying to load definition for instance " + instance.getClass()
				+ " but there is not defined definition accessor! Nothing will be returned.");
		return null;
	}

	@Override
	public Map<String, Serializable> filterProperties(DefinitionModel model,
			Map<String, Serializable> properties, DisplayType displayType) {
		Map<String, Serializable> props = new LinkedHashMap<>();
		if ((displayType == DisplayType.EDITABLE) && (model != null) && (model.getFields() != null)) {
			// removed all non editable properties
			for (PropertyDefinition definition : model.getFields()) {
				if ((definition.getDisplayType() != DisplayType.EDITABLE)
						&& !definition.isMandatory()) {
					if (trace) {
						LOGGER.trace("Ignoring property " + definition.getName()
								+ " from the source map");
					}
					// the previous implementation removed the property
				} else {
					// if the we keep the property then we try to convert it
					if (properties.containsKey(definition.getName())) {
						props.put(
								definition.getName(),
								convertPropertyValue(definition,
										properties.get(definition.getName())));
					}
				}
			}
			return props;
		}
		// convert property values
		for (PropertyDefinition definition : model.getFields()) {
			if (properties.containsKey(definition.getName())) {
				props.put(definition.getName(),
						convertPropertyValue(definition, properties.get(definition.getName())));
			}
		}

		return props;
	}

	/**
	 * Convert property value.
	 *
	 * @param definition
	 *            the definition
	 * @param serializable
	 *            the serializable
	 * @return the serializable
	 */
	private Serializable convertPropertyValue(PropertyDefinition definition,
			Serializable serializable) {
		if (serializable == null) {
			return null;
		}
		Serializable converted = evaluatorManager.ruleConvertTo(definition, serializable);
		if (trace) {
			LOGGER.trace("Converted property value " + definition.getName() + " from "
					+ serializable + " to " + converted);
		}
		return converted;
	}

	@Override
	public List<Pair<String, String>> removeDefinitionsWithoutInstances(
			Set<String> definitionsToCheck) {
		List<Pair<String, String>> removed = new LinkedList<>();
		Set<String> allActiveDefinitions = new LinkedHashSet<>(200);
		for (DefinitionAccessor accessor : accessors) {
			Set<String> activeDefinitions = accessor.getActiveDefinitions();
			if (!activeDefinitions.isEmpty()) {
				HashSet<String> notActive = new HashSet<>(definitionsToCheck);
				notActive.removeAll(allActiveDefinitions);
			}
			allActiveDefinitions.addAll(activeDefinitions);
			for (String definitionId : activeDefinitions) {
				if (accessor.removeDefinition(definitionId, -1)) {
					removed.add(DefinitionIdentityUtil.parseDefinitionId(definitionId));
				}
			}
		}
		return removed;
	}

	/**
	 * The Class TypeDefinitionLookup.
	 *
	 * @author BBonev
	 */
	class TypeDefinitionLookup extends
			EntityLookupCallbackDAOAdaptor<String, DataTypeDefinition, String> {

		@Override
		public String getValueKey(DataTypeDefinition value) {
			if (value == null) {
				return null;
			}
			if (EqualsHelper.nullSafeEquals(DataTypeDefinition.DATE, value.getName(), true)) {
				// we assume the date type does not have a secondary key as it is equal to the
				// datetime type so it will always be fetched from database
				return null;
			}
			return value.getJavaClassName();
		}

		@Override
		public Pair<String, DataTypeDefinition> findByValue(DataTypeDefinition value) {
			String valueKey = value.getJavaClassName();
			if (valueKey == null) {
				return null;
			}
			String name = value.getName();
			String query = EmfQueries.QUERY_TYPE_DEFINITION_BY_CLASS_KEY;
			List<Pair<String, Object>> args = new ArrayList<>(2);
			args.add(new Pair<String, Object>("javaClassName", valueKey));
			// if the name is provided also we will filter by it too
			if (StringUtils.isNotNullOrEmpty(name)) {
				query = EmfQueries.QUERY_TYPE_DEFINITION_BY_NAME_AND_CLASS_KEY;
				args.add(new Pair<String, Object>("name", name));
			}
			List<DataType> list = dbDao.fetchWithNamed(query, args);
			if (list.isEmpty()) {
				return null;
			}
			if (list.size() > 1) {
				// this should not happen or someone broke the DB on purpose
				throw new DictionaryException("More then one data type definition found");
			}
			DataType type = list.get(0);
			return new Pair<String, DataTypeDefinition>(type.getName(), type);
		}

		@Override
		public Pair<String, DataTypeDefinition> findByKey(String key) {
			if (key == null) {
				return null;
			}
			List<DataType> list = dbDao.fetchWithNamed(EmfQueries.QUERY_TYPE_DEFINITION_KEY,
					Arrays.asList(new Pair<String, Object>("name", key)));
			if (list.isEmpty()) {
				return null;
			}
			if (list.size() > 1) {
				// this should not happen or someone broke the DB on purpose
				throw new DictionaryException("More then one data type definition found");
			}
			DataType type = list.get(0);
			return new Pair<String, DataTypeDefinition>(key, type);
		}

		@Override
		public Pair<String, DataTypeDefinition> createValue(DataTypeDefinition value) {
			if (value == null) {
				return null;
			}

			DataTypeDefinition typeDefinition = (DataTypeDefinition) dbDao
					.saveOrUpdate((Entity<?>) value);

			return new Pair<>(typeDefinition.getName(), typeDefinition);
		}

	}

	/**
	 * Type lookup cache by type URI
	 *
	 * @author BBonev
	 */
	class TypeDefinitionUriLookup extends
			EntityLookupCallbackDAOAdaptor<String, String, Serializable> {

		@Override
		public Pair<String, String> findByKey(String key) {
			if (key == null) {
				return null;
			}
			List<DataType> list = dbDao.fetchWithNamed(EmfQueries.QUERY_TYPE_DEFINITION_BY_URI_KEY,
					Arrays.asList(new Pair<String, Object>("uri", "%" + key + "%")));
			if (list.isEmpty()) {
				return null;
			}
			if (list.size() > 1) {
				for (DataType dataType : list) {
					for (String string : dataType.getUries()) {
						if (string.equals(key)) {
							return new Pair<>(key, dataType.getName());
						}
					}
				}
				// this should not happen or someone broke the DB on purpose
				// throw new DictionaryException("More then one data type definition found");
				LOGGER.warn("More then one data type definition found for URI=" + key + "\n" + list);
			}
			String type = list.get(0).getName();
			return new Pair<>(key, type);
		}

		@Override
		public Pair<String, String> createValue(String value) {
			throw new UnsupportedOperationException("This cache cannot create values.");
		}

	}

	/**
	 * Prototype definition lookup class.
	 *
	 * @author BBonev
	 */
	class PrototypeDefinitionLookup
			extends
			EntityLookupCallbackDAOAdaptor<Long, PrototypeDefinition, Quad<String, String, Boolean, Long>> {

		@Override
		public Quad<String, String, Boolean, Long> getValueKey(PrototypeDefinition value) {
			if (value == null) {
				return null;
			}
			return new Quad<>(value.getIdentifier(), value.getContainer(), value.isMultiValued(),
					value.getDataType().getId());
		}

		@Override
		public Pair<Long, PrototypeDefinition> findByValue(PrototypeDefinition value) {
			Quad<String, String, Boolean, Long> pair = getValueKey(value);
			if (pair == null) {
				return null;
			}

			List<Pair<String, Object>> args = new ArrayList<>(2);
			args.add(new Pair<String, Object>("name", pair.getFirst()));
			args.add(new Pair<String, Object>("container", pair.getSecond()));
			args.add(new Pair<String, Object>("multiValued", pair.getThird()));
			args.add(new Pair<String, Object>("type", pair.getForth()));
			List<Object> list = dbDao.fetchWithNamed(EmfQueries.QUERY_PROTO_TYPE_DEFINITION_KEY,
					args);
			if (list.isEmpty()) {
				return null;
			}
			if (list.size() > 1) {
				LOGGER.warn("Found mode then one proto type definition for key " + pair);
			}
			PrototypeDefinition definition = (PrototypeDefinition) list.get(0);
			return new Pair<>(definition.getId(), definition);
		}

		@Override
		public Pair<Long, PrototypeDefinition> findByKey(Long key) {
			try {
				PrototypeDefinitionImpl definitionImpl = dbDao.find(PrototypeDefinitionImpl.class,
						key);
				return new Pair<Long, PrototypeDefinition>(key, definitionImpl);
			} catch (CmfDatabaseException e) {
				if (debug) {
					LOGGER.debug("Prototype property with " + key + " not found!");
				}
				return null;
			}
		}

		@Override
		public Pair<Long, PrototypeDefinition> createValue(PrototypeDefinition value) {
			PrototypeDefinition update = dbDao.saveOrUpdateInNewTx(value);
			return new Pair<>(update.getId(), update);
		}

	}

	/**
	 * Fetches all max revisions for the given type.
	 *
	 * @author BBonev
	 */
	@SuppressWarnings("rawtypes")
	class MaxRevisionsLookup
			extends
			EntityLookupCallbackDAOAdaptor<Pair<Class, String>, List<DefinitionModel>, Serializable> {

		@Override
		@SuppressWarnings({ "unchecked" })
		public Pair<Pair<Class, String>, List<DefinitionModel>> findByKey(Pair<Class, String> key) {
			List<DefinitionModel> result = null;
			DefinitionAccessor accessor = getDefinitionAccessor(key.getFirst());
			result = accessor.getAllDefinitions(key.getSecond());

			if (result == null) {
				return null;
			}
			return new Pair<>(key, result);
		}

		@Override
		public Pair<Pair<Class, String>, List<DefinitionModel>> createValue(
				List<DefinitionModel> value) {
			throw new UnsupportedOperationException("Max revison cannot be created");
		}
	}
}
