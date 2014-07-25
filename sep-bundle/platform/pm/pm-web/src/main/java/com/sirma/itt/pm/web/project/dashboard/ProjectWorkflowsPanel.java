package com.sirma.itt.pm.web.project.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
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
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * This class manage project workflows panel. The workflows will be retrieved from the semantic
 * repository. Here we can define filters and actions for the panel.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectWorkflowsPanel extends
		DashboardPanelActionBase<Instance, SearchArguments<Instance>> implements Serializable,
		DashboardPanelController {

	/** The constant serial version UID. */
	private static final long serialVersionUID = 1L;

	/** The project workflow placeholder for actions. */
	private static final String PROJECT_WORKFLOW_ACTIONS_PLACEHOLDER = "project-workflow-actions-panel";

	/** The default project workflow filter. */
	private String projectWorkflowDashletFilter = DashboardWorkflowFilter.NOT_COMPLETE
			.getFilterName();

	/** The project workflow filters. */
	private List<SelectorItem> projectWorkflowFilters;

	/** The project workflows list. */
	private List<Instance> projectWorkflows;

	/** The selected project workflow filters. */
	private String selectedProjectWorkflowFilter;

	/** The list with workflow actions for the current panel. */
	private Map<Serializable, List<Action>> workflowActions;

	/** The workflow util. */
	@Inject
	private WorkflowUtil workflowUtil;

	/** The current context instance(project instance). */
	private ProjectInstance context;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		context = getDocumentContext().getInstance(ProjectInstance.class);
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
	public void executeDefaultFilter() {
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<Instance> searchArguments) {
		searchService.search(Instance.class, searchArguments);
		setProjectWorkflows(searchArguments.getResult());
		setWorkflowActions(getActionsForInstances(projectWorkflows,
				PROJECT_WORKFLOW_ACTIONS_PLACEHOLDER));
		notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<Instance> getSearchArguments() {

		SearchArguments<Instance> searchArguments = null;

		if (DashboardWorkflowFilter.ALL_WORKFLOWS.getFilterName().equals(
				selectedProjectWorkflowFilter)) {
			searchArguments = searchService.getFilter(NamedQueries.QUERY_ALL_WORKFLOWS_FOR_OBJECT,
					Instance.class, null);
		} else if (DashboardWorkflowFilter.NOT_COMPLETE.getFilterName().equals(
				selectedProjectWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_WORKFLOWS_FOR_OBJECT, Instance.class, null);
		} else if (DashboardWorkflowFilter.HIGH_PRIORITY.getFilterName().equals(
				selectedProjectWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_WORKFLOWS_FOR_OBJECT_WITH_HIGH_PRIORITY,
					Instance.class, null);
		} else if (DashboardWorkflowFilter.OVERDUE.getFilterName().equals(
				selectedProjectWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_OVERDUE_WORKFLOWS_FOR_OBJECT, Instance.class,
					null);
		}

		if (searchArguments != null) {
			searchArguments.getArguments().put("object", context.getId());
		}

		return searchArguments;
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
	public void updateSelectedFilterField(String selectedItemId) {
		DashboardWorkflowFilter workflowFilter = DashboardWorkflowFilter
				.getFilterType(selectedItemId);
		if (workflowFilter != null) {
			selectedProjectWorkflowFilter = selectedItemId;
		}
	}

	/**
	 * Retrieve the label for project workflow activator link.
	 * 
	 * @return activator link label.
	 */
	public String getWorkflowActivatorLinkLabel() {
		String filterType = projectWorkflowDashletFilter;

		if (selectedProjectWorkflowFilter != null) {
			filterType = selectedProjectWorkflowFilter;
		}

		return labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
				+ filterType);
	}

	/**
	 * This method load all filters for workflow dashlet in project dahsboard.
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
		projectWorkflowFilters = filters;
		selectedProjectWorkflowFilter = projectWorkflowDashletFilter;
	}

	/**
	 * Getter for default project workflow filter.
	 * 
	 * @return default filter
	 */
	public String getprojectWorkflowDashletFilter() {
		return projectWorkflowDashletFilter;
	}

	/**
	 * Setter for default project workflow filter.
	 * 
	 * @param projectWorkflowDashletFilter
	 *            default filter
	 */
	public void setprojectWorkflowDashletFilter(String projectWorkflowDashletFilter) {
		this.projectWorkflowDashletFilter = projectWorkflowDashletFilter;
	}

	/**
	 * Setter for workflow filter list.
	 * 
	 * @return workflow filter list
	 */
	public List<SelectorItem> getprojectWorkflowFilters() {
		return projectWorkflowFilters;
	}

	/**
	 * Setter for workflow filter list.
	 * 
	 * @param projectWorkflowFilters
	 *            workflow filter list
	 */
	public void setprojectWorkflowFilters(List<SelectorItem> projectWorkflowFilters) {
		this.projectWorkflowFilters = projectWorkflowFilters;
	}

	/**
	 * Getter for selected workflow filter.
	 * 
	 * @return selected workflow filter
	 */
	public String getSelectedWorkflowFilter() {
		return selectedProjectWorkflowFilter;
	}

	/**
	 * Setter for selected workflow filter.
	 * 
	 * @param selectedWorkflowFilter
	 *            selected workflow filter
	 */
	public void setSelectedWorkflowFilter(String selectedWorkflowFilter) {
		this.selectedProjectWorkflowFilter = selectedWorkflowFilter;
	}

	/**
	 * Getter for project workflows.
	 * 
	 * @return project workflow list
	 */
	public List<Instance> getProjectWorkflows() {
		waitForDataToLoad();
		return projectWorkflows;
	}

	/**
	 * Setter for project workflows.
	 * 
	 * @param projectWorkflows
	 *            project workflows
	 */
	public void setProjectWorkflows(List<Instance> projectWorkflows) {
		this.projectWorkflows = workflowUtil.mapWorkflowData(projectWorkflows);
	}

	/**
	 * Setter for workflow actions.
	 * 
	 * @param workflowActions
	 *            list with workflow actions
	 */
	public void setWorkflowActions(Map<Serializable, List<Action>> workflowActions) {
		this.workflowActions = workflowActions;
	}

	/**
	 * Getter for workflow actions based on specific instance.
	 * 
	 * @param instance
	 *            current workflow instance
	 * @return lsit with actions
	 */
	public List<Action> getWorkflowActions(Instance instance) {
		if ((instance == null) || (workflowActions == null)) {
			return new ArrayList<Action>();
		}
		return workflowActions.get(instance.getId());
	}
}
