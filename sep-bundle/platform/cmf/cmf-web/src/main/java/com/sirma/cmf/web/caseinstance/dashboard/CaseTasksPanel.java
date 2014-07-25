package com.sirma.cmf.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.cmf.web.userdashboard.filter.DashboardTaskFilter;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.constants.TaskProperties;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * <b>CaseTasksPanel</b> manage functionality for dashlet, located in case dashboard. The content is
 * represented as task records, actions and filters.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseTasksPanel extends
		DashboardPanelActionBase<AbstractTaskInstance, SearchArguments<AbstractTaskInstance>>
		implements Serializable, DashboardPanelController {

	/** The constant serial version identifier. */
	private static final long serialVersionUID = 1553948189077034150L;

	/** The dashlet action located in the toolbar. */
	private final Set<String> dashletActions = new HashSet<String>(
			Arrays.asList(ActionTypeConstants.CREATE_TASK));

	/** The task instances list. */
	private List<AbstractTaskInstance> taskInstances;

	/** The task filters. */
	private List<SelectorItem> taskFilters;

	/** The available task actions. */
	private Map<Serializable, List<Action>> taskActions;

	/**
	 * The authority service
	 */
	@Inject
	private AuthorityService authorityService;

	/** The default task filter. */
	private final String caseDefaultTaskFilter = DashboardTaskFilter.ACTIVE_TASKS.getFilterName();

	/** The selected task filter. */
	private String selectedTaskFilter;

	/** The case instance, represent current context. */
	private CaseInstance context;

	private static final String CASE_TASK_DASHLET_PLACEHOLDER = "case-dashboard-tasks";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initData() {
		init();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		context = getDocumentContext().getInstance(CaseInstance.class);
	}

	/**
	 * Initialize the task panel.
	 */
	public void init() {
		onOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardTaskFilter[] filterTypes = DashboardTaskFilter.values();

		for (DashboardTaskFilter caseDashboardTaskFilter : filterTypes) {
			String filterName = caseDashboardTaskFilter.getFilterName();
			String value = caseDashboardTaskFilter.getLabel(labelProvider);
			filters.add(new SelectorItem(filterName, value, ""));
		}

		taskFilters = filters;
		selectedTaskFilter = caseDefaultTaskFilter;
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
	 * Gets the task filter activator link label.
	 * 
	 * @return the task filter activator link label
	 */
	public String getTaskFilterActivatorLinkLabel() {

		String filterType = caseDefaultTaskFilter;

		if (selectedTaskFilter != null) {
			filterType = selectedTaskFilter;
		}

		return labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
				+ filterType);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<AbstractTaskInstance> searchArguments) {
		log.debug("CMFWeb: Executing CaseTasksPanel.filter: searchArguments " + searchArguments);
		searchService.search(AbstractTaskInstance.class, searchArguments);
		taskInstances = searchArguments.getResult();
		setTaskActions(getActionsForInstances(taskInstances, CASE_TASK_DASHLET_PLACEHOLDER));
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
		Context<String, Object> searchContext = new Context<String, Object>(3);
		searchContext.put(SearchFilterProperties.INCLUDE_OWNER, Boolean.FALSE);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT_KEY,
				TaskProperties.WORKING_INSTANCE_ON_TASK);
		searchContext.put(SearchFilterProperties.INSTANCE_CONTEXT, context);

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
	 * Verify button visibility based on instance and operation.
	 * 
	 * @param instance
	 *            current instance
	 * @param operation
	 *            current operation
	 * @return true - allowed/false - not allowed
	 */
	public boolean isAllowedButton(CaseInstance instance, String operation) {
		return authorityService.isActionAllowed(instance, operation, "");
	}

	/**
	 * Getter for all task filters.
	 * 
	 * @return all task filters
	 */
	public List<SelectorItem> getTaskFilters() {
		return taskFilters;
	}

	/**
	 * Setter for all task filters.
	 * 
	 * @param taskFilters
	 *            container for all task filters
	 */
	public void setTaskFilters(List<SelectorItem> taskFilters) {
		this.taskFilters = taskFilters;
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
		return "case-tasks-dashlet";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

	/**
	 * Setter for task actions.
	 * 
	 * @param taskActions
	 *            task actions
	 */
	public void setTaskActions(Map<Serializable, List<Action>> taskActions) {
		this.taskActions = taskActions;
	}

	/**
	 * Getter for task action based on current instance.
	 * 
	 * @param instance
	 *            current instance
	 * @return list with actions for current instance
	 */
	public List<Action> getTaskActions(Instance instance) {
		if ((instance == null) || (taskActions == null)) {
			return new ArrayList<Action>();
		}
		return taskActions.get(instance.getId());
	}

}
