package com.sirma.cmf.web.userdashboard.filter;

import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.util.DateRangeUtil;

/**
 * The Enum DashboardDateFilter.
 *
 * @author svelikov
 */
public enum DashboardDateFilter {

	/** The all. */
	ALL("all", true),

	/** The today. */
	TODAY("today", true),

	/** The last week. */
	LAST_WEEK("last_week", true),

	/** The last month. */
	LAST_MONTH("last_month", true);

	/** The Constant CMF_USER_DASHBOARD_DATE_FILTER_PREF. */
	public static final String CMF_USER_DASHBOARD_DATE_FILTER_PREF = "cmf.user.dashboard.date.filter.";

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
	private DashboardDateFilter(String filterName, boolean enabled) {
		this.filterName = filterName;
		this.isEnabled = enabled;
	}

	/**
	 * Get filter type by name if exists.
	 *
	 * @param filterName
	 *            Filter name.
	 * @return {@link DashboardDateFilter}.
	 */
	public static DashboardDateFilter getFilterType(String filterName) {
		DashboardDateFilter[] availableTypes = values();
		for (DashboardDateFilter dateFilterType : availableTypes) {
			if (dateFilterType.filterName.equals(filterName)) {
				return dateFilterType;
			}
		}

		return null;
	}

	/**
	 * Gets the date range.
	 *
	 * @param filterName
	 *            the filter name
	 * @return the date range
	 */
	public static DateRange getDateRange(String filterName) {

		DashboardDateFilter filterType = getFilterType(filterName);
		if(filterType != null){
			switch (filterType) {
				case ALL:
					break;
				case TODAY:
					return DateRangeUtil.getToday();
				case LAST_WEEK:
					return DateRangeUtil.getLast7Days();
				case LAST_MONTH:
					return DateRangeUtil.getLast30Days();
				default:
					break;
			}
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
		return labelProvider.getValue(CMF_USER_DASHBOARD_DATE_FILTER_PREF + filterName);
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
