package com.sirma.cmf.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * <b>CaseDocumentsPanel</b> manage functionality for panel, located in case dashboard. The
 * content is represented as document records, actions and filters.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseDocumentsPanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	/** The constant serial version identifier. */
	private static final long serialVersionUID = 5328697527361900449L;

	/** The document instance list. */
	private List<DocumentInstance> documentInstances;

	/** The document panel actions located in the toolbar. */
	private final Set<String> dashletActions = new HashSet<String>(Arrays.asList(
			ActionTypeConstants.CREATE_DOCUMENTS_SECTION, ActionTypeConstants.UPLOAD));

	/** The available document actions for result records. */
	private Map<Serializable, List<Action>> documentActions;

	/** The document panel placeholder. */
	private static final String CASE_DOCUMENT_DASHLET_PLACEHOLDER = "case-dashboard-document";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initData() {
		onOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		// Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isAsynchronousLoadingSupported() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		CaseInstance caseInstance = getDocumentContext().getInstance(CaseInstance.class);
		// CaseInstance reloadedInstance = caseInstanceService.loadByDbId(caseInstance.getId());
		instanceService.refresh(caseInstance);
		CaseDefinition instanceDefinition = (CaseDefinition) dictionaryService
				.getInstanceDefinition(caseInstance);
		getDocumentContext()
				.populateContext(caseInstance, CaseDefinition.class, instanceDefinition);
		List<SectionInstance> sections = caseInstance.getSections();
		documentInstances = new ArrayList<DocumentInstance>();
		if (sections != null) {
			for (SectionInstance section : sections) {
				documentInstances.addAll(getAllWithAttachments(section.getContent()));
			}
			if ((documentInstances != null) && !documentInstances.isEmpty()) {
				Collections.sort(documentInstances,
						Collections.reverseOrder(new DocumentComparator()));
			}
		}
		setDocumentActions(getActionsForInstances(documentInstances,
				CASE_DOCUMENT_DASHLET_PLACEHOLDER));

		notifyForLoadedData();

		log.debug("CMFWeb: found [" + documentInstances.size() + "] document instances");
	}

	/**
	 * Filters {@link DocumentInstance} list and leaves only those with attachments.
	 * 
	 * @param documentInstances
	 *            List with {@link DocumentInstance} objects.
	 * @return List with {@link DocumentInstance} objects that has attachments.
	 */
	private List<DocumentInstance> getAllWithAttachments(List<Instance> documentInstances) {

		List<DocumentInstance> result = new ArrayList<DocumentInstance>(documentInstances.size());

		for (Instance documentInstance : documentInstances) {
			if ((documentInstance instanceof DocumentInstance)
					&& ((DocumentInstance) documentInstance).hasDocument()) {
				result.add((DocumentInstance) documentInstance);
			}
		}

		return result;
	}

	/**
	 * Generates a css row classes string used in rich:list in order to hide rows for document
	 * instances that has no attachments.
	 * 
	 * @param documentInstances
	 *            DocumentInstances list.
	 * @return String with row classes.
	 */
	public String documentRowClasses(List<DocumentInstance> documentInstances) {
		List<String> classes = new ArrayList<String>();
		if (documentInstances != null) {
			for (DocumentInstance documentInstance : documentInstances) {
				if (documentInstance.hasDocument()) {
					classes.add("");
				} else {
					classes.add("hidden-row");
				}
			}
		}
		String csv = "";
		if (!classes.isEmpty()) {
			csv = classes.toString();
			csv = csv.substring(1, csv.length() - 1);
		}
		return csv;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		// Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// Auto-generated method stub
	}

	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		// Auto-generated method stub
		return null;
	}

	/**
	 * Getter method for documentInstances.
	 * 
	 * @return the documentInstances
	 */
	public List<DocumentInstance> getDocumentInstances() {
		waitForDataToLoad();
		return documentInstances;
	}

	/**
	 * Setter method for documentInstances.
	 * 
	 * @param documentInstances
	 *            the documentInstances to set
	 */
	public void setDocumentInstances(List<DocumentInstance> documentInstances) {
		this.documentInstances = documentInstances;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		return dashletActions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return "document-section-actions";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

	/**
	 * Setter for document actions.
	 * 
	 * @param documentActions
	 *            document actions
	 */
	public void setDocumentActions(Map<Serializable, List<Action>> documentActions) {
		this.documentActions = documentActions;
	}

	/**
	 * Getter for document actions based on instance.
	 * 
	 * @param instance
	 *            current document instance
	 * @return list with document actions
	 */
	public List<Action> getDocumentActions(Instance instance) {
		return documentActions.get(instance.getId());
	}
}
