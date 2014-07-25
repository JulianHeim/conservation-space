package com.sirma.itt.pm.web.project.dashboard;

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
import com.sirma.itt.pm.PMTest;

/**
 * Test class for {@link ProjectWorkflowsPanel}.
 * 
 * @author cdimitrov
 */
@Test
public class ProjectWorkflowsPanelTest extends PMTest {

	/** The size of supported filters. */
	public int supportedWorkflowFilters = 4;

	/** Reference to the project panel that will be tested. */
	private ProjectWorkflowsPanel projectWorkflowPanel;

	/** The label provider, for retrieving panel filter labels. */
	private LabelProvider labelProvider;

	/**
	 * Default test constructor.
	 */
	public ProjectWorkflowsPanelTest() {

		projectWorkflowPanel = new ProjectWorkflowsPanel() {

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

		ReflectionUtils.setField(projectWorkflowPanel, "labelProvider", labelProvider);
	}

	/**
	 * Test method for empty project workflow filters.
	 */
	public void getWorkflowFiltersEmptyTest() {
		projectWorkflowPanel.setprojectWorkflowFilters(null);
		Assert.assertNull(projectWorkflowPanel.getprojectWorkflowFilters());
	}

	/**
	 * Test method for all supported workflow filters in the project.
	 */
	public void getWorkflowFilterAllSupportedTest() {
		projectWorkflowPanel.loadFilters();
		Assert.assertEquals(projectWorkflowPanel.getprojectWorkflowFilters().size(),
				supportedWorkflowFilters);
	}

	/**
	 * Test method for default project workflow filter.
	 */
	public void getWorkflowActivatorLinkLabelDefaultTest() {
		projectWorkflowPanel.loadFilters();
		Assert.assertEquals(projectWorkflowPanel.getWorkflowActivatorLinkLabel(), "not_complete");
	}

	/**
	 * Test method for project workflow searched arguments.
	 */
	public void getSearchArgumentsNotAvailableTest() {
		Assert.assertNotNull(projectWorkflowPanel.getSearchArguments());
	}

	/**
	 * Test method for retrieving actions based on empty workflow instance.
	 */
	public void getWorkflowActionEmptyInstanceTest() {
		Assert.assertEquals(projectWorkflowPanel.getWorkflowActions(null), new ArrayList<Action>());
	}

}
