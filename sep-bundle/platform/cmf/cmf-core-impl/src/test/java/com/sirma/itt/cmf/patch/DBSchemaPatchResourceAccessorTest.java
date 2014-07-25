package com.sirma.itt.cmf.patch;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.itt.emf.patch.DBSchemaPatch;
import com.sirma.itt.emf.patch.DBSchemaPatchResourceAccessor;

/**
 * Tests for {@link DBSchemaPatchResourceAccessor}.
 * 
 * @author Adrian Mitev
 */
public class DBSchemaPatchResourceAccessorTest {

	/**
	 * Tests the construction of the tests
	 */
	@Test
	public void testChangelogXMLConstruction() {
		List<DBSchemaPatch> patches = new ArrayList<DBSchemaPatch>();
		patches.add(new TestDBSchemaPatch("test/patch1.xml"));
		patches.add(new TestDBSchemaPatch("test/patch2.xml"));

		DBSchemaPatchResourceAccessor accessor = new DBSchemaPatchResourceAccessor(patches);
		String constructedChangelog = accessor.getConstructedChangelog();
		Assert.assertTrue(constructedChangelog.contains("test/patch1.xml"));
		Assert.assertTrue(constructedChangelog.contains("test/patch2.xml"));
	}

	/**
	 * Patch patch used for testing purposes.
	 * 
	 * @author Adrian Mitev
	 */
	private static class TestDBSchemaPatch implements DBSchemaPatch {

		private final String patchPath;

		/**
		 * Initializes patch path.
		 * 
		 * @param patchPath
		 *            this will be return when calling getPath() method.
		 */
		public TestDBSchemaPatch(String patchPath) {
			this.patchPath = patchPath;
		}

		@Override
		public String getPath() {
			return patchPath;
		}

	}

}
