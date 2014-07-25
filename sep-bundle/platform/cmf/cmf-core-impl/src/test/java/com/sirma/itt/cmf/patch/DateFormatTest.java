package com.sirma.itt.cmf.patch;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.itt.emf.time.ISO8601DateFormat;

/**
 * Test class for ISO8601DateFormat.
 */
public class DateFormatTest {

	/**
	 * Test parser.
	 */
	@Test
	public void testParser() {
		String isoDate = "2013-05-27T00:00:00.000+03:00";
		Assert.assertEquals(isoDate, ISO8601DateFormat.format(ISO8601DateFormat.parse(isoDate)));
	}

}
