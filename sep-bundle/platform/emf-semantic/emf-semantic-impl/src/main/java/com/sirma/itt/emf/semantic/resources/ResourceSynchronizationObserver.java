package com.sirma.itt.emf.semantic.resources;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.emf.db.DbDao;
import com.sirma.itt.emf.db.SemanticDb;
import com.sirma.itt.emf.instance.PropertiesUtil;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.resources.event.ResourceAddedEvent;
import com.sirma.itt.emf.resources.event.ResourceSynchronizationEvent;
import com.sirma.itt.emf.resources.event.ResourceUpdatedEvent;
import com.sirma.itt.emf.resources.model.Resource;

/**
 * Observer that listens for new resources or modifications to update the semantic database.
 *
 * @author BBonev
 */
@Stateless
public class ResourceSynchronizationObserver {

	/** The db dao. */
	@Inject
	@SemanticDb
	private DbDao dbDao;

	/** The lock. */
	private static ReentrantLock lock = new ReentrantLock();

	@Inject
	private ResourceService resourceService;

	/**
	 * Listens for newly added resources.
	 *
	 * @param event
	 *            the event
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void onNewResource(@Observes ResourceAddedEvent event) {
		dbDao.saveOrUpdate(cleanProperties(event.getInstance()));
	}

	/**
	 * Listens for updated resources.
	 *
	 * @param event
	 *            the event
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void onResourceUpdate(@Observes ResourceUpdatedEvent event) {
		dbDao.saveOrUpdate(cleanProperties(event.getInstance()),
				cleanProperties(event.getOldInstance()));
	}

	/**
	 * Cleans null properties
	 * 
	 * @param instance
	 *            the instance
	 * @return the resource
	 */
	private Resource cleanProperties(Resource instance) {
		if (instance == null) {
			return null;
		}
		return PropertiesUtil.cleanNullProperties(instance);
	}

	/**
	 * Listens for forced resource synchronizations.
	 * 
	 * @param event
	 *            the event
	 */
	@Asynchronous
	public void onForcedSynchronization(@Observes ResourceSynchronizationEvent event) {
		if (!event.isForced()) {
			return;
		}
		if (lock.isLocked()) {
			return;
		}
		lock.lock();
		try {
			runSynchronization();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Perform the synchronization.
	 */
	private void runSynchronization() {
		List<Resource> resources = resourceService.getAllResources(ResourceType.USER, null);
		synchResourcesList(resources);
		resources = resourceService.getAllResources(ResourceType.GROUP, null);
		synchResourcesList(resources);
	}

	/**
	 * Synch resources to DB
	 * 
	 * @param resources
	 *            the resources
	 */
	private void synchResourcesList(List<Resource> resources) {
		for (Resource resource : resources) {
			dbDao.saveOrUpdate(resource);
		}
	}

}
