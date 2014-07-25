package com.sirma.itt.idoc.web.events;

import org.json.JSONObject;

import com.sirma.itt.emf.instance.model.InstanceReference;

/**
 * Event payload object.
 * 
 * @author yasko
 */
public class CreateRelationshipFromWidgetEvent {

	/** The widget name. */
	private String widgetName;
	/** The widget config. */
	private JSONObject widgetConfig;
	/** The widget value. */
	private JSONObject widgetValue;
	/** The from. */
	private InstanceReference from;

	/**
	 * Constructor.
	 * 
	 * @param widgetName
	 *            Widget name.
	 * @param widgetConfig
	 *            Widget configuration.
	 * @param widgetValue
	 *            Widget specific configuration, instance references, etc.
	 * @param from
	 *            Instance from which to create the relationship.
	 */
	public CreateRelationshipFromWidgetEvent(String widgetName, JSONObject widgetConfig,
			JSONObject widgetValue, InstanceReference from) {
		this.widgetName = widgetName;
		this.widgetConfig = widgetConfig;
		this.widgetValue = widgetValue;
		this.from = from;
	}

	/**
	 * Gets the widget name.
	 * 
	 * @return the widgetName
	 */
	public String getWidgetName() {
		return widgetName;
	}

	/**
	 * Gets the widget config.
	 * 
	 * @return the widgetConfig
	 */
	public JSONObject getWidgetConfig() {
		return widgetConfig;
	}

	/**
	 * Gets the widget value.
	 * 
	 * @return the widgetValue
	 */
	public JSONObject getWidgetValue() {
		return widgetValue;
	}

	/**
	 * Sets the widget value.
	 * 
	 * @param widgetValue
	 *            the widgetValue to set
	 */
	public void setWidgetValue(JSONObject widgetValue) {
		this.widgetValue = widgetValue;
	}

	/**
	 * Gets the from.
	 * 
	 * @return the from
	 */
	public InstanceReference getFrom() {
		return from;
	}

}
