package com.sirma.itt.cmf.beans;

import java.io.File;

/**
 * File descriptor for local filest that are proxied against desired id
 *
 * @author bbanchev
 */
public class LocalProxyFileDescriptor extends LocalFileDescriptor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -2730182502454744150L;
	private String mimicName;

	/**
	 * Instantiates a new local file descriptor.
	 *
	 * @param mimicName
	 *            is the name to represent descriptor with
	 * @param path
	 *            the actual path
	 */
	public LocalProxyFileDescriptor(String mimicName, String path) {
		super(path);
		if (mimicName == null) {
			throw new IllegalArgumentException("Cannot create a NULL LocalProxyFileDescriptor");
		}
		this.mimicName = new File(new File(path).getParentFile(), mimicName).getAbsolutePath();
	}

	/**
	 * Instantiates a new local file descriptor.
	 *
	 * @param mimicName
	 *            is the name to represent descriptor with
	 * @param file
	 *            the actual file
	 */
	public LocalProxyFileDescriptor(String mimicName, File file) {
		super(file);
		if (mimicName == null) {
			throw new IllegalArgumentException("Cannot create a NULL LocalProxyFileDescriptor");
		}
		this.mimicName = new File(file.getParentFile(), mimicName).getAbsolutePath();
	}

	public String getProxiedId() {
		return mimicName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mimicName == null) ? 0 : mimicName.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalProxyFileDescriptor other = (LocalProxyFileDescriptor) obj;
		if (mimicName == null) {
			if (other.mimicName != null)
				return false;
		} else if (!mimicName.equals(other.mimicName))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LocalProxyFileDescriptor [path=" + path + ", mimicName=" + mimicName + "]";
	}

	@Override
	public String getContainerId() {
		return null;
	}

}
