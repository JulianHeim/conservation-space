package com.sirma.itt.emf.db;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.event.instance.EntityPersistedEvent;
import com.sirma.itt.emf.instance.model.Instance;

/**
 * Generate unique identifiers for persisting in databases.
 *
 * @author BBonev
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
@javax.ejb.Lock(LockType.READ)
public class SequenceEntityGenerator {

	/** The lock. REVIEW:BB probably could be implemented with RWLock. */
	private static Lock lock = new ReentrantLock();

	/** The list of all generated IDs that are tracked for persistence. */
	private static Set<Serializable> generatedIds = new LinkedHashSet<>(1024);

	/** A place for ids that has been persisted before transaction completion. */
	private static Set<Serializable> toPersistIds = new LinkedHashSet<>(1024);

	/** The generator. */
	private static DbIdGenerator generator;

	/** The id generator. */
	@Inject
	private DbIdGenerator idGenerator;

	/**
	 * Initialize.
	 */
	@PostConstruct
	public void initialize() {
		generator = idGenerator;
	}

	/**
	 * Generate an id that is optionally tracked if persisted or not.
	 * 
	 * @param track
	 *            if the id should be tracked or not.
	 * @return the generated db id.
	 */
	public Serializable generate(boolean track) {
		return generateIdInternal(track);
	}

	/**
	 * Generate an id that is not tracked if persisted or not.
	 * 
	 * @return the generated db id.
	 */
	public static Serializable generateId() {
		return generateIdInternal(false);
	}

	/**
	 * Generate an id that is tracked or not if persisted or not.
	 * 
	 * @param track
	 *            if the id should be tracked or not.
	 * @return the generated db id.
	 */
	public static Serializable generateId(boolean track) {
		return generateIdInternal(track);
	}

	/**
	 * Generate an id for the given entity if an id has not been set already.
	 * 
	 * @param <I>
	 *            the generic ID type
	 * @param <E>
	 *            the concrete entity type
	 * @param entity
	 *            the entity to set the id
	 * @param toKeepTrack
	 *            if the instance should keep track of the generated id if persisted or not. If the
	 *            Id is generated without tracking the method {@link #isPersisted(Entity)} will
	 *            return <code>true</code> always no matter if the entity was persisted or not.
	 * @return the updated entity (same reference as the argument)
	 */
	@SuppressWarnings("unchecked")
	public static <I extends Serializable, E extends Entity<I>> E generateStringId(E entity,
			boolean toKeepTrack) {
		if ((entity != null) && (entity.getId() == null)) {
			entity.setId((I) generateIdInternal(toKeepTrack));
		}
		return entity;
	}

	/**
	 * Generate id internal.
	 *
	 * @param toKeepTrack
	 *            the to keep track
	 * @return the string
	 */
	private static Serializable generateIdInternal(boolean toKeepTrack) {
		lock.lock();
		try {
			Serializable id = generator.generateId().toString();
			if (toKeepTrack) {
				generatedIds.add(id);
			}
			return id;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Checks if the given ID is persisted. The method will return <code>true</code> for IDs that
	 * are tracked by the generator and is known to be persisted (after receiving the persist
	 * event). Passing a long value to the method will result in <code>true</code> as output. For
	 * any ID that is not tracked the by class a positive response will be returned.
	 * 
	 * @param id
	 *            the id to check
	 * @return true, if is persisted and <code>false</code> if tracked and is known for not
	 *         persisted or <code>null</code> value.
	 */
	public static boolean isIdPersisted(Serializable id) {
		if (id == null) {
			return false;
		}
		// all entities that have a long ID and that id is present are considered for persisted
		if (id instanceof Long) {
			return true;
		}
		lock.lock();
		try {
			boolean inPersisted = toPersistIds.contains(id);
			boolean notInGenerated = !generatedIds.contains(id);
			return inPersisted || notInGenerated;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Checks if the given entity has been persisted.
	 *
	 * @param <S>
	 *            the ID type
	 * @param entity
	 *            the entity
	 * @return <code>true</code>, if is persisted and <code>false</code> if not yet.
	 */
	public static <S extends Serializable> boolean isPersisted(Entity<S> entity) {
		if ((entity == null) || (entity.getId() == null)) {
			return false;
		}
		return isIdPersisted(entity.getId());
	}

	/**
	 * Marks the given entity as persisted.
	 *
	 * @param <S>
	 *            the generic type
	 * @param entity
	 *            the entity
	 * @return <code>true</code>, if the entity successfully has been marked as persisted and
	 *         <code>false</code> if something got wrong ( the entity was invalid or the entity
	 *         already has been persisted.
	 */
	public static <S extends Serializable> boolean persisted(Entity<S> entity) {
		if ((entity == null) || (entity.getId() == null)) {
			return false;
		}
		lock.lock();
		try {
			if (generatedIds.remove(entity.getId())) {
				toPersistIds.add(entity.getId());
				return true;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	/**
	 * Register the entity that is going to be persisted or the entity ID was generated externally.
	 * <b>NOTE: </b> After registration the methods {@link #isPersisted(Entity)} and
	 * {@link #isIdPersisted(Serializable)} will return <code>false</code> for the id that is set to
	 * the entity.
	 * 
	 * @param <S>
	 *            the generic type
	 * @param entity
	 *            the entity
	 */
	public static <S extends Serializable> void register(Entity<S> entity) {
		if ((entity != null) && (entity.getId() != null)) {
			lock.lock();
			try {
				generatedIds.add(entity.getId());
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Register an ID that is going to be persisted or ID was generated externally or not tracked
	 * initially.<br>
	 * <b>NOTE: </b> After registration the methods {@link #isPersisted(Entity)} and
	 * {@link #isIdPersisted(Serializable)} will return <code>false</code> for the same id.
	 * 
	 * @param id
	 *            the entity
	 */
	public static void registerId(Serializable id) {
		if (id != null) {
			lock.lock();
			try {
				generatedIds.add(id);
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * On entity persist failure.
	 * 
	 * @param <E>
	 *            the element type
	 * @param <S>
	 *            the generic type
	 * @param event
	 *            the event
	 */
	public <E extends Entity<S>, S extends Serializable> void onEntityPersistFailure(
			@Observes(during = TransactionPhase.AFTER_FAILURE) EntityPersistedEvent<E, S> event) {
		E instance = event.getInstance();
		lock.lock();
		try {
			toPersistIds.remove(instance.getId());
			generatedIds.add(instance.getId());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * On entity persist failure.
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstancePersistFailure(
			@Observes(during = TransactionPhase.AFTER_FAILURE) EntityPersistedEvent<Instance, Serializable> event) {
		Instance instance = event.getInstance();
		lock.lock();
		try {
			toPersistIds.remove(instance.getId());
			generatedIds.add(instance.getId());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * On entity persist success.
	 * 
	 * @param <E>
	 *            the element type
	 * @param <S>
	 *            the generic type
	 * @param event
	 *            the event
	 */
	public <E extends Entity<S>, S extends Serializable> void onEntityPersistSuccess(
			@Observes(during = TransactionPhase.AFTER_SUCCESS) EntityPersistedEvent<E, S> event) {
		E instance = event.getInstance();
		lock.lock();
		try {
			toPersistIds.remove(instance.getId());
			generatedIds.remove(instance.getId());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * On entity persist success.
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstancePersistSuccess(
			@Observes(during = TransactionPhase.AFTER_SUCCESS) EntityPersistedEvent<Instance, Serializable> event) {
		Instance instance = event.getInstance();
		lock.lock();
		try {
			toPersistIds.remove(instance.getId());
			generatedIds.remove(instance.getId());
		} finally {
			lock.unlock();
		}
	}

}
