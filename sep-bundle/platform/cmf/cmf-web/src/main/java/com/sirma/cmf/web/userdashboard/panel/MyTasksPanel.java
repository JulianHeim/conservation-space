/*
 *
 */
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
import com.sirma.cmf.web.standaloneTask.StandaloneTaskAction;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.cmf.web.userdashboard.filter.DashboardTaskFilter;
import com.sirma.cmf.web.workflow.task.TaskListTableAction;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.emf.converter.TypeConverterUtil;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * <b>MyTasksPanel</b> manage functionality for dashlet, located in personal/user dashboard. The
 * content is represented as task records, actions and filters.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyTasksPanel extends
		DashboardPanelActionBase<AbstractTaskInstance, SearchArguments<AbstractTaskInstance>>
		implements Serializable, DashboardPanelController {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -5181022575609543468L;

	/** The task instances. */
	private List<AbstractTaskInstance> taskInstances;

	/** The task filters. */
	private List<SelectorItem> taskFilters;

	/** The default task filter. */
	private String defaultTaskFilter = DashboardTaskFilter.ACTIVE_TASKS.getFilterName();

	/** The selected task filter. */
	private String selectedTaskFilter;

	/** The available task actions. */
	private Map<Serializable, List<Action>> taskActions;

	private static final String TASK_DASHLET_ACTION_PLACEHOLDER = "user-dashboard-tasks";

	/** The task list table action. */
	@Inject
	private TaskListTableAction taskListTableAction;

	/** The standalnoe task action */
	@Inject
	private StandaloneTaskAction standaloneTaskAction;

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

		DashboardTaskFilter[] filterTypes = DashboardTaskFilter.values();

		for (DashboardTaskFilter dashboardTaskFilter : filterTypes) {
			String filterName = dashboardTaskFilter.getFilterName();
			String value = dashboardTaskFilter.getLabel(labelProvider);
			filters.add(new SelectorItem(filterName, value, ""));
		}

		taskFilters = filters;
		selectedTaskFilter = defaultTaskFilter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		log.debug("CMFWeb: Executing MyTasksPanel.executeDefaultFilter");

		// fetch results
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<AbstractTaskInstance> searchArguments) {
		log.debug("CMFWeb: Executing MyTasksPanel.filter: searchArguments " + searchArguments);
		searchService.search(AbstractTaskInstance.class, searchArguments);
		taskInstances = searchArguments.getResult();
		setTaskActions(getActionsForInstances(taskInstances, TASK_DASHLET_ACTION_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {

		selectedTaskFilter = selectedItemId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<AbstractTaskInstance> getSearchArguments() {
		SearchArguments<AbstractTaskInstance> searchArguments = null;
		Context<String, Object> searchContext = new Context<String, Object>(1);
		searchContext.put(SearchFilterProperties.INCLUDE_OWNER, Boolean.TRUE);

		if (DashboardTaskFilter.ALL_TASKS.getFilterName().equals(selectedTaskFilter)) {
			searchArguments = searchService.getFilter("getAllTasksFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.ACTIVE_TASKS.getFilterName().equals(selectedTaskFilter)) {
			searchArguments = searchService.getFilter("getOpenTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.HIGH_PRIORITY_TASKS.getFilterName().equals(
				selectedTaskFilter)) {
			searchArguments = searchService.getFilter("getHighPriorityTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.DUE_DATE_TODAY_TASKS.getFilterName().equals(
				selectedTaskFilter)) {
			searchArguments = searchService.getFilter("getDueDateTodayTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.UNASSIGNED_TASKS.getFilterName().equals(selectedTaskFilter)) {
			searchArguments = searchService.getFilter("getPoolableTaskFilter",
					AbstractTaskInstance.class, searchContext);
		} else if (DashboardTaskFilter.OVERDUE_DATE_TASKS.getFilterName()
				.equals(selectedTaskFilter)) {
			searchArguments = searchService.getFilter("getOverdueDateTaskFilter",
					AbstractTaskInstance.class, searchContext);
		}

		return searchArguments;
	}

	/**
	 * Gets the task filter activator link label.
	 * 
	 * @return the task filter activator link label
	 */
	public String getTaskFilterActivatorLinkLabel() {
		String filterType = defaultTaskFilter;

		if (selectedTaskFilter != null) {
			filterType = selectedTaskFilter;
		}

		return labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
				+ filterType);
	}

	/**
	 * Open selected task.
	 * 
	 * @param taskInstance
	 *            the task instance
	 * @return the string
	 */
	public String openTask(AbstractTaskInstance taskInstance) {
		if (taskInstance instanceof TaskInstance) {
			TaskInstance instance = (TaskInstance) taskInstance;
			Instance owningInstance = instance.getOwningInstance();
			if (owningInstance instanceof CaseInstance) {
				getDocumentContext().addInstance(owningInstance);
			} else {
				// TODO: add support for other objects
			}

			return taskListTableAction.open(instance);
		}

		StandaloneTaskInstance instance = (StandaloneTaskInstance) taskInstance;
		Instance owningInstance = TypeConverterUtil.getConverter().convert(Instance.class,
				instance.getOwningReference());
		if (owningInstance instanceof CaseInstance) {
			getDocumentContext().addInstance(owningInstance);
		} else if (owningInstance instanceof WorkflowInstanceContext) {
			getDocumentContext().addInstance(owningInstance);
		} else {
			// TODO: add support for other objects
		}

		return standaloneTaskAction.open(instance);
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
	 * Getter method for defaultTaskFilter.
	 * 
	 * @return the defaultTaskFilter
	 */
	public String getDefaultTaskFilter() {
		return defaultTaskFilter;
	}

	/**
	 * Setter method for defaultTaskFilter.
	 * 
	 * @param defaultTaskFilter
	 *            the defaultTaskFilter to set
	 */
	public void setDefaultTaskFilter(String defaultTaskFilter) {
		this.defaultTaskFilter = defaultTaskFilter;
	}

	/**
	 * Getter method for selectedTaskFilter.
	 * 
	 * @return the selectedTaskFilter
	 */
	public String getSelectedTaskFilter() {
		return selectedTaskFilter;
	}

	/**
	 * Setter method for selectedTaskFilter.
	 * 
	 * @param selectedTaskFilter
	 *            the selectedTaskFilter to set
	 */
	public void setSelectedTaskFilter(String selectedTaskFilter) {
		this.selectedTaskFilter = selectedTaskFilter;
	}

	/**
	 * Getter method for taskFilters.
	 * 
	 * @return the taskFilters
	 */
	public List<SelectorItem> getTaskFilters() {
		return taskFilters;
	}

	/**
	 * Setter method for taskFilters.
	 * 
	 * @param taskFilters
	 *            the taskFilters to set
	 */
	public void setTaskFilters(List<SelectorItem> taskFilters) {
		this.taskFilters = taskFilters;
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
		return null;
	}

	/**
	 * Getter for actions based on instance.
	 * 
	 * @param instance
	 *            current instance
	 * @return list with actions
	 */
	public List<Action> getTaskActions(Instance instance) {

		if ((instance == null) || (taskActions == null)) {
			return new ArrayList<Action>();
		}

		return taskActions.get(instance.getId());
	}

	/**
	 * Setter for task actions.
	 * 
	 * @param taskActions
	 *            current task actions
	 */
	public void setTaskActions(Map<Serializable, List<Action>> taskActions) {
		this.taskActions = taskActions;
	}

}
