package com.sirma.itt.idoc.widgets.observer;

import java.util.Arrays;

import javax.enterprise.event.Observes;

import org.json.JSONException;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.idoc.web.events.CreateRelationshipFromWidgetEvent;
import com.sirma.itt.idoc.web.events.Widget;

/**
 * Handles the {@link CreateRelationshipFromWidgetEvent} for a imageWidget widget.
 * 
 * @author yasko
 */
public class ImageWidgetRelationshipObserver extends BaseWidgetRelationshipObserver {

	/**
	 * Observer for the {@link CreateRelationshipFromWidgetEvent} triggered for a imageWidget
	 * widget. This method checks if there are manually selected objects and if there are it creates
	 * a 'references' relationship.
	 * 
	 * @param event
	 *            Event payload.
	 * @throws JSONException
	 *             Error while retrieving data from the json config.
	 */
	public void handleImageWidgetRelationships(
			@Observes @Widget(name = "imageWidget") CreateRelationshipFromWidgetEvent event)
			throws JSONException {

		if (event.getWidgetValue().has("imageId")) {
			InstanceReference pair = getInstanceReference(DocumentInstance.class.getSimpleName()
					.toLowerCase(), event.getWidgetValue().getString("imageId"));
			createObjectReferencesLink(event.getFrom(), Arrays.asList(pair));
		}
	}
}
