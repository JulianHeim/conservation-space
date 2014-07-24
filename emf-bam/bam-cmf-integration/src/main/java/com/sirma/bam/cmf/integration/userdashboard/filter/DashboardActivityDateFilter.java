package com.sirma.bam.cmf.integration.userdashboard.filter;

import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.util.DateRangeUtil;

/**
 * Date filters for activity dashlets.
 * 
 * @author cdimitrov
 */
public enum DashboardActivityDateFilter {

	/** The today filter. */
	TODAY("today", true),

	/** The last 7 days filter. */
	LAST_7DAYS("last_7days", true),
	
	/** The last 14 days filter. */
	LAST_14DAYS("last_14days", true),

	/** The last 28 days filter. */
	LAST_28DAYS("last_28days", true);

	/** The label prefix constant. */
	public static final String CMF_USER_DASHBOARD_ACTIVITIES_DATE_FILTER_PREF = "cmf.user.dashboard.activities.date.filter.";

	/** The filter name. */
	private String filterName;

	/** The is enable. */
	private boolean isEnabled;

	/**
	 * Default constructor.
	 * 
	 * @param filterName filter name
	 * @param enabled is filter enable
	 */
	private DashboardActivityDateFilter(String filterName, boolean enabled) {
		this.filterName = filterName;
		this.isEnabled = enabled;
	}

	/**
	 * Get filter type by name if exists.
	 * 
	 * @param filterName
	 *            Filter name.
	 * @return {@link DashboardActivityDateFilter}.
	 */
	public static DashboardActivityDateFilter getFilterType(
			String filterName) {
		DashboardActivityDateFilter[] availableTypes = values();
		for (DashboardActivityDateFilter dateFilterType : availableTypes) {
			if (dateFilterType.filterName.equals(filterName)) {
				return dateFilterType;
			}
		}
		return null;
	}
	
	/**
	 * Getter for date range based on filter.
	 * 
	 * @param filterName filter name
	 * @return date range
	 */
	public static DateRange getDateRange(String filterName) {
		DashboardActivityDateFilter filterType = getFilterType(filterName);
		switch (filterType) {
			case TODAY:
				return DateRangeUtil.getToday();
			case LAST_7DAYS:
				return DateRangeUtil.getLast7Days();
			case LAST_14DAYS:
				return DateRangeUtil.getNDaysRange(14, 0, false);
			case LAST_28DAYS:
				return DateRangeUtil.getNDaysRange(30, 0, false);
			default: break;
		}
		DateRange dateRange = DateRangeUtil.getAll();

		return dateRange;
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
		return labelProvider.getValue(CMF_USER_DASHBOARD_ACTIVITIES_DATE_FILTER_PREF
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
