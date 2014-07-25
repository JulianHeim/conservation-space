package com.sirma.cmf.web.userdashboard.panel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.cmf.CMFTest;
import com.sirma.cmf.web.userdashboard.filter.DashboardCaseFilter;
import com.sirma.cmf.web.userdashboard.filter.DashboardDateFilter;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;

/**
 * Test method for {@link ProjectCasePanel}
 * 
 * @author cdimitrov
 */
@Test
public class MyCasesPanelTest extends CMFTest {

	/** The case panel reference. */
	private MyCasesPanel myCasesPanel;

	/** The label provider that will be used for retrieving filter lables. */
	private LabelProvider labelProvider;

	/** The default supported case filters. */
	private int supportedCaseFilters = 4;

	/** The default supported date filters. */
	private int supportedDateFilters = 4;

	/** The case instance that will be used in the tests. */
	private CaseInstance caseInstance;

	/**
	 * Default constructor for the test class.
	 */
	public MyCasesPanelTest() {

		myCasesPanel = new MyCasesPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			public SearchArguments<CaseInstance> getSearchArguments() {
				return new SearchArguments<CaseInstance>();
			}
		};

		// mock the label builder
		labelProvider = mock(LabelProvider.class);

		// case filter labels
		when(
				labelProvider.getValue(DashboardCaseFilter.CMF_USER_DASHBOARD_CASE_FILTER_PREF
						+ "all_cases")).thenReturn("all_cases");
		when(
				labelProvider.getValue(DashboardCaseFilter.CMF_USER_DASHBOARD_CASE_FILTER_PREF
						+ "created_by_me")).thenReturn("created_by_me");
		when(
				labelProvider.getValue(DashboardCaseFilter.CMF_USER_DASHBOARD_CASE_FILTER_PREF
						+ "owned_by_me")).thenReturn("owned_by_me");
		when(
				labelProvider.getValue(DashboardCaseFilter.CMF_USER_DASHBOARD_CASE_FILTER_PREF
						+ "i_worked_on")).thenReturn("i_worked_on");

		// date filter labels
		when(
				labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
						+ "all")).thenReturn("all");
		when(
				labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
						+ "last_month")).thenReturn("last_month");
		when(
				labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
						+ "last_week")).thenReturn("last_week");
		when(
				labelProvider.getValue(DashboardDateFilter.CMF_USER_DASHBOARD_DATE_FILTER_PREF
						+ "today")).thenReturn("today");

		ReflectionUtils.setField(myCasesPanel, "labelProvider", labelProvider);

		caseInstance = createCaseInstance((Long.valueOf(1)));

	}

	/**
	 * Test for empty case filters.
	 */
	public void getCaseFiltersEmptyTest() {
		myCasesPanel.setCaseFilters(null);
		Assert.assertNull(myCasesPanel.getCaseFilters());
	}

	/**
	 * Test for empty date filters.
	 */
	public void getDateFiltersEmptyTest() {
		myCasesPanel.setDateFilters(null);
		Assert.assertNull(myCasesPanel.getDateFilters());
	}

	/**
	 * Test for all supported case filters. Default are four {@link DashboardCaseFilter}.
	 */
	public void getCaseFilterAllSupportedTest() {
		myCasesPanel.loadFilters();
		Assert.assertEquals(myCasesPanel.getCaseFilters().size(), supportedCaseFilters);
	}

	/**
	 * Test for all supported case filters. Default are four {@link DashboardDateFilter}.
	 */
	public void getDateFilterAllSupportedTest() {
		myCasesPanel.loadFilters();
		Assert.assertEquals(myCasesPanel.getCaseFilters().size(), supportedDateFilters);
	}

	/**
	 * Test for default case activator link label.
	 */
	public void getCaseFilterActivatorLinkLabelDefaultTest() {
		myCasesPanel.loadFilters();
		Assert.assertEquals(myCasesPanel.getCaseFilterActivatorLinkLabel(), "all_cases");
	}

	/**
	 * Test for default date activator link label.
	 */
	public void getDateFilterActivatorLinkLabelDefaultTest() {
		myCasesPanel.loadFilters();
		Assert.assertEquals(myCasesPanel.getDateFilterActivatorLinkLabel(), "last_week");
	}

	/**
	 * Test method for not empty search arguments.
	 */
	public void getSearchArgumentsEmptyTest() {
		Assert.assertNotNull(myCasesPanel.getSearchArguments());
	}

	/**
	 * Test method for retrieving all actions based on null-able case instance.
	 */
	public void getCaseActionsEmptyInstanceTest() {
		Assert.assertEquals(myCasesPanel.getCaseActions(null), new ArrayList<Action>());
	}

	/**
	 * Test method for retrieving all actions based on supported case instance.
	 */
	public void getCaseActionsSupportedInstanceTest() {
		Map<Serializable, List<Action>> actions = new HashMap<Serializable, List<Action>>();
		actions.put(caseInstance.getId(), new ArrayList<Action>(3));

		myCasesPanel.setCaseActions(actions);
		Assert.assertEquals(myCasesPanel.getCaseActions(caseInstance), new ArrayList<Action>(3));
	}

}
