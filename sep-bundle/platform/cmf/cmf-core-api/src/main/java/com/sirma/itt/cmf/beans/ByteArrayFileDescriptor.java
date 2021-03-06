package com.sirma.itt.cmf.beans;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.sirma.itt.emf.adapter.DMSFileDescriptor;

/**
 * In memory file descriptor
 * 
 * @author BBonev
 */
public class ByteArrayFileDescriptor implements DMSFileDescriptor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 2156094388607522553L;

	/** The id. */
	private String id;

	/** The container id. */
	private String containerId;

	/** The data. */
	private byte[] data;

	/**
	 * Instantiates a new byte array file descriptor.
	 * 
	 * @param id
	 *            the name of the file
	 * @param data
	 *            the file data
	 */
	public ByteArrayFileDescriptor(String id, byte[] data) {
		this.id = id;
		this.data = data;
	}

	/**
	 * Instantiates a new byte array file descriptor.
	 * 
	 * REVIEW Imeto na id parameter-a da se smeni na fileName, zashto tova koeto
	 * se iziskva da se podade realno e imeto na faila.
	 * 
	 * @param id
	 *            the name of the file
	 * @param containerId
	 *            the container id, can be the target/source case instance DMS
	 *            ID (Optional)
	 * @param data
	 *            the file data
	 */
	public ByteArrayFileDescriptor(String id, String containerId, byte[] data) {
		this.id = id;
		this.containerId = containerId;
		this.data = data;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContainerId() {
		return containerId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream download() {
		return new ByteArrayInputStream(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result)
				+ ((containerId == null) ? 0 : containerId.hashCode());
		result = (prime * result) + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ByteArrayFileDescriptor)) {
			return false;
		}
		ByteArrayFileDescriptor other = (ByteArrayFileDescriptor) obj;
		if (containerId == null) {
			if (other.containerId != null) {
				return false;
			}
		} else if (!containerId.equals(other.containerId)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ByteArrayFileDescriptor [id=");
		builder.append(id);
		builder.append(", containerId=");
		builder.append(containerId);
		builder.append(", data=");
		builder.append(data == null ? "null" : "BYTE_DATA(" + data.length + ")");
		builder.append("]");
		return builder.toString();
	}

}
