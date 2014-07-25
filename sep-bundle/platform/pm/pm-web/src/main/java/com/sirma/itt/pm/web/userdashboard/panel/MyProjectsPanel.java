package com.sirma.itt.pm.web.userdashboard.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.Sorter;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.constants.ProjectProperties;
import com.sirma.itt.pm.domain.model.ProjectInstance;
import com.sirma.itt.pm.web.constants.PmNavigationConstants;
import com.sirma.itt.pm.web.userdashboard.panel.filters.PMUserDashboardProjectFilter;

/**
 * <b>MyProjectsPanel</b> manage functionality for dashlet, located in personal/user dashboard. The
 * content is represented as project records, actions and filters.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyProjectsPanel extends
		DashboardPanelActionBase<ProjectInstance, SearchArguments<ProjectInstance>> implements
		Serializable, DashboardPanelController {

	private static final long serialVersionUID = 7487956194152421786L;

	public static final String MYPROJECTS_DASHLET = "myprojects-dashlet";

	/** Constant that represent sorter for project records based on titles. */
	private static final Sorter TITLE_SORTER = new Sorter(ProjectProperties.TITLE,
			Sorter.SORT_ASCENDING);

	/** List with projects that are generated based on filters. */
	private List<ProjectInstance> projectInstancesList;

	/** Filter that project dashlet support. */
	private List<SelectorItem> projectFilter;

	/** Currently selected project filter. */
	private String selectedProjectFilter;

	/** Actions that are filtered through security manager. */
	private Map<Serializable, List<Action>> actions;

	/** On-default selected filter {@see PMUserDashboardProjectFilter}. */
	private final String defaultProjectFilter = PMUserDashboardProjectFilter.ACTIVE_PROJECTS
			.getFilterName();

	/** The placeholder for project actions. */
	private static final String PROJECT_DASHLET_ACTIONS_PLACEHOLDER = "project-dashboard-projects";

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
		loadProjectFilters();
	}

	/**
	 * Loads project filters into the dashlet.
	 */
	private void loadProjectFilters() {
		List<SelectorItem> filters = new ArrayList<SelectorItem>();
		PMUserDashboardProjectFilter[] filterTypes = PMUserDashboardProjectFilter.values();

		for (PMUserDashboardProjectFilter dashboardProjectFilter : filterTypes) {
			String filterName = dashboardProjectFilter.getFilterName();
			filters.add(new SelectorItem(
					filterName,
					labelProvider
							.getValue(PMUserDashboardProjectFilter.PM_PROJECT_USER_DASHBOARD_PROJECTS_FILTER_PREF
									+ filterName), ""));
		}

		projectFilter = filters;
		selectedProjectFilter = defaultProjectFilter;
	}

	/**
	 * Getter for project filters activator link label.
	 * 
	 * @return the project filter activator link label
	 */
	public String getProjectActivatorLinkLabel() {
		String filterType = defaultProjectFilter;

		if (selectedProjectFilter != null) {
			filterType = selectedProjectFilter;
		}

		return labelProvider
				.getValue(PMUserDashboardProjectFilter.PM_PROJECT_USER_DASHBOARD_PROJECTS_FILTER_PREF
						+ filterType);
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
	public void filter(SearchArguments<ProjectInstance> searchArguments) {
		if (searchArguments != null) {
			searchArguments.setSorter(TITLE_SORTER);
			searchService.search(ProjectInstance.class, searchArguments);
			setProjectInstancesList(searchArguments.getResult());
			Map<Serializable, List<Action>> actionsForInstances = getActionsForInstances(
					projectInstancesList, PROJECT_DASHLET_ACTIONS_PLACEHOLDER);
			setActions(actionsForInstances);
			notifyForLoadedData();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		log.debug("PMWeb: Executing MyProjectsPanel.updateSelectedFilterField [" + selectedItemId
				+ "]");

		PMUserDashboardProjectFilter filters = PMUserDashboardProjectFilter
				.getFilterType(selectedItemId);

		if (filters != null) {
			selectedProjectFilter = selectedItemId;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<ProjectInstance> getSearchArguments() {
		SearchArguments<ProjectInstance> searchArguments = new SearchArguments<ProjectInstance>();
		if (PMUserDashboardProjectFilter.ALL_PROJECTS.getFilterName().equals(selectedProjectFilter)) {
			searchArguments = searchService.getFilter("listAllProjects", ProjectInstance.class,
					null);
		} else if (PMUserDashboardProjectFilter.ACTIVE_PROJECTS.getFilterName().equals(
				selectedProjectFilter)) {
			searchArguments = searchService.getFilter("listActiveProjects", ProjectInstance.class,
					null);
		} else if (PMUserDashboardProjectFilter.COMPLETED_PROJECTS.getFilterName().equals(
				selectedProjectFilter)) {
			searchArguments = searchService.getFilter("listCompletedProjects",
					ProjectInstance.class, null);
		}
		Context<String, Object> context = new Context<>(1);
		context.put(SearchFilterProperties.USER_ID, userId);
		SearchArguments<ProjectInstance> permissionQuery = searchService.getFilter(
				"permissionsProject", ProjectInstance.class, context);
		if (searchArguments.getQuery() != null) {
			searchArguments.setQuery(permissionQuery.getQuery().and(searchArguments.getQuery()));
		}
		return searchArguments;
	}

	/**
	 * Getter method for projectInstancesList.
	 * 
	 * @return the projectInstancesList
	 */
	public List<ProjectInstance> getProjectInstancesList() {
		waitForDataToLoad();
		return projectInstancesList;
	}

	/**
	 * Search projects.
	 * 
	 * @return the string
	 */
	public String searchProjects() {
		return executeToolbarAction(MYPROJECTS_DASHLET, PmNavigationConstants.PROJECT_SEARCH);
	}

	/**
	 * Setter method for projectInstancesList.
	 * 
	 * @param projectInstancesList
	 *            the projectInstancesList to set
	 */
	public void setProjectInstancesList(List<ProjectInstance> projectInstancesList) {
		this.projectInstancesList = projectInstancesList;
	}

	/**
	 * Getter method for project filter.
	 * 
	 * @return project filters
	 */
	public List<SelectorItem> getProjectFilter() {
		return projectFilter;
	}

	/**
	 * Setter method for project filter.
	 * 
	 * @param projectFilter
	 *            project filters
	 */
	public void setProjectFilter(List<SelectorItem> projectFilter) {
		this.projectFilter = projectFilter;
	}

	/**
	 * Getter method for selected project filter.
	 * 
	 * @return selected project filters
	 */
	public String getSelectedProjectFilter() {
		return selectedProjectFilter;
	}

	/**
	 * Setter method for selected project filter.
	 * 
	 * @param selectedProjectFilter
	 *            currently selected project filter
	 */
	public void setSelectedProjectFilter(String selectedProjectFilter) {
		this.selectedProjectFilter = selectedProjectFilter;
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
		return MYPROJECTS_DASHLET;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return null;
	}

	/**
	 * Getter method for actions.
	 * 
	 * @param instance
	 *            the instance
	 * @return the actions for instance
	 */
	public List<Action> getActionsForInstance(Instance instance) {

		if ((instance == null) || (actions == null)) {
			return new ArrayList<Action>();
		}

		return actions.get(instance.getId());
	}

	/**
	 * Setter method for actions.
	 * 
	 * @param actions
	 *            the actions to set
	 */
	public void setActions(Map<Serializable, List<Action>> actions) {
		this.actions = actions;
	}

}
