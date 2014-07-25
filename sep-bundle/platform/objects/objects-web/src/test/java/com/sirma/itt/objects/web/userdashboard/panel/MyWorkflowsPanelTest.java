package com.sirma.itt.objects.web.userdashboard.panel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.cmf.web.userdashboard.filter.DashboardWorkflowFilter;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.objects.ObjectsTest;

/**
 * Test class for {@link MyWorkflowsPanel}.
 * 
 * @author cdimitrov
 */
@Test
public class MyWorkflowsPanelTest extends ObjectsTest {

	/** The size of supported filters. */
	public int supportedWorkflowFilters = 4;

	/** Reference to the panel that will be tested. */
	private MyWorkflowsPanel myWorkflowPanel;

	/** The label provider, for retrieving panel filter labels. */
	private LabelProvider labelProvider;

	/**
	 * Default test constructor.
	 */
	public MyWorkflowsPanelTest() {

		myWorkflowPanel = new MyWorkflowsPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			public SearchArguments<Instance> getSearchArguments() {
				return new SearchArguments<Instance>();
			}

		};

		// mock the label builder
		labelProvider = mock(LabelProvider.class);

		when(
				labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
						+ "all_workflows")).thenReturn("all_workflows");
		when(
				labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
						+ "not_complete")).thenReturn("not_complete");
		when(
				labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
						+ "high_priority")).thenReturn("high_priority");
		when(
				labelProvider.getValue(DashboardWorkflowFilter.CMF_DASHBOARD_WORKFLOW_FILTER_PREF
						+ "overdue")).thenReturn("overdue");

		ReflectionUtils.setField(myWorkflowPanel, "labelProvider", labelProvider);
	}

	/**
	 * Test method for empty workflow filters.
	 */
	public void getWorkflowFiltersEmptyTest() {
		myWorkflowPanel.setWorkflowFilters(null);
		Assert.assertNull(myWorkflowPanel.getWorkflowFilters());
	}

	/**
	 * Test method for all supported workflow filters.
	 */
	public void getWorkflowFilterAllSupportedTest() {
		myWorkflowPanel.loadFilters();
		Assert.assertEquals(myWorkflowPanel.getWorkflowFilters().size(), supportedWorkflowFilters);
	}

	/**
	 * Test method for default filter.
	 */
	public void getWorkflowActivatorLinkLabelDefaultTest() {
		myWorkflowPanel.loadFilters();
		Assert.assertEquals(myWorkflowPanel.getWorkflowActivatorLinkLabel(), "not_complete");
	}

	/**
	 * Test method for searched arguments.
	 */
	public void getSearchArgumentsNotAvailableTest() {
		Assert.assertNotNull(myWorkflowPanel.getSearchArguments());
	}

	/**
	 * Test method for retrieving actions based on empty workflow instance.
	 */
	public void getWorkflowActionEmptyInstanceTest() {
		Assert.assertEquals(myWorkflowPanel.getWorkflowActions(null), new ArrayList<Action>());
	}

}
