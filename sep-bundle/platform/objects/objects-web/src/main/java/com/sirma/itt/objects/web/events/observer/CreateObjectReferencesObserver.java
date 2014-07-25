package com.sirma.itt.objects.web.events.observer;

import javax.enterprise.event.Observes;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.idoc.web.events.observer.AbstractDocumentLinkHandler;
import com.sirma.itt.objects.constants.ObjectProperties;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.event.ObjectCreatedEvent;
import com.sirma.itt.objects.event.ObjectPersistedEvent;

/**
 * Listens for object created/persisted events and creates 'references' relationships from the
 * object to the objects configured in for display in widgets.
 * 
 * @author yasko
 */
public class CreateObjectReferencesObserver extends AbstractDocumentLinkHandler {

	/**
	 * Listens for the {@link ObjectCreatedEvent} and scans for widgets, so that references could be
	 * created.
	 * 
	 * @param event
	 *            Event payload.
	 */
	public void handleDocumentPersistedEvent(@Observes ObjectCreatedEvent event) {
		handle(event.getInstance());
	}

	/**
	 * Listens for the {@link ObjectPersistedEvent} and scans for widgets, so that references could
	 * be created.
	 * 
	 * @param event
	 *            Event payload.
	 */
	public void handleDocumentPersistedEvent(@Observes ObjectPersistedEvent event) {
		handle(event.getInstance());
	}

	/**
	 * Initiates view content scan for widgets that should create relationships.
	 * 
	 * @param instance
	 *            {@link ObjectInstance} to link from.
	 */
	private void handle(ObjectInstance instance) {
		DocumentInstance documentInstance = (DocumentInstance) instance.getProperties().get(
				ObjectProperties.DEFAULT_VIEW);

		if (documentInstance != null) {
			// FIXME view creation should set this
			documentInstance.setPurpose("iDoc");
			handle(documentInstance, instance);
		}
	}
}