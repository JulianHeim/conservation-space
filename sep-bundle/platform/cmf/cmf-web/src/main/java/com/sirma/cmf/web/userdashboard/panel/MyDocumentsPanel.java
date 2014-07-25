package com.sirma.cmf.web.userdashboard.panel;

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
import com.sirma.cmf.web.userdashboard.filter.DashboardDocumentFilter;
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

/**
 * <b>MyDocumentsPanel</b> manage functionality for dashlet, located in personal/user dashboard. The
 * content is represented as document records, actions and filters.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyDocumentsPanel
		extends
		DashboardPanelActionBase<CaseInstance, SearchArgumentsMap<CaseInstance, List<DocumentInstance>>>
		implements Serializable, DashboardPanelController {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6798947303154434519L;

	/** The result map. */
	private Map<CaseInstance, List<DocumentInstance>> resultMap;

	/** The case instances. */
	private List<CaseInstance> caseInstances;

	/** The document filters. */
	private List<SelectorItem> documentFilters;

	/** The default document filter. */
	private String defaultDocumentFilter = DashboardDocumentFilter.LAST_USED.getFilterName();

	/** The selected document filter. */
	private String selectedDocumentFilter;

	/** The available document actions. */
	private Map<Serializable, List<Action>> documentActions;

	/** The list with document instances. */
	private List<DocumentInstance> documents;

	/** Placeholder for document actions. */
	private static final String DOCUMENT_DASHLET_ACTIONS_PLACEHOLDER = "dashboard-document";

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
	public void loadFilters() {
		List<SelectorItem> filters = new ArrayList<SelectorItem>();
		DashboardDocumentFilter[] filterTypes = DashboardDocumentFilter.values();

		for (DashboardDocumentFilter dashboardDocumentFilter : filterTypes) {
			String filterName = dashboardDocumentFilter.getFilterName();
			filters.add(new SelectorItem(filterName, labelProvider
					.getValue(DashboardDocumentFilter.CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF
							+ filterName), ""));
		}

		documentFilters = filters;
		selectedDocumentFilter = defaultDocumentFilter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		// fetch results
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArgumentsMap<CaseInstance, List<DocumentInstance>> searchArguments) {
		log.debug("CMFWeb: Executing MyDocumentsPanel.filter: searchArguments " + searchArguments);
		searchService.search(DocumentInstance.class, searchArguments);
		resultMap = searchArguments.getResultMap();
		caseInstances = new ArrayList<CaseInstance>(resultMap.keySet());
		documents = documentUtil.extractCaseDocuments(caseInstances, resultMap);
		setDocumentActions(getActionsForInstances(documents, DOCUMENT_DASHLET_ACTIONS_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		selectedDocumentFilter = selectedItemId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArgumentsMap<CaseInstance, List<DocumentInstance>> getSearchArguments() {
		SearchArgumentsMap<CaseInstance, List<DocumentInstance>> searchArguments = null;
		Context<String, Object> context = new Context<>(1);
		context.put(SearchFilterProperties.USER_ID, userId);

		if (DashboardDocumentFilter.I_AM_EDITING.getFilterName().equals(selectedDocumentFilter)) {
			searchArguments = searchService.getFilter("listLockedDocumentsFromUser",
					CaseInstance.class, context);
		} else if (DashboardDocumentFilter.LAST_USED.getFilterName().equals(selectedDocumentFilter)) {
			searchArguments = searchService.getFilter("listEditedDocumentsByUser",
					CaseInstance.class, context);
		}

		return searchArguments;
	}

	/**
	 * Gets the document filter activator link label.
	 * 
	 * @return the document filter activator link label
	 */
	public String getDocumentFilterActivatorLinkLabel() {
		String filterType = defaultDocumentFilter;

		if (selectedDocumentFilter != null) {
			filterType = selectedDocumentFilter;
		}

		return labelProvider
				.getValue(DashboardDocumentFilter.CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF
						+ filterType);
	}

	/**
	 * Getter method for documentFilters.
	 * 
	 * @return the documentFilters
	 */
	public List<SelectorItem> getDocumentFilters() {
		return documentFilters;
	}

	/**
	 * Setter method for documentFilters.
	 * 
	 * @param documentFilters
	 *            the documentFilters to set
	 */
	public void setDocumentFilters(List<SelectorItem> documentFilters) {
		this.documentFilters = documentFilters;
	}

	/**
	 * Getter method for defaultDocumentFilter.
	 * 
	 * @return the defaultDocumentFilter
	 */
	public String getDefaultDocumentFilter() {
		return defaultDocumentFilter;
	}

	/**
	 * Setter method for defaultDocumentFilter.
	 * 
	 * @param defaultDocumentFilter
	 *            the defaultDocumentFilter to set
	 */
	public void setDefaultDocumentFilter(String defaultDocumentFilter) {
		this.defaultDocumentFilter = defaultDocumentFilter;
	}

	/**
	 * Getter method for selectedDocumentFilter.
	 * 
	 * @return the selectedDocumentFilter
	 */
	public String getSelectedDocumentFilter() {
		return selectedDocumentFilter;
	}

	/**
	 * Setter method for selectedDocumentFilter.
	 * 
	 * @param selectedDocumentFilter
	 *            the selectedDocumentFilter to set
	 */
	public void setSelectedDocumentFilter(String selectedDocumentFilter) {
		this.selectedDocumentFilter = selectedDocumentFilter;
	}

	/**
	 * Getter method for resultMap.
	 * 
	 * @return the resultMap
	 */
	public Map<CaseInstance, List<DocumentInstance>> getResultMap() {
		return resultMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Getter for available actions based on instance.
	 * 
	 * @param instance
	 *            current instance
	 * @return list with available actions
	 */
	public List<Action> getDocumentActions(Instance instance) {
		if ((instance == null) || (documentActions == null)) {
			return new ArrayList<Action>();
		}
		return documentActions.get(instance.getId());
	}

	/**
	 * Setter for document document actions.
	 * 
	 * @param documentActions
	 *            available document actions
	 */
	public void setDocumentActions(Map<Serializable, List<Action>> documentActions) {
		this.documentActions = documentActions;
	}

	/**
	 * Getter for list with document instances.
	 * 
	 * @return list with document instances
	 */
	public List<DocumentInstance> getDocuments() {
		waitForDataToLoad();
		return documents;
	}

}
