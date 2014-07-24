package com.sirma.itt.cmf.alfresco4.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sirma.itt.cmf.alfresco4.ServiceURIRegistry;
import com.sirma.itt.cmf.beans.DMSProxyFileDescriptor;
import com.sirma.itt.emf.adapter.CMFRenditionAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.remote.RESTClient;

/**
 * The RenditionAlfresco4Service is the alfresco 4 implementation for
 * {@link CMFRenditionAdapterService}
 */
public class RenditionAlfresco4Service implements CMFRenditionAdapterService {
	private static final Logger LOGGER = Logger.getLogger(RenditionAlfresco4Service.class);
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	/**
	 *
	 */
	private static final long serialVersionUID = 8841619126746978716L;
	@Inject
	private RESTClient restClient;

	@Override
	public String getPrimaryThumbnailURI(String dmsId) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public int downloadThumbnail(String dmsId, OutputStream buffer) throws DMSException {
		InputStream download = null;
		try {
			String[] idParts = dmsId.split("/");
			if (DEBUG_ENABLED) {
				LOGGER.debug("Downloading thumbnail for " + dmsId);
			}
			DMSProxyFileDescriptor thumbnailDescriptor = new DMSProxyFileDescriptor(
					MessageFormat.format(ServiceURIRegistry.CONTENT_THUMBNAIL_ACCESS_URI,
							idParts[idParts.length - 1]), restClient);
			download = thumbnailDescriptor.download();
			int copied = 0;
			if (download != null) {
				copied = IOUtils.copy(download, buffer);
			}
			return copied;
		} catch (Exception e) {
			throw new DMSException(e);
		} finally {
			IOUtils.closeQuietly(download);
		}
	}

}
