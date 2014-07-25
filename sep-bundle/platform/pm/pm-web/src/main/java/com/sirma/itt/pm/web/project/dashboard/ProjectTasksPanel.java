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
import com.sirma.cmf.web.userdashboard.filter.DashboardTaskFilter;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.constants.ProjectProperties;
import com.sirma.itt.pm.domain.model.ProjectInstance;
import com.sirma.itt.pm.security.PmActionTypeConstants;

/**
 * <b>ProjectTasksPanel</b> manage functionality for dashlet, located in project dashboard. The
 * content is represented as task records, actions and filters.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectTasksPanel extends
		DashboardPanelActionBase<AbstractTaskInstance, SearchArguments<AbstractTaskInstance>>
		implements Serializable, DashboardPanelController {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 2264175323860334L;

	/** The Constant DASHLET_ACTIONS. */
	private static final Set<String> DASHLET_ACTIONS = new HashSet<String>(Arrays.asList(
			PmActionTypeConstants.CREATE_TASK, PmActionTypeConstants.START_WORKFLOW));

	/** The task instances. */
	private List<AbstractTaskInstance> taskInstances;

	/** project tasks filter. */
	private List<SelectorItem> projectTasksFilters;

	/** currently selected project task filter. */
	private String selectedProjectTasksFilter;

	/** by default selected filter {@see DashboardTaskFilter}. */
	private final String defaultProjectTasksFilter = DashboardTaskFilter.ACTIVE_TASKS
			.getFilterName();

	/** The available task actions. */
	private Map<Serializable, List<Action>> taskActions;

	private static final String PROJECT_TASK_DASHLET_PLACEHOLDER = "project-dashboard-tasks";

	/** The project instance that will be extracted from the context. */
	private ProjectInstance projectInstance;

	@Override
	public void initData() {
		onOpen();
	}

	@Override
	protected void initializeForAsynchronousInvocation() {
		projectInstance = getDocumentContext().getInstance(ProjectInstance.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardTaskFilter[] filterTypes = DashboardTaskFilter.values();

		for (DashboardTaskFilter taskFilter : filterTypes) {

			String filterName = taskFilter.getFilterName();

			filters.add(new SelectorItem(filterName,
					labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
							+ filterName), ""));
		}

		projectTasksFilters = filters;
		selectedProjectTasksFilter = defaultProjectTasksFilter;
	}

	/**
	 * Retrieved all filter labels for project tasks dashlet.
	 * 
	 * @return label value
	 */
	public String getTasksFilterActivatorLinkLabel() {
		String filterType = defaultProjectTasksFilter;

		if (selectedProjectTasksFilter != null) {
			filterType = selectedProjectTasksFilter;
		}

		return labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
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
	public void filter(SearchArguments<AbstractTaskInstance> searchArguments) {
		log.debug("CMFWeb: Executing ProjectTasksPanel.filter: searchArguments " + searchArguments);
		searchService.search(AbstractTaskInstance.class, searchArguments);
		taskInstances = searchArguments.getResult();
		setTaskActions(getActionsForInstances(taskInstances, PROJECT_TASK_DASHLET_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		log.debug("PMWeb: Executing ProjectTasksPanel.updateSelectedFilterField [" + selectedItemId
				+ "]");

		DashboardTaskFilter taskFilter = DashboardTaskFilter.getFilterType(selectedItemId);

		if (taskFilter != null) {
			selectedProjectTasksFilter = selectedItemId;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<AbstractTaskInstance> getSearchArguments() {
		SearchArguments<AbstractTaskInstance> searchArguments = null;
		Context<String, Object> searchContext = new Context<String, Object>(1);
		searchContext.put(SearchFilterProperties.INCLUDE_OWNER, Boolean.FALSE);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT, projectInstance);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
				ProjectProperties.OWNED_INSTANCES);

		if (DashboardTaskFilter.ALL_TASKS.getFilterName().equals(selectedProjectTasksFilter)) {
			searchArguments = searchService.getFilter("getAllTasksFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.ACTIVE_TASKS.getFilterName().equals(
				selectedProjectTasksFilter)) {
			searchArguments = searchService.getFilter("getOpenTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.HIGH_PRIORITY_TASKS.getFilterName().equals(
				selectedProjectTasksFilter)) {
			searchArguments = searchService.getFilter("getHighPriorityTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.DUE_DATE_TODAY_TASKS.getFilterName().equals(
				selectedProjectTasksFilter)) {
			searchArguments = searchService.getFilter("getDueDateTodayTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.OVERDUE_DATE_TASKS.getFilterName().equals(
				selectedProjectTasksFilter)) {
			searchArguments = searchService.getFilter("getOverdueDateTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.UNASSIGNED_TASKS.getFilterName().equals(
				selectedProjectTasksFilter)) {
			searchArguments = searchService.getFilter("getPoolableTaskFilter",
					AbstractTaskInstance.class, searchContext);
		}

		return searchArguments;
	}

	/**
	 * Retrieve project tasks filters.
	 * 
	 * @return list with filters
	 */
	public List<SelectorItem> getProjectTasksFilters() {
		return projectTasksFilters;
	}

	/**
	 * Set project tasks filters.
	 * 
	 * @param projectTasksFilters
	 *            list with filters
	 */
	public void setProjectTasksFilters(List<SelectorItem> projectTasksFilters) {
		this.projectTasksFilters = projectTasksFilters;
	}

	/**
	 * Get current selected filter from the user.
	 * 
	 * @return currently selected filter
	 */
	public String getSelectedProjectTasksFilter() {
		return selectedProjectTasksFilter;
	}

	/**
	 * Set current filter.
	 * 
	 * @param selectedProjectTasksFilter
	 *            currently selected filter from the user
	 */
	public void setSelectedProjectTasksFilter(String selectedProjectTasksFilter) {
		this.selectedProjectTasksFilter = selectedProjectTasksFilter;
	}

	/**
	 * Get default filter that will be active on dashlet load.
	 * 
	 * @return default selected filter
	 */
	public String getDefaultProjectTasksFilter() {
		return defaultProjectTasksFilter;
	}

	/**
	 * Getter method for taskInstances.
	 * 
	 * @return the taskInstances
	 */
	public List<AbstractTaskInstance> getTaskInstances() {
		waitForDataToLoad();
		return taskInstances;
	}

	/**
	 * Setter method for taskInstances.
	 * 
	 * @param taskInstances
	 *            the taskInstances to set
	 */
	public void setTaskInstances(List<AbstractTaskInstance> taskInstances) {
		this.taskInstances = taskInstances;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return "project-tasks-dashlet";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		return DASHLET_ACTIONS;
	}

	/**
	 * Getter for task actions based on current instance.
	 * 
	 * @param instance
	 *            current instance
	 * @return task actions
	 */
	public List<Action> getTaskActions(Instance instance) {
		if ((instance == null) || (taskActions == null)) {
			return new ArrayList<Action>();
		}
		return taskActions.get(instance.getId());
	}

	/**
	 * Setter for project task actions.
	 * 
	 * @param taskActions
	 *            task actions
	 */
	public void setTaskActions(Map<Serializable, List<Action>> taskActions) {
		this.taskActions = taskActions;
	}

}
