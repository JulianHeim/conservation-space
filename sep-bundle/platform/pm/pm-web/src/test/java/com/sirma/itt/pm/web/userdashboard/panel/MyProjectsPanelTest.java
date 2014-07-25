package com.sirma.itt.pm.web.userdashboard.panel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.pm.PMTest;
import com.sirma.itt.pm.domain.model.ProjectInstance;
import com.sirma.itt.pm.web.userdashboard.panel.filters.PMUserDashboardProjectFilter;

/**
 * Test class for {@link MyProjectsPanel}.
 * 
 * @author cdimitrov
 */

@Test
public class MyProjectsPanelTest extends PMTest {

	/** The project panel reference. */
	private MyProjectsPanel myProjectsPanel;

	/** The project instance that will be used in the tests. */
	private ProjectInstance projectInstance;

	private LabelProvider labelProvider;

	/**
	 * Default constructor.
	 */
	public MyProjectsPanelTest() {

		// create project instance for the test class.
		projectInstance = createProjectInstance(Long.valueOf(1), "dmsId");

		myProjectsPanel = new MyProjectsPanel() {

			private static final long serialVersionUID = -3378182947622659867L;

			@Override
			public SearchArguments<ProjectInstance> getSearchArguments() {
				return new SearchArguments<ProjectInstance>();
			}
		};

		labelProvider = mock(LabelProvider.class);

		when(
				labelProvider
						.getValue(PMUserDashboardProjectFilter.PM_PROJECT_USER_DASHBOARD_PROJECTS_FILTER_PREF
								+ "all_projects")).thenReturn("all-projects");

		when(
				labelProvider
						.getValue(PMUserDashboardProjectFilter.PM_PROJECT_USER_DASHBOARD_PROJECTS_FILTER_PREF
								+ "active_projects")).thenReturn("active-projects");

		when(
				labelProvider
						.getValue(PMUserDashboardProjectFilter.PM_PROJECT_USER_DASHBOARD_PROJECTS_FILTER_PREF
								+ "completed_projects")).thenReturn("completed-projects");

		ReflectionUtils.setField(myProjectsPanel, "labelProvider", labelProvider);

	}

	/**
	 * Test method for retrieving actions based on null-able project instance.
	 */
	public void getActionsForInstanceEmptyInstanceTest() {
		Assert.assertEquals(myProjectsPanel.getActionsForInstance(null), new ArrayList<Action>());
	}

	/**
	 * Test method for retrieving actions based on project instance and empty sub-actions(root
	 * actions).
	 */
	public void getActionsForInstanceEmptySubActionsTest() {
		myProjectsPanel.setActions(null);

		Assert.assertEquals(myProjectsPanel.getActionsForInstance(projectInstance),
				new ArrayList<Action>());
	}

	/**
	 * Test method for retrieving actions based on project instance.
	 */
	public void getActionsForInstanceSupportedActionsTest() {
		Map<Serializable, List<Action>> actions = new HashMap<Serializable, List<Action>>();
		actions.put(projectInstance.getId(), new ArrayList<Action>(2));

		myProjectsPanel.setActions(actions);

		Assert.assertEquals(myProjectsPanel.getActionsForInstance(projectInstance),
				new ArrayList<Action>(2));
	}

	/**
	 * Test method for not null-able result of searched arguments.
	 */
	public void getSearchArgumentsNotNullTest() {
		Assert.assertNotEquals(myProjectsPanel.getSearchArguments(), null);
	}

	/**
	 * Test method for retrieving default project filter.
	 */
	public void getProjectActivatorLinkLabelDefaultSupportedTest() {
		myProjectsPanel.loadFilters();
		Assert.assertEquals(myProjectsPanel.getProjectActivatorLinkLabel(), "active-projects");
	}

	/**
	 * Test method for null-able project filters.
	 */
	public void getProjectFiltersNotSupportedTest() {
		myProjectsPanel.setProjectFilter(null);
		Assert.assertNull(myProjectsPanel.getProjectFilter());
	}

	/**
	 * Test method for all project filters - default they are tree.
	 */
	public void getProjectFilterAllTest() {
		myProjectsPanel.loadFilters();
		Assert.assertEquals(myProjectsPanel.getProjectFilter().size(), 3);
	}

}
