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
import com.sirma.cmf.web.userdashboard.filter.DashboardTaskFilter;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;

/**
 * Test class for {@link MyTasksPanel}.
 * 
 * @author cdimitrov
 */
@Test
public class MyTasksPanelTest extends CMFTest {

	/** The panel reference that will be tested. */
	private MyTasksPanel myTasksPanel;

	/** The label builder that will be used for retrieving panel filters. */
	private LabelProvider labelProvider;

	/** The number for panel filters. */
	private int taskFiltersNumber = 6;

	/**
	 * Default test constructor, for initializing test components.
	 */
	public MyTasksPanelTest() {
		myTasksPanel = new MyTasksPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			public SearchArguments<AbstractTaskInstance> getSearchArguments() {
				return new SearchArguments<AbstractTaskInstance>();
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

		ReflectionUtils.setField(myTasksPanel, "labelProvider", labelProvider);
	}

	/**
	 * Test method for panel search arguments.
	 */
	public void getSearchArgumentsTest() {
		Assert.assertNotNull(myTasksPanel.getSearchArguments());
	}

	/**
	 * Test method for not supported panel filters.
	 */
	public void getTasksFilterUnssuportedTest() {
		myTasksPanel.setTaskFilters(null);
		Assert.assertNull(myTasksPanel.getTaskFilters());
	}

	/**
	 * Test method for supported panel filters.
	 */
	public void getTasksFilterSupportedTest() {
		myTasksPanel.loadFilters();
		Assert.assertNotNull(myTasksPanel.getTaskFilters());
		Assert.assertEquals(myTasksPanel.getTaskFilters().size(), taskFiltersNumber);
	}

	/**
	 * Test method for not supported panel actions.
	 */
	public void getActionsNotSupportedTest() {
		myTasksPanel.setTaskActions(null);
		Assert.assertEquals(myTasksPanel.getTaskActions(null), new ArrayList<Action>());

		AbstractTaskInstance taskInstance = createStandaloneTaskInstance(1L);
		Assert.assertEquals(myTasksPanel.getTaskActions(taskInstance), new ArrayList<Action>());
	}

	/**
	 * Test method for supported panel filters, based on specific instance.
	 */
	public void getActionsSupportedTest() {
		Map<Serializable, List<Action>> actions = new HashMap<Serializable, List<Action>>();
		AbstractTaskInstance taskInstance = createStandaloneTaskInstance(2L);
		actions.put(taskInstance.getId(), new ArrayList<Action>(1));

		myTasksPanel.setTaskActions(actions);
		Assert.assertEquals(myTasksPanel.getTaskActions(taskInstance), new ArrayList<Action>(1));
	}

	/**
	 * Test method for default activator link label.
	 */
	public void getDefaultTaskActivatorLinkLabelTest() {
		myTasksPanel.loadFilters();
		Assert.assertEquals(myTasksPanel.getTaskFilterActivatorLinkLabel(), "active_tasks");
	}

}
