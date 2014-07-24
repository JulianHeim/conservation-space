/**
 * 
 */
package com.sirma.itt.emf.bam.rest.simpleweb;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

/**
 * Mockup server imitating a BAM server behavior.
 * 
 * @author Mihail Radkov
 */
public class BAMServerMockup implements Container {

	/** The mockup server username. */
	private String username;
	/** The mockup server password. */
	private String password;
	/** Flag indicating if the server will timeout the session. */
	private boolean sessionTimeout;
	/** Timeout value. */
	private int timeoutValue;

	/**
	 * Class constructor. Takes user and pass for authentication.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 */
	public BAMServerMockup(String username, String password) {
		this.username = username;
		this.password = password;
		this.timeoutValue = 0;
	}

	/**
	 * Handles requests and responses.
	 * 
	 * @param request
	 *            the incoming request
	 * @param response
	 *            the outgoing response
	 */
	@Override
	public void handle(Request request, Response response) {
		try {
			if (!checkAuth(request)) {
				response.setCode(500);
			} else if (!checkContent(request)) {
				response.setCode(500);
			} else if (sessionTimeout) {
				simulateSessionTimeout(response);
			} else if (timeoutValue > 0) {
				simulateConnectionTimeout(timeoutValue);
			} else {
				response.setCode(202);
			}
			response.getPrintStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simulates a connection timeout. Sleeps for a provided time.
	 * 
	 * @param value
	 *            the provided time in milliseconds
	 */
	private void simulateConnectionTimeout(int value) {
		try {
			Thread.sleep(value);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simulates a session timeout upon a response.
	 * 
	 * @param response
	 *            the provided response
	 */
	private void simulateSessionTimeout(Response response) {
		response.setCode(500);
		response.addValue("Connection", "close");
		sessionTimeout = false;
	}

	/**
	 * Checks the content of the request.
	 * 
	 * @param request
	 *            the provided request
	 * @return true if the content is all right and false if not
	 */
	private boolean checkContent(Request request) {
		if (!"application/json".equalsIgnoreCase(request.getContentType().getType())) {
			return false;
		}
		try {
			String content = request.getContent();
			new JSONArray(content);
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * Checks the authentication parameter in the request.
	 * 
	 * @param request
	 *            the provided request
	 * @return true if it's correct and false otherwise
	 */
	private boolean checkAuth(Request request) {
		String param = request.getValue("Authorization");
		String expected = getAuth();
		if (expected.equals(param)) {
			return true;
		}
		return false;
	}

	/**
	 * Builds the expected authorization.
	 * 
	 * @return the authorization as string
	 */
	private String getAuth() {
		return "Basic " + Base64.encodeBase64String((username + ":" + password).getBytes());
	}

	/**
	 * Sets the timeout.
	 * 
	 * @param timeout
	 *            the new timeout
	 */
	public void setSessionTimeout(boolean timeout) {
		this.sessionTimeout = timeout;
	}

	/**
	 * Sets the timeout value.
	 * 
	 * @param timeoutValue
	 *            the timeoutValue to set
	 */
	public void setTimeoutValue(int timeoutValue) {
		this.timeoutValue = timeoutValue;
	}
}
