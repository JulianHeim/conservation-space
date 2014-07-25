package com.sirma.cmf.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.services.RenditionService;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * This class will display all object that have thumbnails. Will be used on the case dashboard.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseMediaPanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The thumbnail prefix. */
	private static final String TUMBNAIL_PREFIX = "data:image/png;base64,";

	/** The string splitter identifier. */
	private static final String STRING_SPLITTER = "/";

	/** The document instances. */
	private List<DocumentInstance> documentInstances;

	/** The media actions for all records. */
	private Map<Serializable, List<Action>> mediaActions;

	/** The media panel actions placeholder. */
	private static final String CASE_MEDIA_DASHLET_ACTIONS = "case-dashboard-media-panel";

	/** The rendition service. */
	@Inject
	private RenditionService renditionService;

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
	protected boolean isAsynchronousLoadingSupported() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		CaseInstance caseInstance = getDocumentContext().getInstance(CaseInstance.class);
		if (caseInstance != null) {
			List<SectionInstance> sections = caseInstance.getSections();
			documentInstances = new ArrayList<DocumentInstance>();
			if (sections != null) {
				for (SectionInstance section : sections) {
					documentInstances.addAll(getAllWithAttachments(section.getContent()));
				}
				if(documentInstances != null && !documentInstances.isEmpty()){
					Collections.sort(documentInstances, Collections.reverseOrder(new DocumentComparator()));
				}
				setMediaActions(getActionsForInstances(documentInstances, CASE_MEDIA_DASHLET_ACTIONS));
			}
		}
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
				if (isImage((DocumentInstance) documentInstance)) {
					result.add((DocumentInstance) documentInstance);
				}
			}
		}

		return result;
	}

	/**
	 * This method help for detecting specific MIME types for the file. TODO: after mapping for MIME
	 * type is implemented this method will be removed.
	 * 
	 * @param documentInstance
	 *            document instance.
	 * @return boolean value
	 */
	protected boolean isImage(DocumentInstance documentInstance) {
		boolean isValid = false;
		if ((documentInstance == null) || (documentInstance.getProperties() == null)) {
			return isValid;
		}
		String mimeType = (String) documentInstance.getProperties()
				.get(DocumentProperties.MIMETYPE);
		if (mimeType != null) {
			String prefMime = mimeType.split(STRING_SPLITTER)[0];
			if ("image".equals(prefMime)) {
				isValid = true;
			}
		}
		return isValid;
	}

	/**
	 * Get thumbnail based on the instance.
	 * 
	 * @param instance
	 *            current instance
	 * @return thumbnail
	 */
	public String checkForThumbnail(Instance instance) {

		String thumbnail = renditionService.getThumbnail(instance);

		if (thumbnail != null) {
			return TUMBNAIL_PREFIX + thumbnail;
		}
		return null;
	}

	/**
	 * Get compact tree header for current document instance.
	 * 
	 * @param instance
	 *            document instance
	 * @return compact tree header
	 */
	public String getDefaultHeader(DocumentInstance instance) {
		if ((instance == null) || (instance.getProperties() == null)) {
			return null;
		}
		return (String) instance.getProperties().get(DefaultProperties.HEADER_COMPACT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		// TODO Auto-generated method stub
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
	 * Setter for media actions for every record in the panel.
	 * 
	 * @param mediaActions
	 *            supported media actions
	 */
	public void setMediaActions(Map<Serializable, List<Action>> mediaActions) {
		this.mediaActions = mediaActions;
	}

	/**
	 * Getter for media actions based on current instance.
	 * 
	 * @param instance
	 *            current instance
	 * @return supported actions for current instance
	 */
	public List<Action> getMediaAction(Instance instance) {
		return mediaActions.get(instance.getId());
	}

}