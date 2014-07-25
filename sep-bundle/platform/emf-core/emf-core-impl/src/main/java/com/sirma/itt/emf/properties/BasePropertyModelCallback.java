package com.sirma.itt.emf.properties;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.domain.model.PathElement;
import com.sirma.itt.emf.instance.EntityType;
import com.sirma.itt.emf.instance.EntityTypeProvider;
import com.sirma.itt.emf.properties.dao.PropertyModelCallback;
import com.sirma.itt.emf.properties.entity.EntityId;
import com.sirma.itt.emf.properties.model.PropertyModel;
import com.sirma.itt.emf.properties.model.PropertyModelKey;

/**
 * Implements common functions for {@link PropertyModelCallback}.
 *
 * @param <E>
 *            the element type
 * @author BBonev
 */
public abstract class BasePropertyModelCallback<E extends PropertyModel> implements
		PropertyModelCallback<E> {

	/** The logger. */
	@Inject
	private Logger logger;
	/** The type provider. */
	@Inject
	protected EntityTypeProvider typeProvider;

	@Override
	public PropertyModelKey createModelKey(Entity<?> baseEntity, Long revision) {
		int entityTypeIdentifier = getEntityTypeIdentifier(baseEntity);
		if (entityTypeIdentifier == 0) {
			// the entity is not supported
			return null;
		}

		PathElement pathElement = null;
		if (baseEntity instanceof PathElement) {
			pathElement = (PathElement) baseEntity;
		}
		return new EntityId(baseEntity.getId().toString(), entityTypeIdentifier, revision,
				pathElement);
	}

	/**
	 * Gets the entity type identifier. The Id is used to distinguish between different entity types
	 * when saving and retrieving properties. The default implementation is located into the enum
	 * {@link EntityTypeProvider}. If the entity is not supported then the method will return 0. The
	 * method should be overridden if added new entity type that is not supported by CMF.
	 * 
	 * @param entity
	 *            the entity
	 * @return the entity type identifier
	 */
	protected int getEntityTypeIdentifier(Entity<?> entity) {
		EntityType type = typeProvider.getEntityType(entity);
		if (type == null) {
			return 0;
		}
		return type.getTypeId();
	}

	@Override
	public Map<PropertyModelKey, PropertyModel> getModel(E model) {
		Map<PropertyModelKey, PropertyModel> map = new LinkedHashMap<PropertyModelKey, PropertyModel>();
		createSubModel(model, map);
		createModel(model, map);
		return map;
	}

	@Override
	public Map<PropertyModelKey, PropertyModel> getModelForLoading(E model) {
		Map<PropertyModelKey, PropertyModel> map = new LinkedHashMap<PropertyModelKey, PropertyModel>();
		// currently this will work only of we need to refresh the properties of the model
		// otherwise the model's properties are empty
		createSubModel(model, map);
		createModel(model, map);
		return map;
	}

	@Override
	public void updateModel(Map<PropertyModelKey, PropertyModel> target,
			Map<PropertyModelKey, Map<String, Serializable>> properties) {

		for (Entry<PropertyModelKey, Map<String, Serializable>> entry : properties.entrySet()) {
			PropertyModel model = target.remove(entry.getKey());
			if (model != null) {
				if (entry.getValue() != null) {
					model.setProperties(entry.getValue());
				} else if (model.getProperties() == null) {
					model.setProperties(new LinkedHashMap<String, Serializable>());
				}
			}
		}
		for (Entry<PropertyModelKey, PropertyModel> entry : target.entrySet()) {
			entry.getValue().setProperties(new LinkedHashMap<String, Serializable>());
		}
	}

	/**
	 * Creates the model and populates the model mapping.
	 *
	 * @param <M>
	 *            the generic type
	 * @param model
	 *            the model to iterate
	 * @param modelMapping
	 *            the target model mapping
	 */
	protected <M extends PropertyModel> void createModel(M model,
			Map<PropertyModelKey, PropertyModel> modelMapping) {
		if (model instanceof Entity) {
			PropertyModelKey key = createModelKey((Entity<?>) model, model.getRevision());
			if (key != null) {
				if (!modelMapping.containsKey(key)) {
					modelMapping.put(key, model);
				} else {
					logger.warn("\n>>>>>>>>>>>>>>\n>The given PropertyModel has a duplicate key: "
							+ key + "\n>>>>>>>>>>>>>>");
				}
			} else {
				logger.warn("No properties support for " + model.getClass());
			}
		}
	}

	/**
	 * Creates the sub model of the given model and adds it to the given model mapping.
	 *
	 * @param <M>
	 *            the generic type
	 * @param model
	 *            the model to iterate and check
	 * @param modelMapping
	 *            the target model mapping
	 */
	protected <M extends PropertyModel> void createSubModel(M model,
			Map<PropertyModelKey, PropertyModel> modelMapping) {
		if ((model == null) || (model.getProperties() == null)) {
			return;
		}
		for (Serializable serializable : model.getProperties().values()) {
			// if we have complex model we can load all properties of the model also
			if ((serializable instanceof Entity) && (serializable instanceof PropertyModel)) {
				Entity<?> entity = (Entity<?>) serializable;
				if (entity.getId() != null) {
					PropertyModel propertyModel = (PropertyModel) serializable;
					PropertyModelKey key = createModelKey(entity, propertyModel.getRevision());
					if (key != null) {
						if (!modelMapping.containsKey(key)) {
							// add the sub-models before the current model, so to be persisted before
							// the current model
							createSubModel(propertyModel, modelMapping);
							modelMapping.put(key, propertyModel);
						} else {
							logger.warn("\n>>>>>>>>>>>>>>\n>The given PropertyModel has a duplicate key: "
									+ key + "\n>>>>>>>>>>>>>>");
						}
					} else {
						logger.warn("No properties support for " + entity.getClass());
					}
				}
			}
		}
	}

}
