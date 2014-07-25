package com.sirma.cmf.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.constants.CMFConstants;
import com.sirma.cmf.web.form.FormViewMode;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * CaseDetailsPanel backing bean.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseDetailsPanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	private static final long serialVersionUID = 6205869710050734957L;

	/**
	 * The dashlet actions is a set of defined allowed actions that may be executed from particular
	 * dashelet.
	 */
	private Set<String> dashletActions;

	@Override
	public void initData() {
		dashletActions = getActionsForCurrentInstance();
		onOpen();
		renderCaseFields();
	}

	@Override
	protected boolean getFilterActions() {
		return false;
	}

	/**
	 * Builds a case properties fields for preview.
	 */
	public void renderCaseFields() {
		UIComponent panel = getPanel(CMFConstants.CASE_DATA_PANEL);
		if (panel != null) {
			panel.getChildren().clear();
		}

		reloadCaseInstance();
		CaseInstance caseInstance = getDocumentContext().getInstance(CaseInstance.class);
		CaseDefinition caseDefinition = (CaseDefinition) dictionaryService
				.getInstanceDefinition(caseInstance);
		getDocumentContext().populateContext(caseInstance, CaseDefinition.class, caseDefinition);

		invokeReader(caseDefinition, caseInstance, panel, FormViewMode.PREVIEW, null);
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
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		// Auto-generated method stub
	}

	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// Auto-generated method stub
	}

	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> dashletActionIds() {
		return dashletActions;
	}

	@Override
	public String targetDashletName() {
		return "case-details-dashlet";
	}

	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

}
