/**
 * 
 */
package com.sirma.itt.emf.bam.agent;

import com.google.gson.Gson;

/**
 * Simple class describing a BAM event with a payload data. Contains functionality for converting it
 * to a JSON string.
 * 
 * @author Mihail Radkov
 */
public class BAMEvent {

	/** Event's payload data. */
	private Object[] payloadData;

	/** Makes a JSON string representation of instances of this class. */
	private static final transient Gson GSON = new Gson();

	/**
	 * Class constructor. It requires the following parameter:
	 * 
	 * @param payloadData
	 *            - payload data for the event
	 */
	public BAMEvent(Object[] payloadData) {
		this.payloadData = payloadData;
	}

	/**
	 * Converts instances of this class to a JSON string representation.
	 * 
	 * @return a JSON string
	 */
	public String toJson() {
		return "[" + GSON.toJson(this) + "]";
	}

	/**
	 * Getter for payloadData.
	 * 
	 * @return the payloadData
	 */
	public Object[] getPayloadData() {
		return payloadData;
	}

	/**
	 * Setter for payloadData.
	 * 
	 * @param payloadData
	 *            the payloadData to set
	 */
	public void setPayloadData(Object[] payloadData) {
		this.payloadData = payloadData;
	}
}
