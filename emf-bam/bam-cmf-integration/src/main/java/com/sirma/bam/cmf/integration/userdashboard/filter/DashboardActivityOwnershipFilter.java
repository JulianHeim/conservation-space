package com.sirma.bam.cmf.integration.userdashboard.filter;

import com.sirma.itt.emf.label.LabelProvider;

/**
 * Class represent ownership filter for activity dashlets.
 * 
 * @author cdimitrov
 */
public enum DashboardActivityOwnershipFilter {
	
	/** The my activity filter. */
	MY_ACTIVITIES("my_activities", true),
	
	/** The others activity filter. */
	OTHERS_ACTIVITIES("other_activities", true),

	/** The all activity filter. */
	ALL_ACTIVITIES("all_activities", true);
	
	/** The label prefix constant. */
	public static final String CMF_USER_DASHBOARD_ACTIVITIES_OWNERSHIP_FILTER_PREF = "cmf.user.dashboard.activities.ownership.filter.";

	/** The filter name. */
	private String filterName;
	
	/** The filter is enable. */
	private boolean isEnabled;
	
	/**
	 * Default constructor for ownership filter.
	 * 
	 * @param filterName filter name
	 * @param enabled is filter enable
	 */
	private DashboardActivityOwnershipFilter(String filterName, boolean enabled) {
		this.filterName = filterName;
		this.isEnabled = enabled;
	}

	/** 
	 * Getter for filter type.
	 * 
	 * @param filterName filter name
	 * @return ownership filter
	 */
	public static DashboardActivityOwnershipFilter getFilterType(
			String filterName) {
		DashboardActivityOwnershipFilter[] availableTypes = values();
		for (DashboardActivityOwnershipFilter ownershitFilterType : availableTypes) {
			if (ownershitFilterType.filterName.equals(filterName)) {
				return ownershitFilterType;
			}
		}
		return null;
	}

	/**
	 * Getter for filter name.
	 * 
	 * @return filter name
	 */
	public String getFilterName() {
		return filterName;
	}

	/**
	 * Setter for filter name.
	 * 
	 * @param filterName filter name
	 */
	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	/**
	 * Getter for ownership filter label
	 * 
	 * @param labelProvider label provider
	 * @return ownership filter label
	 */
	public String getLabel(LabelProvider labelProvider) {
		return labelProvider
				.getValue(CMF_USER_DASHBOARD_ACTIVITIES_OWNERSHIP_FILTER_PREF
						+ filterName);
	}

	/**
	 * Getter for filter is enable.
	 * 
	 * @return is filter enable
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Setter for filter is enable.
	 * 
	 * @param isEnabled filter is enable
	 */
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

}
