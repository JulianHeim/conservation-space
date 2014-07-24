/**
 * 
 */
package com.sirma.itt.emf.bam.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.sirma.itt.emf.bam.TestUtils;

/**
 * Test class for testing <b>CSVLogger</b> class.
 * 
 * @author Mihail Radkov
 */
public class CSVLoggerTest {

	/**
	 * A file path used in this class's tests.
	 */
	private String filePath = System.getProperty("user.home") + System.getProperty("file.separator") + "testingCSVLogger.txt";

	/**
	 * The CSV tested logger.
	 */
	private CSVLogger logger;

	/**
	 * Setup method executed before every method annotated with @Test.
	 */
	@Before
	public void before() {
		TestUtils.deleteFile(filePath);
		logger = new CSVLogger(filePath, '!');
	}

	/**
	 * Cleanup method executed after every method annotated with @Test.
	 */
	@After
	public void after() {
		try {
			logger.closeCSVLogger();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			TestUtils.deleteFile(filePath);
		}
		assertFalse(logger.isFileExisting(filePath));
	}

	/**
	 * Tests the CSVLogger with passing a null value for file path. An IllegalArgumentException is
	 * expected.
	 */
	@Test
	// TODO: How to skip @After?
	public void nullTest() {
		boolean thrown = false;
		CSVLogger logger2 = null;
		try {
			logger2 = new CSVLogger(null, '@');
		} catch (IllegalArgumentException ex) {
			thrown = true;
		}
		assertTrue(thrown);
		assertNull(logger2);
	}

	/**
	 * Test method for {@link com.sirma.itt.emf.bam.csv.CSVLogger#CSVLogger(java.lang.String, char)}
	 * .
	 * 
	 * @throws IOException
	 *             if some IO related problem occurs during testing
	 */
	@Test
	public void testCSVLogger() throws IOException {
		assertTrue(logger.isFileExisting(filePath));
		String logFile = FileUtils.readFileToString(new File(filePath));
		assertEquals("sep=!" + System.getProperty("line.separator"), logFile);

		// Testing with existing file before creating an CSVLogger.
		String testFile2Path = System.getProperty("user.home") + "/testFile2.txt";
		TestUtils.deleteFile(testFile2Path);

		File testFile2 = new File(testFile2Path);
		assertTrue(testFile2.createNewFile());

		CSVLogger logger2 = new CSVLogger(testFile2Path, '*');

		logFile = FileUtils.readFileToString(testFile2);
		assertEquals("", logFile);

		logger2.closeCSVLogger();
		TestUtils.deleteFile(testFile2Path);
	}

	/**
	 * Test method for {@link com.sirma.itt.emf.bam.csv.CSVLogger#writeMessage(java.lang.String[])}.
	 */
	@Test
	public void testWriteMessage() {
		assertTrue(logger.isFileExisting(filePath));

		String[] testContent1 = { "test1", "test2", "test3", "test3", "", "test5" };
		String[] testContent2 = { "apple", "tomatoe", "pepper" };

		logger.writeMessage(testContent1);
		logger.writeMessage(null);
		logger.writeMessage(testContent2);

		CSVReader reader = null;
		List<String[]> results = null;

		try {
			logger.closeCSVLogger();
			reader = new CSVReader(new FileReader(filePath), '!');
			results = reader.readAll();
		} catch (IOException e1) {
			e1.printStackTrace();
			fail();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}

		assertEquals(Arrays.toString(testContent1), Arrays.toString(results.get(1)));
		assertEquals(Arrays.toString(testContent2), Arrays.toString(results.get(2)));

	}

	/**
	 * Test method for {@link com.sirma.itt.emf.bam.csv.CSVLogger#isFileExisting(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testIsFileExisting() {
		assertTrue(logger.isFileExisting(filePath));

		try {
			logger.closeCSVLogger();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		TestUtils.deleteFile(filePath);
		assertFalse(logger.isFileExisting(filePath));

		logger.isFileExisting(null);
	}

	/**
	 * Test method for {@link com.sirma.itt.emf.bam.csv.CSVLogger#closeCSVLogger()}.
	 */
	@Test
	public void testCloseCSVLogger() {
		try {
			logger.closeCSVLogger();
			logger.closeCSVLogger();
			logger.closeCSVLogger();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

}
