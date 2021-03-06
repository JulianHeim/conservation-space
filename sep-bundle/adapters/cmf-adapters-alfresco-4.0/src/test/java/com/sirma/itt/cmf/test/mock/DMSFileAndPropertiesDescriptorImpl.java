/*
 *
 */
package com.sirma.itt.cmf.test.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import com.sirma.itt.emf.adapter.DMSFileAndPropertiesDescriptor;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;

/**
 * The DMSFileAndPropertiesDescriptor mocked implementation.
 *
 * @author bbanchev
 */
public class DMSFileAndPropertiesDescriptorImpl implements DMSFileAndPropertiesDescriptor {
	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -7945895120650702107L;

	/** The file. */
	private File file;

	/** The container id. */
	private String containerId;

	/** The properties. */
	private Map<String, Serializable> properties;

	/**
	 * Instantiates a new dMS file and properties descriptor impl.
	 *
	 * @param file
	 *            the file
	 * @param containerId
	 *            the container id
	 * @param properties
	 *            the properties
	 */
	public DMSFileAndPropertiesDescriptorImpl(File file, String containerId,
			Map<String, Serializable> properties) {
		this.file = file;
		this.containerId = containerId;
		this.properties = properties;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return file.getAbsolutePath();
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
		try {
			return new FileInputStream(file);
		} catch (Exception e) {
			throw new EmfRuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Serializable> getProperties() {
		return properties;
	}

}