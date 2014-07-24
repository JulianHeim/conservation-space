/**
 * 
 */
package com.sirma.itt.emf.bam.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for testing the <b>BAMRESTPublisher</b> class.
 * 
 * @author Mihail Radkov
 */
public class BAMRESTPublisherTest {

	/** Host for testing the publisher. */
	private String host = "localhost";

	/** Port for testing the publisher. */
	private int port = 7000;

	/** Username for testing the publisher. */
	private String username = "admin";

	/** Password for testing the publisher. */
	private String password = "admin";

	/** Value for a connection timeout in ms. */
	private int testTimeout = 2000;

	/** The tested publisher. */
	private BAMRESTPublisher publisher;

	/**
	 * Method executed before every test in this class.
	 */
	@Before
	public void before() {
		this.publisher = new BAMRESTPublisher(host, port, username, password, testTimeout);
	}

	/**
	 * Executed after every test.
	 */
	@After
	public void after() {
		try {
			publisher.closePublisher();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.bam.rest.BAMRESTPublisher#BAMRESTPublisher(java.lang.String, int, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testBAMRESTPublisher() {
		HttpHost bamServer = publisher.getBamServer();
		assertEquals("https://" + host + ":" + port, bamServer.toURI());
		assertEquals(3, publisher.getPost().getAllHeaders().length);
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.rest.BAMRESTPublisher#postMethod(java.lang.String)}.
	 */
	@Test
	public void testPostMethodString() {
		// Testing without a server.
		int response = publisher.postMethod("test content");
		assertEquals(0, response);
	}

	/**
	 * Test method for {@link com.sirma.itt.emf.bam.rest.BAMRESTPublisher#setURI(java.lang.String)}.
	 */
	@Test
	public void testSetURI() {
		assertEquals("https://localhost:7000", publisher.getPost().getURI().toString());

		assertTrue(publisher.setURI("/testing"));
		assertEquals("https://localhost:7000/testing", publisher.getPost().getURI().toString());

		assertFalse(publisher.setURI("  r23 f23 f"));
		assertEquals("https://localhost:7000/testing", publisher.getPost().getURI().toString());
	}

	/**
	 * Test method for {@link com.sirma.itt.bam.rest.BAMRESTPublisher#closePublisher()}.
	 */
	@Test
	public void testClosePublisher() {
		try {
			publisher.closePublisher();
			publisher.closePublisher();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.bam.rest.BAMRESTPublisher#setupHeaders(org.apache.http.HttpHost)} .
	 */
	@Test
	public void testSetupHeaders() {
		HttpPost testPost = new HttpPost();
		publisher.setupHeaders(testPost);

		Header[] headers = testPost.getAllHeaders();
		assertEquals(2, headers.length);

		headers = testPost.getHeaders("Accept");
		assertEquals(1, headers.length);
		assertEquals("Accept", headers[0].getName());
		assertEquals("application/json", headers[0].getValue());

		headers = testPost.getHeaders("Content-Type");
		assertEquals(1, headers.length);
		assertEquals("Content-Type", headers[0].getName());
		assertEquals("application/json", headers[0].getValue());

		try {
			publisher.setupHeaders(null);
		} catch (NullPointerException e) {
			fail();
		}
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.bam.rest.BAMRESTPublisher#setAuthentication(java.lang.String,java.lang.String,org.apache.http.HttpPost)}
	 * .
	 */
	@Test
	public void testSetAuthentication() {
		HttpPost testPost = new HttpPost();
		publisher.setAuthentication(username, password, testPost);

		Header[] headers = testPost.getHeaders("Authorization");
		assertEquals(1, headers.length);
		assertEquals("Authorization", headers[0].getName());

		String auth = Base64.encodeBase64String((username + ":" + password).getBytes());
		assertEquals("Basic " + auth, headers[0].getValue());

		try {
			publisher.setAuthentication("user", "pass", null);
			publisher.setAuthentication(null, "pass", null);
			publisher.setAuthentication(null, null, null);
		} catch (NullPointerException e) {
			fail();
		}
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.bam.rest.BAMRESTPublisher#createHost(java.lang.String, int, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateHost() {
		String testHost = "testHost";
		int testPort = 1234;
		String testScheme = "http";

		HttpHost testHttpHost = publisher.createHost(testHost, testPort, testScheme);
		assertEquals(testScheme + "://" + testHost + ":" + testPort, testHttpHost.toURI());

		try {
			testHttpHost = publisher.createHost(null, testPort, testScheme);
			fail();
		} catch (IllegalArgumentException ex) {
		}

		try {
			testHttpHost = publisher.createHost(testHost, testPort, null);
			fail();
		} catch (IllegalArgumentException ex) {
		}

		try {
			testHttpHost = publisher.createHost(testHost, 65599, testScheme);
			fail();
		} catch (IllegalArgumentException ex) {
		}

		testHttpHost = publisher.createHost(testHost, -1, testScheme);
	}

	/**
	 * Test method for {@link com.sirma.itt.bam.rest.BAMRESTPublisher#createClient(int)} .
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateClient() {
		try {
			publisher.createClient(1234);
			publisher.createClient(0);
		} catch (IllegalArgumentException e) {
			fail();
		}
		publisher.createClient(-1);
	}

	/**
	 * Testing the cloning of the publisher's post getter.
	 */
	@Test
	public void cloneTest() {
		HttpPost post = publisher.getPost();
		assertNotEquals(post, publisher.getPost());
	}
}
