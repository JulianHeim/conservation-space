/**
 * 
 */
package com.sirma.itt.emf.bam.rest;

import java.io.IOException;

/**
 * Dummy publisher that doesn't contain any functionality.
 * 
 * @author Mihail Radkov
 */
public class DummyRESTPublisher implements RESTPublisher {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setURI(String uri) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int postMethod(String content) {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void closePublisher() throws IOException {
	}

}
