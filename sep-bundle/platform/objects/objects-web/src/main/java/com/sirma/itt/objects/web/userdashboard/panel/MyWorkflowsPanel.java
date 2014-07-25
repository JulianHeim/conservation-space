package com.sirma.itt.objects.web.userdashboard.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.SelectorItem;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.cmf.web.userdashboard.filter.DashboardWorkflowFilter;
import com.sirma.cmf.web.userdashboard.panel.WorkflowUtil;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.NamedQueries;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * This class manage project workflows dashlet. The workflows will be retrieved from the semantic
 * repository. Here we can define filters and actions for the panel.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyWorkflowsPanel extends DashboardPanelActionBase<Instance, SearchArguments<Instance>>
		implements Serializable, DashboardPanelController {

	/** The serial version identifier. */
	private static final long serialVersionUID = 1L;

	/** The default workflow filter. */
	private String workflowDefaultFilter = DashboardWorkflowFilter.NOT_COMPLETE.getFilterName();

	/** The list with all available filters. */
	private List<SelectorItem> workflowFilters;

	/** The list with workflow instances. */
	private List<Instance> workflows;

	/** The selected workflow filter. */
	private String selectedWorkflowFilter;

	/** The list with workflow actions for the current panel. */
	private Map<Serializable, List<Action>> workflowActions;

	/** The placeholder for current panel. */
	private static final String WORKFLOW_DASHLET_ACTION_PLACEHOLDER = "user-dashboard-workflow";

	/** The workflow util. */
	@Inject
	private WorkflowUtil workflowUtil;

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
	public void executeDefaultFilter() {
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<Instance> searchArguments) {
		searchService.search(Instance.class, searchArguments);
		setWorkflows(searchArguments.getResult());
		setWorkflowActions(getActionsForInstances(workflows, WORKFLOW_DASHLET_ACTION_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<Instance> getSearchArguments() {

		SearchArguments<Instance> searchArguments = new SearchArguments<>();

		if (DashboardWorkflowFilter.ALL_WORKFLOWS.getFilterName().equals(selectedWorkflowFilter)) {
			searchArguments = searchService.getFilter(NamedQueries.QUERY_ALL_WORKFLOWS_FOR_OBJECT,
					Instance.class, null);
		} else if (DashboardWorkflowFilter.NOT_COMPLETE.getFilterName().equals(
				selectedWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_WORKFLOWS_FOR_OBJECT, Instance.class, null);
		} else if (DashboardWorkflowFilter.HIGH_PRIORITY.getFilterName().equals(
				selectedWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_WORKFLOWS_FOR_OBJECT_WITH_HIGH_PRIORITY,
					Instance.class, null);
		} else if (DashboardWorkflowFilter.OVERDUE.getFilterName().equals(selectedWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_OVERDUE_WORKFLOWS_FOR_OBJECT, Instance.class,
					null);
		}

		if (searchArguments != null) {
			searchArguments.getArguments().put("user", userURI);
		}

		return searchArguments;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		// Auto-generated method stub
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		// Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		// Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		DashboardWorkflowFilter workflowFilter = DashboardWorkflowFilter
				.getFilterType(selectedItemId);
		if (workflowFilter != null) {
			selectedWorkflowFilter = selectedItemId;
		}
	}

	/**
	 * Retrieve the label for workflow activator link.
	 * 
	 * @return activator link label.
	 */
	public String getWorkflowActivatorLinkLabel() {
		String filterType = workflowDefaultFilter;

		if (selectedWorkflowFilter != null) {
			filterType = selectedWorkflowFilter;
		}

		return labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
				+ filterType);
	}

	/**
	 * This method load all filters for workflow dashlet in user dahsboard.
	 */
	@Override
	public void loadFilters() {

		List<SelectorItem> filters = new ArrayList<SelectorItem>();

		DashboardWorkflowFilter[] filterTypes = DashboardWorkflowFilter.values();

		for (DashboardWorkflowFilter workflowFilter : filterTypes) {

			String filterName = workflowFilter.getFilterName();

			filters.add(new SelectorItem(filterName, labelProvider
					.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
							+ filterName), ""));
		}
		workflowFilters = filters;
		selectedWorkflowFilter = workflowDefaultFilter;
	}

	/**
	 * Getter for default workflow filter.
	 * 
	 * @return default filter
	 */
	public String getWorkflowDefaultFilter() {
		return workflowDefaultFilter;
	}

	/**
	 * Setter for default workflow filter.
	 * 
	 * @param workflowDefaultFilter
	 *            default workflow filter
	 */
	public void setWorkflowDefaultFilter(String workflowDefaultFilter) {
		this.workflowDefaultFilter = workflowDefaultFilter;
	}

	/**
	 * Getter for workflow filters.
	 * 
	 * @return workflow filter list
	 */
	public List<SelectorItem> getWorkflowFilters() {
		return workflowFilters;
	}

	/**
	 * Setter for workflow filters.
	 * 
	 * @param workflowFilters
	 *            workflow filters list
	 */
	public void setWorkflowFilters(List<SelectorItem> workflowFilters) {
		this.workflowFilters = workflowFilters;
	}

	/**
	 * Getter for selected workflow filter.
	 * 
	 * @return selected workflow filter
	 */
	public String getSelectedWorkflowFilter() {
		return selectedWorkflowFilter;
	}

	/**
	 * Setter for selected workflow filter.
	 * 
	 * @param selectedWorkflowFilter
	 *            selected workflow filter
	 */
	public void setSelectedWorkflowFilter(String selectedWorkflowFilter) {
		this.selectedWorkflowFilter = selectedWorkflowFilter;
	}

	/**
	 * Getter for workflow instances.
	 * 
	 * @return list with workflow instances
	 */
	public List<Instance> getWorkflows() {
		waitForDataToLoad();
		return workflows;
	}

	/**
	 * Setter for workflow instances.
	 * 
	 * @param workflows
	 *            workflow instance list
	 */
	public void setWorkflows(List<Instance> workflows) {
		this.workflows = workflowUtil.mapWorkflowData(workflows);
	}

	/**
	 * Sets the list with workflow actions for the current panel.
	 * 
	 * @param workflowActions
	 *            the new list with workflow actions for the current panel
	 */
	public void setWorkflowActions(Map<Serializable, List<Action>> workflowActions) {
		this.workflowActions = workflowActions;
	}

	/**
	 * Gets the workflow actions.
	 * 
	 * @param instance
	 *            the instance
	 * @return the workflow actions
	 */
	public List<Action> getWorkflowActions(Instance instance) {
		if (instance == null || workflowActions == null) {
			return new ArrayList<Action>();
		}
		return workflowActions.get(instance.getId());
	}

}
