package com.sirma.itt.pm.web.project.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.cmf.web.DocumentContext;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.emf.search.SearchArgumentsMap;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.pm.PMTest;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * Test class for {@link ProjectsDocumentPanel}.
 * 
 * @author cdimitrov
 */
@Test
public class ProjectsDocumentPanelTest extends PMTest {

	/** The reference for tested panel. */
	private ProjectsDocumentPanel projectDocumentPanel;

	/** The project instance, that will be used for panel context. */
	private ProjectInstance projectInstnace;

	/**
	 * Default test constructor, used for initializing test components.
	 */
	public ProjectsDocumentPanelTest() {
		projectDocumentPanel = new ProjectsDocumentPanel() {

			private static final long serialVersionUID = 1L;

			/** The document context, provide additional panel component. */
			private DocumentContext documentContext = new DocumentContext();

			@Override
			public SearchArgumentsMap<CaseInstance, List<DocumentInstance>> getSearchArguments() {
				return new SearchArgumentsMap<CaseInstance, List<DocumentInstance>>();
			}

			@Override
			public DocumentContext getDocumentContext() {
				return documentContext;
			}
		};
	}

	/**
	 * Method for testing panel context data.
	 */
	public void getPanelContextInstanceTest() {
		Assert.assertNull(projectDocumentPanel.getDocumentContext().get(ProjectInstance.class));
		projectInstnace = createProjectInstance(1L, "dmsId");
		projectDocumentPanel.getDocumentContext().addInstance(projectInstnace);
		Assert.assertEquals(
				projectDocumentPanel.getDocumentContext().getInstance(ProjectInstance.class),
				projectInstnace);
	}

	/**
	 * Test method for available actions in the panel.
	 */
	public void getDocumentActionNotAvailableTest() {
		projectDocumentPanel.setDashletActions(null);
		Assert.assertNull(projectDocumentPanel.getDashletActions());

		Assert.assertEquals(projectDocumentPanel.getDocumentActions(null), new ArrayList<Action>());

		DocumentInstance documentInstance = new DocumentInstance();
		Assert.assertEquals(projectDocumentPanel.getDocumentActions(documentInstance),
				new ArrayList<Action>());
	}

}
