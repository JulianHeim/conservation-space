package com.sirma.cmf.web.userdashboard.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.cmf.web.userdashboard.filter.DashboardCaseFilter;
import com.sirma.cmf.web.userdashboard.filter.DashboardDateFilter;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.constants.CaseProperties;
import com.sirma.itt.cmf.services.adapter.CMFPermissionAdapterService;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.info.VersionInfo;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.search.Query;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * <b>MyCasesPanel</b> manage functionality for dashlet, located in personal/user dashboard. The
 * content is represented as case records, actions and filters.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyCasesPanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	/** Comment for serialVersionUID. */
	private static final long serialVersionUID = -7542862392405840580L;

	/** The case instances. */
	private List<CaseInstance> caseInstances;

	/** The case filters. */
	private List<SelectorItem> caseFilters;

	/** The date filters. */
	private List<SelectorItem> dateFilters;

	/** The selected case filter. */
	private String selectedCaseFilter;

	/** The selected date filter. */
	private String selectedDateFilter;

	/** The default case filter. */
	private String defaultCaseFilter = DashboardCaseFilter.ALL_CASES.getFilterName();

	/** The default date filter. */
	private String defaultDateFilter = DashboardDateFilter.LAST_WEEK.getFilterName();

	/** The module restriction value. */
	private boolean moduleRestriction;

	/** The available actions for case dashlet. */
	private Map<Serializable, List<Action>> caseActions;

	/** The placeholder for actions. */
	private static final String CASE_ACTIONS_PALCEHOLDER = "user-dashboard-cases";

	/** The versions info. */
	@Inject
	@ExtensionPoint(value = VersionInfo.TARGET_NAME)
	private Iterable<VersionInfo> versionInfo;

	@Override
	public void initData() {
		onOpen();
	}

	@Override
	public void loadFilters() {
		loadCaseFilters();
		loadDateFilters();
	}

	@Override
	public void executeDefaultFilter() {
		// fetch results
		filter(getSearchArguments());
	}

	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		log.debug("CMFWeb: Executing MyCasesPanel.updateSelectedFilterField [" + selectedItemId
				+ "]");
		DashboardDateFilter dashboardDateFilter = DashboardDateFilter.getFilterType(selectedItemId);
		DashboardCaseFilter caseFilterType = DashboardCaseFilter.getFilterType(selectedItemId);
		if (dashboardDateFilter != null) {
			selectedDateFilter = selectedItemId;
		} else if (caseFilterType != null) {
			selectedCaseFilter = selectedItemId;
		}
	}

	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		SearchArguments<CaseInstance> searchArguments = new SearchArguments<CaseInstance>();

		Context<String, Object> context = new Context<String, Object>(4);
		context.put(SearchFilterProperties.INSTANCE_CONTEXT, userId);

		if (DashboardCaseFilter.ALL_CASES.getFilterName().equals(selectedCaseFilter)) {
			context.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY, CaseProperties.CREATED_BY);
			context.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());
			Query queryCreator = searchService.getFilter("listAllCaseInstances",
					CaseInstance.class, context).getQuery();

			context.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
					CMFPermissionAdapterService.LIST_OF_ACTIVE_USERS);
			context.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());
			Query queryActive = searchService.getFilter("listAllCaseInstances", CaseInstance.class,
					context).getQuery();

			context.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
					CMFPermissionAdapterService.LIST_OF_ALLOWED_USERS);
			context.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());
			Query queryAllowed = searchService.getFilter("listAllCaseInstances",
					CaseInstance.class, context).getQuery();
			searchArguments.setQuery(queryCreator.or(queryActive).or(queryAllowed));

		} else if (DashboardCaseFilter.CREATED_BY_ME.getFilterName().equals(selectedCaseFilter)) {
			context.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY, CaseProperties.CREATED_BY);
			context.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());
			searchArguments = searchService.getFilter("listAllCaseInstances", CaseInstance.class,
					context);
		} else if (DashboardCaseFilter.OWNED_BY_ME.getFilterName().equals(selectedCaseFilter)) {
			context.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
					CMFPermissionAdapterService.LIST_OF_ACTIVE_USERS);
			context.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());
			searchArguments = searchService.getFilter("listAllCaseInstances", CaseInstance.class,
					context);
		} else if (DashboardCaseFilter.I_WORKED_ON.getFilterName().equals(selectedCaseFilter)) {
			context.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
					CMFPermissionAdapterService.LIST_OF_ALLOWED_USERS);
			context.put(SearchFilterProperties.DATE_RANGE_QUERY, getDateRangeQuery());
			searchArguments = searchService.getFilter("listAllCaseInstances", CaseInstance.class,
					context);
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
	 * Load case filters.
	 */
	private void loadCaseFilters() {

		// load case filters
		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardCaseFilter[] filterTypes = DashboardCaseFilter.values();

		for (DashboardCaseFilter dashboardCaseFilter : filterTypes) {
			String filterName = dashboardCaseFilter.getFilterName();
			filters.add(new SelectorItem(filterName,
					labelProvider.getValue(DashboardCaseFilter.CMF_USER_DASHBOARD_CASE_FILTER_PREF
							+ filterName), ""));
		}

		caseFilters = filters;
		selectedCaseFilter = defaultCaseFilter;
	}

	/**
	 * Load date filters.
	 */
	private void loadDateFilters() {
		// load date filters
		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardDateFilter[] filterTypes = DashboardDateFilter.values();

		for (DashboardDateFilter dashboardDateFilter : filterTypes) {
			String filterName = dashboardDateFilter.getFilterName();
			filters.add(new SelectorItem(filterName,
					labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
							+ filterName), ""));
		}

		dateFilters = filters;
		selectedDateFilter = defaultDateFilter;
	}

	/**
	 * Gets the case filter activator link label.
	 * 
	 * @return the activator link label
	 */
	public String getCaseFilterActivatorLinkLabel() {
		String filterType = defaultCaseFilter;

		if (selectedCaseFilter != null) {
			filterType = selectedCaseFilter;
		}

		return labelProvider.getValue(DashboardCaseFilter.CMF_USER_DASHBOARD_CASE_FILTER_PREF
				+ filterType);
	}

	/**
	 * Gets the date filter activator link label.
	 * 
	 * @return the date filter activator link label
	 */
	public String getDateFilterActivatorLinkLabel() {
		String filterType = defaultDateFilter;

		if (selectedDateFilter != null) {
			filterType = selectedDateFilter;
		}

		return labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
				+ filterType);
	}

	/**
	 * Getter method for caseInstances.
	 * 
	 * @return the caseInstances
	 */
	public List<CaseInstance> getCaseInstances() {
		waitForDataToLoad();
		return caseInstances;
	}

	/**
	 * Getter method for caseFilters.
	 * 
	 * @return the caseFilters
	 */
	public List<SelectorItem> getCaseFilters() {
		return caseFilters;
	}

	/**
	 * Setter method for caseFilters.
	 * 
	 * @param caseFilters
	 *            the caseFilters to set
	 */
	public void setCaseFilters(List<SelectorItem> caseFilters) {
		this.caseFilters = caseFilters;
	}

	/**
	 * Getter method for dateFilters.
	 * 
	 * @return the dateFilters
	 */
	public List<SelectorItem> getDateFilters() {
		return dateFilters;
	}

	/**
	 * Setter method for dateFilters.
	 * 
	 * @param dateFilters
	 *            the dateFilters to set
	 */
	public void setDateFilters(List<SelectorItem> dateFilters) {
		this.dateFilters = dateFilters;
	}

	/**
	 * Getter method for selectedCaseFilter.
	 * 
	 * @return the selectedCaseFilter
	 */
	public String getSelectedCaseFilter() {
		return selectedCaseFilter;
	}

	/**
	 * Setter method for selectedCaseFilter.
	 * 
	 * @param selectedCaseFilter
	 *            the selectedCaseFilter to set
	 */
	public void setSelectedCaseFilter(String selectedCaseFilter) {
		this.selectedCaseFilter = selectedCaseFilter;
	}

	/**
	 * Getter method for selectedDateFilter.
	 * 
	 * @return the selectedDateFilter
	 */
	public String getSelectedDateFilter() {
		return selectedDateFilter;
	}

	/**
	 * Setter method for selectedDateFilter.
	 * 
	 * @param selectedDateFilter
	 *            the selectedDateFilter to set
	 */
	public void setSelectedDateFilter(String selectedDateFilter) {
		this.selectedDateFilter = selectedDateFilter;
	}

	/**
	 * Getter method for defaultCaseFilter.
	 * 
	 * @return the defaultCaseFilter
	 */
	public String getDefaultCaseFilter() {
		return defaultCaseFilter;
	}

	/**
	 * Setter method for defaultCaseFilter.
	 * 
	 * @param defaultCaseFilter
	 *            the defaultCaseFilter to set
	 */
	public void setDefaultCaseFilter(String defaultCaseFilter) {
		this.defaultCaseFilter = defaultCaseFilter;
	}

	/**
	 * Getter method for defaultDateFilter.
	 * 
	 * @return the defaultDateFilter
	 */
	public String getDefaultDateFilter() {
		return defaultDateFilter;
	}

	/**
	 * Setter method for defaultDateFilter.
	 * 
	 * @param defaultDateFilter
	 *            the defaultDateFilter to set
	 */
	public void setDefaultDateFilter(String defaultDateFilter) {
		this.defaultDateFilter = defaultDateFilter;
	}

	@Override
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		log.debug("CMFWeb: Executing MyCasesPanel.filter: searchArguments " + searchArguments);
		if (searchArguments.getResult() == null) {
			searchService.search(CaseInstance.class, searchArguments);
		}
		caseInstances = searchArguments.getResult();
		setCaseActions(getActionsForInstances(caseInstances, CASE_ACTIONS_PALCEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * Getter method that restrict CMF elements based on the loaded modules. TODO: Need to be
	 * discussed.
	 * 
	 * @return boolean value
	 */
	// REVIEW: this is used to decide if create case instance should be allowed. Add extension point
	// and provide the button from PM module instead!
	public boolean getModuleRestriction() {
		Iterator<VersionInfo> iteratorInfo = versionInfo.iterator();
		moduleRestriction = true;
		while (iteratorInfo.hasNext()) {
			VersionInfo versionInfo = iteratorInfo.next();
			if (versionInfo.getModuleDescription().equalsIgnoreCase("pm module")) {
				moduleRestriction = false;
				break;
			}
		}
		return moduleRestriction;
	}

	@Override
	public Set<String> dashletActionIds() {
		return null;
	}

	@Override
	public String targetDashletName() {
		return null;
	}

	@Override
	public Instance dashletActionsTarget() {
		return null;
	}

	/**
	 * Retrieve dashlet actions based on current instance identifier.
	 * 
	 * @param instance
	 *            current instance.
	 * @return list with available actions
	 */
	public List<Action> getCaseActions(Instance instance) {

		if ((instance == null) || (caseActions == null)) {
			return new ArrayList<Action>();
		}

		return caseActions.get(instance.getId());
	}

	/**
	 * Setter method for case actions.
	 * 
	 * @param caseActions
	 *            map case actions
	 */
	public void setCaseActions(Map<Serializable, List<Action>> caseActions) {
		this.caseActions = caseActions;
	}

}
