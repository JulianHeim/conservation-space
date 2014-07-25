package com.sirma.itt.emf.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.security.Secure;

/**
 * The class is responsible for obtaining content from descriptor and storing it on temp location or
 * in memory.
 *
 * @author BBonev
 * @author bbanchev
 */
@ApplicationScoped
public class ContentServiceImpl implements ContentService, Serializable {
	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -1034216178534973036L;
	/** The logger. */
	@Inject
	private Logger logger;
	@Inject
	private TempFileProvider tempFileProviderImpl;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File getContent(DMSFileDescriptor location) {
		if (location == null) {
			return null;
		}
		InputStream inputStream = location.download();
		if (inputStream == null) {
			// failed to download the resource
			return null;
		}
		OutputStream output = null;
		try {
			File tempFile = tempFileProviderImpl.createTempFile("tempContent", ".tmp");
			output = new FileOutputStream(tempFile);
			IOUtils.copy(inputStream, output);
			return tempFile;
		} catch (FileNotFoundException e) {
			logger.warn("Failed to read file from File System", e);
		} catch (IOException e) {
			logger.warn("", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(output);
		}
		return null;
	}

	@Override
	@Secure
	public long getContent(DMSFileDescriptor location, OutputStream output) {
		if (location == null || output == null) {
			return -1L;
		}
		InputStream inputStream = location.download();
		if (inputStream == null) {
			// failed to download the resource
			return -1L;
		}
		try {
			return IOUtils.copyLarge(inputStream, output);
		} catch (IOException e) {
			logger.warn("", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return -1L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Secure
	public File getContent(DMSFileDescriptor location, String fileName) {
		if (location == null) {
			return null;
		}
		InputStream inputStream = location.download();
		if (inputStream == null) {
			// failed to download the resource
			return null;
		}
		OutputStream output = null;
		try {
			File tempFile = tempFileProviderImpl.createTempFile(fileName, "");
			output = new FileOutputStream(tempFile);
			IOUtils.copy(inputStream, output);
			return tempFile;
		} catch (FileNotFoundException e) {
			logger.warn("Failed to read file from File System", e);
		} catch (IOException e) {
			logger.warn("", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(output);
		}
		return null;
	}

}
