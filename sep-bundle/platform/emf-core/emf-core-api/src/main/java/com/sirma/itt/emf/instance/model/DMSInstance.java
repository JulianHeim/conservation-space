package com.sirma.itt.emf.instance.model;

/**
 * The Interface DMSInstance marks an instance that has a DMS representation. The method
 * {@link #getDmsId()} should return the id in DMS
 */
public interface DMSInstance {

	/**
	 * Gets the DMS id.
	 * 
	 * @return the DMS id
	 */
	String getDmsId();

	/**
	 * Sets the DMS id.
	 * 
	 * @param dmsId
	 *            the new DMS id
	 */
	void setDmsId(String dmsId);
}
