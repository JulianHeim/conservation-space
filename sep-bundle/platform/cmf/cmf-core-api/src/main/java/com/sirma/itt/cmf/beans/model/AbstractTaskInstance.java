package com.sirma.itt.cmf.beans.model;

import java.io.Serializable;
import java.util.HashMap;

import com.sirma.itt.emf.domain.model.Node;
import com.sirma.itt.emf.instance.model.EmfInstance;
import com.sirma.itt.emf.instance.model.ScheduleSynchronizationInstance;

/**
 * Base instance class for task implementations.
 *
 * @author BBonev
 */
@SuppressWarnings("serial")
public abstract class AbstractTaskInstance extends EmfInstance implements
		ScheduleSynchronizationInstance {

	/** The task instance id. */
	protected String taskInstanceId;
	/** The state. */
	protected TaskState state;
	/** isEditable. */
	protected boolean editable;
	/** is reassignable. */
	protected boolean reassignable;

	/**
	 * Default initialization for task. {@link AbstractTaskInstance#setProperties(java.util.Map)} is
	 * invoked to produce not managed by cdi instnace.
	 */
	public AbstractTaskInstance() {
		setProperties(new HashMap<String, Serializable>());
	}

	/**
	 * Checks if is standalone.
	 *
	 * @return true, if is standalone
	 */
	public abstract boolean isStandalone();

	/**
	 * Checks if is over due.
	 *
	 * @return true, if is over due
	 */
	public abstract boolean isOverDue();

	/**
	 * Gets the task instance id.
	 *
	 * @return the task instance id
	 */
	public String getTaskInstanceId() {
		return taskInstanceId;
	}

	/**
	 * Setter method for taskInstanceId.
	 *
	 * @param taskInstanceId
	 *            the taskInstanceId to set
	 */
	public void setTaskInstanceId(String taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	/**
	 * Getter method for state.
	 *
	 * @return the state
	 */
	public TaskState getState() {
		return state;
	}

	/**
	 * Setter method for state.
	 *
	 * @param state
	 *            the state to set
	 */
	public void setState(TaskState state) {
		this.state = state;
	}

	/**
	 * Checks if is reassignable.
	 *
	 * @return the reassignable
	 */
	public boolean isReassignable() {
		return reassignable;
	}

	/**
	 * Sets the reassignable.
	 *
	 * @param reassignable
	 *            the reassignable to set
	 */
	public void setReassignable(boolean reassignable) {
		this.reassignable = reassignable;
	}

	/**
	 * Checks if is editable.
	 *
	 * @return the editable
	 */
	public boolean isEditable() {
		return editable || (getState() == TaskState.IN_PROGRESS);
	}

	/**
	 * Sets the editable.
	 *
	 * @param editable
	 *            the editable to set
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasChildren() {
		for (Serializable serializable : getProperties().values()) {
			if (serializable instanceof Node) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Node getChild(String name) {
		Serializable serializable = getProperties().get(name);
		if (serializable instanceof Node) {
			return (Node) serializable;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((getId() == null) ? 0 : getId().hashCode());
		result = (prime * result) + ((taskInstanceId == null) ? 0 : taskInstanceId.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AbstractTaskInstance other = (AbstractTaskInstance) obj;
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!getId().equals(other.getId())) {
			return false;
		}
		if (taskInstanceId == null) {
			if (other.taskInstanceId != null) {
				return false;
			}
		} else if (!taskInstanceId.equals(other.taskInstanceId)) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the task type.
	 *
	 * @return the task type
	 */
	public abstract TaskType getTaskType();

	/**
	 * Sets the task type.
	 *
	 * @param type
	 *            the new task type
	 */
	public void setTaskType(TaskType type) {
		// empty bean method to match a bean definition
	}

}
