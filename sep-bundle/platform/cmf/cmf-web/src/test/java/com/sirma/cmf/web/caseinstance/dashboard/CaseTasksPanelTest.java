package com.sirma.cmf.web.caseinstance.dashboard;

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
import com.sirma.cmf.web.DocumentContext;
import com.sirma.cmf.web.userdashboard.filter.DashboardTaskFilter;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;

/**
 * Test class for {@link CaseTasksPanel}
 * 
 * @author cdimitrov
 */
@Test
public class CaseTasksPanelTest extends CMFTest {

	/** The reference for tested panel. */
	private CaseTasksPanel caseTasksPanel;

	/** The label provider. */
	private LabelProvider labelProvider;

	/** The size of the supported filters. */
	private int taskFiltersNumber = 6;

	/**
	 * The default test constructor.
	 */
	public CaseTasksPanelTest() {
		caseTasksPanel = new CaseTasksPanel() {

			private static final long serialVersionUID = 1L;

			/** The document context. */
			private DocumentContext docContext = new DocumentContext();

			/**
			 * Getter method for document context
			 * 
			 * @return current document context
			 */
			public DocumentContext getDocumentContext() {
				return docContext;
			}

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

		ReflectionUtils.setField(caseTasksPanel, "labelProvider", labelProvider);
	}

	/**
	 * Test method for retrieving context data.
	 */
	public void retrievePanelContextTest() {
		Assert.assertNull(caseTasksPanel.getDocumentContext().getCurrentInstance());
		Assert.assertNull(caseTasksPanel.getDocumentContext().getContextInstance());

		CaseInstance caseInstance = createCaseInstance(6L);
		caseTasksPanel.getDocumentContext().setCurrentInstance(caseInstance);
		Assert.assertEquals(caseTasksPanel.getDocumentContext().getCurrentInstance(), caseInstance);
	}

	/**
	 * Test method for current panel search arguments.
	 */
	public void getSearchArgumentsTest() {
		Assert.assertNotNull(caseTasksPanel.getSearchArguments());
	}

	/**
	 * Test method for unsupported filters.
	 */
	public void getTasksFilterUnssuportedTest() {
		caseTasksPanel.setTaskFilters(null);
		Assert.assertNull(caseTasksPanel.getTaskFilters());
	}

	/**
	 * Test method for supported filters.
	 */
	public void getTasksFilterSupportedTest() {
		caseTasksPanel.loadFilters();
		Assert.assertNotNull(caseTasksPanel.getTaskFilters());
		Assert.assertEquals(caseTasksPanel.getTaskFilters().size(), taskFiltersNumber);
	}

	/**
	 * Test method for unsupported panel actions.
	 */
	public void getActionsNotSupportedTest() {
		caseTasksPanel.setTaskActions(null);
		Assert.assertEquals(caseTasksPanel.getTaskActions(null), new ArrayList<Action>());

		AbstractTaskInstance taskInstance = createStandaloneTaskInstance(1L);
		Assert.assertEquals(caseTasksPanel.getTaskActions(taskInstance), new ArrayList<Action>());
	}

	/**
	 * Test method for supported panel actions.
	 */
	public void getActionsSupportedTest() {
		Map<Serializable, List<Action>> actions = new HashMap<Serializable, List<Action>>();
		AbstractTaskInstance taskInstance = createStandaloneTaskInstance(2L);
		actions.put(taskInstance.getId(), new ArrayList<Action>(1));

		caseTasksPanel.setTaskActions(actions);
		Assert.assertEquals(caseTasksPanel.getTaskActions(taskInstance), new ArrayList<Action>(1));
	}

	/**
	 * Test method for retrieving default panel filter label.
	 */
	public void getDefaultTaskActivatorLinkLabelTest() {
		caseTasksPanel.loadFilters();
		Assert.assertEquals(caseTasksPanel.getTaskFilterActivatorLinkLabel(), "active_tasks");
	}

}
