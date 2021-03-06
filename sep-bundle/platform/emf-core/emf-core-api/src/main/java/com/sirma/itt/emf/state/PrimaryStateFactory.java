package com.sirma.itt.emf.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.context.ApplicationScoped;

/**
 * A factory for creating PrimaryState objects.
 * 
 * @author BBonev
 */
@ApplicationScoped
public class PrimaryStateFactory {

	/** The mapping. */
	protected Map<String, PrimaryStateType> mapping = new LinkedHashMap<>(50);
	/** The lock used to synchronize read/write to the cache. */
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Creates simple implementations for the given key and stores the created objects in memory to
	 * reuse them.
	 * 
	 * @param key
	 *            the key
	 * @return the primary state type
	 */
	public PrimaryStateType create(String key) {
		if (key == null) {
			return create(PrimaryStateType.INITIAL);
		}
		PrimaryStateType type = getFromCache(key);
		if (type == null) {
			type = new DefaultPrimaryStateTypeImpl(key);
			addToCache(key, type);
		}
		return type;
	}

	/**
	 * Adds the to cache.
	 * 
	 * @param key
	 *            the key
	 * @param type
	 *            the type
	 */
	protected void addToCache(String key, PrimaryStateType type) {
		lock.writeLock().lock();
		try {
			mapping.put(key, type);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Gets the from cache.
	 * 
	 * @param key
	 *            the key
	 * @return the from cache
	 */
	protected PrimaryStateType getFromCache(String key) {
		lock.readLock().lock();
		try {
			return mapping.get(key);
		} finally {
			lock.readLock().unlock();
		}
	}
}
