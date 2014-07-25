package com.sirma.cmf.web.userdashboard.filter;

import com.sirma.itt.emf.label.LabelProvider;

/**
 * The Enum DashboardCaseFilter.
 * 
 * @author svelikov
 */
public enum DashboardCaseFilter {

	/** The all cases. */
	ALL_CASES("all_cases", true),

	/** The active cases. */
	CREATED_BY_ME("created_by_me", true),

	/** The my cases. */
	OWNED_BY_ME("owned_by_me", true),

	/** The i worked on. */
	I_WORKED_ON("i_worked_on", true);

	/** The Constant CMF_USER_DASHBOARD_CASE_FILTER_PREF. */
	public static final String CMF_USER_DASHBOARD_CASE_FILTER_PREF = "cmf.user.dashboard.case.filter.";

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
	private DashboardCaseFilter(String filterName, boolean enabled) {
		this.filterName = filterName;
		this.isEnabled = enabled;
	}

	/**
	 * Get filter type by name if exists.
	 * 
	 * @param filterName
	 *            Filter name.
	 * @return {@link DashboardCaseFilter}.
	 */
	public static DashboardCaseFilter getFilterType(String filterName) {
		DashboardCaseFilter[] availableTypes = values();
		for (DashboardCaseFilter caseFilterType : availableTypes) {
			if (caseFilterType.filterName.equals(filterName)) {
				return caseFilterType;
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
		return labelProvider.getValue(CMF_USER_DASHBOARD_CASE_FILTER_PREF
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
