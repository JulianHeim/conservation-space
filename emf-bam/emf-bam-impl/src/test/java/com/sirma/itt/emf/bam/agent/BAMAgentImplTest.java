/**
 * 
 */
package com.sirma.itt.emf.bam.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.sirma.itt.emf.bam.TestUtils;
import com.sirma.itt.emf.bam.csv.CSVLogger;

/**
 * Test class for testing <b>BAMAgentImpl</b>.
 * 
 * @author Mihail Radkov
 */
public class BAMAgentImplTest {

	/** The agent under test. */
	private BAMAgentImpl agent;

	/**
	 * Called before every test.
	 */
	@Before
	public void before() {
		agent = new BAMAgentImpl();
	}

	/**
	 * Creates a sample payload array list.
	 * 
	 * @return a list with sample payload
	 */
	private List<Object> createPayload() {
		List<Object> payload = new ArrayList<Object>();
		payload.add("test1");
		payload.add("test2");
		payload.add("test3");
		payload.add("test4");
		return payload;
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.agent.BAMAgentImpl#constructJsonMessage(java.lang.Object[])}
	 * .
	 */
	@Test
	public void testConstructJsonMessage() {
		List<Object> payload = createPayload();
		String json = agent.constructJsonMessage(payload);
		String expected = "[{\"payloadData\":[\"test1\",\"test2\",\"test3\",\"test4\",null,null,null,null,null,null,null,null,null]}]";
		assertEquals(expected, json);
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.agent.BAMAgentImpl#constructCSVMessage(java.util.List, int, long)}
	 * .
	 */
	@Test
	public void testConstructCSVMessage() {
		List<Object> payload = createPayload();
		int responseCode = 202;
		long sendTime = 9001;
		String[] csvMessage = agent.constructCSVMessage(payload, responseCode,
				sendTime);

		assertEquals(6, csvMessage.length);
		assertEquals(String.valueOf(responseCode), csvMessage[0]);
		assertEquals(String.valueOf(sendTime), csvMessage[1]);
		assertEquals("test1", csvMessage[2]);
		assertEquals("test2", csvMessage[3]);
		assertEquals("test3", csvMessage[4]);
		assertEquals("test4", csvMessage[5]);
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.agent.BAMAgentImpl#readFileAsString(java.lang.String)}
	 * .
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReadFileAsString() {
		String expected = "testing the method readFileAsString in"
				+ System.getProperty("line.separator") + "BAMAgentImpl";
		String filePath = "com/sirma/itt/emf/bam/definitions/testDefinition.def";
		String result = agent.readFileAsString(filePath);

		assertEquals(expected, result);
		agent.readFileAsString("!@#WE$RTY");
		fail();
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.agent.BAMAgentImpl#initializeStreamDefinition(java.lang.String, com.sirma.itt.emf.bam.rest.RESTPublisher)}
	 * .
	 */
	@Test
	public void testInitializeStreamDefinition() {
		MockupRESTPublisher mockup = new MockupRESTPublisher();
		String filePath = "com/sirma/itt/emf/bam/definitions/testDefinition.def";
		String expected = "testing the method readFileAsString in"
				+ System.getProperty("line.separator") + "BAMAgentImpl";

		String streams = "streams";
		String emfStream = "emf stream";

		assertTrue(agent.initializeStreamDefinition(filePath, mockup, streams,
				emfStream));
		assertEquals(expected, mockup.getContent());
		// TODO: Is this a bad test? With a hard coded string?
		assertEquals(emfStream, mockup.getUrl());

		mockup.setCorrect(false);
		assertFalse(agent.initializeStreamDefinition(filePath, mockup, streams,
				emfStream));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.agent.BAMAgentImpl#handleEvent(List, com.sirma.itt.emf.bam.rest.RESTPublisher, com.sirma.itt.emf.bam.csv.CSVLogger)
	 * }
	 * .
	 * 
	 * @throws IOException
	 *             if IO problem occurs while testing
	 */
	@Test
	// TODO: Is this making the other tests oblivious/unnecessary?
	public void testHandleEvent() throws IOException {
		MockupRESTPublisher publisher = new MockupRESTPublisher();

		String filePath = System.getProperty("user.home")
				+ "\\testingCSVLogger.txt";
		TestUtils.deleteFile(filePath);
		CSVLogger csvLogger = new CSVLogger(filePath, '#');

		List<Object> event = new LinkedList<Object>();
		event.add("test");

		long start = System.currentTimeMillis();
		agent.handleEvent(event, publisher, csvLogger);
		long stop = System.currentTimeMillis();

		CSVReader reader = new CSVReader(new FileReader(filePath), '#');
		List<String[]> records = reader.readAll();
		String[] record = records.get(records.size() - 1);
		long time = Long.valueOf(record[1]);

		assertTrue((stop - start) >= time);

		String expected = "[{\"payloadData\":[\"test\",null,null,null,null,null,null,null,null,null,null,null,null]}]";
		assertEquals(expected, publisher.getContent());

		reader.close();
		csvLogger.closeCSVLogger();
		TestUtils.deleteFile(filePath);
	}
}
