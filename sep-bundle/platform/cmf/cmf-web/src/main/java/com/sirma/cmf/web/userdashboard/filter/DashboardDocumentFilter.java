package com.sirma.cmf.web.userdashboard.filter;

import com.sirma.itt.emf.label.LabelProvider;

/**
 * The Enum DashboardDocumentFilter.
 * 
 * @author svelikov
 */
public enum DashboardDocumentFilter {

	/** The i am editing. */
	I_AM_EDITING("i_am_editing", true),

	/** The last used. */
	LAST_USED("last_used", true);

	/** The Constant CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF. */
	public static final String CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF = "cmf.user.dashboard.document.filter.";

	/**
	 * The filter name.
	 */
	private String filterName;

	/** The is enabled. */
	private boolean isEnabled;

	/**
	 * Constructor.
	 * 
	 * @param filterName
	 *            The filter name.
	 * @param enabled
	 *            the enabled
	 */
	private DashboardDocumentFilter(String filterName, boolean enabled) {
		this.filterName = filterName;
		this.isEnabled = enabled;
	}

	/**
	 * Get filter type by name if exists.
	 * 
	 * @param filterName
	 *            Filter name.
	 * @return {@link DashboardDocumentFilter}.
	 */
	public static DashboardDocumentFilter getFilterType(String filterName) {
		DashboardDocumentFilter[] availableTypes = values();
		for (DashboardDocumentFilter filterType : availableTypes) {
			if (filterType.filterName.equals(filterName)) {
				return filterType;
			}
		}

		return null;
	}

	/**
	 * Getter method for filterName.
	 * 
	 * @return the filterName
	 */
	public String getFilterName() {
		return filterName;
	}

	/**
	 * Setter method for filterName.
	 * 
	 * @param filterName
	 *            the filterName to set
	 */
	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	/**
	 * Getter method for label.
	 * 
	 * @param labelProvider
	 *            the label provider
	 * @return the label
	 */
	public String getLabel(LabelProvider labelProvider) {
		return labelProvider.getValue(CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF
				+ filterName);
	}

	/**
	 * Getter method for isEnabled.
	 * 
	 * @return the isEnabled
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Setter method for isEnabled.
	 * 
	 * @param isEnabled
	 *            the isEnabled to set
	 */
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

}
