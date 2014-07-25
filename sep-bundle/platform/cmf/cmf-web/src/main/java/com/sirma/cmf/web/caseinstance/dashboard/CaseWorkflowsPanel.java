package com.sirma.cmf.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.cmf.services.WorkflowService;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * CaseWorkflowsPanel backing bean.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseWorkflowsPanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	private static final long serialVersionUID = -2035974545606925473L;

	private final Set<String> dashletActions = new HashSet<String>(
			Arrays.asList(ActionTypeConstants.START_WORKFLOW));

	@Inject
	private WorkflowService workflowService;

	private List<WorkflowInstanceContext> workflowInstances;

	private CaseInstance context;

	@Override
	public void initData() {
		onOpen();
	}

	@Override
	protected void initializeForAsynchronousInvocation() {
		context = getDocumentContext().getInstance(CaseInstance.class);
	}

	@Override
	public void executeDefaultFilter() {
		List<WorkflowInstanceContext> workflowsHistory = workflowService
				.getWorkflowsHistory(context);
		if (workflowsHistory.isEmpty()) {
			workflowInstances = new ArrayList<WorkflowInstanceContext>();
		} else {
			workflowInstances = workflowsHistory;
		}

		notifyForLoadedData();

		log.debug("CMFWeb: found [" + workflowInstances.size() + "] workflow instances");
	}

	@Override
	public void loadFilters() {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		// not used
		return null;
	}

	/**
	 * Getter method for workflowInstances.
	 * 
	 * @return the workflowInstances
	 */
	public List<WorkflowInstanceContext> getWorkflowInstances() {
		waitForDataToLoad();
		return workflowInstances;
	}

	/**
	 * Setter method for workflowInstances.
	 * 
	 * @param workflowInstances
	 *            the workflowInstances to set
	 */
	public void setWorkflowInstances(List<WorkflowInstanceContext> workflowInstances) {
		this.workflowInstances = workflowInstances;
	}

	@Override
	public Set<String> dashletActionIds() {
		return dashletActions;
	}

	@Override
	public String targetDashletName() {
		return "case-workflows-dashlet";
	}

	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

}
