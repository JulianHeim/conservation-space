package com.sirma.itt.emf.util;

import java.io.Serializable;

import com.sirma.itt.emf.converter.TypeConverterUtil;

/**
 * Utility class for containing method for working with {@link java.security.MessageDigest} API.
 * 
 * @author BBonev
 */
public class DigestUtils {

	/**
	 * Calculate digest of the given serializable by first calling a conversion to String using the
	 * proper source to String converter via {@link com.sirma.itt.emf.converter.TypeConverter}
	 * facilities.
	 *
	 * @param content
	 *            the content
	 * @return the calculated digest
	 */
	public static String calculateDigest(Serializable content) {
		return calculateDigest(TypeConverterUtil.getConverter().convert(String.class, content));
	}

	/**
	 * Calculate digest of the given string content.
	 *
	 * @param content
	 *            the content
	 * @return the calculated digest
	 */
	public static String calculateDigest(String content) {
		if (content == null) {
			return null;
		}
		try {
			return org.apache.commons.codec.digest.DigestUtils.shaHex(content);
		} catch (RuntimeException e) {
			try {
				// fall back algorithm
				return org.apache.commons.codec.digest.DigestUtils.md5Hex(content);
			} catch (RuntimeException e1) {
				// LOGGER.debug(e1);
			}
		}
		return null;
	}
}
