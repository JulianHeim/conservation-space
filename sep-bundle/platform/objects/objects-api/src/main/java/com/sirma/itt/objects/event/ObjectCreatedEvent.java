package com.sirma.itt.objects.event;

import com.sirma.itt.emf.event.AbstractInstanceEvent;
import com.sirma.itt.emf.event.HandledEvent;
import com.sirma.itt.emf.util.Documentation;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * Event fired when {@link ObjectInstance} has been created and saved for the first time. The event
 * is fired after object creation. If the instance is modified in the event observer the flag
 * handled should be set to <code>true</code> so the changes are merged.
 * 
 * @author BBonev
 */
@Documentation("Event fired when {@link ObjectInstance} has been created and saved for the first time."
		+ " The event is fired after object creation. If the instance is modified in the event observer the flag"
		+ " handled should be set to <code>true</code> so the changes are merged.")
public class ObjectCreatedEvent extends AbstractInstanceEvent<ObjectInstance> implements
		HandledEvent {

	/** The handled. */
	private boolean handled;

	/**
	 * Instantiates a new object created event.
	 * 
	 * @param instance
	 *            the instance
	 */
	public ObjectCreatedEvent(ObjectInstance instance) {
		super(instance);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHandled() {
		return handled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHandled(boolean handled) {
		this.handled = handled;
	}

}
