package com.sirma.itt.emf.bam.agent;

import java.util.List;

/**
 * Interface describing an EMF events handler that sends them to a BAM server.
 * 
 * @author Mihail Radkov
 */
public interface BAMAgent {

	/**
	 * Adds an event from EMF to be handled.
	 * 
	 * @param event
	 *            the event's payload
	 */
	void addEvent(List<Object> event);

}
