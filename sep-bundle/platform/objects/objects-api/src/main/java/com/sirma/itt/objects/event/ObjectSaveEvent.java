package com.sirma.itt.objects.event;

import com.sirma.itt.emf.event.AbstractInstanceEvent;
import com.sirma.itt.emf.util.Documentation;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * Event fired before {@link ObjectInstance} to be saved to database.
 * 
 * @author BBonev
 */
@Documentation("Event fired before {@link ObjectInstance} to be saved to database.")
public class ObjectSaveEvent extends AbstractInstanceEvent<ObjectInstance> {

	/**
	 * Instantiates a new object save event.
	 * 
	 * @param instance
	 *            the instance
	 */
	public ObjectSaveEvent(ObjectInstance instance) {
		super(instance);
	}

}
