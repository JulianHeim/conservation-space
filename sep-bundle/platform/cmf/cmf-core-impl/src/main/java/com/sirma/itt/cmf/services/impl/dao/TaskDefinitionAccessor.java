package com.sirma.itt.cmf.services.impl.dao;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.sirma.itt.cmf.beans.definitions.TaskDefinition;
import com.sirma.itt.cmf.beans.definitions.impl.TaskDefinitionImpl;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.emf.cache.CacheConfiguration;
import com.sirma.itt.emf.cache.Eviction;
import com.sirma.itt.emf.db.DbDao;
import com.sirma.itt.emf.definition.dao.CommonDefinitionAccessor;
import com.sirma.itt.emf.definition.dao.DefinitionAccessor;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.util.Documentation;

/**
 * Implementation of the interface {@link DefinitionAccessor} that handles the task definitions
 *
 * @author BBonev
 */
@Stateless
public class TaskDefinitionAccessor extends CommonDefinitionAccessor<TaskDefinition> implements
		DefinitionAccessor {
	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -1769831948229567951L;

	/** The Constant TASK_DEFINITION_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 100), doc = @Documentation(""
			+ "Cache used to contain the standalone task definitions by definition id and revision and container. "
			+ "The cache will have an entry for every distinct definition of a loaded standalone that is unique for every active container(tenant) and different definition versions."
			+ "Example value expression: tenants * taskDefinitions * 10. Here 10 is the number of the different versions of a single task definition. "
			+ "If the definitions does not change that much the number could be smaller like 2-5. "
			+ "<br>Minimal value expression: tenants * taskDefinitions * 10"))
	private static final String TASK_DEFINITION_CACHE = "TASK_DEFINITION_CACHE";

	/** The Constant TASK_DEFINITION_MAX_REVISION_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 100), doc = @Documentation(""
			+ "Cache used to contain the latest standalone task definitions. The cache will have at most an entry for every different task definition per active tenant. "
			+ "<br>Minimal value expression: tenants * taskDefinitions"))
	private static final String TASK_DEFINITION_MAX_REVISION_CACHE = "TASK_DEFINITION_MAX_REVISION_CACHE";

	/** The Constant SUPPORTED_OBJECTS. */
	private static final Set<Class<?>> SUPPORTED_OBJECTS;

	@Inject
	private DbDao dbDao;

	static {
		SUPPORTED_OBJECTS = new HashSet<Class<?>>();
		SUPPORTED_OBJECTS.add(TaskDefinition.class);
		SUPPORTED_OBJECTS.add(TaskDefinitionImpl.class);
		SUPPORTED_OBJECTS.add(StandaloneTaskInstance.class);
	}

	/**
	 * Inits the.
	 */
	@Override
	@PostConstruct
	public void initinializeCache() {
		super.initinializeCache();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Set<Class<?>> getSupportedObjects() {
		return SUPPORTED_OBJECTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public <E extends DefinitionModel> E getDefinition(Instance instance) {
		if ((instance instanceof StandaloneTaskInstance) && (instance.getRevision() > 0)) {
			DefinitionModel definition = getDefinition(
					((AbstractTaskInstance) instance).getContainer(), instance.getIdentifier(),
					instance.getRevision());
			return (E) definition;
		}
		return null;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public <E extends TopLevelDefinition> E saveDefinition(E definition) {
		return super.saveDefinition(definition, this);
	}

	@Override
	public Set<String> getActiveDefinitions() {
		return Collections.emptySet();
	}

	@Override
	protected Class<TaskDefinition> getTargetDefinition() {
		return TaskDefinition.class;
	}

	@Override
	protected String getBaseCacheName() {
		return TASK_DEFINITION_CACHE;
	}

	@Override
	protected String getMaxRevisionCacheName() {
		return TASK_DEFINITION_MAX_REVISION_CACHE;
	}

}
