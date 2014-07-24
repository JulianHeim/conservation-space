package com.sirma.itt.bam.pm.integration.project.dashboard;

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
import com.sirma.itt.cmf.services.CaseService;
import com.sirma.itt.emf.bam.activity.BAMActivity;
import com.sirma.itt.emf.bam.activity.BAMActivityCriteria;
import com.sirma.itt.emf.bam.activity.BAMActivityRetriever;
import com.sirma.itt.emf.bam.activity.CriteriaType;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * This class will manage recent activity dashlet for project dashboard.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectActivitiesPanel
		extends
		DashboardPanelActionBase<ProjectInstance, SearchArguments<ProjectInstance>>
		implements Serializable, DashboardPanelController {

	/** The serial version constant. */
	private static final long serialVersionUID = 1L;

	/** The ownership filter list. */
	private List<SelectorItem> ownershipFilters;

	/** The date filter list. */
	private List<SelectorItem> dateFilters;

	/** The selected ownership filter. */
	private String selectedOwnershipFilter;

	/** The selected date filter. */
	private String selectedDateFilter;
	
	/** The resource service. */
	@Inject
	private ResourceService resourceService;

	/** The default owership filter. */
	private String defaultOwnershipFilter = DashboardActivityOwnershipFilter.ALL_ACTIVITIES
			.getFilterName();

	/** The default date filter. */
	private String defaultDateFilter = DashboardActivityDateFilter.LAST_7DAYS
			.getFilterName();
	
	/** The project instance in the context. */
	private ProjectInstance contextProject;
	
	/** The username. */
	private String userName;
	
	private List<Activities> activit;
	/** The BAM retriever service. */
	@Inject
	private BAMActivityRetriever bamRetriever;
	
	/** The case service. */
	@Inject
	private CaseService caseService;
	
	@Inject
	private ActivityUtil activityUtil;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		contextProject = getDocumentContext().getInstance(ProjectInstance.class);
	}

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
		loadOwnershipFilters();
		loadDateFilters();
		userName = (String) currentUser.getUserInfo().get("userId");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<ProjectInstance> searchArguments) {
		getProjectActivities();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<ProjectInstance> getSearchArguments() {
		return new SearchArguments<>();
	}
	
	/**
	 * Getting project activities.
	 */
	private void getProjectActivities(){
		
		BAMActivityCriteria criteria = new BAMActivityCriteria();
		List<String> projectIds = new ArrayList<String>();
		List<BAMActivity> bamActivities = new ArrayList<BAMActivity>();
		projectIds.add((String) contextProject.getProperties().get(DefaultProperties.UNIQUE_IDENTIFIER));
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
	 * Retrieve the date for project activities.
	 * 
	 * @return date range
	 */
	private DateRange getDateRange(){
		return DashboardActivityDateFilter.getDateRange(selectedDateFilter);
	}

	/**
	 * Getter for ownership activator link label.
	 * 
	 * @return ownership activator link label
	 */
	public String getOwnershipFilterActivatorLinkLabel() {
		String filterType = defaultOwnershipFilter;

		if (selectedOwnershipFilter != null) {
			filterType = selectedOwnershipFilter;
		}

		return labelProvider
				.getValue(DashboardActivityOwnershipFilter.CMF_USER_DASHBOARD_ACTIVITIES_OWNERSHIP_FILTER_PREF
						+ filterType);
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
	 * Getter for date activator link label.
	 * 
	 * @return date activator link label
	 */
	public String getDateFilterActivatorLinkLabel() {
		String filterType = defaultDateFilter;

		if (selectedDateFilter != null) {
			filterType = selectedDateFilter;
		}

		return labelProvider
				.getValue(DashboardActivityDateFilter.CMF_USER_DASHBOARD_ACTIVITIES_DATE_FILTER_PREF
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
		DashboardActivityOwnershipFilter dashboardOwnershipFilter = DashboardActivityOwnershipFilter
				.getFilterType(selectedItemId);
		DashboardActivityDateFilter dashboardDateFilter = DashboardActivityDateFilter
				.getFilterType(selectedItemId);

		if (dashboardOwnershipFilter != null) {
			selectedOwnershipFilter = selectedItemId;
		} else if (dashboardDateFilter != null) {
			selectedDateFilter = selectedItemId;
		}
	}
	
	/**
	 * This method load all ownership filters.
	 */
	private void loadOwnershipFilters() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardActivityOwnershipFilter[] filterTypes = DashboardActivityOwnershipFilter
				.values();

		for (DashboardActivityOwnershipFilter ownershipFilterCurrent : filterTypes) {
			String filterName = ownershipFilterCurrent.getFilterName();
			filters.add(new SelectorItem(
					filterName,
					labelProvider
							.getValue(DashboardActivityOwnershipFilter.CMF_USER_DASHBOARD_ACTIVITIES_OWNERSHIP_FILTER_PREF
									+ filterName), ""));
		}

		ownershipFilters = filters;
		selectedOwnershipFilter = defaultOwnershipFilter;
	}
	
	/**
	 * Getter that will group project header(title and type).
	 * 
	 * @return project grouped title
	 */
	public String getProjectTitle(){
		StringBuilder titleGroup = new StringBuilder();
		titleGroup.append("(");
		titleGroup.append(contextProject.getProperties().get("type"));
		titleGroup.append(")");
		return titleGroup.toString()+" "+contextProject.getProperties().get("title");
	}
	
	/**
	 * This method load all date filters.
	 */
	private void loadDateFilters() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardActivityDateFilter[] filterTypes = DashboardActivityDateFilter
				.values();

		for (DashboardActivityDateFilter dateFilter : filterTypes) {
			String filterName = dateFilter.getFilterName();
			filters.add(new SelectorItem(
					filterName,
					labelProvider
							.getValue(DashboardActivityDateFilter.CMF_USER_DASHBOARD_ACTIVITIES_DATE_FILTER_PREF
									+ filterName), ""));
		}

		dateFilters = filters;
		selectedDateFilter = defaultDateFilter;
	}

	/**
	 * Getter method for ownership filter.
	 * 
	 * @return list with ownership filters
	 */
	public List<SelectorItem> getOwnershipFilters() {
		return ownershipFilters;
	}

	/**
	 * Setter for ownership filters.
	 * 
	 * @param ownershipFilters
	 *            list with ownership filter
	 */
	public void setOwnershipFilters(List<SelectorItem> ownershipFilters) {
		this.ownershipFilters = ownershipFilters;
	}

	/**
	 * Getter method for date filters.
	 * 
	 * @return date filter list
	 */
	public List<SelectorItem> getDateFilters() {
		return dateFilters;
	}

	/**
	 * Setter method for date filter.
	 * 
	 * @param dateFilters
	 *            date filter list
	 */
	public void setDateFilters(List<SelectorItem> dateFilters) {
		this.dateFilters = dateFilters;
	}

	/**
	 * Getter for project instance.
	 * 
	 * @return project instance.
	 */
	public ProjectInstance getContextProject() {
		return contextProject;
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
	 * Setter for recent activity.
	 * 
	 * @param activit activity list
	 */
	public void setActivit(List<Activities> activit) {
		this.activit = activit;
	}
}
