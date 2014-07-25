package com.sirma.itt.cmf.services.impl.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionRef;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionTemplate;
import com.sirma.itt.cmf.beans.definitions.SectionDefinition;
import com.sirma.itt.cmf.beans.definitions.impl.CaseDefinitionImpl;
import com.sirma.itt.cmf.beans.definitions.impl.DocumentDefinitionRefImpl;
import com.sirma.itt.cmf.beans.definitions.impl.DocumentDefinitionRefProxy;
import com.sirma.itt.cmf.beans.definitions.impl.SectionDefinitionImpl;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.emf.cache.CacheConfiguration;
import com.sirma.itt.emf.cache.Eviction;
import com.sirma.itt.emf.db.DbDao;
import com.sirma.itt.emf.definition.DefinitionIdentityUtil;
import com.sirma.itt.emf.definition.dao.CommonDefinitionAccessor;
import com.sirma.itt.emf.definition.dao.DefinitionAccessor;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.Identity;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.util.Documentation;
import com.sirma.itt.emf.util.PathHelper;

/**
 * Implementation of the interface {@link DefinitionAccessor} that handles the case definitions and
 * case instances.
 * 
 * @author BBonev
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CaseDefinitionAccessor extends CommonDefinitionAccessor<CaseDefinition> implements
		DefinitionAccessor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 8567515787773054313L;
	/** The set of supported objects that are returned by the method {@link #getSupportedObjects()}. */
	private static final Set<Class<?>> SUPPORTED_OBJECTS;
	/** The Constant CASE_DEFINITION_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 50), doc = @Documentation(""
			+ "Cache used to contain the case definitions by definition id and revision and container. "
			+ "The cache will have an entry for every distinct definition of a loaded case that is unique for every active container(tenant) and different definition versions."
			+ "Example value expression: tenants * caseDefinitions * 10. Here 10 is the number of the different versions of a single case definition. "
			+ "If the definitions does not change that much the number could be smaller like 2-5. "
			+ "<br>Minimal value expression: tenants * caseDefinitions * 10"))
	private static final String CASE_DEFINITION_CACHE = "CASE_DEFINITION_CACHE";
	/** The Constant CASE_DEFINITION_MAX_REVISION_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 50), doc = @Documentation(""
			+ "Cache used to contain the latest case definitions. The cache will have at most an entry for every different case definition per active tenant. "
			+ "<br>Minimal value expression: tenants * caseDefinitions"))
	private static final String CASE_DEFINITION_MAX_REVISION_CACHE = "CASE_DEFINITION_MAX_REVISION_CACHE";

	@Inject
	private DbDao dbDao;

	static {
		SUPPORTED_OBJECTS = new HashSet<Class<?>>();
		SUPPORTED_OBJECTS.add(CaseDefinition.class);
		SUPPORTED_OBJECTS.add(CaseInstance.class);
		SUPPORTED_OBJECTS.add(DocumentInstance.class);
		SUPPORTED_OBJECTS.add(CaseDefinitionImpl.class);
		SUPPORTED_OBJECTS.add(SectionInstance.class);
		SUPPORTED_OBJECTS.add(DocumentDefinitionRefImpl.class);
		SUPPORTED_OBJECTS.add(SectionDefinition.class);
		SUPPORTED_OBJECTS.add(SectionDefinitionImpl.class);
	}

	@Override
	@PostConstruct
	public void initinializeCache() {
		super.initinializeCache();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Class<?>> getSupportedObjects() {
		return SUPPORTED_OBJECTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <D extends DefinitionModel> D getDefinition(Instance instance) {
		if (instance instanceof CaseInstance) {
			CaseDefinition definition = getDefinition(((CaseInstance) instance).getContainer(),
					instance.getIdentifier(), instance.getRevision());
			return (D) definition;
		} else if (instance instanceof DocumentInstance) {
			DocumentInstance documentInstance = (DocumentInstance) instance;
			if (documentInstance.isStandalone()) {
				return (D) new DocumentDefinitionRefProxy(
						dictionaryServiceInstance.get()
						.getDefinition(DocumentDefinitionTemplate.class, instance.getIdentifier(),
								instance.getRevision()));
			}
			// get the document path
			String parentPath = documentInstance.getParentPath();
			// get the case definition ID
			String root = PathHelper.extractRootPath(parentPath);
			// get the case definition for the given data
			CaseDefinition caseDefinition = getDefinition(getCurrentContainer(), root,
					instance.getRevision());
			// and from the definition extract the document definition by his path
			Identity identity = PathHelper.iterateByPath(caseDefinition, parentPath);
			if (identity instanceof DocumentDefinitionRef) {
				return (D) identity;
			}
			throw new EmfRuntimeException("Invalid document instance with path " + parentPath
					+ " and revision " + instance.getRevision());
		} else if (instance instanceof SectionInstance) {
			SectionInstance sectionInstance = (SectionInstance) instance;
			String caseDefinition = PathHelper.extractRootPath(sectionInstance.getDefinitionPath());
			CaseDefinition definition = getDefinition(((SectionInstance) instance).getContainer(),
					caseDefinition, instance.getRevision());
			return (D) PathHelper.find(definition.getSectionDefinitions(),
					sectionInstance.getIdentifier());
		}
		return null;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public <E extends TopLevelDefinition> E saveDefinition(E definition) {
		return super.saveDefinition(definition, this);
	}

	@Override
	protected void updateCache(DefinitionModel definition, boolean propertiesOnly,
			boolean isMaxRevision) {
		super.updateCache(definition, propertiesOnly, isMaxRevision);
		CaseDefinition caseDefinition = (CaseDefinition) definition;
		for (SectionDefinition sectionDefinition : caseDefinition.getSectionDefinitions()) {
			injectLabelProvider(sectionDefinition);
			injectLabelProvider(sectionDefinition.getDocumentDefinitions());
		}
	}

	@Override
	protected Class<CaseDefinition> getTargetDefinition() {
		return CaseDefinition.class;
	}

	@Override
	protected String getBaseCacheName() {
		return CASE_DEFINITION_CACHE;
	}

	@Override
	protected String getMaxRevisionCacheName() {
		return CASE_DEFINITION_MAX_REVISION_CACHE;
	}

	@Override
	public Set<String> getActiveDefinitions() {
		// REVIEW: move to named query
		List<Object[]> list = dbDao
				.fetch("select c.caseDefinitionId, c.container from CaseEntity c group by c.caseDefinitionId, c.container",
						new ArrayList<Pair<String, Object>>(0));
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		for (Object[] objects : list) {
			String definitionId = DefinitionIdentityUtil.createDefinitionId((String) objects[0],
					(String) objects[1]);
			set.add(definitionId);
		}
		return set;
	}

}
