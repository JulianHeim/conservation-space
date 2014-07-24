/**
 * 
 */
package com.sirma.itt.emf.bam.rest;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.commons.utils.string.StringUtils;

/**
 * Class designed for publishing data to a BAM server using REST. It has
 * functionality for setting headers, destination and executing POST methods.
 * TODO: More java doc!
 * 
 * @author Mihail Radkov
 */
public class BAMRESTPublisher implements RESTPublisher {

	/** Logger used for displaying messages related to this class. */
	private final Logger logger = LoggerFactory
			.getLogger(BAMRESTPublisher.class);

	/**
	 * Object containing information about the BAM server such as host, port and
	 * scheme.
	 */
	private HttpHost bamServer;

	/** The http client that will execute methods to the BAM server. */
	private CloseableHttpClient httpClient;

	/**
	 * Post method containing necessary data for the BAM server such as headers
	 * and content.
	 */
	private HttpPost post;

	/** Value for connection/socket timeout in milliseconds. */
	private int timeout;

	// TODO: Default constructor?

	/**
	 * Class constructor. Calls setup methods. Use https for scheme. Requires
	 * the following parameters:
	 * 
	 * @param bamHost
	 *            - the BAM server's host
	 * @param bamPort
	 *            - the BAM server's port
	 * @param bamUsername
	 *            - the username used for connecting to BAM
	 * @param bamPassword
	 *            - the password used for connecting to BAM
	 * @param timeout
	 *            connection timeout value in milliseconds
	 */
	public BAMRESTPublisher(String bamHost, int bamPort, String bamUsername,
			String bamPassword, int timeout) {
		this(bamHost, bamPort, "https", bamUsername, bamPassword, timeout);
	}

	/**
	 * Class constructor. Calls setup methods. Requires the following
	 * parameters:
	 * 
	 * @param bamHost
	 *            - the BAM server's host
	 * @param bamPort
	 *            - the BAM server's port
	 * @param scheme
	 *            - the server's scheme
	 * @param bamUsername
	 *            - the username used for connecting to BAM
	 * @param bamPassword
	 *            - the password used for connecting to BAM
	 * @param timeout
	 *            connection timeout value in milliseconds
	 */
	public BAMRESTPublisher(String bamHost, int bamPort, String scheme,
			String bamUsername, String bamPassword, int timeout) {
		bamServer = createHost(bamHost, bamPort, scheme);
		this.timeout = timeout;
		setupPublisher();
		setupHeaders(post);
		setAuthentication(bamUsername, bamPassword, post);
		logger.info("Publisher initialized.");
	}

	/**
	 * Setups the publisher's components - the http client and the post method.
	 */
	private void setupPublisher() {
		httpClient = createClient(timeout);
		post = new HttpPost();
		setURI("");
		logger.debug("Http client and POST method initialized.");
	}

	/**
	 * Creates a new closeable http client based on a provided timeout. The
	 * timeout value sets the connection and socket timeouts. If the value is
	 * below 0, then a new {@link IllegalArgumentException} is thrown.
	 * 
	 * @param timeout
	 *            the provided timeout value
	 * @return new closeable http client
	 */
	public CloseableHttpClient createClient(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException(
					"The timeout cannot be smaller than zero.");
		}
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(timeout).setConnectTimeout(5000).build();
		return HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.build();
	}

	/**
	 * Setups a post method's headers specifying that it will sent
	 * application/json content.
	 * 
	 * @param method
	 *            the post method
	 */
	public void setupHeaders(HttpPost method) {
		if (method != null) {
			method.setHeader("Accept", "application/json");
			method.setHeader("Content-Type", "application/json");
			logger.debug("Accept & Content-Type headers added to POST method.");
		}
	}

	/**
	 * Creates a new instance of HttpHost defined by the provided host, port and
	 * scheme. Performs null checks for the host&scheme and a port range check.
	 * If some of them is not correct an illegal argument exception is thrown.
	 * 
	 * @param host
	 *            - the provided host
	 * @param port
	 *            - the provided port
	 * @param scheme
	 *            - the provided scheme
	 * @return a new instance of HttpHost corresponding to the provided
	 *         parameters
	 */
	public HttpHost createHost(String host, int port, String scheme) {
		if (StringUtils.isNullOrEmpty(host)
				|| StringUtils.isNullOrEmpty(scheme)) {
			throw new IllegalArgumentException(
					"Cannot pass null or empty values for host or scheme.");
		} else if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("Invalid port range.");
		}
		return new HttpHost(host, port, scheme);
	}

	/**
	 * Adds a basic authentication header to the POST method based on the
	 * provided user and password.
	 * 
	 * @param username
	 *            - the provided username
	 * @param password
	 *            - the provided password
	 * @param httpPost
	 *            the post method
	 */
	public void setAuthentication(String username, String password,
			HttpPost httpPost) {
		if (StringUtils.isNotNull(username) && StringUtils.isNotNull(password)
				&& httpPost != null) {
			String auth = Base64.encodeBase64String((username + ":" + password)
					.getBytes());
			httpPost.setHeader("Authorization", "Basic " + auth);
			logger.debug("Authentication added to POST method.");
		}
	}

	/**
	 * {@inheritDoc}If the publisher cannot connect to the server, a zero is
	 * returned. If the server's session times out, it tries to reset it by
	 * creating a new http client and hence creating a new session.
	 */
	// TODO: Synchronized?
	@Override
	public int postMethod(String content) {
		int responseCode = 0;
		// Creating the entity
		post.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON));

		CloseableHttpResponse response = null;
		try {
			// Executing the post method and receiving a response.
			response = httpClient.execute(post);
			responseCode = response.getStatusLine().getStatusCode();

			// Checking if the session with BAM is expired.
			if (responseCode == 500) {
				Header connHeader = response.getFirstHeader("Connection");
				if (connHeader != null && "close".equals(connHeader.getValue())) {
					closeResponse(response);
					httpClient = createClient(timeout);
					response = httpClient.execute(post);
					responseCode = response.getStatusLine().getStatusCode();
				}
			}
		} catch (HttpHostConnectException e) {
			logger.warn("Cannot connect to the server to send an event.");
		} catch (SocketTimeoutException e) {
			logger.warn("Connection with BAM timeouted.", e);
		} catch (IOException e) {
			logger.error(
					"IOException occured while executing the POST method.", e);
			e.printStackTrace();
		} finally {
			closeResponse(response);
		}
		return responseCode;
	}

	/**
	 * {@inheritDoc} The URI is constructed by getting the http host's URI and
	 * the provided URI.
	 */
	@Override
	public boolean setURI(String uri) {
		try {
			post.setURI(new URI(bamServer.toURI() + uri));
			return true;
		} catch (URISyntaxException e) {
			logger.warn(
					"Wrong URI syntax for setting a new URI for the POST method.",
					e);
			return false;
		}
	}

	/**
	 * Closes an instance of <b>CloseableHttpResponse</b>. Performs a check if
	 * the instance is null or not.
	 * 
	 * @param response
	 *            the instance
	 */
	// Test it?
	private void closeResponse(CloseableHttpResponse response) {
		if (response != null) {
			try {
				response.close();
			} catch (IOException e) {
				logger.error(
						"IOException occured while closing a http response.", e);
			}
		}
	}

	/**
	 * {@inheritDoc} Closes the http client and logs the event.
	 */
	@Override
	public void closePublisher() throws IOException {
		httpClient.close();
		// TODO: post.abort(); ?
		logger.info("Http client closed.");
	}

	/**
	 * Getter for bamServer.
	 * 
	 * @return the bamServer
	 */
	public HttpHost getBamServer() {
		return bamServer;
	}

	/**
	 * Getter for post. Returns a clone of the object.
	 * 
	 * @return the post
	 */
	public HttpPost getPost() {
		HttpPost clone = new HttpPost();
		for (Header header : post.getAllHeaders()) {
			clone.addHeader(header);
		}
		clone.setURI(post.getURI());
		clone.setEntity(post.getEntity());
		return clone;
	}

}
