package com.sirma.itt.cmf.event.workflow;

import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.emf.definition.model.TransitionDefinition;
import com.sirma.itt.emf.event.AbstractInstanceTwoPhaseEvent;
import com.sirma.itt.emf.event.OperationEvent;
import com.sirma.itt.emf.util.Documentation;

/**
 * Event fired before workflow transition to be executed in Activiti engine.<br>
 * For more concrete selection is provided a qualifier for the task type
 * {@link com.sirma.itt.cmf.workflows.TaskType} ) and operation id (
 * {@link com.sirma.itt.cmf.workflows.TaskTransition}).
 * 
 * @author BBonev
 */
@SuppressWarnings("unchecked")
@Documentation("Event fired before workflow transition to be executed in Activiti engine. <br>For more concrete selection is provided a qualifier for the task type <b><code>com.sirma.itt.cmf.workflows.TaskType</code></b> and operation id <b><code>link com.sirma.itt.cmf.workflows.TaskTransition</code></b>.")
public class BeforeWorkflowTransitionEvent extends
		AbstractInstanceTwoPhaseEvent<WorkflowInstanceContext, AfterWorkflowTransitionEvent>
		implements OperationEvent {

	/** The task instance. */
	private final TaskInstance taskInstance;

	/** The transition definition. */
	private final TransitionDefinition transitionDefinition;

	/**
	 * Instantiates a new before workflow transition event.
	 * 
	 * @param instance
	 *            the instance
	 * @param taskInstance
	 *            the task instance
	 * @param transitionDefinition
	 *            the transition definition
	 */
	public BeforeWorkflowTransitionEvent(WorkflowInstanceContext instance,
			TaskInstance taskInstance, TransitionDefinition transitionDefinition) {
		super(instance);
		this.taskInstance = taskInstance;
		this.transitionDefinition = transitionDefinition;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AfterWorkflowTransitionEvent createNextEvent() {
		return new AfterWorkflowTransitionEvent(getInstance(), getTaskInstance(),
				getTransitionDefinition());
	}

	/**
	 * Getter method for taskInstance.
	 *
	 * @return the taskInstance
	 */
	public TaskInstance getTaskInstance() {
		return taskInstance;
	}

	/**
	 * Getter method for operationId.
	 *
	 * @return the operationId
	 */
	@Override
	public String getOperationId() {
		return getTransitionDefinition().getIdentifier();
	}

	/**
	 * Getter method for transitionDefinition.
	 * 
	 * @return the transitionDefinition
	 */
	public TransitionDefinition getTransitionDefinition() {
		return transitionDefinition;
	}

}
