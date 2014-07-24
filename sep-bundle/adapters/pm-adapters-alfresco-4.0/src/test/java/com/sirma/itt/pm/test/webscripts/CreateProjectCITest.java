package com.sirma.itt.pm.test.webscripts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sirma.itt.cmf.test.PmBaseAlfrescoTest;
import com.sirma.itt.emf.adapter.DMSDefintionAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.pm.alfresco4.services.ProjectInstanceAlfresco4Service;
import com.sirma.itt.pm.domain.definitions.ProjectDefinition;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * The Class CreateProjectTest.
 */
public class CreateProjectCITest extends PmBaseAlfrescoTest {

	/** The instance adapter. */
	private ProjectInstanceAlfresco4Service instanceAdapter;

	@Override
	@BeforeClass
	protected void setUp() {
		super.setUp();
		instanceAdapter = getMockupProvider().mockupProjectAdapter();
	}


	/**
	 * Test create.
	 */
	@Test
	public void testCreate() {
		ProjectInstance projectInstance = new ProjectInstance();
		DMSDefintionAdapterService mockupDefinitonAdapter = mockupProvider.mockupDefinitonAdapter();
		try {
			List<DMSFileDescriptor> allProjectDefinitions = mockupDefinitonAdapter
					.getDefinitions(ProjectDefinition.class);
			projectInstance.setIdentifier(allProjectDefinitions.get(0).getId());
			projectInstance.setProperties(new HashMap<String, Serializable>());
			projectInstance.getProperties().put("projectNumber", "4234-43-3-44433");
			projectInstance.getProperties().put("type", "4234-43-3-44433");
			instanceAdapter.createProjectInstance(projectInstance);
		} catch (DMSException e) {
			e.printStackTrace();

		}
	}
}