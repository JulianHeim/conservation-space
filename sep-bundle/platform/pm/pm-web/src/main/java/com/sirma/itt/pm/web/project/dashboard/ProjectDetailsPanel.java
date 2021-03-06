package com.sirma.itt.pm.web.project.dashboard;

import java.io.Serializable;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.constants.CMFConstants;
import com.sirma.cmf.web.form.FormViewMode;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.domain.definitions.ProjectDefinition;
import com.sirma.itt.pm.domain.model.ProjectInstance;
import com.sirma.itt.pm.services.ProjectService;

/**
 * ProjectDetailsPanel backing bean.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectDetailsPanel extends
		DashboardPanelActionBase<ProjectInstance, SearchArguments<ProjectInstance>> implements
		Serializable, DashboardPanelController {

	private static final long serialVersionUID = -6570328215066229325L;

	private Set<String> dashletActionIds;

	@Inject
	private ProjectService projectService;

	@Override
	public void initData() {
		dashletActionIds = getActionsForCurrentInstance();
		onOpen();
		renderProjectFields();
	}

	@Override
	protected boolean getFilterActions() {
		return false;
	}

	/**
	 * Builds a project properties fields for preview.
	 */
	public void renderProjectFields() {
		UIComponent panel = getPanel(CMFConstants.INSTANCE_DATA_PANEL);
		if (panel != null) {
			panel.getChildren().clear();
		}

		ProjectInstance projectInstance = getDocumentContext().getInstance(ProjectInstance.class);
		projectService.refresh(projectInstance);
		ProjectDefinition definition = (ProjectDefinition) dictionaryService
				.getInstanceDefinition(projectInstance);
		getDocumentContext().populateContext(projectInstance, ProjectDefinition.class, definition);
		getDocumentContext().addContextInstance(projectInstance);

		invokeReader(definition, projectInstance, panel, FormViewMode.PREVIEW, null);
	}

	@Override
	public void loadFilters() {
		// Auto-generated method stub
	}

	@Override
	public void executeDefaultFilter() {
		// Auto-generated method stub
	}

	@Override
	public void filter(SearchArguments<ProjectInstance> searchArguments) {
		// Auto-generated method stub
	}

	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// Auto-generated method stub
	}

	@Override
	public SearchArguments<ProjectInstance> getSearchArguments() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> dashletActionIds() {
		return dashletActionIds;
	}

	@Override
	public String targetDashletName() {
		return "project-details-dashlet";
	}

	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

}
