package com.sirma.itt.cmf.patch;

import java.util.TimeZone;

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
		int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (60 * 60 * 1000);
		StringBuilder offsetBuilder = new StringBuilder();
		offsetBuilder.append(offset);

		if (offset >= 0) {
			offsetBuilder.insert(0, "+");
		}

		if (Math.abs(offset) < 10) {
			offsetBuilder.insert(1, "0");
		}

		offsetBuilder.append(":00");

		String isoDate = "2013-05-27T00:00:00.000" + offsetBuilder.toString();
		Assert.assertEquals(isoDate, ISO8601DateFormat.format(ISO8601DateFormat.parse(isoDate)));
	}

}
