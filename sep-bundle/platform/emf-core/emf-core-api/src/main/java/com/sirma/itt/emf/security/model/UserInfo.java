package com.sirma.itt.emf.security.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.sirma.itt.emf.resources.EmfResourcesUtil;
import com.sirma.itt.emf.resources.ResourceProperties;

/**
 * The CmfUserInfo is wrapper for user info.
 */
public class UserInfo extends HashMap<String, Serializable> {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -5539490108404891654L;

	public static final String FIRST_NAME = ResourceProperties.FIRST_NAME;
	public static final String LAST_NAME = ResourceProperties.LAST_NAME;
	public static final String USER_ID = ResourceProperties.USER_ID;
	public static final String EMAIL = ResourceProperties.EMAIL;
	public static final String LANGUAGE = ResourceProperties.LANGUAGE;
	public static final String AVATAR = ResourceProperties.AVATAR;
	public static final String JOB_TITLE = ResourceProperties.JOB_TITLE;

	private String displayName;

	/**
	 * Instantiates a new cmf user info.
	 *
	 * @param firstName
	 *            the first name
	 * @param lastName
	 *            the last name
	 */
	public UserInfo(String firstName, String lastName) {
		put(ResourceProperties.FIRST_NAME, firstName);
		put(ResourceProperties.LAST_NAME, lastName);
	}

	/**
	 * Default constructor.
	 */
	public UserInfo() {

	}

	/**
	 * Gets the display name of the user object.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		if (displayName == null) {
			displayName = EmfResourcesUtil.buildDisplayName(this);
		}
		return displayName;
	}

	/**
	 * Provides the language attribute..
	 *
	 * @return value of the language attribute.
	 */
	public String getLanguage() {
		return (String) get(ResourceProperties.LANGUAGE);
	}

	@Override
	public Serializable put(String key, Serializable value) {
		displayName = null;
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Serializable> m) {
		displayName = null;
		super.putAll(m);
	}

	@Override
	public Serializable remove(Object key) {
		displayName = null;
		return super.remove(key);
	}

	@Override
	public void clear() {
		displayName = null;
		super.clear();
	}

}
