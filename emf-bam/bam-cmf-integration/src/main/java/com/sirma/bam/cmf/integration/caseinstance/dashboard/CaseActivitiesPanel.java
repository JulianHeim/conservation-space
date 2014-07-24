package com.sirma.bam.cmf.integration.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.bam.cmf.integration.userdashboard.filter.DashboardActivityDateFilter;
import com.sirma.bam.cmf.integration.userdashboard.filter.DashboardActivityOwnershipFilter;
import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.emf.bam.activity.BAMActivity;
import com.sirma.itt.emf.bam.activity.BAMActivityCriteria;
import com.sirma.itt.emf.bam.activity.BAMActivityRetriever;
import com.sirma.itt.emf.bam.activity.CriteriaType;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.search.SearchArgumentsMap;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * This class represent recent activities for case dashboard.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type="CaseDashboard")
@ViewAccessScoped
public class CaseActivitiesPanel
		extends
		DashboardPanelActionBase<Instance, SearchArgumentsMap<Instance, List<Instance>>>
		implements Serializable, DashboardPanelController {

	/** The serial version constant. */
	private static final long serialVersionUID = 1L;

	/** The case ownership filters. */
	private List<SelectorItem> ownershipFilters;
	
	/** The case date filters. */
	private List<SelectorItem> dateFilters;
	
	/** The selected ownership filter. */
	private String selectedOwnershipFilter;
	
	/** The selected date filter. */
	private String selectedDateFilter;
	
	/** The default ownership filter. */
	private String defaultOwnershipFilter = DashboardActivityOwnershipFilter.ALL_ACTIVITIES.getFilterName();
	
	/** The default date filter. */
	private String defaultDateFilter = DashboardActivityDateFilter.LAST_7DAYS.getFilterName();
	
	/** The case instance from the context. */
	private CaseInstance contextCase;
	
	/** The user name. */
	private String userName;
	
	/** The BAM recent activity retriever. */
	@Inject
	private BAMActivityRetriever bamRetriever;
	
	/** The resource service. */
	@Inject 
	private ResourceService resourceService;
	
	/** The activity util. */
	@Inject
	private ActivityUtil activityUtil;
	
	/** The recent activity list. */
	private List<Activities> activit;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		contextCase = getDocumentContext().getInstance(CaseInstance.class);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		loadOwnershipFilters();
		loadDateFilters();
		userName = (String) currentUser.getUserInfo().get("userId");
	}
	
	/**
	 * Load ownership filters.
	 */
	private void loadOwnershipFilters() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardActivityOwnershipFilter[] filterTypes = DashboardActivityOwnershipFilter.values();

		for (DashboardActivityOwnershipFilter ownershipFilterCurrent : filterTypes) {
			String filterName = ownershipFilterCurrent.getFilterName();
			filters.add(new SelectorItem(filterName,
					labelProvider.getValue(DashboardActivityOwnershipFilter.CMF_USER_DASHBOARD_ACTIVITIES_OWNERSHIP_FILTER_PREF
							+ filterName), ""));
		}

		ownershipFilters = filters;
		selectedOwnershipFilter = defaultOwnershipFilter;
	}

	/**
	 * Load date filters.
	 */
	private void loadDateFilters() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardActivityDateFilter[] filterTypes = DashboardActivityDateFilter.values();

		for (DashboardActivityDateFilter dateFilter : filterTypes) {
			String filterName = dateFilter.getFilterName();
			filters.add(new SelectorItem(filterName,
					labelProvider.getValue(DashboardActivityDateFilter.CMF_USER_DASHBOARD_ACTIVITIES_DATE_FILTER_PREF
							+ filterName), ""));
		}

		dateFilters = filters;
		selectedDateFilter = defaultDateFilter;
	}
	
	
	/**
	 * Retrieve labels for ownership filters.
	 * 
	 * @return labels for ownership filters
	 */
	public String getOwnershipFilterActivatorLinkLabel() {
		String filterType = defaultOwnershipFilter;

		if (selectedOwnershipFilter != null) {
			filterType = selectedOwnershipFilter;
		}

		return labelProvider.getValue(DashboardActivityOwnershipFilter.CMF_USER_DASHBOARD_ACTIVITIES_OWNERSHIP_FILTER_PREF
				+ filterType);
	}

	/**
	 * Retrieve labels for date filters.
	 * 
	 * @return labels for date filters
	 */
	public String getDateFilterActivatorLinkLabel() {
		String filterType = defaultDateFilter;

		if (selectedDateFilter != null) {
			filterType = selectedDateFilter;
		}

		return labelProvider.getValue(DashboardActivityDateFilter.CMF_USER_DASHBOARD_ACTIVITIES_DATE_FILTER_PREF
				+ filterType);
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
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		getCaseActivities();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(
			SearchArgumentsMap<Instance, List<Instance>> searchArguments) {
		getCaseActivities();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		DashboardActivityOwnershipFilter dashboardOwnershipFilter = DashboardActivityOwnershipFilter.getFilterType(selectedItemId);
		DashboardActivityDateFilter dashboardDateFilter = DashboardActivityDateFilter.getFilterType(selectedItemId);

		if (dashboardOwnershipFilter != null) {
			selectedOwnershipFilter = selectedItemId;
		} else if (dashboardDateFilter != null) {
			selectedDateFilter = selectedItemId;
		}
	}
	
	/**
	 * Getter for user display name.
	 * 
	 * @param name username
	 * @return user display name
	 */
	public String getDisplayedName(String name){
		return resourceService.getDisplayName(name);
	}
	
	/**
	 * Loads all recent activities for case dashboard based on filters.
	 */
	private void getCaseActivities(){
		BAMActivityCriteria criteria = new BAMActivityCriteria();
		List<BAMActivity> bamActivities = new ArrayList<BAMActivity>();
		List<String> caseListIds = new ArrayList<String>();
		caseListIds.add(getCaseIds());
		
		if(DashboardActivityOwnershipFilter.MY_ACTIVITIES.getFilterName().equals(selectedOwnershipFilter)){
			bamActivities = bamRetriever.getActivities(criteria.setIncludedUsername(userName).setDateRange(getDateRange()).setCriteriaType(CriteriaType.CASE).setIds(caseListIds));
			activit = activityUtil.constructActivities(bamActivities);
		}else if(DashboardActivityOwnershipFilter.OTHERS_ACTIVITIES.getFilterName().equals(selectedOwnershipFilter)){
			bamActivities = bamRetriever.getActivities(criteria.setExcludedUsername(userName).setDateRange(getDateRange()).setCriteriaType(CriteriaType.CASE).setIds(caseListIds));
			activit = activityUtil.constructActivities(bamActivities);
		}else if(DashboardActivityOwnershipFilter.ALL_ACTIVITIES.getFilterName().equals(selectedOwnershipFilter)){
			bamActivities = bamRetriever.getActivities(criteria.setDateRange(getDateRange()).setCriteriaType(CriteriaType.CASE).setIds(caseListIds));
			activit = activityUtil.constructActivities(bamActivities);
		}
		notifyForLoadedData();
	}
	
	/**
	 * Getter for recent activity date range.
	 * 
	 * @return selected date range.
	 */
	private DateRange getDateRange(){
		return DashboardActivityDateFilter.getDateRange(selectedDateFilter);
	}
	
	/**
	 * Getter for current case id.
	 * 
	 * @return case id
	 */
	private String getCaseIds(){
		return (String) contextCase.getProperties().get(DefaultProperties.UNIQUE_IDENTIFIER);
	}

	@Override
	public SearchArgumentsMap<Instance, List<Instance>> getSearchArguments() {
		return new SearchArgumentsMap<Instance, List<Instance>>();
	}

	/**
	 * Getter for ownership filters.
	 * 
	 * @return ownership filters
	 */
	public List<SelectorItem> getOwnershipFilters() {
		return ownershipFilters;
	}

	/**
	 * Setter for ownership filters.
	 * 
	 * @param ownershipFilters ownership filters
	 */
	public void setOwnershipFilters(List<SelectorItem> ownershipFilters) {
		this.ownershipFilters = ownershipFilters;
	}

	/**
	 * Getter for date filters.
	 * 
	 * @return date filters
	 */
	public List<SelectorItem> getDateFilters() {
		return dateFilters;
	}

	/**
	 * Setter for date filters.
	 * 
	 * @param dateFilters date filters list
	 */
	public void setDateFilters(List<SelectorItem> dateFilters) {
		this.dateFilters = dateFilters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initData() {
		onOpen();
	}

	/**
	 * Getter for activity list.
	 * 
	 * @return activity list
	 */
	public List<Activities> getActivit() {
		return activit;
	}

	/**
	 * Setter for activity list.
	 * 
	 * @param activit activity list
	 */
	public void setActivit(List<Activities> activit) {
		this.activit = activit;
	}
	
}
