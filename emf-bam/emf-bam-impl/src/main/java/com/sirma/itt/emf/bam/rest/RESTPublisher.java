/**
 * 
 */
package com.sirma.itt.emf.bam.rest;

import java.io.IOException;

/**
 * Interface describing a data publisher using the POST method and REST.
 * 
 * @author Mihail Radkov
 */
public interface RESTPublisher {

	// TODO: Create a method 'connect' ?

	/**
	 * Sets the URI to which POST methods will be send.
	 * 
	 * @param uri
	 *            the provided URI
	 * @return true if it was set successfully and false if not
	 */
	boolean setURI(String uri);

	/**
	 * Executes a post method to an already set URI with a provided content and returns a status
	 * code.
	 * 
	 * @param content
	 *            the provided content
	 * @return the status code of the response
	 */
	int postMethod(String content);

	/**
	 * Closes the REST publisher.
	 * 
	 * @throws IOException
	 *             if while closing the publisher an IO problem occurs
	 */
	void closePublisher() throws IOException;

}
