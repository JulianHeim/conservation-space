/**
 *
 */
package com.sirma.itt.cmf.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.emf.adapter.CMFRenditionAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.instance.model.DMSInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.properties.PropertiesService;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.services.RenditionService;

/**
 * The RenditionServiceImpl.
 */
@ApplicationScoped
public class RenditionServiceImpl implements RenditionService {

	/** The cmf rendition adapter service for dms. */
	@Inject
	private CMFRenditionAdapterService cmfRenditionAdapterService;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = Logger.getLogger(RenditionServiceImpl.class);

	/** The Constant DEBUG_ENABLED. */
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	@Inject
	private PropertiesService propertiesService;

	/*
	 * (non-Javadoc)
	 * @see
	 * com.sirma.itt.emf.services.RenditionService#getPrimaryThumbnail(com.sirma.itt.emf.instance
	 * .model.Instance)
	 */
	@Override
	@Secure
	public String getPrimaryThumbnail(Instance instance) {

		if ((instance != null) && (instance.getProperties() != null)) {
			// TODO
		}
		return getDefaultThumbnail(instance);
	}

	/**
	 * Generates thumbnail request for backend dms system
	 *
	 * @param instance
	 *            the instance to get thumbnail for
	 * @param dmsIdOfImage
	 *            the dmsid of the object that thumbnail is retreived
	 * @return the thumbnail as base64 encoding
	 * @throws DMSException
	 */
	private String getDMSThumbnail(Instance instance, Serializable dmsIdOfImage)
			throws DMSException {
		if (dmsIdOfImage == null) {
			return getDefaultThumbnail(instance);
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int downloadThumbnail = cmfRenditionAdapterService.downloadThumbnail(
				dmsIdOfImage.toString(), buffer);
		if (downloadThumbnail > 0) {
			return new String(Base64.encodeBase64(buffer.toByteArray()));
		}

		return null;
	}

	/**
	 * Gets the default thumbnail.
	 *
	 * @param instance
	 *            the instance
	 * @return the default thumbnail
	 */
	private String getDefaultThumbnail(Instance instance) {
		// TODO not known for now
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.sirma.itt.emf.services.RenditionService#getPrimaryThumbnail(com.sirma.itt.emf.instance
	 * .model.Instance)
	 */
	@Override
	@Secure
	public String getThumbnail(Instance instance) {

		if (instance instanceof DocumentInstance) {
			Serializable dmsId = ((DMSInstance) instance).getDmsId();
			if (dmsId == null) {
				return getDefaultThumbnail(instance);
			}
			Serializable thumbnail = instance.getProperties().get(DefaultProperties.THUMBNAIL_IMAGE);
			// if null or not a string then we should get the instance thumbnail again
			if (!(thumbnail instanceof String)) {
				try {
					thumbnail = getDMSThumbnail(instance, dmsId);
					if (thumbnail != null) {
						// add thumbnail to the instance properties
						instance.getProperties().put(DefaultProperties.THUMBNAIL_IMAGE, thumbnail);
						propertiesService.saveProperties(instance);
						return thumbnail.toString();
					}
				} catch (DMSException e) {
					// dont throw exception on fail
					if (DEBUG_ENABLED) {
						LOGGER.debug(e.getMessage(), e);
					}
				}
				return null;
			}
			return thumbnail.toString();
		}
		return getDefaultThumbnail(instance);
	}
}
