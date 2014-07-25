package com.sirma.itt.emf.adapter;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Descriptor for files on remote dms system.
 *
 * @author borislav banchev
 */
public interface DMSFileDescriptor extends Serializable {
	
	/**
	 * The id if the file.
	 *
	 * @return the id
	 */
	String getId();
	
	/**
	 * Gets the container id.
	 *
	 * @return the container id
	 */
	String getContainerId();

	/**
	 * Fetch the file from the dms system.
	 *
	 * @return the stream with the content
	 */
	InputStream download();

}
