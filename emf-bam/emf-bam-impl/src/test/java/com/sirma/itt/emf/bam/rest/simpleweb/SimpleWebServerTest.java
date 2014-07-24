/**
 * 
 */
package com.sirma.itt.emf.bam.rest.simpleweb;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import com.google.gson.Gson;
import com.sirma.itt.emf.bam.rest.BAMRESTPublisher;
import com.sirma.itt.emf.bam.rest.RESTPublisher;

/**
 * Test class for testing the BAMRESTPublisher publishing functionality using a mockup BAM server.
 * 
 * @author Mihail Radkov
 */
public class SimpleWebServerTest {

	/** Server's port. */
	private final int port = 7005;
	/** The username for connecting to the server. */
	private final String username = "admin";
	/** The password for connecting to the server. */
	private final String password = "admin";
	/** Value for a connection timeout in ms. */
	private int testTimeout = 2000;
	/** Connection to the mockup server. */
	private Connection connection;
	/** The tested publisher. */
	private BAMRESTPublisher publisher;
	/** The mockup server. */
	private BAMServerMockup mockupServer;

	/**
	 * Executed before every test. Creates the mockup server.
	 * 
	 * @throws IOException
	 *             if while creating the test server some problem occurs
	 */
	@Before
	public void before() throws IOException {
		mockupServer = new BAMServerMockup(username, password);
		Server server = new ContainerServer(mockupServer);
		connection = new SocketConnection(server);
		SocketAddress address = new InetSocketAddress(port);
		connection.connect(address);
		publisher = new BAMRESTPublisher("localhost", port, "http", username, password, testTimeout);
	}

	/**
	 * Executed after every test. Closes the publisher and shuts down the mockup server.
	 * 
	 * @throws IOException
	 *             if while closing the connection some problem occurs
	 */
	@After
	public void after() throws IOException {
		connection.close();
		closePublisher(publisher);
	}

	/**
	 * Test with correct content.
	 */
	@Test
	public void correctTest() {
		assertEquals(202, publisher.postMethod(generateJson()));
	}

	/**
	 * Test with incorrect content.
	 */
	@Test
	public void wrongContentTest() {
		assertEquals(500, publisher.postMethod("incorrect json"));
	}

	/**
	 * Test with wrong authentication.
	 */
	@Test
	public void wrongAuthTest() {
		BAMRESTPublisher publisher2 = new BAMRESTPublisher("localhost", port, "http", username
				+ "wrong", password + "wrong", testTimeout);
		assertEquals(500, publisher2.postMethod(generateJson()));
		closePublisher(publisher2);
	}

	/**
	 * Tests the publisher's behavior when the server timeouts the session.
	 */
	@Test
	public void sessionTimeoutTest() {
		mockupServer.setSessionTimeout(true);
		assertEquals(202, publisher.postMethod(generateJson()));
	}

	/**
	 * Tests the publisher's behavior when the server timeouts the session.
	 */
	@Test
	public void connectionTimeoutTest() {
		mockupServer.setTimeoutValue(testTimeout + 1000);
		assertEquals(0, publisher.postMethod(generateJson()));
	}

	/**
	 * Closes a REST publisher.
	 * 
	 * @param pub
	 *            the provided REST publisher
	 */
	private void closePublisher(RESTPublisher pub) {
		try {
			pub.closePublisher();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a simple json string.
	 * 
	 * @return generated json
	 */
	private String generateJson() {
		return "[" + new Gson().toJson("some content") + "]";
	}
}
