/**
 * 
 */
package com.sirma.itt.emf.bam.agent;

import java.io.IOException;

import com.sirma.itt.emf.bam.rest.RESTPublisher;

/**
 * Mockup class used for testing.
 * 
 * @author Mihail Radkov
 */
public class MockupRESTPublisher implements RESTPublisher {

	/** The correct. */
	private boolean correct;

	/** The url. */
	private String url;

	/** The content. */
	private String content;

	/**
	 * Default constructor.
	 */
	public MockupRESTPublisher() {
		correct = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setURI(String uri) {
		this.url = uri;
		return correct;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int postMethod(String content) {
		this.content = content;
		if (correct) {
			return 202;
		}
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void closePublisher() throws IOException {
	}

	/**
	 * Checks if is correct.
	 * 
	 * @return true, if is correct
	 */
	public boolean isCorrect() {
		return correct;
	}

	/**
	 * Sets the correct.
	 * 
	 * @param correct
	 *            the new correct
	 */
	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	/**
	 * Gets the url.
	 * 
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the url.
	 * 
	 * @param url
	 *            the new url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Gets the content.
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Sets the content.
	 * 
	 * @param content
	 *            the new content
	 */
	public void setContent(String content) {
		this.content = content;
	}

}
