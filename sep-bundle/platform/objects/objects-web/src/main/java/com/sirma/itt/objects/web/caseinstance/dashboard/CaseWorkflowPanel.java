package com.sirma.itt.objects.web.caseinstance.dashboard;

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
import com.sirma.cmf.web.userdashboard.filter.DashboardWorkflowFilter;
import com.sirma.cmf.web.userdashboard.panel.WorkflowUtil;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.NamedQueries;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * This class manage case workflows panel. The workflows will be retrieved from the semantic
 * repository. Here we can define filters and actions for the panel.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseWorkflowPanel extends
		DashboardPanelActionBase<Instance, SearchArguments<Instance>> implements Serializable,
		DashboardPanelController {

	private static final long serialVersionUID = 1L;

	/** The actions placeholder for result records. */
	private static final String CASE_WORKFLOW_ACTIONS_PLACEHOLDER = "case-workflow-actions";

	/** The panel filter actions located in the toolbar. */
	private final Set<String> dashletActions = new HashSet<String>(
			Arrays.asList(ActionTypeConstants.START_WORKFLOW));

	/** The default workflow filter. */
	private String caseWorkflowDefaultFilter = DashboardWorkflowFilter.NOT_COMPLETE.getFilterName();

	/** The list with available workflow filters. */
	private List<SelectorItem> caseWorkflowFilters;

	/** The list with workflow instances. */
	private List<Instance> caseWorkflows;

	private String selectedCaseWorkflowFilter;

	/** The current context instance(case instance). */
	private CaseInstance context;

	/** The list with workflow actions for the current panel. */
	private Map<Serializable, List<Action>> workflowActions;

	/** The workflow util. */
	@Inject
	private WorkflowUtil workflowUtil;

	@Override
	protected void initializeForAsynchronousInvocation() {
		context = getDocumentContext().getInstance(CaseInstance.class);
	}

	@Override
	public void initData() {
		onOpen();
	}

	@Override
	public void executeDefaultFilter() {
		filter(getSearchArguments());
	}

	@Override
	public void filter(SearchArguments<Instance> searchArguments) {
		searchService.search(Instance.class, searchArguments);
		setCaseWorkflows(searchArguments.getResult());
		setWorkflowActions(getActionsForInstances(caseWorkflows, CASE_WORKFLOW_ACTIONS_PLACEHOLDER));
		notifyForLoadedData();
	}

	@Override
	public SearchArguments<Instance> getSearchArguments() {

		SearchArguments<Instance> searchArguments = null;

		if (DashboardWorkflowFilter.ALL_WORKFLOWS.getFilterName()
				.equals(selectedCaseWorkflowFilter)) {
			searchArguments = searchService.getFilter(NamedQueries.QUERY_ALL_WORKFLOWS_FOR_OBJECT,
					Instance.class, null);
		} else if (DashboardWorkflowFilter.NOT_COMPLETE.getFilterName().equals(
				selectedCaseWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_WORKFLOWS_FOR_OBJECT, Instance.class, null);
		} else if (DashboardWorkflowFilter.HIGH_PRIORITY.getFilterName().equals(
				selectedCaseWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_WORKFLOWS_FOR_OBJECT_WITH_HIGH_PRIORITY,
					Instance.class, null);
		} else if (DashboardWorkflowFilter.OVERDUE.getFilterName().equals(
				selectedCaseWorkflowFilter)) {
			searchArguments = searchService.getFilter(
					NamedQueries.QUERY_NOT_COMPLETED_OVERDUE_WORKFLOWS_FOR_OBJECT, Instance.class,
					null);
		}

		if (searchArguments != null) {
			// NOTE: replace literal with constant
			searchArguments.getArguments().put("object", context.getId());
		}

		return searchArguments;
	}

	@Override
	public Set<String> dashletActionIds() {
		return dashletActions;
	}

	@Override
	public String targetDashletName() {
		return "case-workflow-dashlet";
	}

	@Override
	public Instance dashletActionsTarget() {
		if (context == null) {
			context = getDocumentContext().getInstance(CaseInstance.class);
		}
		return context;
	}

	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		DashboardWorkflowFilter workflowFilter = DashboardWorkflowFilter
				.getFilterType(selectedItemId);
		if (workflowFilter != null) {
			selectedCaseWorkflowFilter = selectedItemId;
		}
	}

	/**
	 * Retrieve the label for workflow activator link.
	 * 
	 * @return activator link label.
	 */
	public String getWorkflowActivatorLinkLabel() {
		String filterType = caseWorkflowDefaultFilter;

		if (selectedCaseWorkflowFilter != null) {
			filterType = selectedCaseWorkflowFilter;
		}

		return labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
				+ filterType);
	}

	/**
	 * This method load all filters for workflow dashlet in case dahsboard.
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
		caseWorkflowFilters = filters;
		selectedCaseWorkflowFilter = caseWorkflowDefaultFilter;
	}

	/**
	 * Getter for default workflow filter.
	 * 
	 * @return default filter
	 */
	public String getCaseWorkflowDefaultFilter() {
		return caseWorkflowDefaultFilter;
	}

	/**
	 * Setter for default filter.
	 * 
	 * @param caseWorkflowDefaultFilter
	 *            default filter
	 */
	public void setCaseWorkflowDefaultFilter(String caseWorkflowDefaultFilter) {
		this.caseWorkflowDefaultFilter = caseWorkflowDefaultFilter;
	}

	/**
	 * Getter for list with workflow filters.
	 * 
	 * @return workflow filters
	 */
	public List<SelectorItem> getCaseWorkflowFilters() {
		return caseWorkflowFilters;
	}

	/**
	 * Setter for workflow filter list.
	 * 
	 * @param caseWorkflowFilters
	 *            case workflow filters
	 */
	public void setCaseWorkflowFilters(List<SelectorItem> caseWorkflowFilters) {
		this.caseWorkflowFilters = caseWorkflowFilters;
	}

	/**
	 * Getter for case workflows.
	 * 
	 * @return workflow list
	 */
	public List<Instance> getCaseWorkflows() {
		waitForDataToLoad();
		return caseWorkflows;
	}

	/**
	 * Setter for case workflow.
	 * 
	 * @param caseWorkflows
	 *            case workflows
	 */
	public void setCaseWorkflows(List<Instance> caseWorkflows) {
		this.caseWorkflows = workflowUtil.mapWorkflowData(caseWorkflows);
	}

	/**
	 * Getter for selected workflow filter.
	 * 
	 * @return selected workflow filter
	 */
	public String getSelectedCaseWorkflowFilter() {
		return selectedCaseWorkflowFilter;
	}

	/**
	 * Setter for selected case workflow filter.
	 * 
	 * @param selectedCaseWorkflowFilter
	 *            selected workflow filter
	 */
	public void setSelectedCaseWorkflowFilter(String selectedCaseWorkflowFilter) {
		this.selectedCaseWorkflowFilter = selectedCaseWorkflowFilter;
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
	 *            current instance
	 * @return list with supported actions
	 */
	public List<Action> getWorkflowActions(Instance instance) {
		if ((instance == null) || (workflowActions == null)) {
			return new ArrayList<Action>();
		}
		return workflowActions.get(instance.getId());
	}

}
