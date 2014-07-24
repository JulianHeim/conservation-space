package com.sirma.itt.bam.pm.integration.web.userdashboard.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.bam.cmf.integration.caseinstance.dashboard.Activities;
import com.sirma.bam.cmf.integration.caseinstance.dashboard.ActivityUtil;
import com.sirma.bam.cmf.integration.userdashboard.filter.DashboardActivityDateFilter;
import com.sirma.bam.cmf.integration.userdashboard.filter.DashboardActivityOwnershipFilter;
import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.emf.bam.activity.BAMActivity;
import com.sirma.itt.emf.bam.activity.BAMActivityCriteria;
import com.sirma.itt.emf.bam.activity.BAMActivityRetriever;
import com.sirma.itt.emf.bam.activity.CriteriaType;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchArgumentsMap;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * This class will manage recent activity dashlet for personal dashboard.
 * <b>NOTE: this class need re-factoring</b>
 * @author cdimitrov
 */
@Named
@InstanceType(type="UserDashboard")
@ViewAccessScoped
public class MyActivitiesPanel
		extends
		DashboardPanelActionBase<Instance, SearchArgumentsMap<Instance, List<Instance>>>
		implements Serializable, DashboardPanelController {
	
	/** The serial version constant. */
	private static final long serialVersionUID = 6280630857325539513L;
	
	/** The ownership filter list. */
	private List<SelectorItem> ownershipFilters;
	
	/** The date filter list. */
	private List<SelectorItem> dateFilters;
	
	/** The selected ownership filter. */
	private String selectedOwnershipFilter;
	
	/** The selected date filter. */
	private String selectedDateFilter;
	
	/** The default ownership filter. */
	private String defaultOwnershipFilter = DashboardActivityOwnershipFilter.ALL_ACTIVITIES.getFilterName();
	
	/** The default date filter. */
	private String defaultDateFilter = DashboardActivityDateFilter.LAST_7DAYS.getFilterName();
	
	/** The user identifier constant. */
	private static final String USER_ID = "userId";
	
	/** The user displayed name. */
	private String userName;
	
	private List<Activities> activit;
	
	/** The BAM retriever service. */
	@Inject
	private BAMActivityRetriever bamRetriever;
	
	/** The resource service. */
	@Inject
	private ResourceService resourceService;
	
	/** The activity util. */
	@Inject
	private ActivityUtil activityUtil;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		loadOwnershipFilters();
		loadDateFilters();
		userName = (String) currentUser.getUserInfo().get(USER_ID);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(
			SearchArgumentsMap<Instance, List<Instance>> searchArguments) {
		getProjectActivities();
	}
	
	/** 
	 * Retrieve date range by filter selection.
	 * 
	 * @return date range
	 */
	private DateRange getDateRange(){
		return DashboardActivityDateFilter.getDateRange(selectedDateFilter);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArgumentsMap<Instance, List<Instance>> getSearchArguments() {
		return new SearchArgumentsMap<Instance, List<Instance>>();
	}
	
	/**
	 * Getter for projects identifiers that the user is member.
	 * 
	 * @return list with projects IDs.
	 */
	private List<String> getProjectIds(){
		
		SearchArguments<ProjectInstance> searchArguments = null;
		searchArguments = searchService.getFilter("listAllProjects", ProjectInstance.class, null);
		
		Context<String, Object> context = new Context<>(1);
		context.put(SearchFilterProperties.USER_ID, userId);
		SearchArguments<ProjectInstance> permissionQuery = searchService.getFilter("permissionsProject", ProjectInstance.class, context);
		searchArguments.setQuery(permissionQuery.getQuery().and(searchArguments.getQuery()));
		
		searchService.search(ProjectInstance.class, searchArguments);
		List<String> projectListIds = new ArrayList<String>();
		for(ProjectInstance instance : searchArguments.getResult()){
			if(instance!=null){
				projectListIds.add((String) instance.getProperties().get(DefaultProperties.UNIQUE_IDENTIFIER));
			}
		}
		return projectListIds;
	}
	
	/**
	 * Retrieve display name for current resource.
	 * 
	 * @param name current username
	 * @return displayed name
	 */
	public String getDisplayedName(String name){
		return resourceService.getDisplayName(name);
	}
	
	/**
	 * This method will retrieve and store activities based on the projects.
	 */
	private void getProjectActivities(){
		
		BAMActivityCriteria criteria = new BAMActivityCriteria();
		List<BAMActivity> bamActivities = new ArrayList<BAMActivity>();
		List<String> projectIds = getProjectIds();
		if(DashboardActivityOwnershipFilter.MY_ACTIVITIES.getFilterName().equals(selectedOwnershipFilter)){
			bamActivities = bamRetriever.getActivities(criteria.setIncludedUsername(userName).setDateRange(getDateRange()).setCriteriaType(CriteriaType.PROJECT).setIds(projectIds));
			activit = activityUtil.constructActivities(bamActivities);
		}else if(DashboardActivityOwnershipFilter.OTHERS_ACTIVITIES.getFilterName().equals(selectedOwnershipFilter)){
			bamActivities = bamRetriever.getActivities(criteria.setExcludedUsername(userName).setDateRange(getDateRange()).setCriteriaType(CriteriaType.PROJECT).setIds(projectIds));
			activit = activityUtil.constructActivities(bamActivities);
		}else if(DashboardActivityOwnershipFilter.ALL_ACTIVITIES.getFilterName().equals(selectedOwnershipFilter)){
			bamActivities = bamRetriever.getActivities(criteria.setDateRange(getDateRange()).setCriteriaType(CriteriaType.PROJECT).setIds(projectIds));
			activit = activityUtil.constructActivities(bamActivities);
		}
		
		notifyForLoadedData();
	}
	
	/**
	 * Load all ownership filters for activity dashlet.
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
	 * Load all date filters for activity dashlet.
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
	 * Getter method for ownership activator label.
	 * 
	 * @return ownership filter label.
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
	 * Get date date filter activator label.
	 * 
	 * @return date filter label
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
		getProjectActivities();
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
	 * Getter method for ownership filter.
	 * 
	 * @return list with ownership filter
	 */
	public List<SelectorItem> getOwnershipFilters() {
		return ownershipFilters;
	}

	/**
	 * Setter method for ownership filter.
	 * 
	 * @param ownershipFilters list with ownership filters.
	 */
	public void setOwnershipFilters(List<SelectorItem> ownershipFilters) {
		this.ownershipFilters = ownershipFilters;
	}

	/**
	 * Getter method for date filters.
	 * 
	 * @return list date filter list
	 */
	public List<SelectorItem> getDateFilters() {
		return dateFilters;
	}
	
	/**
	 * Setter for date filter.
	 * 
	 * @param dateFilters date filter
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
	 * Getter for activity.
	 * 
	 * @return activity list
	 */
	public List<Activities> getActivit() {
		return activit;
	}

	/**
	 * Setter for activity.
	 * 
	 * @param activit activity list
	 */
	public void setActivit(List<Activities> activit) {
		this.activit = activit;
	}
}
