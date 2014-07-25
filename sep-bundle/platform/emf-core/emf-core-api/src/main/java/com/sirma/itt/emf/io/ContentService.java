package com.sirma.itt.emf.io;

import java.io.File;
import java.io.OutputStream;

import com.sirma.itt.emf.adapter.DMSFileDescriptor;

/**
 * The Interface ContentService provides means for working/accessing with documents content.
 *
 * @author BBonev
 */
public interface ContentService {

	/**
	 * Fetches the content for the given dms descriptor and store it in a temporary file
	 *
	 * @param location
	 *            the location to fetch
	 * @return the path to the local file
	 */
	public File getContent(DMSFileDescriptor location);

	/**
	 * Fetches the location content and store it in a temporary file.
	 *
	 * @param location
	 *            the location to fetch
	 * @param fileName
	 *            the file name (last segment) to store the file as.
	 * @return the path to the local file
	 */
	public File getContent(DMSFileDescriptor location, String fileName);

	/**
	 * Retrieve the content of document by directly streaming to the output param
	 *
	 * @param location
	 *            the document descriptor
	 * @param output
	 *            the stream to use.
	 * @return the number of bytes directly copied. If {@link java.io.IOException} or any other -1L
	 *         is returned
	 */
	long getContent(DMSFileDescriptor location, OutputStream output);

}
