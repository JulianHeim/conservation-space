package com.sirma.itt.cmf.security.evaluator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.constants.TaskProperties;
import com.sirma.itt.cmf.constants.allowed_action.AllowedActionType;
import com.sirma.itt.cmf.domain.ObjectTypesCmf;
import com.sirma.itt.cmf.services.TaskService;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.RoleEvaluator;
import com.sirma.itt.emf.security.RoleEvaluatorRuntimeSettings;
import com.sirma.itt.emf.security.RoleEvaluatorType;
import com.sirma.itt.emf.security.SecurityModel;
import com.sirma.itt.emf.security.evaluator.BaseRoleEvaluator;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.EmfAction;
import com.sirma.itt.emf.security.model.Role;
import com.sirma.itt.emf.security.model.RoleIdentifier;

/**
 * Evaluator implementation for standalone task instances.
 *
 * @author BBonev
 */
@ApplicationScoped
@RoleEvaluatorType(ObjectTypesCmf.STANDALONE_TASK)
@Extension(target = RoleEvaluator.TARGET_NAME, order = 45)
public class StandaloneTaskRoleEvaluator extends BaseRoleEvaluator<StandaloneTaskInstance>
		implements RoleEvaluator<StandaloneTaskInstance> {
	private static final List<Class<?>> SUPPORTED = Arrays
			.asList(new Class<?>[] { StandaloneTaskInstance.class });
	/** The Constant HIGH_PRIORITY_ROLES. */
	private static final RoleIdentifier[] HIGH_PRIORITY_ROLES = new RoleIdentifier[] {
			SecurityModel.ActivitiRoles.POSSIBLE_ASSIGNEE, SecurityModel.ActivitiRoles.ASSIGNEE };

	/** The reassign. */
	public static final Action REASSIGN = new EmfAction(AllowedActionType.REASSIGN_TASK.getType());

	/** The subtask. */
	public static final Action SUBTASK_CREATE = new EmfAction(
			AllowedActionType.SUBTASK_CREATE.getType());
	/** The start progress. */
	public static final Action START_PROGRESS = new EmfAction(
			AllowedActionType.TASK_START_PROGRESS.getType());

	/** The hold. */
	public static final Action HOLD = new EmfAction(AllowedActionType.TASK_HOLD.getType());
	/** The claim. */
	public static final Action CLAIM = new EmfAction(AllowedActionType.TASK_CLAIM.getType());
	public static final Action CANCEL = new EmfAction(AllowedActionType.STOP.getType());
	/** The release. */
	public static final Action RELEASE = new EmfAction(AllowedActionType.TASK_RELEASE.getType());

	/** Task edit. */
	public static final Action EDIT = new EmfAction(
			AllowedActionType.STANDALONE_EDIT_TASK.getType());

	/** The task service. */
	@Inject
	private TaskService taskService;

	@Override
	protected RoleIdentifier[] getPreferredHighPriorityRoles() {
		return HIGH_PRIORITY_ROLES;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<Role, RoleEvaluator<StandaloneTaskInstance>> evaluate(
			StandaloneTaskInstance target, Resource resource, RoleEvaluatorRuntimeSettings settings) {
		if ((target == null) || (resource == null)) {
			return null;
		}
		Pair<Role, RoleEvaluator<StandaloneTaskInstance>> role = evaluateInternal(target, resource,
				settings);
		Pair<Role, RoleEvaluator<StandaloneTaskInstance>> highestRole = calculateHighestRoleFromChain(
				target, resource, role, settings, getPreferredHighPriorityRoles());
		return highestRole;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Pair<Role, RoleEvaluator<StandaloneTaskInstance>> evaluateInternal(
			StandaloneTaskInstance target, Resource resource,
			final RoleEvaluatorRuntimeSettings settings) {

		Map<String, Serializable> properties = target.getProperties();
		// may be started by
		String userId = resource.getIdentifier();
		if (userId.equals(properties.get(TaskProperties.CREATED_BY))
				|| userId.equals(properties.get(TaskProperties.START_BY))) {
			if (taskService.isClaimable(target, userId)) {
				return constructRoleModel(SecurityModel.ActivitiRoles.POSSIBLE_ASSIGNEE);
			}// if he owns the task
			else if (userId.equals(properties.get(TaskProperties.TASK_OWNER))
			// || taskService.isReleasable(target, resource.getIdentifier())
			) {
				return constructRoleModel(SecurityModel.ActivitiRoles.ASSIGNEE);
			}
			return constructRoleModel(SecurityModel.BaseRoles.CREATOR);
		}
		// if he owns the task
		if (userId.equals(properties.get(TaskProperties.TASK_OWNER))
				|| taskService.isReleasable(target, userId)) {
			return constructRoleModel(SecurityModel.ActivitiRoles.ASSIGNEE);
		}
		if (taskService.isClaimable(target, userId)) {
			return constructRoleModel(SecurityModel.ActivitiRoles.POSSIBLE_ASSIGNEE);
		}
		// if it is admin
		if (isAdminOrSystemUser(resource)) {
			return constructRoleModel(SecurityModel.BaseRoles.ADMINISTRATOR);
		}
		// TODO: do we check WF?
		return constructRoleModel(SecurityModel.BaseRoles.CONSUMER);
	}

	@Override
	protected Boolean filterInternal(StandaloneTaskInstance target, Resource resource, Role role,
			Set<Action> actions) {

		Map<String, Serializable> properties = target.getProperties();

		Serializable owner = properties.get(TaskProperties.TASK_OWNER);
		if (SecurityModel.ActivitiRoles.POSSIBLE_ASSIGNEE.equals(role.getRoleId())
				|| (owner == null)) {
			// if (!taskService.isClaimable(target, resource.getIdentifier())) {
			// actions.clear();
			// } else {
			// actions.retainAll(Collections.singletonList(CLAIM));
			// }
			actions.retainAll(Collections.singletonList(CLAIM));
			return Boolean.FALSE;
		}
		actions.remove(CLAIM);

		// assignee role
		String resourceId = resource.getIdentifier();
		if (resourceId.equals(owner)) {
			actions.remove(CANCEL);
			// REVIEW: this state checks should be removed
			if (!taskService.isReleasable(target, resourceId)) {
				actions.remove(RELEASE);
			}
			// state actions should be filter on transition state
			// String status = (String) properties.get(TaskProperties.STATUS);
			// if (stateService.isState(PrimaryStates.OPENED, allowedClass(), status)) {
			// actions.remove(START_PROGRESS);
			// } else if (stateService.isState(PrimaryStates.ON_HOLD, allowedClass(), status)) {
			// actions.remove(HOLD);
			// }
		} else {
			actions.remove(START_PROGRESS);
			actions.remove(RELEASE);
		}
		// if the action survived till now lets check if there is something to create at all
		if ((actions.contains(SUBTASK_CREATE) && !instanceService.isChildAllowed(target,
				ObjectTypesCmf.STANDALONE_TASK)) || (owner == null)) {
			actions.remove(SUBTASK_CREATE);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getContainer(StandaloneTaskInstance target) {
		return target.getContainer();
	}

	@Override
	public List<Class<?>> getSupportedObjects() {
		return SUPPORTED;
	}

}
