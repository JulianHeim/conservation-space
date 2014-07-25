package com.sirma.itt.emf.services;

import com.sirma.itt.emf.instance.model.Instance;

/**
 * The RenditionService provides logic for working with thumbnails for any instance.
 *
 * @author bbanchev
 */
public interface RenditionService {

	/**
	 * Gets the primary thumbnail for instance. If there is association for primary image it is
	 * retrieved its thumbnail from alfresco
	 *
	 * @param instance
	 *            the instance to get thumbnail for
	 * @return the primary thumbnail or the default as
	 *         {@link org.apache.commons.codec.binary.Base64} encoded
	 */
	String getPrimaryThumbnail(Instance instance);

	/**
	 * Gets the thumbnail rendered in dms or the default if there is no thumbnail for that type.
	 *
	 * @param instance
	 *            the instance should be {@link com.sirma.itt.emf.instance.model.DMSInstance}
	 *            subtype
	 * @return the thumbnail or the default as {@link org.apache.commons.codec.binary.Base64}
	 *         encoded
	 */
	String getThumbnail(Instance instance);
}
