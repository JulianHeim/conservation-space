package com.sirma.itt.cmf.beans.model;

/**
 * The TaskState. Also used in task definition xml for task list table value
 * attributes.
 * 
 * @author BBonev
 */
public enum TaskState {

	/** The in progress task state. */
	IN_PROGRESS,
	/** The completed task state. */
	COMPLETED,
	/** Reserved value for when searching tasks. */
	ALL;
}
