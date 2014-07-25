package com.sirma.cmf.web.caseinstance;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.caseinstance.tab.CaseTabConstants;
import com.sirma.cmf.web.constants.NavigationConstants;
import com.sirma.cmf.web.form.FormViewMode;
import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.constants.CaseProperties;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.instance.model.NullInstance;
import com.sirma.itt.emf.security.action.EMFAction;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.web.action.event.EMFActionEvent;

/**
 * CaseInstance processing manager.
 * 
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class CaseAction extends CaseLandingPage {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 2135275835545918747L;

	/** The case search action. */
	@Inject
	private CaseSearchAction caseSearchAction;

	/** Reason for closing the case. */
	private String caseClosingReason;

	/** The case section title. */
	private String caseSectionTitle;

	/**
	 * Observer for create action.
	 * 
	 * @param event
	 *            The event payload object.
	 */
	public void caseCreateNoContext(
			@Observes @EMFAction(value = ActionTypeConstants.CREATE_CASE, target = NullInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.caseCreate");
		navigationMenuAction.setSelectedMenu(NavigationConstants.NAVIGATE_MENU_CASE_LIST);
		getDocumentContext().clearAndLeaveContext();
		event.setNavigation(NavigationConstants.NAVIGATE_NEW_CASE);
	}

	/**
	 * Creates the case documents section.
	 * 
	 * @param event
	 *            the event
	 */
	public void createCaseDocumentsSection(
			@Observes @EMFAction(value = ActionTypeConstants.CREATE_DOCUMENTS_SECTION, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.createCaseDocumentsSection");
		getDocumentContext().getCurrentOperation(CaseInstance.class.getSimpleName());
	}

	/**
	 * Creates the case objects section. TODO: move method to objects module
	 * 
	 * @param event
	 *            the event
	 */
	public void createCaseObjectsSection(
			@Observes @EMFAction(value = ActionTypeConstants.CREATE_OBJECTS_SECTION, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.createCaseDocumentsSection");
	}

	/**
	 * Create the case section.
	 * 
	 * @return navigation string
	 */
	public String createCaseSection() {
		String currentOperation = getDocumentContext().getCurrentOperation(
				CaseInstance.class.getSimpleName());
		log.debug("CMFWeb: Executing CaseAction operation [" + currentOperation
				+ "], section title[" + caseSectionTitle + "]");

		String navigation = null;
		if (ActionTypeConstants.CREATE_DOCUMENTS_SECTION.equals(currentOperation)) {
			navigation = "case-documents";
			getDocumentContext().setSelectedTab("caseDocuments");
		} else if (ActionTypeConstants.CREATE_OBJECTS_SECTION.equals(currentOperation)) {
			// TODO: move this to objects module
			navigation = "case-objects";
			getDocumentContext().setSelectedTab("caseObjects");
		}
		return navigation;
	}

	/**
	 * Link case.
	 * 
	 * @param event
	 *            the event
	 */
	public void linkCase(
			@Observes @EMFAction(value = ActionTypeConstants.LINK, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.linkCase");
		CaseInstance selectedInstance = (CaseInstance) event.getInstance();
		if (selectedInstance != null) {
			getDocumentContext().setRootInstance(selectedInstance.getOwningInstance());
			getDocumentContext().addInstance(selectedInstance);
			event.setNavigation(NavigationConstants.NAVIGATE_CASE_LINK);
		}
	}

	/**
	 * Edit case opration observer. This operation should be executed only if there are case
	 * instance provided with the event and the case definition is found for that instance.
	 * 
	 * @param event
	 *            Event payload object.
	 */
	public void caseEdit(
			@Observes @EMFAction(value = ActionTypeConstants.EDIT_DETAILS, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.caseEdit");
		String navigation = NavigationConstants.RELOAD_PAGE;
		CaseInstance selectedCaseInstance = (CaseInstance) event.getInstance();
		if (selectedCaseInstance != null) {
			getDocumentContext().setRootInstance(selectedCaseInstance.getOwningInstance());
			CaseDefinition caseDefinition = dictionaryService.getDefinition(
					getInstanceDefinitionClass(), selectedCaseInstance.getIdentifier());
			if (caseDefinition != null) {
				getDocumentContext().populateContext(selectedCaseInstance,
						getInstanceDefinitionClass(), caseDefinition);
				// TODO: why this is set here?
				setSelectedType((String) selectedCaseInstance.getProperties().get(
						CaseProperties.TYPE));
				getDocumentContext().setFormMode(FormViewMode.EDIT);
				navigation = NavigationConstants.NAVIGATE_TAB_CASE_DETAILS;
				getDocumentContext().setSelectedTab(CaseTabConstants.DETAILS);
			}
		}
		event.setNavigation(navigation);
	}

	/**
	 * Case close action observer.
	 * 
	 * @param event
	 *            Event payload object.
	 */
	public void caseCloseObserver(
			@Observes @EMFAction(value = ActionTypeConstants.COMPLETE, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.caseCloseObserver");
		getDocumentContext().addInstance(event.getInstance());
	}

	/**
	 * Close selected case. This action is invoked when user clicks on 'ok' button from case closing
	 * reason popup panel.
	 * 
	 * @return navigation string
	 */
	public String caseClose() {
		if (!StringUtils.isNullOrEmpty(caseClosingReason)) {
			log.debug("CMFWeb: Executing CaseAction.caseClose");
			CaseInstance caseInstance = getDocumentContext().getInstance(getInstanceClass());

			caseInstanceService.closeCaseInstance(caseInstance, createOperation());
			// reset
			caseClosingReason = null;
		}
		return NavigationConstants.RELOAD_PAGE;
	}

	/**
	 * Case delete action.
	 * 
	 * @param event
	 *            Event payload object.
	 */
	public void caseDelete(
			@Observes @EMFAction(value = ActionTypeConstants.DELETE, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.caseDelete");
		caseInstanceService.delete((CaseInstance) event.getInstance(), createOperation(), false);
		// reset
		getDocumentContext().clear();
		// execute search for active cases and open the result list.
		caseSearchAction.executeDefaultCaseSearch();
		event.setNavigation(NavigationConstants.NAVIGATE_CASE_LIST_PAGE);
	}

	/**
	 * Invoked on case stop operation. Stop in dms and cmf
	 * 
	 * @param event
	 *            is the case stop event
	 */
	public void caseStopObserver(
			@Observes @EMFAction(value = ActionTypeConstants.STOP, target = CaseInstance.class) final EMFActionEvent event) {
		log.debug("CMFWeb: Executing observer CaseAction.caseStopObserver");
		getDocumentContext().addInstance(event.getInstance());
		caseInstanceService.closeCaseInstance((CaseInstance) event.getInstance(), new Operation(
				ActionTypeConstants.STOP));
	}

	/**
	 * Getter method for caseClosingReason.
	 * 
	 * @return the caseClosingReason
	 */
	public String getCaseClosingReason() {
		return caseClosingReason;
	}

	/**
	 * Setter method for caseClosingReason.
	 * 
	 * @param caseClosingReason
	 *            the caseClosingReason to set
	 */
	public void setCaseClosingReason(String caseClosingReason) {
		this.caseClosingReason = caseClosingReason;
	}

	/**
	 * Getter method for caseSectionTitle.
	 * 
	 * @return the caseSectionTitle
	 */
	public String getCaseSectionTitle() {
		return caseSectionTitle;
	}

	/**
	 * Setter method for caseSectionTitle.
	 * 
	 * @param caseSectionTitle
	 *            the caseSectionTitle to set
	 */
	public void setCaseSectionTitle(String caseSectionTitle) {
		this.caseSectionTitle = caseSectionTitle;
	}

}
