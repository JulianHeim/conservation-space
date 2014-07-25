package com.sirma.cmf.web.actionsmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.richfaces.function.RichFunction;

import com.sirma.cmf.web.constants.NavigationConstants;
import com.sirma.cmf.web.form.FormViewMode;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.NullInstance;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.action.ActionTypeBinding;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.state.StateService;
import com.sirma.itt.emf.state.transition.StateTransitionManager;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.web.action.event.EMFActionEvent;

/**
 * Allowed actions manager.
 * 
 * @author svelikov
 */
@Named
@SessionScoped
public class ActionsManager extends com.sirma.cmf.web.Action implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -4100299582266298139L;

	/** Allowed action event object. */
	@Inject
	private Event<EMFActionEvent> allowedActionEvent;

	/** The transition manager. */
	@Inject
	private StateTransitionManager transitionManager;

	/** State service instance. */
	@Inject
	private StateService stateService;

	/** AuthorityService instance. */
	@Inject
	private AuthorityService authorityService;

	/**
	 * Context actions executor. Allowed action name which is provided is used to build and fire cdi
	 * event with dynamic selection of the event qualifier.
	 * <p>
	 * Use the method with context instance.
	 * </p>
	 * 
	 * @param name
	 *            The action name.
	 * @param initiatedOn
	 *            Navigation string of the page where this action was initiated. This is needed when
	 *            is needed to redirect to the same page later.
	 * @return Navigation string.
	 */
	public String executeContextAction(final String name, final String initiatedOn) {

		return executeContextAction(name, initiatedOn, null);
	}

	/**
	 * Execute context action passing current context instance with the event. <br />
	 * REVIEW: This is used only for create case with no context (project) and for create project
	 * operations and should be removed when we have possibility to have these operations from
	 * evaluator.
	 * 
	 * @param actionId
	 *            the action
	 * @param initiatedOn
	 *            the initiated on
	 * @param context
	 *            the context
	 * @return navigation string.
	 */
	public String executeContextAction(final String actionId, final String initiatedOn,
			Instance context) {
		TimeTracker timer = TimeTracker.createAndStart();
		log.debug("Started operation [" + actionId + "] initiated on [" + initiatedOn
				+ "] context [" + context + "]");
		getDocumentContext().addContextInstance(context);
		// we don't have action for these dummy action buttons like 'create case' and 'create
		// project'
		getDocumentContext().clearSelectedAction();
		EMFActionEvent event = fireActionEvent(null, actionId, context, initiatedOn);
		setCurrentOperation(context, actionId);
		log.debug("Operation [ " + actionId + "] took " + timer.stopInSeconds() + " s");
		return event.getNavigation();
	}

	/**
	 * Execute allowed action.
	 * 
	 * @param action
	 *            the action
	 * @param instance
	 *            the instance
	 * @return the string
	 */
	public String executeAllowedAction(Action action, Instance instance) {
		if ((instance != null) && (action != null)) {
			TimeTracker timer = TimeTracker.createAndStart();
			log.debug("Started operation [" + action + "] for instance type["
					+ instance.getClass().getSimpleName() + "] and id[" + instance.getId() + "]");
			String actionId = action.getActionId();
			setCurrentOperation(instance, actionId);
			getDocumentContext().setSelectedAction(action);
			EMFActionEvent event = fireActionEvent(action, actionId, instance, null);
			log.debug("Operation [" + action + "] took " + timer.stopInSeconds() + " s");
			return event.getNavigation();
		}
		log.warn("Can not execute operation [" + action + "] for null instance!");
		return NavigationConstants.RELOAD_PAGE;
	}

	/**
	 * Calculate form view mode according to whether there are uncompleted required instance
	 * properties.
	 * 
	 * @param instance
	 *            the instance
	 * @param actionId
	 *            the action id
	 */
	protected void calculateFormViewMode(Instance instance, String actionId) {
		Set<String> requiredFields = transitionManager.getRequiredFields(instance,
				stateService.getPrimaryState(instance), actionId);
		FormViewMode effectiveFormMode = FormViewMode.PREVIEW;
		boolean containsAll = instance.getProperties().keySet().containsAll(requiredFields);
		if (!containsAll) {
			effectiveFormMode = FormViewMode.EDIT;
		}
		getDocumentContext().setFormMode(effectiveFormMode);
	}

	/**
	 * Fire action event.
	 * 
	 * @param action
	 *            the action
	 * @param actionId
	 *            the action id
	 * @param instance
	 *            the instance
	 * @param navigation
	 *            the navigation
	 * @return the cMF action event
	 */
	protected EMFActionEvent fireActionEvent(Action action, String actionId, Instance instance,
			String navigation) {
		EMFActionEvent event = new EMFActionEvent(instance, navigation, actionId, action);

		Class<?> target = NullInstance.class;
		if (instance != null) {
			target = instance.getClass();
		}

		ActionTypeBinding binding = new ActionTypeBinding(actionId, target);
		allowedActionEvent.select(binding).fire(event);
		return event;
	}

	/**
	 * Generates document allowed actions list.
	 * 
	 * @param documentInstance
	 *            is the document to check
	 * @param placeholder
	 *            the placeholder
	 * @return document allowed actions list.
	 */
	public List<Action> getDocumentActions(Instance documentInstance, String placeholder) {
		List<Action> actions = new ArrayList<Action>();
		if ((documentInstance != null) && (documentInstance instanceof DocumentInstance)) {
			if (!((DocumentInstance) documentInstance).hasDocument()) {
				return actions;
			}

			Set<Action> allowedActions = authorityService.getAllowedActions(documentInstance,
					placeholder);
			if (log.isTraceEnabled()) {
				log.debug("Found actions:" + allowedActions + "\nfor instance:" + documentInstance);
			}
			actions.addAll(allowedActions);
		}
		return actions;
	}

	/**
	 * Gets instance actions actions.
	 * 
	 * @param instance
	 *            the instance
	 * @param placeholder
	 *            the placeholder
	 * @return the actions
	 */
	public List<Action> getActions(Instance instance, String placeholder) {
		if (instance == null) {
			return CollectionUtils.emptyList();
		}
		Set<Action> allowedActions = authorityService.getAllowedActions(instance, placeholder);
		if (log.isTraceEnabled()) {
			log.debug("Found actions:" + allowedActions + "\nfor instance:" + instance);
		}
		return new ArrayList<Action>(allowedActions);
	}

	/**
	 * Calculate onclick attribute for action buttons. If onclick attribute is set, then its value
	 * is used. Otherwise if confirmation attribute is set, then it will be applied.
	 * 
	 * @param action
	 *            the action
	 * @return the onclick attribute as string
	 */
	public String calculateOnclickAttribute(Action action) {
		StringBuilder onclick = new StringBuilder();
		if (StringUtils.isNotNullOrEmpty(action.getOnclick())) {
			onclick.append(action.getOnclick());
		} else {
			if (StringUtils.isNotNullOrEmpty(action.getConfirmationMessage())) {
				onclick.append("return CMF.utilityFunctions.riseConfirmation('")
						.append(action.getConfirmationMessage()).append("', ")
						.append(RichFunction.component("confirmationPopup")).append(")");
			}
		}
		return onclick.toString();
	}

	/**
	 * Sets the current operation.
	 * 
	 * @param instance
	 *            the instance
	 * @param operationId
	 *            the operation id
	 */
	protected void setCurrentOperation(Instance instance, String operationId) {
		if (instance != null) {
			getDocumentContext().setCurrentOperation(instance.getClass().getSimpleName(),
					operationId);
		} else {
			getDocumentContext().setCurrentOperation(NullInstance.class.getSimpleName(),
					operationId);
		}
	}

	/**
	 * Gets the action style class for the action buttons in ui.
	 * 
	 * @param buttonClass
	 *            the button class
	 * @param currentInstance
	 *            the current instance
	 * @param action
	 *            the action
	 * @param compactMode
	 *            the compact mode
	 * @return the action style class
	 */
	public String getActionStyleClass(String buttonClass, Instance currentInstance, Action action,
			String compactMode) {
		// #{btnActionClass!=null? btnActionClass : 'allowed-action-button'}
		// #{currentInstance.getClass().getSimpleName()} #{action.actionId != null ? action.actionId
		// : ''}#{minActionsMode != null ? ' has-tooltip' : ''}
		StringBuilder builder = new StringBuilder();
		if (currentInstance != null) {
			builder.append(currentInstance.getClass().getSimpleName().toLowerCase());
		}
		if (StringUtils.isNotNullOrEmpty(buttonClass)) {
			builder.append(" ").append(buttonClass);
		} else {
			builder.append(" allowed-action-button");
		}
		if ((action != null) && (action.getActionId() != null)) {
			builder.append(" ").append(action.getActionId());
		}
		if (StringUtils.isNotNullOrEmpty(compactMode)) {
			builder.append(" has-tooltip");
		}
		return builder.toString().trim();
	}
}
