package com.sirma.cmf.web.standaloneTask;

import java.io.Serializable;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.DocumentContext;
import com.sirma.cmf.web.constants.NavigationConstants;
import com.sirma.cmf.web.form.FormViewMode;
import com.sirma.cmf.web.form.picklist.PicklistController;
import com.sirma.cmf.web.workflow.task.BaseTaskActions;
import com.sirma.itt.cmf.beans.definitions.TaskDefinition;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.cmf.event.task.standalone.StandaloneTaskClaimEvent;
import com.sirma.itt.cmf.event.task.standalone.StandaloneTaskOnHoldEvent;
import com.sirma.itt.cmf.event.task.standalone.StandaloneTaskReleaseEvent;
import com.sirma.itt.cmf.services.TaskService;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.security.action.EMFAction;
import com.sirma.itt.emf.web.action.event.EMFActionEvent;

/**
 * Action bean for standalone task instance actions.
 * 
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class StandaloneTaskAction extends StandaloneTaskLandingPage implements Serializable {

	private static final long serialVersionUID = -2984866316687601752L;

	private String newAssigneeId;

	@Inject
	private PicklistController picklistController;

	@Inject
	private TaskService taskService;

	/**
	 * Creates the standalone task in task.
	 * 
	 * @param event
	 *            the event
	 */
	public void createStandaloneTaskInTask(
			@Observes @EMFAction(value = ActionTypeConstants.CREATE_TASK, target = StandaloneTaskInstance.class) EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer StandaloneTaskAction.createStandaloneTaskInTask.");
		String navigation = executeCreateStandaloneTask(event.getInstance());
		event.setNavigation(navigation);
	}

	/**
	 * Creates the standalone task in case.
	 * 
	 * @param event
	 *            the event
	 */
	public void createStandaloneTaskInCase(
			@Observes @EMFAction(value = ActionTypeConstants.CREATE_TASK, target = CaseInstance.class) EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer StandaloneTaskAction.createStandaloneTaskInCase.");
		String navigation = executeCreateStandaloneTask(event.getInstance());
		event.setNavigation(navigation);
	}

	/**
	 * Execute create standalone task.
	 * 
	 * @param instance
	 *            the instance
	 * @return the string
	 */
	private String executeCreateStandaloneTask(Instance instance) {
		updateContextInstance(instance);
		return NavigationConstants.STANDALONE_TASK_DETAILS_PAGE;
	}

	/**
	 * Update the new context instance and clear the old task context.
	 * 
	 * @param instance
	 *            current instance
	 */
	private void updateContextInstance(Instance instance) {
		initContextForInstanceEdit(instance);
		removeFromContext();
	}

	/**
	 * This method will gather all data for populating the form, for editing standalone task.
	 * 
	 * @param event
	 *            current event
	 */
	public void editTask(
			@Observes @EMFAction(value = ActionTypeConstants.EDIT_DETAILS, target = StandaloneTaskInstance.class) final EMFActionEvent event) {
		StandaloneTaskInstance standaloneTask = (StandaloneTaskInstance) event.getInstance();

		TaskDefinition taskDefinition = dictionaryService.getDefinition(
				getInstanceDefinitionClass(), standaloneTask.getIdentifier());

		getDocumentContext().populateContext(standaloneTask, getInstanceDefinitionClass(),
				taskDefinition);

		initContextForInstanceEdit(standaloneTask);

		event.setNavigation(NavigationConstants.STANDALONE_TASK_DETAILS_PAGE);
	}

	/**
	 * Reassign operation observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void reassignOperationObserver(
			@Observes @EMFAction(value = ActionTypeConstants.REASSIGN_TASK, target = StandaloneTaskInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer StandaloneTaskAction.reassignOperationObserver");

		getDocumentContext().put(BaseTaskActions.SELECTED_TASK_INSTANCE, event.getInstance());

		picklistController.loadItems(true, ResourceType.USER.getName());

		event.setNavigation(NavigationConstants.RELOAD_PAGE);
	}

	/**
	 * Hold task action observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void holdTask(
			@Observes @EMFAction(value = ActionTypeConstants.SUSPEND, target = StandaloneTaskInstance.class) final EMFActionEvent event) {

		log.debug("CMFWeb: Executing observer StandaloneTaskAction.holdTask");
		StandaloneTaskInstance taskInstance = (StandaloneTaskInstance) event.getInstance();
		eventService.fire(new StandaloneTaskOnHoldEvent(taskInstance));
		taskService.save(taskInstance, createOperation());

		reloadTaskInstance(taskInstance);

		event.setInstance(taskInstance);
		getDocumentContext().put(DocumentContext.FORCE_RELOAD_FORM, true);
		event.setNavigation(NavigationConstants.RELOAD_PAGE);
		getDocumentContext().setFormMode(FormViewMode.EDIT);
		getDocumentContext().clearSelectedAction();
	}

	/**
	 * Claim task from pool action handler. TODO: Optimization
	 * 
	 * @param event
	 *            the event
	 */
	public void claimTask(
			@Observes @EMFAction(value = ActionTypeConstants.CLAIM, target = StandaloneTaskInstance.class) final EMFActionEvent event) {

		log.debug("CMFWeb: Executing observer StandaloneTaskAction.claimTask");

		Instance taskInstance = event.getInstance();
		StandaloneTaskInstance standaloneTaskInstance = null;

		if (taskInstance != null) {
			standaloneTaskInstance = (StandaloneTaskInstance) taskInstance;

			eventService.fire(new StandaloneTaskClaimEvent(standaloneTaskInstance));
			standaloneTaskService.save(standaloneTaskInstance, createOperation());

			reloadTaskInstance(standaloneTaskInstance);
			event.setInstance(standaloneTaskInstance);
			getDocumentContext().put(DocumentContext.FORCE_RELOAD_FORM, true);
			event.setNavigation(NavigationConstants.RELOAD_PAGE);
			getDocumentContext().setFormMode(FormViewMode.EDIT);
			getDocumentContext().clearSelectedAction();
		} else {
			log.error("CMFWeb: Cann't claim task because there is no task in context!");
			return;
		}
	}

	/**
	 * Release task to pool action handler. TODO: Optimization
	 * 
	 * @param event
	 *            the event
	 */
	public void releaseTask(
			@Observes @EMFAction(value = ActionTypeConstants.RELEASE, target = StandaloneTaskInstance.class) final EMFActionEvent event) {

		log.debug("CMFWeb: Executing observer StandaloneTaskAction.releaseTask");

		Instance instance = event.getInstance();
		StandaloneTaskInstance taskInstance = null;

		if (instance != null) {
			taskInstance = (StandaloneTaskInstance) instance;
			eventService.fire(new StandaloneTaskReleaseEvent(taskInstance));
			standaloneTaskService.save(taskInstance, createOperation());

			reloadTaskInstance(taskInstance);

			event.setInstance(taskInstance);
			getDocumentContext().put(DocumentContext.FORCE_RELOAD_FORM, true);
			event.setNavigation(NavigationConstants.RELOAD_PAGE);
			getDocumentContext().setFormMode(FormViewMode.PREVIEW);
		} else {
			log.error("CMFWeb: Cann't release task because there is no task in context!");
			return;
		}

	}

	/**
	 * Reload task instance.
	 * 
	 * @param taskInstance
	 *            the task instance
	 */
	private void reloadTaskInstance(AbstractTaskInstance taskInstance) {
		instanceService.refresh(taskInstance);
	}

	/**
	 * Getter method for newAssigneeId.
	 * 
	 * @return the newAssigneeId
	 */
	public String getNewAssigneeId() {
		return newAssigneeId;
	}

	/**
	 * Setter method for newAssigneeId.
	 * 
	 * @param newAssigneeId
	 *            the newAssigneeId to set
	 */
	public void setNewAssigneeId(String newAssigneeId) {
		this.newAssigneeId = newAssigneeId;
	}

}
