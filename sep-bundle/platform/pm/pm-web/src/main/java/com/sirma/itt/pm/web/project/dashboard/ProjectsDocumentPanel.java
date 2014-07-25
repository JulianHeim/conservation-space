package com.sirma.itt.pm.web.project.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.document.DocumentUtil;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArgumentsMap;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.constants.ProjectProperties;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * <b>ProjectsDocumentPanel</b> manage functionality for dashlet, located in project dashboard. The
 * content is represented as document records, actions and filters.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectsDocumentPanel
		extends
		DashboardPanelActionBase<CaseInstance, SearchArgumentsMap<CaseInstance, List<DocumentInstance>>>
		implements Serializable, DashboardPanelController {

	/** The Constant serial version identifier. */
	private static final long serialVersionUID = 8421197115069895908L;

	/** The document filters. */
	private List<SelectorItem> projectDocumentFilters;

	/** The case instances. */
	private List<CaseInstance> caseInstances;

	/** The result map. */
	private Map<CaseInstance, List<DocumentInstance>> resultMap;

	/** The available document actions. */
	private Map<Serializable, List<Action>> documentActions;

	/** The available document instances. */
	private List<DocumentInstance> documentInstances;

	private static final String PROJECT_DOCUMENT_DASHLET_PLACEHOLDER = "project-dashboard-document";

	/** The project instance extracted from the context. */
	private ProjectInstance context;

	/** The search service. */
	@Inject
	private SearchService searchService;

	/** Holds command logic that is used in document and media panels. */
	@Inject
	private DocumentUtil documentUtil;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initData() {
		onOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		context = getDocumentContext().getInstance(ProjectInstance.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		// TODO: Removing filters - need to clean the code.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArgumentsMap<CaseInstance, List<DocumentInstance>> searchArguments) {
		searchService.search(DocumentInstance.class, searchArguments);
		resultMap = searchArguments.getResultMap();
		caseInstances = new ArrayList<CaseInstance>(resultMap.keySet());
		documentInstances = documentUtil.extractCaseDocuments(caseInstances, resultMap);
		setDocumentActions(getActionsForInstances(documentInstances,
				PROJECT_DOCUMENT_DASHLET_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArgumentsMap<CaseInstance, List<DocumentInstance>> getSearchArguments() {
		SearchArgumentsMap<CaseInstance, List<DocumentInstance>> searchArguments = null;

		Context<String, Object> searchContext = new Context<String, Object>(2);
		searchContext.put(SearchFilterProperties.USER_ID, userId);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
				ProjectProperties.OWNED_INSTANCES);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT, context);

		searchArguments = searchService.getFilter("listLastModifiedDocuments", CaseInstance.class,
				searchContext);

		return searchArguments;
	}

	/**
	 * Getter for all case and document instances
	 * 
	 * @return map with case and documents
	 */
	public Map<CaseInstance, List<DocumentInstance>> getResultMap() {
		return resultMap;
	}

	/**
	 * Setter for all case and document instances
	 * 
	 * @param resultMap
	 *            case and documents
	 */
	public void setResultMap(Map<CaseInstance, List<DocumentInstance>> resultMap) {
		this.resultMap = resultMap;
	}

	/**
	 * Getter for all project document filters
	 * 
	 * @return list with available project document filters
	 */
	public List<SelectorItem> getProjectDocumentFilters() {
		return projectDocumentFilters;
	}

	/**
	 * Setter for all project document filters
	 * 
	 * @param projectDocumentFilters
	 *            project document filters
	 */
	public void setProjectDocumentFilters(List<SelectorItem> projectDocumentFilters) {
		this.projectDocumentFilters = projectDocumentFilters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

	/**
	 * Setter method for document actions.
	 * 
	 * @param documentActions
	 *            document actions
	 */
	public void setDocumentActions(Map<Serializable, List<Action>> documentActions) {
		this.documentActions = documentActions;
	}

	/**
	 * Getter method for document action based on current document.
	 * 
	 * @param instance
	 *            current document instance
	 * @return document actions
	 */
	public List<Action> getDocumentActions(Instance instance) {
		if (instance == null || documentActions == null) {
			return new ArrayList<Action>();
		}
		return documentActions.get(instance.getId());
	}

	/**
	 * Getter method for document instances list.
	 * 
	 * @return document instance list
	 */
	public List<DocumentInstance> getDocumentInstances() {
		waitForDataToLoad();
		return documentInstances;
	}

	/**
	 * Setter method for document instance list.
	 * 
	 * @param documentInstances
	 *            document instance list
	 */
	public void setDocumentInstances(List<DocumentInstance> documentInstances) {
		this.documentInstances = documentInstances;
	}

}
