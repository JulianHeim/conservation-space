package com.sirma.itt.objects.services.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.annotation.Proxy;
import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.dao.AllowedChildrenProvider;
import com.sirma.itt.emf.definition.dao.AllowedChildrenTypeProvider;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.event.instance.InstanceAttachedEvent;
import com.sirma.itt.emf.event.instance.InstanceDetachedEvent;
import com.sirma.itt.emf.instance.dao.AllowedChildrenHelper;
import com.sirma.itt.emf.instance.dao.BaseAllowedChildrenProvider;
import com.sirma.itt.emf.instance.dao.InstanceDao;
import com.sirma.itt.emf.instance.dao.InstanceEventProvider;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.dao.ServiceRegister;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.state.operation.event.OperationExecutedEvent;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.objects.constants.ObjectProperties;
import com.sirma.itt.objects.domain.ObjectTypesObject;
import com.sirma.itt.objects.domain.definitions.ObjectDefinition;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.event.BeforeObjectDeleteEvent;
import com.sirma.itt.objects.event.BeforeObjectPersistEvent;
import com.sirma.itt.objects.event.ObjectChangeEvent;
import com.sirma.itt.objects.event.ObjectCreateEvent;
import com.sirma.itt.objects.event.ObjectCreatedEvent;
import com.sirma.itt.objects.event.ObjectPersistedEvent;
import com.sirma.itt.objects.event.ObjectSaveEvent;
import com.sirma.itt.objects.exceptions.DmsObjectException;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;
import com.sirma.itt.objects.services.ObjectService;
import com.sirma.itt.objects.services.adapters.CMFObjectInstanceAdapterService;

/**
 * Default {@link ObjectService} implementation.
 *
 * @author BBonev
 */
@Stateless
public class ObjectServiceImpl implements ObjectService {

	private static final Logger LOGGER = Logger.getLogger(ObjectServiceImpl.class);
	private static final boolean debug = LOGGER.isDebugEnabled();

	/** The adapter service. */
	@Inject
	private CMFObjectInstanceAdapterService adapterService;
	/** The instance dao. */
	@Inject
	@InstanceType(type = ObjectTypesObject.OBJECT)
	private InstanceDao<ObjectInstance> instanceDao;
	/** The event service. */
	@Inject
	private EventService eventService;
	/** The dictionary service. */
	@Inject
	private DictionaryService dictionaryService;
	/** The type provider. */
	@Inject
	private AllowedChildrenTypeProvider typeProvider;
	/** The service register. */
	@Inject
	private ServiceRegister serviceRegister;

	/** The instance service. */
	@Inject
	@Proxy
	private InstanceService<Instance, DefinitionModel> instanceService;

	/**
	 * Creates the instance.
	 *
	 * @param definition
	 *            the definition
	 * @param operation
	 *            the operation
	 * @return the project instance
	 */
	private ObjectInstance createInstance(ObjectDefinition definition, Operation operation) {
		ObjectInstance instance = instanceDao.createInstance(definition, true);

		instanceDao.instanceUpdated(instance, false);
		// set the initial state
		eventService.fire(new OperationExecutedEvent(operation, instance));
		eventService.fire(new ObjectCreateEvent(instance));

		// fixes not set revision
		instanceDao.synchRevisions(instance, instance.getRevision());
		return instance;
	}

	/**
	 * Save instance.
	 *
	 * @param instance
	 *            the instance
	 * @param operation
	 *            the operation
	 * @return the project instance
	 */
	private ObjectInstance saveInstance(ObjectInstance instance, Operation operation) {
		// 1. call the adapter 2. Convert to caseEntity 3.persist data, 4.
		// persist properties
		if (operation != null) {
			RuntimeConfiguration.setConfiguration(RuntimeConfigurationProperties.CURRENT_OPERATION,
					operation.getOperation());
		}
		TimeTracker tracker = TimeTracker.createAndStart();
		try {
			// update project instance state if needed
			eventService.fire(new OperationExecutedEvent(operation, instance));

			boolean onCreate = false;
			BeforeObjectPersistEvent persistEvent = null;

			// set the properties that relates to modifications but does not
			// save it
			instanceDao.instanceUpdated(instance, false);

			Serializable serializable = instance.getProperties().remove(
					ObjectProperties.DEFAULT_VIEW);
			DocumentInstance view = null;
			if (serializable instanceof DocumentInstance) {
				view = (DocumentInstance) serializable;
			}

			if (instance.getDmsId() == null) {
				onCreate = true;
				eventService.fire(new ObjectChangeEvent(instance));

				persistEvent = new BeforeObjectPersistEvent(instance);
				eventService.fire(persistEvent);
				// create the case instance in the DMS and update the fields related
				// to creating a case
				onCreateObject(instance);
			} else {
				eventService.fire(new ObjectChangeEvent(instance));
				// set that the case has been updated and save to DMS
				onUpdateObject(instance);
			}

			try {
				eventService.fire(new ObjectSaveEvent(instance));

				if (view != null) {
					instance.getProperties().put(ObjectProperties.DEFAULT_VIEW, view);
				}

				// persist entity and properties
				ObjectInstance old = instanceDao.persistChanges(instance);

				if (view != null) {
					instance.getProperties().put(ObjectProperties.DEFAULT_VIEW, view);
				}
				if (onCreate) {
					ObjectCreatedEvent event = new ObjectCreatedEvent(instance);
					eventService.fire(event);
					if (event.isHandled()) {
						instanceDao.saveEntity(instance);
					}
					if (persistEvent != null) {
						eventService.fireNextPhase(persistEvent);
					}
				}
				if (!RuntimeConfiguration
						.isConfigurationSet(RuntimeConfigurationProperties.DO_NOT_FIRE_PERSIST_EVENT)) {
					eventService.fire(new ObjectPersistedEvent(instance, old));
				}
			} catch (RuntimeException e) {
				// delete the instance from DMS site
				if (onCreate) {
					try {
						adapterService.deleteInstance(instance, true);
					} catch (DMSException e1) {
						throw new DmsObjectException(
								"Failed to delete object instance from DMS on rollback", e);
					}
				}
				throw e;
			}
		} finally {
			if (debug) {
				StringBuilder builder = new StringBuilder();
				builder.append("Total object ").append(instance.getId()).append(" save took ")
						.append(tracker.stopInSeconds()).append(" s");
				LOGGER.debug(builder.toString());
			}
			if (operation != null) {
				RuntimeConfiguration
						.clearConfiguration(RuntimeConfigurationProperties.CURRENT_OPERATION);
			}
			instance.getProperties().remove(ObjectProperties.DEFAULT_VIEW);
		}
		return instance;
	}

	/**
	 * On update project.
	 *
	 * @param instance
	 *            the instance
	 */
	private void onUpdateObject(ObjectInstance instance) {
		try {
			adapterService.updateInstance(instance);
		} catch (Exception e) {
			throw new DmsObjectException("Error updating object instance in DMS", e);
		}
	}

	/**
	 * On create project.
	 *
	 * @param instance
	 *            the instance
	 */
	private void onCreateObject(ObjectInstance instance) {
		try {
			String dmsId = adapterService.createInstance(instance);
			instance.setDmsId(dmsId);
		} catch (Exception e) {
			throw new DmsObjectException("Error creating object instance in DMS", e);
		}
	}

	/**
	 * Batch load instances.
	 *
	 * @param <S>
	 *            the generic type
	 * @param dmsIds
	 *            the dms ids
	 * @param loadAllProperties
	 *            the load all properties
	 * @return the list
	 */
	private <S extends Serializable> List<ObjectInstance> batchLoadInstances(List<S> dmsIds,
			boolean loadAllProperties) {
		if ((dmsIds == null) || dmsIds.isEmpty()) {
			return Collections.emptyList();
		}
		return instanceDao.loadInstances(dmsIds, loadAllProperties);
	}

	/**
	 * Batch load instances by id.
	 *
	 * @param <S>
	 *            the generic type
	 * @param ids
	 *            the ids
	 * @param loadAllProperties
	 *            the load all properties
	 * @return the list
	 */
	private <S extends Serializable> List<ObjectInstance> batchLoadInstancesById(List<S> ids,
			boolean loadAllProperties) {
		if ((ids == null) || ids.isEmpty()) {
			return Collections.emptyList();
		}

		return instanceDao.loadInstancesByDbKey(ids, loadAllProperties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public ObjectInstance createInstance(ObjectDefinition definition,
			com.sirma.itt.emf.instance.model.Instance parent) {
		return createInstance(definition, new Operation(ObjectActionTypeConstants.CREATE_OBJECT));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public ObjectInstance createInstance(ObjectDefinition definition,
			com.sirma.itt.emf.instance.model.Instance parent, Operation operation) {
		return createInstance(definition, operation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Secure
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ObjectInstance save(ObjectInstance instance, Operation operation) {
		return saveInstance(instance, operation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<ObjectInstance> loadInstances(com.sirma.itt.emf.instance.model.Instance owner) {
		return Collections.emptyList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public ObjectInstance loadByDbId(Serializable id) {
		return instanceDao.loadInstance(id, null, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public ObjectInstance load(Serializable instanceId) {
		return instanceDao.loadInstance(null, instanceId, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <S extends Serializable> List<ObjectInstance> load(List<S> ids) {
		return batchLoadInstances(ids, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <S extends Serializable> List<ObjectInstance> loadByDbId(List<S> ids) {
		return batchLoadInstancesById(ids, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <S extends Serializable> List<ObjectInstance> load(List<S> ids, boolean allProperties) {
		return batchLoadInstances(ids, allProperties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <S extends Serializable> List<ObjectInstance> loadByDbId(List<S> ids,
			boolean allProperties) {
		return batchLoadInstancesById(ids, allProperties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Map<String, List<DefinitionModel>> getAllowedChildren(ObjectInstance owner) {
		// TODO: implement more specific provider
		AllowedChildrenProvider<ObjectInstance> calculator = new BaseAllowedChildrenProvider<ObjectInstance>(
				dictionaryService, typeProvider);
		return AllowedChildrenHelper.getAllowedChildren(owner, calculator, dictionaryService);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<DefinitionModel> getAllowedChildren(ObjectInstance owner, String type) {
		// TODO: implement more specific provider
		AllowedChildrenProvider<ObjectInstance> calculator = new BaseAllowedChildrenProvider<ObjectInstance>(
				dictionaryService, typeProvider);
		return AllowedChildrenHelper.getAllowedChildren(owner, calculator, dictionaryService, type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public boolean isChildAllowed(ObjectInstance owner, String type) {
		AllowedChildrenProvider<ObjectInstance> calculator = new BaseAllowedChildrenProvider<ObjectInstance>(
				dictionaryService, typeProvider);
		return AllowedChildrenHelper.isChildAllowed(owner, calculator, dictionaryService, type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Class<ObjectDefinition> getInstanceDefinitionClass() {
		return ObjectDefinition.class;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh(ObjectInstance instance) {
		instanceDao.loadProperties(instance);
	}

	/**
	 * {@inheritDoc}
	 */
	@Secure
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ObjectInstance cancel(ObjectInstance instance) {
		return save(instance, new Operation(ActionTypeConstants.STOP));
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public ObjectInstance clone(ObjectInstance instanceToClone, Operation operation) {
		ObjectDefinition definition = dictionaryService.getDefinition(getInstanceDefinitionClass(),
				instanceToClone.getIdentifier());
		// Should throw an event and what should be the target of the event?
		eventService.fire(new OperationExecutedEvent(operation, instanceToClone));

		ObjectInstance instance = createInstance(definition, null, operation);

		for (Entry<String, Serializable> propertyEntry : instanceToClone.getProperties().entrySet()) {
			// Checks if property is not in the NOT_CLONABLE list and that it exists in object
			// definition (to avoid idoc custom properties)
			if (!DefaultProperties.NOT_CLONABLE_PROPERTIES.contains(propertyEntry.getKey())) {
				/*
				 * boolean existsInDef = false; for (int i = 0; (i < fields.size()) && !existsInDef;
				 * i++) { PropertyDefinition fieldDefinition = fields.get(i); existsInDef =
				 * fieldDefinition.getName().equals(propertyEntry.getKey()); } if (existsInDef) {
				 */
				instance.getProperties().put(propertyEntry.getKey(), propertyEntry.getValue());
				// }
			}
		}
		return instance;
	}

	@Secure
	@Override
	public void delete(ObjectInstance instance, Operation operation, boolean permanent) {
		BeforeObjectDeleteEvent event = new BeforeObjectDeleteEvent(instance);
		eventService.fire(event);
		// change the state of the object
		saveInstance(instance, operation);
		// delete the instance
		instanceDao.delete(instance);
		eventService.fireNextPhase(event);
	}

	@Override
	public boolean move(ObjectInstance objectInstance, Instance src, Instance dest) {
		if (objectInstance == null) {
			// can't move nothing
			return false;
		}
		if (src != null) {
			instanceService.detach(src, new Operation(ActionTypeConstants.MOVE_OTHER_CASE),
					objectInstance);
		}
		if (dest != null) {
			instanceService.attach(dest, new Operation(ObjectActionTypeConstants.ATTACH_OBJECT),
					objectInstance);
		}
		return true;
	}

	@Override
	public void attach(ObjectInstance targetInstance, Operation operation, Instance... children) {
		List<Instance> list = instanceDao.attach(targetInstance, operation, children);
		InstanceEventProvider<Instance> eventProvider = serviceRegister
				.getEventProvider(targetInstance);
		for (Instance instance : list) {
			InstanceAttachedEvent<Instance> event = eventProvider.createAttachEvent(targetInstance,
					instance);
			eventService.fire(event);
		}
	}

	@Override
	public void detach(ObjectInstance sourceInstance, Operation operation, Instance... instances) {
		List<Instance> list = instanceDao.detach(sourceInstance, operation, instances);
		InstanceEventProvider<Instance> eventProvider = serviceRegister
				.getEventProvider(sourceInstance);
		for (Instance instance : list) {
			InstanceDetachedEvent<Instance> event = eventProvider.createDetachEvent(sourceInstance,
					instance);
			eventService.fire(event);
		}
	}

}
