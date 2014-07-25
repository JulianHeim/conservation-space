package com.sirma.itt.pm.web.project.dashboard;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.cmf.web.DocumentContext;
import com.sirma.cmf.web.userdashboard.filter.DashboardTaskFilter;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.pm.PMTest;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * Test class for {@link ProjectTasksPanel}
 * 
 * @author cdimitrov
 */
@Test
public class ProjectTasksPanelTest extends PMTest {

	/** The reference based on tested panel. */
	private ProjectTasksPanel projectTasksPanel;

	/** The label provider, needed for retrieving panel filter labels. */
	private LabelProvider labelProvider;

	/** The supported panel filter number. */
	private int projectTaskFiltersNumber = 6;

	/**
	 * Default test panel constructor for initializing test componenets.
	 */
	public ProjectTasksPanelTest() {

		projectTasksPanel = new ProjectTasksPanel() {

			private static final long serialVersionUID = 1L;

			/** Simulate document context for the tests. Holds additional data for testing. */
			private DocumentContext docContext = new DocumentContext();

			@Override
			public SearchArguments<AbstractTaskInstance> getSearchArguments() {
				return new SearchArguments<AbstractTaskInstance>();
			}

			@Override
			public DocumentContext getDocumentContext() {
				return docContext;
			}
		};

		labelProvider = mock(LabelProvider.class);

		when(
				labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
						+ "active_tasks")).thenReturn("active_tasks");
		when(
				labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
						+ "all_tasks")).thenReturn("all_tasks");
		when(
				labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
						+ "unassigned_tasks")).thenReturn("unassigned_tasks");
		when(
				labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
						+ "high_priority_tasks")).thenReturn("high_priority_tasks");
		when(
				labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
						+ "due_date_today_tasks")).thenReturn("due_date_today_tasks");
		when(
				labelProvider.getValue(DashboardTaskFilter.CMF_USER_DASHBOARD_TASK_FILTER_PREF
						+ "overdue_date_tasks")).thenReturn("overdue_date_tasks");

		ReflectionUtils.setField(projectTasksPanel, "labelProvider", labelProvider);
	}

	/**
	 * Method for testing the context data.
	 */
	public void getProjectInstanceFromContextTest() {
		Assert.assertNull(projectTasksPanel.getDocumentContext().get(ProjectInstance.class));
		ProjectInstance projectInstance = createProjectInstance(Long.valueOf(1), "dmsId");
		projectTasksPanel.getDocumentContext().addInstance(projectInstance);
		Assert.assertEquals(
				projectTasksPanel.getDocumentContext().getInstance(ProjectInstance.class),
				projectInstance);
	}

	/**
	 * Test method for panel search arguments.
	 */
	public void getSearchArgumentsTest() {
		Assert.assertNotNull(projectTasksPanel.getSearchArguments());
	}

	/**
	 * Test method for not supported panel filters.
	 */
	public void getProjectTasksFilterUnssuportedTest() {
		projectTasksPanel.setProjectTasksFilters(null);
		Assert.assertNull(projectTasksPanel.getProjectTasksFilters());
	}

	/**
	 * Test method for supported panel filters.
	 */
	public void getTasksFilterSupportedTest() {
		projectTasksPanel.loadFilters();
		Assert.assertNotNull(projectTasksPanel.getProjectTasksFilters());
		Assert.assertEquals(projectTasksPanel.getProjectTasksFilters().size(),
				projectTaskFiltersNumber);
	}

	/**
	 * Test method for not supported panel actions.
	 */
	public void getActionsNotSupportedTest() {
		projectTasksPanel.setTaskActions(null);
		Assert.assertEquals(projectTasksPanel.getTaskActions(null), new ArrayList<Action>());

		AbstractTaskInstance taskInstance = new StandaloneTaskInstance();
		Assert.assertEquals(projectTasksPanel.getTaskActions(taskInstance), new ArrayList<Action>());
	}

	/**
	 * Test method for supported panel filters, based on specific instance.
	 */
	public void getActionsSupportedTest() {
		Map<Serializable, List<Action>> actions = new HashMap<Serializable, List<Action>>();
		AbstractTaskInstance taskInstance = new StandaloneTaskInstance();
		actions.put(taskInstance.getId(), new ArrayList<Action>(1));

		projectTasksPanel.setTaskActions(actions);
		Assert.assertEquals(projectTasksPanel.getTaskActions(taskInstance),
				new ArrayList<Action>(1));
	}

	/**
	 * Test method for default activator link label.
	 */
	public void getDefaultTaskActivatorLinkLabelTest() {
		projectTasksPanel.loadFilters();
		Assert.assertEquals(projectTasksPanel.getTasksFilterActivatorLinkLabel(), "active_tasks");
	}
}
