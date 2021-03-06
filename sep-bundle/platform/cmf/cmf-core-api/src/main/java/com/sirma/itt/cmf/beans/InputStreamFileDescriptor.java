package com.sirma.itt.cmf.beans;

import java.io.InputStream;

import com.sirma.itt.emf.adapter.DMSFileDescriptor;

/**
 * File descriptor for input stream.
 *
 * @author BBonev
 */
public class InputStreamFileDescriptor implements DMSFileDescriptor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -8400409459380868147L;

	/** The input stream. */
	private InputStream inputStream;

	/** The id. */
	private String id;

	/**
	 * Instantiates a new input stream file descriptor.
	 * 
	 * @param id
	 *            the id
	 * @param inputStream
	 *            the input stream
	 */
	public InputStreamFileDescriptor(String id, InputStream inputStream) {
		this.id = id;
		this.inputStream = inputStream;
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
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream download() {
		return inputStream;
	}

}
