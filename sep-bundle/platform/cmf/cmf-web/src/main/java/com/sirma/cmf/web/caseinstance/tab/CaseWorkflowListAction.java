package com.sirma.cmf.web.caseinstance.tab;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.EntityAction;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.cmf.services.WorkflowService;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.web.tab.SelectedTab;
import com.sirma.itt.emf.web.tab.TabSelectedEvent;

/**
 * Backing bean for case worklfow list tab. Has observer for the tab selection and fetches the
 * workflows for current instance to be ready for the web page.
 * 
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class CaseWorkflowListAction extends EntityAction implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4964890926019654553L;

	/** The workflow service. */
	@Inject
	private WorkflowService workflowService;

	/** The workflow instance contexts. */
	private List<WorkflowInstanceContext> workflowInstanceContexts;

	/**
	 * Observer for the event fired by the case tab api when a tab is selected.
	 * 
	 * @param event
	 *            the event
	 */
	public void workflowListTabSelectedObserver(
			@Observes @SelectedTab(CaseTabConstants.WORKFLOW_LIST) TabSelectedEvent event) {
		log.debug("CMFWeb: Executing CaseWorkflowListAction.workflowListTabSelected observer");
		if (event.getInstance() != null) {
			workflowInstanceContexts = workflowService.getWorkflowsHistory(event.getInstance());
			log.debug("CMFWeb: Loaded " + workflowInstanceContexts
					+ " workflow instances for the case.");
		} else {
			workflowInstanceContexts = CollectionUtils.EMPTY_LIST;
		}
	}

	/**
	 * Getter method for workflowInstanceContexts.
	 * 
	 * @return the workflowInstanceContexts
	 */
	public List<WorkflowInstanceContext> getWorkflowInstanceContexts() {
		return workflowInstanceContexts;
	}

	/**
	 * Setter method for workflowInstanceContexts.
	 * 
	 * @param workflowInstanceContexts
	 *            the workflowInstanceContexts to set
	 */
	public void setWorkflowInstanceContexts(List<WorkflowInstanceContext> workflowInstanceContexts) {
		this.workflowInstanceContexts = workflowInstanceContexts;
	}

}
