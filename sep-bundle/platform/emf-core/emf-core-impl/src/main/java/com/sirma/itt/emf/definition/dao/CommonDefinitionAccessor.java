package com.sirma.itt.emf.definition.dao;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.sirma.itt.emf.cache.lookup.EntityLookupCache;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache.EntityLookupCallbackDAO;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache.EntityLookupCallbackDAOAdaptor;
import com.sirma.itt.emf.db.EmfQueries;
import com.sirma.itt.emf.definition.DefinitionIdentityUtil;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.Triplet;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.exceptions.DictionaryException;
import com.sirma.itt.emf.exceptions.EmfConfigurationException;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.hash.HashCalculator;

/**
 * Base abstract class that implements common functions among top level definitions.
 * 
 * @param <T>
 *            the generic type
 * @author BBonev
 */
public abstract class CommonDefinitionAccessor<T extends TopLevelDefinition> extends
		BaseDefinitionAccessor implements DefinitionAccessor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -8779876082756515372L;

	@Inject
	protected HashCalculator hashCalculator;

	/**
	 * Initialize the accessor cache.
	 */
	@PostConstruct
	public void initinializeCache() {
		if (!cacheContext.containsCache(getBaseCacheName())
				|| (cacheContext.getCache(getBaseCacheName()) == null)) {
			cacheContext.createCache(getBaseCacheName(), getBaseCacheCallback());
		}
		if ((getMaxRevisionCacheName() != null)
				&& (!cacheContext.containsCache(getMaxRevisionCacheName()) || (cacheContext
						.getCache(getMaxRevisionCacheName()) == null))) {
			cacheContext.createCache(getMaxRevisionCacheName(), getMaxRevisionCacheCallback());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends DefinitionModel> List<E> getAllDefinitions(String container) {
		DataTypeDefinition typeDefinition = getDataTypeDefinition(getTargetDefinition());
		List<E> result = getAllDefinitionsInternal(container, typeDefinition);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E extends DefinitionModel> E getDefinition(String container, String defId) {
		Pair<String, String> parsed = getDefinitionPair(container, defId);
		if (parsed == null) {
			return null;
		}
		// ensure max revision selection
		Pair<Pair<String, String>, T> pair = getMaxRevisionCache().getByKey(parsed);
		return (E) getCacheValue(pair);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E extends DefinitionModel> E getDefinition(String container, String defId, Long version) {
		if (version == null) {
			// return the max version definition
			LOGGER.warn("Requested definition for concrete revision but was null."
					+ " Returning the max revision of the definition!");
			return getDefinition(container, defId);
		}
		EntityLookupCache<Triplet<String, Long, String>, T, Serializable> lookupCache = getDefinitionCache();
		Pair<String, String> parsed = getDefinitionPair(container, defId);
		if (parsed == null) {
			return null;
		}
		Pair<Triplet<String, Long, String>, T> pair = lookupCache
				.getByKey(new Triplet<String, Long, String>(parsed.getFirst(), version, parsed
						.getSecond()));
		return (E) getCacheValue(pair);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends DefinitionModel> List<E> getDefinitionVersions(String container, String defId) {
		// TODO Implement at some point of time
		return Collections.emptyList();
	}

	@Override
	public int computeHash(DefinitionModel model) {
		return hashCalculator.computeHash(model);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean removeDefinition(String definition, long version) {
		// TODO implement removal
		if (version < 0) {
		} else {
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTypeDefinition detectDataTypeDefinition(DefinitionModel topLevelDefinition) {
		if (getTargetDefinition().isInstance(topLevelDefinition)) {
			return getDataTypeDefinition(getTargetDefinition());
		}
		throw new EmfRuntimeException("Not supported object instance: "
				+ topLevelDefinition.getClass());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void updateCache(DefinitionModel definition, boolean propertiesOnly,
			boolean isMaxRevision) {
		// check the definition type
		if (!getTargetDefinition().isInstance(definition)) {
			return;
		}
		T targetDefinition = (T) definition;
		// if we want to update only the case properties
		if (!propertiesOnly) {
			if (isMaxRevision) {
				// overrides the current max revision of the case
				EntityLookupCache<Pair<String, String>, T, Serializable> maxRevisionCache = getMaxRevisionCache();
				maxRevisionCache.setValue(
						DefinitionIdentityUtil.createDefinitionPair(targetDefinition),
						targetDefinition);
			}
			EntityLookupCache<Triplet<String, Long, String>, T, Serializable> definitionCache = getDefinitionCache();
			String container = targetDefinition.getContainer();
			Triplet<String, Long, String> key = new Triplet<String, Long, String>(
					targetDefinition.getIdentifier(), targetDefinition.getRevision(), container);
			T old = definitionCache.getValue(key);
			if (old == null) {
				definitionCache.setValue(key, targetDefinition);
			}
		}

		// populate the label provider in all definitions
		injectLabelProvider(targetDefinition);
	}

	/**
	 * Getter method for caseDefinitionCache.
	 * 
	 * @return the caseDefinitionCache
	 */
	protected EntityLookupCache<Pair<String, String>, T, Serializable> getMaxRevisionCache() {
		return cacheContext.getCache(getMaxRevisionCacheName());
	}

	/**
	 * Getter method for caseDefinitionCache.
	 * 
	 * @return the caseDefinitionCache
	 */
	protected EntityLookupCache<Triplet<String, Long, String>, T, Serializable> getDefinitionCache() {
		return cacheContext.getCache(getBaseCacheName());
	}

	/**
	 * Gets the target definition.
	 * 
	 * @return the target definition
	 */
	protected abstract Class<T> getTargetDefinition();

	/**
	 * Gets the base cache name.
	 * 
	 * @return the base cache name
	 */
	protected abstract String getBaseCacheName();

	/**
	 * Gets the max revision cache name.
	 * 
	 * @return the max revision cache name
	 */
	protected abstract String getMaxRevisionCacheName();

	/**
	 * Gets the base cache callback.
	 * 
	 * @param <K>
	 *            the key type
	 * @param <VK>
	 *            the generic type
	 * @return the base cache callback
	 */
	@SuppressWarnings("unchecked")
	protected <K extends Serializable, VK extends Serializable> EntityLookupCallbackDAO<K, T, VK> getBaseCacheCallback() {
		return (EntityLookupCallbackDAO<K, T, VK>) new DefinitionLookup();
	}

	/**
	 * Gets the max revision cache callback.
	 * 
	 * @param <K>
	 *            the key type
	 * @param <VK>
	 *            the generic type
	 * @return the max revision cache callback
	 */
	@SuppressWarnings("unchecked")
	protected <K extends Serializable, VK extends Serializable> EntityLookupCallbackDAO<K, T, VK> getMaxRevisionCacheCallback() {
		return (EntityLookupCallbackDAO<K, T, VK>) new DefinitionMaxRevisionLookup();
	}

	/**
	 * Default lookup DAO class for fetching a definition by definition id, revision and container
	 * 
	 * @author BBonev
	 */
	protected class DefinitionLookup
			extends
			EntityLookupCallbackDAOAdaptor<Triplet<String, Long, String>, T, Serializable> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<Triplet<String, Long, String>, T> findByKey(
				Triplet<String, Long, String> key) {
			DataTypeDefinition type = getDataTypeDefinition(getTargetDefinition());

			List<T> resultList = getDefinitionsInternal(
					EmfQueries.QUERY_DEFINITION_BY_ID_CONTAINER_REVISION_KEY, key.getFirst(),
					key.getSecond(), key.getThird(), type, null, false);
			if (resultList.isEmpty()) {
				return null;
			}
			if (resultList.size() > 1) {
				// this should not happen or someone broke the DB on purpose
				throw new DictionaryException(
						"More then one case definition and revision found for " + key);
			}
			T result = resultList.get(0);
			// load properties into the cache
			updateCache(result, true, false);
			return new Pair<Triplet<String, Long, String>, T>(key, result);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<Triplet<String, Long, String>, T> createValue(T value) {
			throw new UnsupportedOperationException(getTargetDefinition().getName()
					+ " has external creation");
		}

	}

	/**
	 * Default lookup DAO class for fetching a definition by definition id, container and his
	 * maximum revision.
	 * 
	 * @author BBonev
	 */
	protected class DefinitionMaxRevisionLookup extends
			EntityLookupCallbackDAOAdaptor<Pair<String, String>, T, Serializable> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<Pair<String, String>, T> findByKey(Pair<String, String> key) {
			DataTypeDefinition type = getDataTypeDefinition(getTargetDefinition());
			if (type == null) {
				throw new EmfConfigurationException("No type definition found for: "
						+ getTargetDefinition());
			}
			List<T> resultList = getDefinitionsInternal(EmfQueries.QUERY_MAX_DEFINITION_BY_ID_KEY,
					key.getFirst(), null, key.getSecond(), type, null, false);
			if (resultList.isEmpty()) {
				return null;
			}
			if (resultList.size() > 1) {
				// this should not happen or someone broke the DB on purpose
				throw new DictionaryException("More then one " + getTargetDefinition().getName()
						+ " with max revision found for " + key);
			}
			T result = resultList.get(0);

			// load properties into the cache
			updateCache(result, true, true);
			return new Pair<Pair<String, String>, T>(key, result);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<Pair<String, String>, T> createValue(T value) {
			throw new UnsupportedOperationException(getTargetDefinition().getName()
					+ " has external creation");
		}

	}
}
