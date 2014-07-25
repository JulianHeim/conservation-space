package com.sirma.itt.idoc.web.events;


import com.sirma.itt.emf.instance.model.InstanceReference;

/**
 * Event payload object.
 */
public class CreateRelationshipFromLinkEvent {

	/** The instance id. */
	private String instanceId;
	/** The instance type. */
	private String instanceType;
	/** The from. */
	private InstanceReference from;

	/**
	 * Constructor.
	 * 
	 * @param instanceId
	 *            Instance id.
	 * @param instanceType
	 *            Instance type. .
	 * @param from
	 *            Instance from which to create the relationship.
	 */
	public CreateRelationshipFromLinkEvent(String instanceId, String instanceType,
			InstanceReference from) {
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.from = from;
	}

	/**
	 * Gets the instance id.
	 * 
	 * @return the instance id
	 */
	public String getInstanceId() {
		return instanceId;
	}

	/**
	 * Gets the instance type.
	 * 
	 * @return the instance type
	 */
	public String getInstanceType() {
		return instanceType;
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
