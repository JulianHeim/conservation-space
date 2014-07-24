/**
 * 
 */
package com.sirma.itt.emf.bam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Ignore;

/**
 * Class containing common methods used in several tests.
 * 
 * @author Mihail Radkov
 */
@Ignore
public final class TestUtils {

	/**
	 * Private constructor for utility class.
	 */
	private TestUtils() {
	}

	/**
	 * Checks if a file pointed by the provided path exists and if so deletes it.
	 * 
	 * @param path
	 *            the provided path
	 */
	@Ignore
	public static void deleteFile(String path) {
		try {
			Files.deleteIfExists(Paths.get(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
