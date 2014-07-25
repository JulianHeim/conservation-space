/*
 *
 */
package com.sirma.itt.emf.properties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.db.SequenceEntityGenerator;
import com.sirma.itt.emf.definition.model.ControlDefinition;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinitionModel;
import com.sirma.itt.emf.domain.DisplayType;
import com.sirma.itt.emf.domain.ObjectTypes;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.domain.model.PathElement;
import com.sirma.itt.emf.exceptions.EmfConfigurationException;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.instance.dao.InstanceDao;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.CommonInstance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.properties.dao.PropertiesDao;
import com.sirma.itt.emf.properties.dao.PropertyModelCallback;
import com.sirma.itt.emf.properties.model.PropertyModel;
import com.sirma.itt.emf.resources.EmfResourcesUtil;
import com.sirma.itt.emf.resources.ResourceProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * Implementation of the public service for persisting and retrieving properties.
 *
 * @author BBonev
 */
@Stateless
public class PropertiesServiceImpl implements PropertiesService {

	/** The default instance callback. */
	@Inject
	@InstanceType(type = ObjectTypes.DEFAULT)
	private PropertyModelCallback<PropertyModel> defaultInstanceCallback;

	/** The properties dao. */
	@Inject
	private PropertiesDao propertiesDao;

	/** The callbacks. */
	@Inject
	@Any
	private javax.enterprise.inject.Instance<PropertyModelCallback<PropertyModel>> callbacks;

	/** The callback mapping. */
	private Map<Class<?>, PropertyModelCallback<PropertyModel>> callbackMapping;

	/** The logger. */
	@Inject
	private Logger logger;
	/** The trace. */
	private boolean trace;

	/** The instance dao. */
	@Inject
	@InstanceType(type = ObjectTypes.INSTANCE)
	private javax.enterprise.inject.Instance<InstanceDao<CommonInstance>> instanceDao;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;
	/** The resource service. */
	@Inject
	private Instance<ResourceService> resourceService;

	/** The inbound converter. */
	private final ModelConverter INBOUND_CONVERTER = new InputModelConverter();

	/** The outbound converter. */
	private final ModelConverter OUTBOUND_CONVERTER = new OutputModelConverter();

	/**
	 * Initialize some properties.
	 */
	@PostConstruct
	public void init() {
		trace = logger.isTraceEnabled();
		callbackMapping = CollectionUtils.createHashMap(30);

		for (PropertyModelCallback<PropertyModel> modelCallback : callbacks) {
			Set<Class<?>> supportedObjects = modelCallback.getSupportedObjects();
			for (Class<?> supportedObject : supportedObjects) {
				if (callbackMapping.containsKey(supportedObject)) {
					throw new EmfConfigurationException("Ambiguous property model callback: "
							+ supportedObject + " already defined by "
							+ callbackMapping.get(supportedObject).getClass());
				}
				if (trace) {
					logger.trace("Registering " + supportedObject + " to "
							+ modelCallback.getClass());
				}
				callbackMapping.put(supportedObject, modelCallback);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends PropertyModel> boolean isModelSupported(E instance) {
		try {
			PropertyModelCallback<E> callback = getCallback(instance, null, true, false);
			return callback != null;
		} catch (RuntimeException e) {
			// nothing to do here
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Map<String, Serializable> getEntityProperties(Entity entity, Long revision,
			PathElement path) {
		if (trace) {
			logger.trace("Loading properties for " + entity.getClass().getSimpleName() + ": "
					+ entity.getId());
		}
		return propertiesDao.getEntityProperties(entity, revision, path,
				getCallback(null, entity, true, true));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeProperties(Entity entity, Long revision, PathElement path) {
		if (trace) {
			logger.trace("Deleting properties for " + entity.getClass().getSimpleName() + ": "
					+ entity.getId());
		}
		propertiesDao.removeProperties(entity, revision, path,
				getCallback(null, entity, true, true));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void saveProperties(Entity entity, Long revision, PathElement path,
			Map<String, Serializable> properties) {
		saveProperties(entity, revision, path, properties, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveProperties(Entity entity, Long revision, PathElement path,
			Map<String, Serializable> properties, boolean addOnly) {
		if (trace) {
			logger.trace("Saving properties with mode=" + (addOnly ? "MERGE" : "REPLACE") + " for "
					+ entity.getClass().getSimpleName() + ": " + entity.getId());
		}
		// prepareForPropertiesPersist(properties);

		propertiesDao.saveProperties(entity, revision, path, properties, addOnly,
				getCallback(null, entity, true, true));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public <E extends PropertyModel> void saveProperties(E instance) {
		saveProperties(instance, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends PropertyModel> void saveProperties(E instance, boolean addOnly) {
		saveProperties(instance, addOnly, false);
	}

	/**
	 * Save properties.
	 *
	 * @param <E>
	 *            the element type
	 * @param instance
	 *            the instance
	 * @param addOnly
	 *            the add only
	 * @param saveFullGraph
	 *            the save full graph
	 */
	@Override
	public <E extends PropertyModel> void saveProperties(E instance, boolean addOnly,
			boolean saveFullGraph) {
		if (trace) {
			logger.trace("Saving properties with mode=" + (addOnly ? "MERGE" : "REPLACE")
					+ (saveFullGraph ? "_FULL" : "_BASE") + " for "
					+ instance.getClass().getSimpleName());
		}
		prepareForPropertiesPersist(instance.getProperties());

		propertiesDao.saveProperties(instance, addOnly,
				getCallback(instance, null, saveFullGraph, true));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends PropertyModel> void loadProperties(E instance) {
		loadProperties(instance, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends PropertyModel> void loadProperties(E instance, boolean loadAll) {
		if (trace) {
			logger.trace("Loading properties for " + instance.getClass().getSimpleName());
		}
		propertiesDao.loadProperties(instance, getCallback(instance, null, loadAll, true));
		if (trace) {
			logger.trace("Loaded properties for instance: " + instance);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends PropertyModel> void loadProperties(List<E> instances) {
		loadProperties(instances, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends PropertyModel> void loadProperties(List<E> instances, boolean loadAll) {
		if (instances.isEmpty()) {
			return;
		}
		if (trace) {
			Set<Serializable> list = new LinkedHashSet<Serializable>(
					(int) (instances.size() * 1.2), 0.9f);
			for (E caseInstance : instances) {
				list.add(((Entity<?>) caseInstance).getId());
			}
			logger.trace("Loading properties for " + instances.get(0).getClass().getSimpleName()
					+ " (" + list.size() + "): " + list);
		}
		propertiesDao.loadProperties(instances, getCallback(instances.get(0), null, loadAll, true));
		if (trace) {
			logger.trace("Loaded properties for instances: " + instances);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Map<String, ?> convertForRest(PropertyModel model, DefinitionModel definitionModel) {
		if ((model == null) || (model.getProperties() == null) || model.getProperties().isEmpty()) {
			return new HashMap<>(1);
		}
		if (definitionModel == null) {
			return new HashMap<>(model.getProperties());
		}
		Map<String, Serializable> properties = CollectionUtils.createLinkedHashMap(model
				.getProperties().size() << 1);

		if (definitionModel instanceof RegionDefinitionModel) {
			iterateModel((RegionDefinitionModel) definitionModel,
					escapeStringProperties(model.getProperties()), properties, OUTBOUND_CONVERTER);
		} else {
			iterateModel(definitionModel, escapeStringProperties(model.getProperties()),
					properties, OUTBOUND_CONVERTER);
		}
		return properties;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Map<String, Serializable> convertFromRest(Map<String, ?> source,
			DefinitionModel definitionModel) {
		if ((source == null) || source.isEmpty()) {
			return new LinkedHashMap<>();
		}
		if (definitionModel == null) {
			return (Map<String, Serializable>) source;
		}
		Map<String, Serializable> properties = CollectionUtils.createLinkedHashMap(source.size());

		if (definitionModel instanceof RegionDefinitionModel) {
			iterateModel((RegionDefinitionModel) definitionModel, source, properties,
					INBOUND_CONVERTER);
		} else {
			iterateModel(definitionModel, source, properties, INBOUND_CONVERTER);
		}

		return properties;
	}

	/**
	 * Iterate model.
	 *
	 * @param <A>
	 *            the generic type
	 * @param <B>
	 *            the generic type
	 * @param model
	 *            the model
	 * @param source
	 *            the source
	 * @param output
	 *            the output
	 * @param converter
	 *            the converter
	 */
	@SuppressWarnings("unchecked")
	private <A, B> void iterateModel(DefinitionModel model, Map<String, A> source,
			Map<String, B> output, ModelConverter converter) {
		for (PropertyDefinition propertyDefinition : model.getFields()) {
			if (converter.isAllowedForConvert(propertyDefinition)) {
				Object sourceValue = source.get(propertyDefinition.getName());
				// nothing to do with the missing values
				if (sourceValue == null) {
					continue;
				}

				Object converted;
				if (sourceValue instanceof Collection) {
					List<B> convertedValues = new ArrayList<>(
							((Collection<Object>) sourceValue).size());
					for (Object object : (Collection<Object>) sourceValue) {
						Object temp = converter.convert(propertyDefinition, object);
						// we does not handle null values
						if (temp != null) {
							convertedValues.add((B) temp);
						}
					}
					converted = convertedValues;
				} else {
					converted = converter.convert(propertyDefinition, sourceValue);
				}
				// we does not handle null values
				if (converted != null) {
					output.put(propertyDefinition.getName(), (B) converted);
				}
			}
		}
	}

	/**
	 * Iterate model.
	 *
	 * @param <A>
	 *            the generic type
	 * @param <B>
	 *            the generic type
	 * @param model
	 *            the model
	 * @param source
	 *            the source
	 * @param output
	 *            the output
	 * @param converter
	 *            the converter
	 */
	private <A, B> void iterateModel(RegionDefinitionModel model, Map<String, A> source,
			Map<String, B> output, ModelConverter converter) {
		iterateModel((DefinitionModel) model, source, output, converter);
		for (RegionDefinition regionDefinition : model.getRegions()) {
			iterateModel(regionDefinition, source, output, converter);
		}
	}

	/**
	 * Escapes document properties like title and createdBy names, so they cause js errors.
	 *
	 * @param properties
	 *            Properties that may need escaping.
	 * @return the map
	 */
	private Map<String, Serializable> escapeStringProperties(Map<String, Serializable> properties) {
		return properties;
	}

	/**
	 * Gets the callback for the given instance.
	 *
	 * @param <E>
	 *            the instance type
	 * @param instance
	 *            the instance to get the callback for (can be <code>null</code> but then the entity
	 *            should be provided)
	 * @param entity
	 *            the entity to get the callback for it no instance is present (optional if instance
	 *            is present)
	 * @param loadAll
	 *            the load all properties
	 * @param fail
	 *            if the method should fail if no callback is found
	 * @return the callback
	 */
	@SuppressWarnings("unchecked")
	protected <E extends PropertyModel> PropertyModelCallback<E> getCallback(E instance,
			Entity entity, boolean loadAll, boolean fail) {
		Object target = instance;
		if (target == null) {
			target = entity;
		}
		if (!loadAll && defaultInstanceCallback.canHandle(target)) {
			return (PropertyModelCallback<E>) defaultInstanceCallback;
		}
		Class<?> targetClass = target.getClass();
		PropertyModelCallback<PropertyModel> callback = callbackMapping.get(targetClass);
		if (callback == null) {
			if (trace) {
				logger.trace("No callback registered for " + targetClass
						+ ". Will try to lookup one.");
			}
			for (PropertyModelCallback<PropertyModel> modelCallback : callbacks) {
				if (modelCallback.canHandle(target)) {
					// update the mapping model with the new discovery
					callbackMapping.put(targetClass, modelCallback);
					if (trace) {
						logger.trace("Registering " + targetClass + " to "
								+ modelCallback.getClass());
					}
					callback = modelCallback;
					break;
				}
			}
		}
		if (callback == null) {
			if (fail) {
				throw new EmfRuntimeException("The entity of type " + targetClass
						+ " is not supported for properties saving!");
			}
			return null;
		}
		return (PropertyModelCallback<E>) callback;
	}

	/**
	 * Checks for no persisted custom objects of type {@link CommonInstance}. If not already saved
	 * we will save them
	 *
	 * @param properties
	 *            the properties
	 */
	protected void prepareForPropertiesPersist(Map<String, Serializable> properties) {
		if ((properties == null) || properties.isEmpty()) {
			return;
		}
		for (Serializable serializable : properties.values()) {
			if (serializable instanceof CommonInstance) {
				saveCommonInstanceIfNeeded((CommonInstance) serializable);
			} else if (serializable instanceof Collection) {
				for (Object object : (Collection<?>) serializable) {
					if (object instanceof CommonInstance) {
						saveCommonInstanceIfNeeded((CommonInstance) object);
					} else {
						// no need to iterate more if the objects are not common instance. We does
						// not support non heterogeneous collections
						break;
					}
				}
			}
		}
	}

	/**
	 * Saves the given common instance if needed.
	 *
	 * @param instance
	 *            the instance
	 */
	protected void saveCommonInstanceIfNeeded(CommonInstance instance) {
		if (instance == null) {
			return;
		}
		if (!SequenceEntityGenerator.isPersisted(instance)) {
			instanceDao.get().saveEntity(instance);
		}
		// we check for more properties on the go
		prepareForPropertiesPersist(instance.getProperties());
	}

	/**
	 * Interface that defines a means to convert a definition model properties to other format using
	 * the provided definition.
	 *
	 * @author BBonev
	 */
	interface ModelConverter {

		/**
		 * Checks if is allowed for convert.
		 *
		 * @param property
		 *            the property
		 * @return true, if is allowed for convert
		 */
		boolean isAllowedForConvert(PropertyDefinition property);

		/**
		 * Convert.
		 *
		 * @param property
		 *            the property
		 * @param source
		 *            the source
		 * @return the serializable
		 */
		Object convert(PropertyDefinition property, Object source);
	}

	/**
	 * Converter implementation for the input properties.
	 *
	 * @author BBonev
	 */
	class InputModelConverter implements ModelConverter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isAllowedForConvert(PropertyDefinition property) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object convert(PropertyDefinition property, Object source) {
			if (DataTypeDefinition.DATE.equals(property.getType())
					|| DataTypeDefinition.DATETIME.equals(property.getType())) {
				return typeConverter.convert(Date.class, source);
			} else if (property.getControlDefinition() != null) {
				ControlDefinition control = property.getControlDefinition();
				if (control.getIdentifier().equals("USER")
						|| control.getIdentifier().equals("PICKLIST")) {
					JSONObject object;
					String name;
					if ((source instanceof String) && source.toString().startsWith("{")) {
						try {
							object = new JSONObject(source.toString());
							name = JsonUtil.getStringValue(object, ResourceProperties.USER_ID);
						} catch (JSONException e) {
							logger.error("Failed to parse " + source
									+ " as JSON to extract the user information: " + e.getMessage());
							return source;
						}
					} else if (source instanceof Map) {
						name = (String) ((Map<?, ?>) source).get(ResourceProperties.USER_ID);
					} else {
						return source;
					}

					Resource resource = resourceService.get().getResource(name, ResourceType.USER);
					if (resource != null) {
						return resource.getIdentifier();
					}
					// invalid user provided
					return null;
				} else if (control.getIdentifier().equals("INSTANCE")) {
					JSONObject object = null;
					Map<String, Object> map = null;
					if (source instanceof String) {
						try {
							object = new JSONObject(source.toString());
						} catch (JSONException e) {
							logger.error("Failed to parse " + source
									+ " as JSON to extract the instance information: "
									+ e.getMessage());
							return source;
						}
					} else if (source instanceof Map) {
						map = (Map<String, Object>) source;
					}
					if (((map != null) && map.isEmpty())
							|| ((object != null) && (object.length() == 0))) {
						// no information
						return null;
					}
					CommonInstance instance = new CommonInstance();
					instance.setId(getProperty(String.class, object, map, "id"));
					instance.setIdentifier(getProperty(String.class, object, map, "identifier"));
					instance.setPath(getProperty(String.class, object, map, "path"));
					instance.setRevision(getProperty(Long.class, object, map, "revision"));
					instance.setVersion(getProperty(Long.class, object, map, "version"));

					Object properties = getProperty(Object.class, object, map, "properties");
					if (properties instanceof JSONObject) {
						// convert properties
						JSONObject propertiesObject = (JSONObject) properties;
						Map<String, String> subProperties = CollectionUtils
								.createLinkedHashMap(propertiesObject.length());
						if (propertiesObject.length() > 0) {
							Iterator<?> localIterator = propertiesObject.keys();
							while (localIterator.hasNext()) {
								String key = localIterator.next().toString();
								String value = JsonUtil.getStringValue(propertiesObject, key);
								subProperties.put(key, value);
							}
						}

						Map<String, Serializable> map2 = convertFromRest(subProperties, control);
						instance.setProperties(map2);
					} else if (properties instanceof Map) {
						Map<String, Serializable> map2 = convertFromRest(
								(Map<String, ?>) properties, control);
						instance.setProperties(map2);
					} else {
						instance.setProperties(new LinkedHashMap<String, Serializable>());
					}
					return instance;
				}
			} else if ((source instanceof String) && StringUtils.isBlank((String) source)) {
				// when converting data from REST to internal model when we have only white space in
				// the field we mark it as null and ignore the value at all.
				return null;
			}
			return source;
		}

		/**
		 * Gets the property.
		 *
		 * @param <T>
		 *            the generic type
		 * @param result
		 *            the result
		 * @param object
		 *            the object
		 * @param map
		 *            the map
		 * @param key
		 *            the key
		 * @return the property
		 */
		private <T> T getProperty(Class<T> result, JSONObject object, Map<String, Object> map,
				String key) {
			if (map != null) {
				return typeConverter.convert(result, map.get(key));
			}
			return typeConverter.convert(result, JsonUtil.getValueOrNull(object, key));
		}
	}

	/**
	 * Model converter implementation for the output properties.
	 *
	 * @author BBonev
	 */
	class OutputModelConverter implements ModelConverter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isAllowedForConvert(PropertyDefinition property) {
			return property.getDisplayType() != DisplayType.SYSTEM;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object convert(PropertyDefinition property, Object source) {
			if (DataTypeDefinition.DATE.equals(property.getType())
					|| DataTypeDefinition.DATETIME.equals(property.getType())) {
				return typeConverter.convert(String.class, source);
			} else if (property.getControlDefinition() != null) {
				ControlDefinition control = property.getControlDefinition();
				if (control.getIdentifier().equals("USER")
						|| control.getIdentifier().equals("PICKLIST")) {
					com.sirma.itt.emf.instance.model.Instance instance = null;

					if (source.toString().startsWith("{")) {
						InstanceReference reference = typeConverter.convert(
								InstanceReference.class, source);
						if ((reference != null) && (reference.toInstance() != null)) {
							instance = reference.toInstance();
						}
					} else {
						Resource resource = resourceService.get().getResource(source.toString(),
								ResourceType.UNKNOWN);
						instance = resource;
					}
					if (instance != null) {
						Map<String, String> data = CollectionUtils.createHashMap(instance
								.getProperties().size());
						// copy the resource data
						for (Entry<String, Serializable> entry : instance.getProperties()
								.entrySet()) {
							if (entry.getValue() != null) {
								data.put(entry.getKey(), entry.getValue().toString());
							}
						}
						data.put("label",
								EmfResourcesUtil.buildDisplayName(instance.getProperties()));
						return data;
					}
					// invalid user/group
					return null;
				} else if (source instanceof CommonInstance) {
					Map<String, Serializable> properties = ((CommonInstance) source)
							.getProperties();
					Map<String, String> data = CollectionUtils.createHashMap(properties.size());
					// copy the resource data
					for (Entry<String, Serializable> entry : properties.entrySet()) {
						if (entry.getValue() != null) {
							data.put(entry.getKey(), entry.getValue().toString());
						}
					}
					if (properties.containsKey(DefaultProperties.HEADER_COMPACT)) {
						data.put("label", (String) properties.get(DefaultProperties.HEADER_COMPACT));
					}
					return data;
				}
			}
			return source;
		}

	}

}
