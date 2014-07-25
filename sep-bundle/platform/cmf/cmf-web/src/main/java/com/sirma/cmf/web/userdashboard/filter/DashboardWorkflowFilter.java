package com.sirma.cmf.web.userdashboard.filter;

import com.sirma.itt.emf.label.LabelProvider;

/**
 * The Enum DashboardWorkflowFilter.
 * 
 * @author cdimitrov
 */
public enum DashboardWorkflowFilter {

	/** The all filter. */
	ALL_WORKFLOWS("all_workflows", true),

	/** The not complete filter. */
	NOT_COMPLETE("not_complete", true),
	
	/** The high priority filter. */
	HIGH_PRIORITY("high_priority", true),
	
	/** The overdue filter. */
	OVERDUE("overdue", true);
	
	/** The constant  CMF_DASHBOARD_WORKFLOW_FILTER_PREF. */
	public static final String CMF_DASHBOARD_WORKFLOW_FILTER_PREF = "cmf.dashboard.workflow.filter.";

	/** The filter name. */
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
	private DashboardWorkflowFilter(String filterName, boolean enabled) {
		this.filterName = filterName;
		this.isEnabled = enabled;
	}

	/**
	 * Get filter type by name if exists.
	 * 
	 * @param filterName
	 *            Filter name.
	 * @return {@link DashboardWorkflowFilter}.
	 */
	public static DashboardWorkflowFilter getFilterType(String filterName) {
		DashboardWorkflowFilter[] availableTypes = values();
		for (DashboardWorkflowFilter caseFilterType : availableTypes) {
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
		return labelProvider.getValue(CMF_DASHBOARD_WORKFLOW_FILTER_PREF + filterName);
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
