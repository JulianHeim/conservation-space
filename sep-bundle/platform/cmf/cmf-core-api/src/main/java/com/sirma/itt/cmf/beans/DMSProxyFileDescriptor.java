package com.sirma.itt.cmf.beans;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.log4j.Logger;

import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.remote.DMSClientException;
import com.sirma.itt.emf.remote.RESTClient;

/**
 * Descriptor for proxy DMS requests using the {@link #restClient}
 * 
 * @author bbanchev
 */
public class DMSProxyFileDescriptor implements DMSFileDescriptor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -2730182502454744150L;

	/** The log. */
	private final Logger log = Logger.getLogger(DMSProxyFileDescriptor.class);

	/** the path. */
	private final String path;
	/** rest client to use. */
	private final RESTClient restClient;

	/**
	 * Instantiates a new local file descriptor.
	 * 
	 * @param uri
	 *            the path
	 * @param restClient
	 *            is the {@link RESTClient} to use
	 */
	public DMSProxyFileDescriptor(String uri, RESTClient restClient) {
		this.path = uri;
		this.restClient = restClient;
		if ((this.path == null) || (restClient == null)) {
			throw new IllegalArgumentException("Cannot create a NULL DMSProxyFileDescriptor");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream download() {
		HttpMethod createMethod = restClient.createMethod(new GetMethod(), new Part[0], true);
		try {
			return restClient.request(createMethod, path);
		} catch (DMSClientException e) {
			// throw new EmfRuntimeException(e.getMessage());
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DMSProxyFileDescriptor [path=" + path + "]";
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DMSProxyFileDescriptor other = (DMSProxyFileDescriptor) obj;
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		return true;
	}

	@Override
	public String getContainerId() {
		return null;
	}

}
