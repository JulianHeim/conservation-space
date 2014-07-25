package com.sirma.itt.cmf.event.task.standalone;

import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.emf.event.instance.InstanceChangeEvent;
import com.sirma.itt.emf.util.Documentation;

/**
 * REVIEW: this class duplicates the {@link StandaloneTaskChangeEvent}!!!
 * <p>
 * Event fired before saving a {@link StandaloneTaskInstance}. This event is different from
 * {@link StandaloneTaskCompletedEvent}, because when this is fired the task will not be completed
 * in Activiti, but only saved in DB.
 * 
 * @author BBonev
 */
@Documentation("Event fired before saving a {@link StandaloneTaskInstance}.<br>"
		+ "This event is different from {@link StandaloneTaskCompletedEvent}, "
		+ "because when this is fired the task will not be completed in Activiti, but only saved in DB.")
public class StandaloneTaskChangeSaveEvent extends InstanceChangeEvent<StandaloneTaskInstance> {

	/**
	 * Instantiates a new standalone task change save event.
	 *
	 * @param instance
	 *            the instance
	 */
	public StandaloneTaskChangeSaveEvent(StandaloneTaskInstance instance) {
		super(instance);
	}

}
