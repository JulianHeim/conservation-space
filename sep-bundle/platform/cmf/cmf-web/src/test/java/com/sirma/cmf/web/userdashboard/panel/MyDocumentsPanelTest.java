package com.sirma.cmf.web.userdashboard.panel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.cmf.CMFTest;
import com.sirma.cmf.web.userdashboard.filter.DashboardDocumentFilter;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.search.SearchArgumentsMap;
import com.sirma.itt.emf.security.model.Action;

/**
 * Test class for @link {@link MyDocumentsPanel}.
 * 
 * @author cdimitrov
 */
@Test
public class MyDocumentsPanelTest extends CMFTest {

	/** The reference for tested panel. */
	private MyDocumentsPanel myDocumentPanel;

	/** The label provider that will be used for retrieving filter labels. */
	private LabelProvider labelProvider;

	/** The default supported filter size. */
	private int documentDefailtSize = 2;

	/**
	 * Default test constructor, used for initializing test components.
	 */
	public MyDocumentsPanelTest() {
		myDocumentPanel = new MyDocumentsPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			public SearchArgumentsMap<CaseInstance, List<DocumentInstance>> getSearchArguments() {
				return new SearchArgumentsMap<CaseInstance, List<DocumentInstance>>();
			}
		};

		labelProvider = mock(LabelProvider.class);

		when(
				labelProvider
						.getValue(DashboardDocumentFilter.CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF
								+ "i_am_editing")).thenReturn("i_am_editing");
		when(
				labelProvider
						.getValue(DashboardDocumentFilter.CMF_USER_DASHBOARD_DOCUMENT_FILTER_PREF
								+ "last_used")).thenReturn("last_used");

		ReflectionUtils.setField(myDocumentPanel, "labelProvider", labelProvider);
	}

	/**
	 * Test method for not null-able search arguments for current panel.
	 */
	public void getDocumentSearchArgumentsTest() {
		Assert.assertNotNull(myDocumentPanel.getSearchArguments());
	}

	/**
	 * Test method for retrieving null-able filters.
	 */
	public void getDocumentFilterNotAvailableTest() {
		myDocumentPanel.setDocumentFilters(null);
		Assert.assertNull(myDocumentPanel.getDocumentFilters());
	}

	/**
	 * Test method for retrieving supported panel filters.
	 */
	public void getDocumentFilterSupportedTest() {
		myDocumentPanel.loadFilters();
		Assert.assertNotNull(myDocumentPanel.getDocumentFilters());
		Assert.assertEquals(myDocumentPanel.getDocumentFilters().size(), documentDefailtSize);
	}

	/**
	 * Test method for retrieving default activator label.
	 */
	public void getDefaultActivatorLinkLableTest() {
		Assert.assertEquals(myDocumentPanel.getDocumentFilterActivatorLinkLabel(), "last_used");
	}

	/**
	 * Test method for retrieving not supported actions for document panel.
	 */
	public void getDocumentActionNotAvailable() {
		myDocumentPanel.setDashletActions(null);
		Assert.assertNull(myDocumentPanel.getDashletActions());

		Assert.assertEquals(myDocumentPanel.getDocumentActions(null), new ArrayList<Action>());

		DocumentInstance documentInstance = createDocumentInstance(1L);
		Assert.assertEquals(myDocumentPanel.getDocumentActions(documentInstance),
				new ArrayList<Action>());
	}

}
