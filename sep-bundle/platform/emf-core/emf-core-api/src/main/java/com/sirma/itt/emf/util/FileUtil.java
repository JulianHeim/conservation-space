package com.sirma.itt.emf.util;

import java.io.File;

import com.sirma.itt.emf.domain.Pair;

/**
 * The Class FileUtil holds some basic methods to work with files.
 */
public class FileUtil {

	/**
	 * Split name and extension and return as pair. If the extension could not be obtained null is
	 * returned as second value.
	 *
	 * @param filenameProperty
	 *            could be file or string
	 * @return the pair <name,extension>
	 */
	public static Pair<String, String> splitNameAndExtension(Object filenameProperty) {
		@SuppressWarnings("unchecked")
		Pair<String, String> result = Pair.NULL_PAIR;
		// Extract the extension
		if (filenameProperty != null) {
			String filename = null;
			if (filenameProperty instanceof File) {
				filename = ((File) filenameProperty).getName();
			} else {
				filename = filenameProperty.toString();
			}
			result = new Pair<String, String>(filename, null);
			if (filename.length() > 0) {
				int index = filename.lastIndexOf('.');
				if ((index > -1) && (index < (filename.length() - 1))) {
					result.setFirst(filename.substring(0, index));
					result.setSecond(filename.substring(index + 1).toLowerCase());
				} else {
					result.setFirst(filename);
				}
			}
		}
		return result;
	}
}
