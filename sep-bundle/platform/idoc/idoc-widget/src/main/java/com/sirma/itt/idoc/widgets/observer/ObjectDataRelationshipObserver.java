package com.sirma.itt.idoc.widgets.observer;

import java.util.Arrays;

import javax.enterprise.event.Observes;

import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.idoc.web.events.CreateRelationshipFromWidgetEvent;
import com.sirma.itt.idoc.web.events.Widget;

/**
 * Handles the {@link CreateRelationshipFromWidgetEvent} for a objectData widget.
 * 
 * @author yasko
 */
public class ObjectDataRelationshipObserver extends BaseWidgetRelationshipObserver {

	/**
	 * Observer for the {@link CreateRelationshipFromWidgetEvent} triggered for a objectData widget.
	 * This method checks if there are manually selected objects and if there are it creates a
	 * 'references' relationship.
	 * 
	 * @param event
	 *            Event payload.
	 * @throws JSONException
	 *             Error while retrieving data from the json config.
	 */
	public void handleObjectDataRelationships(
			@Observes @Widget(name = "objectData") CreateRelationshipFromWidgetEvent event)
			throws JSONException {

		JSONObject value = event.getWidgetValue();
		JSONObject config = event.getWidgetConfig();

		String selectionMethod = JsonUtil.getStringValue(config, "objectSelectionMethod");
		
		if ("current-object".equals(selectionMethod) || selectionMethod == null) {
			return;
		}

		JSONObject selectedObject = JsonUtil.getJsonObject(value, "selectedObject");
		if (selectedObject != null) {
			String id = JsonUtil.getStringValue(selectedObject, "dbId");
			String type = JsonUtil.getStringValue(selectedObject, "type");
			if ((id == null) || (type == null)) {
				return;
			}
			
			if (id.equals(event.getFrom().getIdentifier())) {
				return;
			}

			InstanceReference pair = getInstanceReference(type, id);
			createObjectReferencesLink(event.getFrom(), Arrays.asList(pair));
		}
	}
}
