package com.sirma.itt.emf.security;

import java.util.Collection;
import java.util.Set;
import java.util.Stack;

import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.plugin.SupportablePlugin;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.Role;
import com.sirma.itt.emf.security.model.Sealable;

/**
 * Evaluator definition that can evaluate the {@link Role} for the specific {@link Resource} on the
 * given target object.
 * <p>
 * <b>NOTE: </b>Each implementation should be annotation with {@link RoleEvaluatorType}
 * 
 * @param <T>
 *            the target object type
 * @author BBonev
 */
public interface RoleEvaluator<T> extends SupportablePlugin, Sealable {

	/** The target name. */
	String TARGET_NAME = "RoleEvaluatorOnTarget";

	/**
	 * Adds evaluators with grater priority from current in order of priority to be used as chained
	 * call
	 *
	 * @param evaluators
	 *            is the chained evaluators to set
	 * @return the chain of invoked providers
	 */
	Stack<RoleEvaluator<T>> addChainInOrder(Collection<RoleEvaluator<T>> evaluators);

	/**
	 * Checks if the current evaluator can handle the given object.
	 *
	 * @param target
	 *            is the target object to test
	 * @return <code>true</code> if the current instance can handle the given object
	 */
	boolean canHandle(Object target);

	/**
	 * Evaluates the given target object and determines the user role on that object.
	 *
	 * @param target
	 *            the target object to evaluate
	 * @param resource
	 *            the resource to evaluate against
	 * @param settings
	 *            some runtime settings for evaluation
	 * @return the user role
	 */
	Pair<Role, RoleEvaluator<T>> evaluate(T target, Resource resource,
			RoleEvaluatorRuntimeSettings settings);

	/**
	 * Filter actions for the given target object based on the given user and user role.
	 *
	 * @param target
	 *            the target
	 * @param resource
	 *            the resource to filter against
	 * @param role
	 *            the role
	 * @return the sets of actions after filter
	 */
	Set<Action> filterActions(T target, Resource resource, Role role);

}
