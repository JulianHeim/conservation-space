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
import com.sirma.cmf.web.userdashboard.filter.DashboardDateFilter;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.pm.PMTest;
import com.sirma.itt.pm.domain.model.ProjectInstance;
import com.sirma.itt.pm.web.project.dashboard.filters.DashboardCaseFilter;

/**
 * Test method for {@link ProjectCasePanel}
 * 
 * @author cdimitrov
 */
@Test
public class ProjectCasePanelTest extends PMTest {

	/** The project panel reference. */
	private ProjectCasePanel projectCasePanel;

	/** The label provider that will be used for retrieving filter labels. */
	private LabelProvider labelProvider;

	/** The default supported case filters. */
	private int supportedCaseFilters = 2;

	/** The default supported date filters. */
	private int supportedDateFilters = 4;

	/** The project instance that will be used in the tests. */
	private ProjectInstance projectInstance;

	/**
	 * Default constructor for the test class.
	 */
	public ProjectCasePanelTest() {

		projectCasePanel = new ProjectCasePanel() {

			private static final long serialVersionUID = 1L;

			private DocumentContext documentContext = new DocumentContext();

			@Override
			public SearchArguments<CaseInstance> getSearchArguments() {
				return new SearchArguments<CaseInstance>();
			}

			public DocumentContext getDocumentContext() {
				return documentContext;
			}

			public void setDocumentContext(DocumentContext documentContext) {
				this.documentContext = documentContext;
			}

		};

		// mock the label builder
		labelProvider = mock(LabelProvider.class);

		// case filter labels
		when(
				labelProvider.getValue(DashboardCaseFilter.PM_PROJECT_DASHBOARD_CASE_FILTER_PREF
						+ "all_cases")).thenReturn("all_cases");
		when(
				labelProvider.getValue(DashboardCaseFilter.PM_PROJECT_DASHBOARD_CASE_FILTER_PREF
						+ "active_cases")).thenReturn("active_cases");
		when(
				labelProvider.getValue(DashboardCaseFilter.PM_PROJECT_DASHBOARD_CASE_FILTER_PREF
						+ "completed_cases")).thenReturn("completed_cases");

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

		ReflectionUtils.setField(projectCasePanel, "labelProvider", labelProvider);

		projectInstance = createProjectInstance(Long.valueOf(3), "dmsId");

	}

	/**
	 * Test method for empty context instance(project).
	 */
	public void getProjectInstanceFromContextEmptyTest() {
		projectCasePanel.getDocumentContext().addContextInstance(null);
		Assert.assertNull(projectCasePanel.getDocumentContext().getContextInstance());
	}

	/**
	 * Test method for supported context instance(project).
	 */
	public void getProjectInstanceFromContextSupportedTest() {
		projectCasePanel.getDocumentContext().addContextInstance(projectInstance);
		Assert.assertNotNull(projectCasePanel.getDocumentContext().getContextInstance());
	}

	/**
	 * Test for empty project case filters.
	 */
	public void getProjectCaseFiltersEmptyTest() {
		projectCasePanel.setProjectCaseFilters(null);
		Assert.assertNull(projectCasePanel.getProjectCaseFilters());
	}

	/**
	 * Test for empty project case date filters.
	 */
	public void getDateFiltersEmptyTest() {
		projectCasePanel.setProjectDateFilters(null);
		Assert.assertNull(projectCasePanel.getProjectDateFilters());
	}

	/**
	 * Test for all supported project case filters. Default are four {@link DashboardCaseFilter}.
	 */
	public void getCaseFilterAllSupportedTest() {
		projectCasePanel.loadFilters();
		Assert.assertEquals(projectCasePanel.getProjectCaseFilters().size(), supportedCaseFilters);
	}

	/**
	 * Test for all supported case filters. Default are four {@link DashboardDateFilter}.
	 */
	public void getDateFilterAllSupportedTest() {
		projectCasePanel.loadFilters();
		Assert.assertEquals(projectCasePanel.getProjectDateFilters().size(), supportedDateFilters);
	}

	/**
	 * Test for default project case activator link label.
	 */
	public void getCaseFilterActivatorLinkLabelDefaultTest() {
		projectCasePanel.loadFilters();
		Assert.assertEquals(projectCasePanel.getCaseActivatorLinkLabel(), "all_cases");
	}

	/**
	 * Test for default project date activator link label.
	 */
	public void getDateFilterActivatorLinkLabelDefaultTest() {
		projectCasePanel.loadFilters();
		Assert.assertEquals(projectCasePanel.getDateActivatorLinkLabel(), "last_week");
	}

	/**
	 * Test method for not empty search arguments.
	 */
	public void getSearchArgumentsEmptyTest() {
		Assert.assertNotNull(projectCasePanel.getSearchArguments());
	}

	/**
	 * Test method for retrieving all actions based on null-able case instance.
	 */
	public void getCaseActionsEmptyInstanceTest() {
		Assert.assertEquals(projectCasePanel.getCaseRecordActions(null), new ArrayList<Action>());
	}

	/**
	 * Test method for retrieving all actions based on supported case instance.
	 */
	public void getCaseActionsSupportedInstanceTest() {
		Map<Serializable, List<Action>> actions = new HashMap<Serializable, List<Action>>();
		CaseInstance caseInstance = new CaseInstance();
		caseInstance.setId(1);
		actions.put(caseInstance.getId(), new ArrayList<Action>(3));

		projectCasePanel.setCaseRecordActions(actions);
		Assert.assertEquals(projectCasePanel.getCaseRecordActions(caseInstance),
				new ArrayList<Action>(3));
	}

}
