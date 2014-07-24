/**
 * 
 */
package com.sirma.itt.emf.bam.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.instance.model.Instance;

/**
 * Test class for testing the functionality of <b>EmfEventObserver</b>.
 * 
 * @author Mihail Radkov
 */
public class EmfEventObserverTest {

	/**
	 * Object of EmfEventObserver used in the tests below.
	 */
	private EmfEventObserver emfEventObserver;

	/**
	 * Executed before each test.
	 */
	@Before
	public void before() {
		emfEventObserver = new EmfEventObserver();
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.integration.EmfEventObserver#constructPayload(java.util.Map, java.lang.String, java.lang.String[])}
	 * .
	 */
	@Test
	public void testConstructPayload() {
		Map<String, Serializable> properties = new HashMap<>();
		properties.put("prop1", "property 1");
		properties.put("prop2", "property 232");
		properties.put("modifiedBy", "property 3");
		properties.put("identifier", "object identifier");

		List<Object> result = emfEventObserver.constructPayload(properties, "testObject", "prop2",
				null, "prop1");

		assertEquals(7, result.size());

		assertEquals("property 3", result.get(0).toString());
		assertEquals("testObject", result.get(1).toString());
		assertEquals("property 232", result.get(2).toString());
		assertEquals(null, result.get(3));
		assertEquals("property 1", result.get(4).toString());
		assertEquals(null, result.get(5));
		assertEquals("object identifier", result.get(6).toString());
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.integration.EmfEventObserver#constructPayload(java.lang.String, com.sirma.itt.emf.security.model.User)}
	 * .
	 */
	@Test
	public void testHandleUserAction() {
		String action = "test action";
		String user = "auth user";

		String timeFormat = "YYYYMMddHHmmssSSS";
		long time = System.currentTimeMillis();
		String expectedTimestamp = new SimpleDateFormat(timeFormat).format(time);

		List<Object> result = emfEventObserver.constructPayload(action, user, time);

		assertEquals(3, result.size());
		assertTrue(result.get(0).toString().startsWith(expectedTimestamp));
		assertEquals(action, result.get(1).toString());
		assertEquals(user, result.get(2).toString());
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.integration.EmfEventObserver#getProperties(com.sirma.itt.emf.domain.model.Entity)}
	 * .
	 */
	@Test
	public void testGetProperties() {
		Map<String, Serializable> properties = new HashMap<>();
		properties.put("prop1", "property 1");
		properties.put("prop2", "property 2");

		CaseInstance caseInstance = new CaseInstance();
		caseInstance.setProperties(properties);

		Map<String, Serializable> result = emfEventObserver.getProperties(caseInstance);

		assertNotNull(result);
		assertEquals(properties, result);

		assertNull(emfEventObserver.getProperties(null));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.integration.EmfEventObserver#isChanged(java.util.Map, java.util.Map)}
	 * .
	 */
	@Test
	public void testIsChanged() {
		Map<String, Serializable> added = new HashMap<>();
		Map<String, Serializable> removed = new HashMap<>();
		added.put("1", "test1");
		removed.put("1", "test1");

		assertFalse(emfEventObserver.isChanged(added, removed));
		added.put("2", "test2");
		assertTrue(emfEventObserver.isChanged(added, removed));
		removed.put("2", "test2");
		assertFalse(emfEventObserver.isChanged(added, removed));
		removed.put("2", "test3");
		assertTrue(emfEventObserver.isChanged(added, removed));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.integration.EmfEventObserver#getObjectID(com.sirma.itt.emf.domain.model.Entity)}
	 * .
	 */
	@Test
	public void testGetObjectID() {
		Entity instance = new MockupEntity();
		Long id = 9000L;
		instance.setId(id);

		assertNotNull(emfEventObserver.getObjectID(instance));
		assertEquals(id.toString(), emfEventObserver.getObjectID(instance));

		instance = null;
		assertNull(emfEventObserver.getObjectID(instance));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.integration.EmfEventObserver#handleInstanceProperties(com.sirma.itt.emf.instance.model.Instance)}
	 * .
	 */
	@Test
	public void testHandlePropertyChangeEvent() {

		List<Object> testList = emfEventObserver.handleInstanceProperties(new CaseInstance());
		assertNotNull(testList);
		assertEquals(0, testList.size());

		testList = emfEventObserver.handleInstanceProperties(setupInstance(new CaseInstance()));
		assertNotNull(testList);
		assertTrue(testList.contains("caseinstance"));

		testList = emfEventObserver.handleInstanceProperties(setupInstance(new DocumentInstance()));
		assertNotNull(testList);
		assertTrue(testList.contains("documentinstance"));

		testList = emfEventObserver.handleInstanceProperties(setupInstance(new TaskInstance()));
		assertNotNull(testList);
		assertTrue(testList.contains("taskinstance"));

		testList = emfEventObserver
				.handleInstanceProperties(setupInstance(new StandaloneTaskInstance()));
		assertNotNull(testList);
		assertTrue(testList.contains("standalonetaskinstance"));

		testList = emfEventObserver.handleInstanceProperties(setupInstance(new SectionInstance()));
		assertNull(testList);
	}

	/**
	 * Adds a properties map to an instance.
	 * 
	 * @param instance
	 *            the provided instance.
	 * @return the instance with the properties map
	 */
	private Instance setupInstance(Instance instance) {
		instance.setProperties(new LinkedHashMap<String, Serializable>());
		return instance;
	}
}
