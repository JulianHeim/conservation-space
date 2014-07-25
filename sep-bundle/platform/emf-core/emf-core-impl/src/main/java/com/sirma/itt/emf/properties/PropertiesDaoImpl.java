package com.sirma.itt.emf.properties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.cache.CacheConfiguration;
import com.sirma.itt.emf.cache.Eviction;
import com.sirma.itt.emf.cache.Expiration;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache.EntityLookupCallbackDAOAdaptor;
import com.sirma.itt.emf.cache.lookup.EntityLookupCacheContext;
import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.db.DbDao;
import com.sirma.itt.emf.db.EmfQueries;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.domain.model.PathElement;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.instance.PropertiesUtil;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.properties.dao.RelationalNonPersistentPropertiesExtension;
import com.sirma.itt.emf.properties.dao.PropertiesDao;
import com.sirma.itt.emf.properties.dao.PropertyModelCallback;
import com.sirma.itt.emf.properties.entity.EntityId;
import com.sirma.itt.emf.properties.entity.NodePropertyHelper;
import com.sirma.itt.emf.properties.entity.PropertyEntity;
import com.sirma.itt.emf.properties.entity.PropertyKey;
import com.sirma.itt.emf.properties.entity.PropertyValue;
import com.sirma.itt.emf.properties.event.PropertiesChangeEvent;
import com.sirma.itt.emf.properties.model.PropertyModel;
import com.sirma.itt.emf.properties.model.PropertyModelKey;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.Documentation;
import com.sirma.itt.emf.util.EqualsHelper;
import com.sirma.itt.emf.util.EqualsHelper.MapValueComparison;

/**
 * Default implementation of properties DAO.
 *
 * @author BBonev
 */
@Stateless
public class PropertiesDaoImpl implements PropertiesDao, Serializable {

	private static final int PROPERTY_PRINT_LENGHT = 150;

	/** The Constant PROPERTY_ENTITY_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 50000), expiration = @Expiration(maxIdle = 1800000, interval = 60000), doc = @Documentation(""
			+ "Cache used to properties for the loaded active instances. The cache does NOT handle instance that are stored only in a semantic database. "
			+ "The cache SHOULD not be transactional due to invalid state when cascading properties save/load."
			+ "<br>Minimal value expression: (caseCache + documentCache + sectionCache + projectCache + averageNonStartedScheduleEntries + workflowTaskCache + standaloneTaskCache + workflowCache) * 1.2"))
	private static final String PROPERTY_ENTITY_CACHE = "PROPERTY_ENTITY_CACHE";

	private static final long serialVersionUID = -7760421274652309552L;

	/** The Constant FORBIDDEN_PROPERTIES. */
	private Set<String> forbiddenProperties;

	/** The node property helper. */
	@Inject
	private NodePropertyHelper nodePropertyHelper;

	/** The dictionary service. */
	@Inject
	private DictionaryService dictionaryService;

	/** The db dao. */
	@Inject
	private DbDao dbDao;

	/** The logger. */
	@Inject
	private transient Logger logger;

	/** The is debug enabled. */
	private boolean isDebugEnabled;
	/** The is trace enabled. */
	private boolean isTraceEnabled;

	/** The cache context. */
	@Inject
	private EntityLookupCacheContext cacheContext;

	/** The non persistent properties. */
	@Inject
	@ExtensionPoint(value = RelationalNonPersistentPropertiesExtension.TARGET_NAME)
	private Iterable<RelationalNonPersistentPropertiesExtension> nonPersistentProperties;

	/** The event service. */
	@Inject
	private EventService eventService;

	/**
	 * Inits the cache context.
	 */
	@PostConstruct
	public void init() {
		if (!cacheContext.containsCache(PROPERTY_ENTITY_CACHE)) {
			cacheContext.createCache(PROPERTY_ENTITY_CACHE, new PropertiesLookupCallback());
		}

		forbiddenProperties = new LinkedHashSet<String>(50);
		for (RelationalNonPersistentPropertiesExtension extension : nonPersistentProperties) {
			forbiddenProperties.addAll(extension.getNonPersistentProperties());
		}

		isDebugEnabled = logger.isDebugEnabled();
		isTraceEnabled = logger.isTraceEnabled();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public <E extends PropertyModel> void saveProperties(E model, PropertyModelCallback<E> callback) {
		saveProperties(model, false, callback);
	}

	@Override
	public <E extends PropertyModel> void saveProperties(E model, boolean addOnly,
			PropertyModelCallback<E> callback) {
		Map<PropertyModelKey, PropertyModel> map = callback.getModel(model);
		for (Entry<PropertyModelKey, PropertyModel> entry : map.entrySet()) {
			setPropertiesImpl(entry.getKey(), entry.getValue().getProperties(), addOnly,
					entry.getValue());
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends PropertyModel> void loadProperties(E model, PropertyModelCallback<E> callback) {
		loadProperties(Arrays.asList(model), callback);
	}

	@Override
	@SuppressWarnings("unchecked")
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends PropertyModel> void loadProperties(List<E> models,
			PropertyModelCallback<E> callback) {
		if (models.isEmpty()) {
			return;
		}
		Map<PropertyModelKey, PropertyModel> loadedModels = new LinkedHashMap<PropertyModelKey, PropertyModel>();
		for (PropertyModel propertyModel : models) {
			loadedModels.putAll(callback.getModel((E) propertyModel));
		}

		callback.updateModel(loadedModels, loadPropertiesInternal(loadedModels.keySet()));
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Map<String, Serializable> getEntityProperties(Entity entity, Long revision,
			PathElement path, PropertyModelCallback<PropertyModel> callback) {
		PropertyModelKey entityId = callback.createModelKey(entity, revision);
		if (entityId == null) {
			logger.warn("No properties support for " + entity.getClass());
			return Collections.emptyMap();
		}
		entityId.setPathElement(path);
		return getNodePropertiesCached(entityId);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeProperties(Entity entity, Long revision, PathElement path,
			PropertyModelCallback<PropertyModel> callback) {
		PropertyModelKey entityId = callback.createModelKey(entity, revision);
		if (entityId == null) {
			logger.warn("No properties support for " + entity.getClass());
			return;
		}
		entityId.setPathElement(path);

		getPropertiesCache().deleteByKey(entityId);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void saveProperties(Entity entity, Long revision, PathElement path,
			Map<String, Serializable> properties, PropertyModelCallback<PropertyModel> callback) {
		saveProperties(entity, revision, path, properties, false, callback);
	}

	@Override
	public void saveProperties(Entity entity, Long revision, PathElement path,
			Map<String, Serializable> properties, boolean addOnly,
			PropertyModelCallback<PropertyModel> callback) {
		PropertyModelKey key = callback.createModelKey(entity, revision);
		if (key == null) {
			logger.warn("No properties support for " + entity.getClass());
			return;
		}
		key.setPathElement(path);

		PropertyModel model = null;
		if (entity instanceof PropertyModel) {
			model = (PropertyModel) entity;
		}
		setPropertiesImpl(key, properties, addOnly, model);
	}

	/**
	 * Does differencing to add and/or remove properties. Internally, the existing properties will
	 * be retrieved and a difference performed to work out which properties need to be created,
	 * updated or deleted. It is only necessary to pass in old and new values for <i>changes</i>
	 * i.e. when setting a single property, it is only necessary to pass that property's value in
	 * the <b>old</b> and </b>new</b> maps; this improves execution speed significantly - although
	 * it has no effect on the number of resulting DB operations.
	 * <p/>
	 * Note: The cached properties are not updated
	 *
	 * @param entityId
	 *            the node ID
	 * @param newProps
	 *            the properties to add or update
	 * @param isAddOnly
	 *            <tt>true</tt> if the new properties are just an update or <tt>false</tt> if the
	 *            properties are a complete set
	 * @param model
	 *            the target model that holds the provided properties
	 * @return Returns <tt>true</tt> if any properties were changed
	 */
	private boolean setPropertiesImpl(PropertyModelKey entityId,
			Map<String, Serializable> newProps, boolean isAddOnly, PropertyModel model) {
		if (isAddOnly && (newProps.size() == 0)) {
			// No point adding nothing
			return false;
		}

		// Copy inbound values
		newProps = new LinkedHashMap<String, Serializable>(newProps);

		// Remove properties that should not be updated from the user
		newProps.keySet().removeAll(forbiddenProperties);

		// Load the current properties.
		// This means that we have to go to the DB during cold-write operations,
		// but usually a write occurs after a node has been fetched of viewed in
		// some way by the client code. Loading the existing properties has the
		// advantage that the differencing code can eliminate unnecessary writes
		// completely.
		Map<String, Serializable> oldPropsCached = getNodePropertiesCached(entityId);
		// Keep pristine for caching
		Map<String, Serializable> oldProps = new LinkedHashMap<String, Serializable>(oldPropsCached);
		// If we're adding, remove current properties that are not of interest
		if (isAddOnly) {
			oldProps.keySet().retainAll(newProps.keySet());
		}

		// We need to convert the new properties to our internally-used format,
		// which is compatible with model i.e. people may have passed in data
		// which needs to be converted to a model-compliant format. We do this
		// before comparisons to avoid false negatives.
		Map<PropertyKey, PropertyValue> newPropsRaw = nodePropertyHelper
				.convertToPersistentProperties(newProps, entityId.getRevision(),
						entityId.getPathElement());
		newProps = nodePropertyHelper.convertToPublicProperties(newPropsRaw,
				entityId.getRevision(), entityId.getPathElement());
		// Now find out what's changed
		Map<String, MapValueComparison> diff = EqualsHelper.getMapComparison(oldProps, newProps);
		// Keep track of properties to delete and add
		Map<String, Serializable> propsToDelete = new LinkedHashMap<String, Serializable>(
				oldProps.size() * 2);
		Map<String, Serializable> propsToAdd = new LinkedHashMap<String, Serializable>(
				newProps.size() * 2);
		for (Map.Entry<String, MapValueComparison> entry : diff.entrySet()) {
			String qname = entry.getKey();

			switch (entry.getValue()) {
				case EQUAL:
					// Ignore
					break;
				case LEFT_ONLY:
					// Not in the new properties
					propsToDelete.put(qname, oldProps.get(qname));
					break;
				case NOT_EQUAL:
					// Must remove from the LHS
					propsToDelete.put(qname, oldProps.get(qname));
					// Fall through to load up the RHS
				case RIGHT_ONLY:
					// We're adding this
					Serializable value = newProps.get(qname);
					propsToAdd.put(qname, value);
					break;
				default:
					throw new IllegalStateException("Unknown MapValueComparison: "
							+ entry.getValue());
			}
		}

		boolean updated = (propsToDelete.size() > 0) || (propsToAdd.size() > 0);

		// Touch to bring into current txn
		if (updated) {
			try {
				// Apply deletes
				Set<Long> propStringIdsToDelete = convertStringsToIds(propsToDelete,
						entityId.getRevision(), entityId.getPathElement());
				deleteNodeProperties(entityId, propStringIdsToDelete);
				// Now create the raw properties for adding
				newPropsRaw = nodePropertyHelper.convertToPersistentProperties(propsToAdd,
						entityId.getRevision(), entityId.getPathElement());
				insertNodeProperties(entityId, newPropsRaw);
			} catch (Exception e) {
				// Don't trust the properties cache for the node
				// propertiesCache.removeByKey(nodeId);
				// Focused error
				throw new EmfRuntimeException("Failed to write property deltas: \n"
						+ "  Node:          " + entityId + "\n" + "  Old:           " + oldProps
						+ "\n" + "  New:           " + newProps + "\n" + "  Diff:          " + diff
						+ "\n" + "  Delete Tried:  " + propsToDelete + "\n" + "  Add Tried:     "
						+ propsToAdd, e);
			}

			// Build the properties to cache based on whether this is an append
			// or replace
			Map<String, Serializable> propsToCache = null;
			if (isAddOnly) {
				// Combine the old and new properties
				propsToCache = oldPropsCached;
				propsToCache.putAll(propsToAdd);
			} else {
				// Replace old properties
				propsToCache = newProps;
				// Ensure correct types
				propsToCache.putAll(propsToAdd);
			}
			// Update cache
			setNodePropertiesCached(entityId, propsToCache);
			// XXX: this should be defined somewhere - EntityIdType.INSTANCE.getType()
		} else if (entityId.getBeanType() == 7) {
			setNodePropertiesCached(entityId, newProps);
		}
		// Touch to bring into current transaction
		if (updated) {
			Entity entity = null;
			if (model instanceof Entity) {
				entity = (Entity) model;
			}
			String operation = (String) RuntimeConfiguration
					.getConfiguration(RuntimeConfigurationProperties.CURRENT_OPERATION);
			// change event here
			eventService.fire(new PropertiesChangeEvent(entity, propsToAdd, propsToDelete,
					operation));
		}
		// Done
		if (isDebugEnabled && updated) {
			logger.debug("Modified node properties: " + entityId + "\n   Removed: "
					+ printUserFriendly(propsToDelete) + "\n   Added:   "
					+ printUserFriendly(propsToAdd));
		}
		return updated;
	}

	/**
	 * Prints the user friendly.
	 *
	 * @param map
	 *            the map
	 * @return the string
	 */
	private String printUserFriendly(Map<String, Serializable> map) {
		StringBuilder builder = new StringBuilder(1024);
		builder.append('{');
		for (Iterator<Entry<String, Serializable>> it = map.entrySet().iterator(); it.hasNext();) {
			Entry<String, Serializable> entry = it.next();
			builder.append(entry.getKey()).append("=");
			if (entry.getValue() == null) {
				builder.append("null");
			} else {
				String value = entry.getValue().toString();
				if (value.length() > PROPERTY_PRINT_LENGHT) {
					builder.append(value.substring(0, PROPERTY_PRINT_LENGHT)).append(" ... ")
							.append(value.length() - PROPERTY_PRINT_LENGHT).append(" more");
				} else {
					builder.append(value);
				}
			}
			if (it.hasNext()) {
				builder.append(", ");
			}
		}
		builder.append('}');
		return builder.toString();
	}

	/**
	 * Sets the node properties cached.
	 *
	 * @param entityId
	 *            the node id
	 * @param properties
	 *            the props to cache
	 */
	private void setNodePropertiesCached(PropertyModelKey entityId,
			Map<String, Serializable> properties) {
		getPropertiesCache().setValue(entityId,
				Collections.unmodifiableMap(PropertiesUtil.cloneProperties(properties)));
	}

	/**
	 * Insert node properties.
	 *
	 * @param entityId
	 *            the node id
	 * @param newPropsRaw
	 *            the new props raw
	 */
	private void insertNodeProperties(PropertyModelKey entityId,
			Map<PropertyKey, PropertyValue> newPropsRaw) {
		if (newPropsRaw.isEmpty()) {
			return;
		}

		for (Entry<PropertyKey, PropertyValue> entry : newPropsRaw.entrySet()) {
			PropertyEntity propertyEntity = new PropertyEntity();
			propertyEntity.setKey(entry.getKey());
			propertyEntity.setValue(entry.getValue());
			propertyEntity.setEntityId((EntityId) entityId);
			dbDao.saveOrUpdate(propertyEntity);
		}
	}

	/**
	 * Convert strings to ids.
	 *
	 * @param propsToDelete
	 *            the props to delete
	 * @param revision
	 *            the property revision
	 * @param pathElement
	 *            the path element
	 * @return the sets the
	 */
	private Set<Long> convertStringsToIds(Map<String, Serializable> propsToDelete, Long revision,
			PathElement pathElement) {
		Set<Long> result = new LinkedHashSet<Long>((int) (propsToDelete.size() * 1.2), 1f);
		for (Entry<String, Serializable> entry : propsToDelete.entrySet()) {
			Long propertyId = dictionaryService.getPropertyId(entry.getKey(), revision,
					pathElement, entry.getValue());
			if (propertyId != null) {
				result.add(propertyId);
			}
		}
		return result;
	}

	/**
	 * Delete node properties.
	 *
	 * @param entityId
	 *            the node id
	 * @param propStringIdsToDelete
	 *            the prop string ids to delete
	 */
	private void deleteNodeProperties(PropertyModelKey entityId, Set<Long> propStringIdsToDelete) {
		if (propStringIdsToDelete.isEmpty()) {
			return;
		}
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>(3);
		args.add(new Pair<String, Object>("id", propStringIdsToDelete));
		args.add(new Pair<String, Object>("beanId", entityId.getBeanId()));
		args.add(new Pair<String, Object>("beanType", entityId.getBeanType()));
		int removed = dbDao.executeUpdate(EmfQueries.DELETE_PROPERTIES_KEY, args);
		if (isDebugEnabled) {
			logger.debug("Removed " + removed + " properties " + entityId);
		}
	}

	/**
	 * Gets the node properties cached.
	 *
	 * @param entityId
	 *            the node id
	 * @return the node properties cached
	 */
	private Map<String, Serializable> getNodePropertiesCached(PropertyModelKey entityId) {
		Pair<PropertyModelKey, Map<String, Serializable>> cacheEntry = getPropertiesCache()
				.getByKey(entityId);
		if (cacheEntry == null) {
			// when the node is newly created and not persisted, yet. then we have no properties
			return new LinkedHashMap<String, Serializable>();
		}
		Map<String, Serializable> cachedProperties = cacheEntry.getSecond();
		Map<String, Serializable> properties = PropertiesUtil.cloneProperties(cachedProperties);
		// Done
		return properties;
	}

	/**
	 * Load properties for all given entity IDs in one query.
	 *
	 * @param entityIds
	 *            the entity ids
	 * @return the map
	 */
	private Map<PropertyModelKey, Map<String, Serializable>> loadPropertiesInternal(
			Set<PropertyModelKey> entityIds) {

		TimeTracker tracker = null;
		StringBuilder debugMessage = null;
		if (isTraceEnabled) {
			tracker = new TimeTracker().begin().begin();
			debugMessage = new StringBuilder(100);
		}

		// group entity IDs by entity type in here
		Map<Integer, Set<String>> argsMapping = new LinkedHashMap<Integer, Set<String>>(6);

		// mapping for easy convert between DB EntityId and fully filled EntityId
		Map<PropertyModelKey, PropertyModelKey> mapping = new LinkedHashMap<PropertyModelKey, PropertyModelKey>(
				(int) (entityIds.size() * 1.1), 0.95f);

		// the final result
		Map<PropertyModelKey, Map<String, Serializable>> result = new LinkedHashMap<PropertyModelKey, Map<String, Serializable>>(
				(int) (entityIds.size() * 1.2), 1f);

		EntityLookupCache<PropertyModelKey, Map<String, Serializable>, Serializable> propertiesCache = getPropertiesCache();

		for (PropertyModelKey entityId : entityIds) {
			mapping.put(entityId, entityId);
			// check if the given entity if is found in the cache if so we does
			// not fetch it from the DB again
			Map<String, Serializable> cachedValue = propertiesCache.getValue(entityId);
			if (cachedValue == null) {
				// if not found in cache we add it to the query arguments
				// for optimization we group all entities by entity type to
				// simplify queries
				Set<String> set = argsMapping.get(entityId.getBeanType());
				if (set == null) {
					set = new LinkedHashSet<String>();
					argsMapping.put(entityId.getBeanType(), set);
				}
				set.add(entityId.getBeanId());
			} else {
				// add to the result the cached value
				result.put(entityId, PropertiesUtil.cloneProperties(cachedValue));
			}
		}

		// all data is fetched from the cache so no need to continue
		if (argsMapping.isEmpty()) {
			if (isTraceEnabled) {
				logger.trace("Properties for " + entityIds.size()
						+ " entities fetched from cache for " + tracker.stop() + " ms");
			}
			return result;
		} else if (isTraceEnabled) {
			debugMessage.append("Cache lookup took ").append(tracker.stop()).append(" ms.");
			tracker.begin();
		}

		// accumulate the DB result here
		List<PropertyEntity> results = new LinkedList<PropertyEntity>();

		// REVIEW:BB NOTE: probably we can add parallel loading of properties
		// will fetch multiple results for particular type
		for (Entry<Integer, Set<String>> entry : argsMapping.entrySet()) {
			List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>(2);
			params.add(new Pair<String, Object>("beanId", entry.getValue()));
			params.add(new Pair<String, Object>("beanType", entry.getKey()));
			List<PropertyEntity> list = dbDao.fetchWithNamed(EmfQueries.QUERY_PROPERTIES_KEY,
					params);
			if (isTraceEnabled) {
				logger.trace("For beanType=" + entry.getKey() + " AND beanId in ("
						+ entry.getValue() + ") fetched " + list.size() + " results");
			}
			results.addAll(list);
		}

		// nothing is fetched from the DB return the current result
		if (results.isEmpty()) {
			if (isTraceEnabled) {
				debugMessage.append(" No properties found in DB for ")
						.append(tracker.stopInSeconds()).append(" s. Total time ")
						.append(tracker.stopInSeconds()).append(" s");
				logger.trace(debugMessage);
			}
			return result;
		} else if (isTraceEnabled) {
			debugMessage.append(" Db lookup took ").append(tracker.stopInSeconds()).append(" s.");
			tracker.begin();
		}

		if (isTraceEnabled) {
			logger.trace("Total: for " + entityIds.size() + " keys fetched " + results.size()
					+ " results");
		}
		// organize results
		Map<PropertyModelKey, Map<PropertyKey, PropertyValue>> map = new LinkedHashMap<PropertyModelKey, Map<PropertyKey, PropertyValue>>(
				(int) (entityIds.size() * 1.2), 1f);
		for (PropertyEntity propertyEntity : results) {
			// convert from DB entityId to fully populated entity ID
			PropertyModelKey dbEntity = propertyEntity.getEntityId();
			// if (EqualsHelper.nullSafeEquals(propertyEntity.getValue().getActualTypeString(),
			// "INSTANCE")) {
			// propertyEntity.getValue().getLongValue();
			// }
			PropertyModelKey localEntity = mapping.get(dbEntity);

			// fetched not needed result
			if (localEntity == null) {
				// after the optimization for fetching results this should not
				// happen
				logger.warn("DB " + dbEntity + " not needed!!");
				continue;
			}

			Map<PropertyKey, PropertyValue> local = map.get(localEntity);
			if (local == null) {
				local = new LinkedHashMap<PropertyKey, PropertyValue>();
				map.put(localEntity, local);
			}

			local.put(propertyEntity.getKey(), propertyEntity.getValue());
		}

		// convert the results and update the cache
		for (Entry<PropertyModelKey, Map<PropertyKey, PropertyValue>> entry : map.entrySet()) {
			Map<String, Serializable> publicProperties = nodePropertyHelper
					.convertToPublicProperties(entry.getValue(), entry.getKey().getRevision(),
							entry.getKey().getPathElement());
			// update the cache
			setNodePropertiesCached(entry.getKey(), publicProperties);

			result.put(entry.getKey(), publicProperties);
		}

		// fill the properties for not found entities
		Set<PropertyModelKey> foundEntities = new LinkedHashSet<PropertyModelKey>(entityIds);
		// we check by removing the found entries till now
		if (foundEntities.removeAll(result.keySet())) {
			if (isDebugEnabled && !foundEntities.isEmpty()) {
				logger.debug("No properties information for (" + foundEntities.size()
						+ ") entity ids: " + foundEntities);
			}
			for (PropertyModelKey entityId : foundEntities) {
				result.put(entityId, new LinkedHashMap<String, Serializable>());
			}
		}

		if (isTraceEnabled) {
			debugMessage.append(" Property conversion took ").append(tracker.stopInSeconds())
					.append(" s. Total properties fetch time ").append(tracker.stopInSeconds());
			logger.trace(debugMessage);
		}

		return result;
	}

	/**
	 * Getter method for propertiesCache.
	 *
	 * @return the propertiesCache
	 */
	private EntityLookupCache<PropertyModelKey, Map<String, Serializable>, Serializable> getPropertiesCache() {
		return cacheContext.getCache(PROPERTY_ENTITY_CACHE);
	}

	/**
	 * The Class PropertiesLookupCallback. Fetches entity properties by EntityId
	 *
	 * @author BBonev
	 */
	class PropertiesLookupCallback
			extends
			EntityLookupCallbackDAOAdaptor<PropertyModelKey, Map<String, Serializable>, Serializable> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<PropertyModelKey, Map<String, Serializable>> findByKey(PropertyModelKey key) {
			List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>(2);
			args.add(new Pair<String, Object>("beanId", key.getBeanId()));
			args.add(new Pair<String, Object>("beanType", key.getBeanType()));
			List<PropertyEntity> resultList = dbDao.fetchWithNamed(
					EmfQueries.QUERY_PROPERTIES_BY_ENTITY_ID_KEY, args);
			if (resultList.isEmpty()) {
				return null;
			}
			if (isTraceEnabled) {
				logger.trace("Fetched for key " + key + " " + resultList.size() + " results");
			}
			Map<PropertyKey, PropertyValue> propertyValues = new LinkedHashMap<PropertyKey, PropertyValue>(
					(int) (resultList.size() * 1.2), 1f);
			for (PropertyEntity propertyEntity : resultList) {
				propertyValues.put(propertyEntity.getKey(), propertyEntity.getValue());
			}
			if (isTraceEnabled) {
				logger.trace("Returning unique results " + propertyValues.size());
			}
			Map<String, Serializable> publicProperties = nodePropertyHelper
					.convertToPublicProperties(propertyValues, key.getRevision(),
							key.getPathElement());
			return new Pair<PropertyModelKey, Map<String, Serializable>>(key,
					Collections.unmodifiableMap(publicProperties));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<PropertyModelKey, Map<String, Serializable>> createValue(
				Map<String, Serializable> value) {
			throw new UnsupportedOperationException("A node always has a 'map' of properties.");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int deleteByKey(PropertyModelKey key) {
			List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>(2);
			args.add(new Pair<String, Object>("beanId", key.getBeanId()));
			args.add(new Pair<String, Object>("beanType", key.getBeanType()));
			int update = dbDao.executeUpdate(EmfQueries.DELETE_ALL_PROPERTIES_FOR_BEAN_KEY, args);
			if (isDebugEnabled) {
				logger.debug("Removed " + update + " properties for " + key);
			}
			return update;
		}

	}
}
