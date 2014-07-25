package com.sirma.itt.pm.web.project.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.cmf.web.userdashboard.filter.DashboardDateFilter;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.constants.CaseProperties;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.Query;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.constants.ProjectProperties;
import com.sirma.itt.pm.domain.model.ProjectInstance;
import com.sirma.itt.pm.security.PmActionTypeConstants;
import com.sirma.itt.pm.web.project.dashboard.filters.DashboardCaseFilter;

/**
 * <b>ProjectCasePanel</b> manage functionality for dashlet, located in project dashboard. The
 * content is represented as case records, actions and filters.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectCasePanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	/** The serial version identifier. */
	private static final long serialVersionUID = 4701197889327503706L;

	/** Current dashlet actions(in the toolbar). */
	private static final Set<String> dashletActions = new HashSet<String>(
			Arrays.asList(PmActionTypeConstants.CREATE_CASE));

	/** The list with project case filters. */
	private List<SelectorItem> projectCaseFilters;

	/** The list with project case - date filter. */
	private List<SelectorItem> projectDateFilters;

	/** The selected case filter. */
	private String selectedCaseFilter;

	/** The selected date filter. */
	private String selectedDateFilter;

	/** The default date filter. */
	private String defaultDateFilter = DashboardDateFilter.LAST_WEEK.getFilterName();

	/** The default case filter. */
	private String defaultCaseFilter = DashboardCaseFilter.ALL_CASES.getFilterName();

	/** Project instances list. */
	private List<CaseInstance> projectCasesList;

	/** The available case actions. */
	private Map<Serializable, List<Action>> caseRecordActions;

	/** The context instance(project) */
	private ProjectInstance context;

	/** Dashlet cases actions. */
	private static final String PROJECT_CASE_ACTIONS_PLACEHOLDER = "project-dashboard-cases";

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
	 * This method loads all case filters for project case dashlet.
	 */
	private void loadProjectCaseFilter() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardCaseFilter[] filterTypes = DashboardCaseFilter.values();

		for (DashboardCaseFilter dashboardCaseFilter : filterTypes) {

			String filterName = dashboardCaseFilter.getFilterName();

			filters.add(new SelectorItem(filterName, labelProvider
					.getValue(DashboardCaseFilter.PM_PROJECT_DASHBOARD_CASE_FILTER_PREF
							+ filterName), ""));
		}
		projectCaseFilters = filters;
		selectedCaseFilter = defaultCaseFilter;
	}

	/**
	 * This method loads all date filters into the project case dashlet.
	 */
	private void loadProjectDateFilter() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardDateFilter[] filterTypes = DashboardDateFilter.values();

		for (DashboardDateFilter dashboardDateFilter : filterTypes) {

			String filterName = dashboardDateFilter.getFilterName();

			filters.add(new SelectorItem(filterName,
					labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
							+ filterName), ""));
		}

		projectDateFilters = filters;
		selectedDateFilter = defaultDateFilter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		loadProjectCaseFilter();
		loadProjectDateFilter();
	}

	/**
	 * Getter for the case filter activator link label.
	 * 
	 * @return the case filter activator link label
	 */
	public String getCaseActivatorLinkLabel() {
		String filterType = defaultCaseFilter;

		if (selectedCaseFilter != null) {
			filterType = selectedCaseFilter;
		}

		return labelProvider.getValue(DashboardCaseFilter.PM_PROJECT_DASHBOARD_CASE_FILTER_PREF
				+ filterType);
	}

	/**
	 * Gets the date filter activator link label.
	 * 
	 * @return the date filter activator link label
	 */
	public String getDateActivatorLinkLabel() {
		String filterType = defaultDateFilter;

		if (selectedDateFilter != null) {
			filterType = selectedDateFilter;
		}

		return labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
				+ filterType);
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
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		searchService.search(CaseInstance.class, searchArguments);
		setProjectCasesList(searchArguments.getResult());
		setCaseRecordActions(getActionsForInstances(projectCasesList,
				PROJECT_CASE_ACTIONS_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		log.debug("PMWeb: Executing ProjectCaseFilter.updateSelectedFilterField [" + selectedItemId
				+ "]");

		DashboardCaseFilter casefilters = DashboardCaseFilter.getFilterType(selectedItemId);

		DashboardDateFilter datefilters = DashboardDateFilter.getFilterType(selectedItemId);

		if (casefilters != null) {

			selectedCaseFilter = selectedItemId;

		} else if (datefilters != null) {
			selectedDateFilter = selectedItemId;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		SearchArguments<CaseInstance> searchArguments = null;
		Context<String, Object> searchContext = new Context<String, Object>(2);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT, context);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
				ProjectProperties.OWNED_INSTANCES);
		searchContext.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());

		if (DashboardCaseFilter.ALL_CASES.getFilterName().equals(selectedCaseFilter)) {
			searchArguments = searchService.getFilter("listAllCaseInstances", CaseInstance.class,
					searchContext);
		} else if (DashboardCaseFilter.ACTIVE_CASES.getFilterName().equals(selectedCaseFilter)) {
			searchArguments = searchService.getFilter("listActiveCaseInstances",
					CaseInstance.class, searchContext);
		}
		return searchArguments;
	}

	/**
	 * Generate range query on the specified filter gap for creation or modification date.
	 * 
	 * @return the created query.
	 */
	private Query getDateRangeQuery() {
		DateRange dateRange = DashboardDateFilter.getDateRange(selectedDateFilter);
		return new Query(CaseProperties.CREATED_ON, dateRange).or(CaseProperties.MODIFIED_ON,
				dateRange);
	}

	/**
	 * Getter method for projectCaseFilter.
	 * 
	 * @return project case filters
	 */
	public List<SelectorItem> getProjectCaseFilters() {
		return projectCaseFilters;
	}

	/**
	 * Setter method for project case filters.
	 * 
	 * @param projectCaseFilters
	 *            project case filters
	 */
	public void setProjectCaseFilters(List<SelectorItem> projectCaseFilters) {
		this.projectCaseFilters = projectCaseFilters;
	}

	/**
	 * Getter method for project date filters.
	 * 
	 * @return projects date filters
	 */
	public List<SelectorItem> getProjectDateFilters() {
		return projectDateFilters;
	}

	/**
	 * Setter method for project date filters.
	 * 
	 * @param projectDateFilters
	 *            project date filters
	 */
	public void setProjectDateFilters(List<SelectorItem> projectDateFilters) {
		this.projectDateFilters = projectDateFilters;
	}

	/**
	 * Retrieve currently selected filter(case filter).
	 * 
	 * @return selected project case filter
	 */
	public String getSelectedCaseFilter() {
		return selectedCaseFilter;
	}

	/**
	 * Setter method for currently selected case filter.
	 * 
	 * @param selectedCaseFilter
	 *            selected project case filter
	 */
	public void setSelectedCaseFilter(String selectedCaseFilter) {
		this.selectedCaseFilter = selectedCaseFilter;
	}

	/**
	 * Getter method for selected date filter.
	 * 
	 * @return selected project date filter
	 */
	public String getSelectedDateFilter() {
		return selectedDateFilter;
	}

	/**
	 * Setter method for currently selected date filter.
	 * 
	 * @param selectedDateFilter
	 *            selected date filter
	 */
	public void setSelectedDateFilter(String selectedDateFilter) {
		this.selectedDateFilter = selectedDateFilter;
	}

	/**
	 * Getter method for default date filter.
	 * 
	 * @return project date filter
	 */
	public String getDefaultDateFilter() {
		return defaultDateFilter;
	}

	/**
	 * Setter method for default date filter.
	 * 
	 * @param defaultDateFilter
	 *            default date filter
	 */
	public void setDefaultDateFilter(String defaultDateFilter) {
		this.defaultDateFilter = defaultDateFilter;
	}

	/**
	 * Getter method for default project case filter.
	 * 
	 * @return default case filter
	 */
	public String getDefaultCaseFilter() {
		return defaultCaseFilter;
	}

	/**
	 * Setter method for default project case filter.
	 * 
	 * @param defaultCaseFilter
	 *            default case filter
	 */
	public void setDefaultCaseFilter(String defaultCaseFilter) {
		this.defaultCaseFilter = defaultCaseFilter;
	}

	/**
	 * @return the projectCasesList
	 */
	public List<CaseInstance> getProjectCasesList() {
		waitForDataToLoad();
		return projectCasesList;
	}

	/**
	 * @param projectCasesList
	 *            the projectCasesList to set
	 */
	public void setProjectCasesList(List<CaseInstance> projectCasesList) {
		this.projectCasesList = projectCasesList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		return dashletActions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return "project-cases-dashlet";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

	/**
	 * Getter for case actions for current record in the dashlet.
	 * 
	 * @param instance
	 *            current record instance
	 * @return list with available actions
	 */
	public List<Action> getCaseRecordActions(Instance instance) {

		if ((instance == null) || (caseRecordActions == null)) {
			return new ArrayList<Action>();
		}

		return this.caseRecordActions.get(instance.getId());
	}

	/**
	 * Setter for case record actions.
	 * 
	 * @param caseRecordActions
	 *            current case actions
	 */
	public void setCaseRecordActions(Map<Serializable, List<Action>> caseRecordActions) {
		this.caseRecordActions = caseRecordActions;
	}

}
