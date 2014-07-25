package com.sirma.cmf.web.userdashboard.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.document.DocumentUtil;
import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.search.SearchArgumentsMap;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.services.RenditionService;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * <b>MyMediaPanel</b> manage functionality for dashlet, located in personal/user dashboard. The
 * content is represented as document with image records, actions and filters.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyMediaPanel
		extends
		DashboardPanelActionBase<CaseInstance, SearchArgumentsMap<CaseInstance, List<DocumentInstance>>>
		implements Serializable, DashboardPanelController {

	/** The Constant serial version identifier. */
	private static final long serialVersionUID = 1L;

	/** The thumbnail prefix needed to represent the image as <b>base64</b> */
	private static final String TUMBNAIL_PREFIX = "data:image/png;base64,";

	/** The result map, represent as case instance and document instances. */
	private Map<CaseInstance, List<DocumentInstance>> resultMap;

	/** The list with case instances. */
	private List<CaseInstance> caseInstances;

	/** The list with document instances. */
	private List<DocumentInstance> documentInstance;

	/** The media actions for all records. */
	private Map<Serializable, List<Action>> mediaActions;

	/** The media panel actions placeholder. */
	private static final String USER_MEDIA_DASHLET_ACTIONS = "user-dashboard-media-panel";

	/** The search service. */
	@Inject
	private SearchService searchService;

	/** The rendition service. */
	@Inject
	private RenditionService renditionService;

	/** Holds command logic that is used in document and media panels. */
	@Inject
	private DocumentUtil documentUtil;

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
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArgumentsMap<CaseInstance, List<DocumentInstance>> searchArguments) {
		searchService.search(DocumentInstance.class, searchArguments);
		resultMap = searchArguments.getResultMap();
		documentInstance = new ArrayList<DocumentInstance>();
		caseInstances = new ArrayList<CaseInstance>(resultMap.keySet());
		documentInstance = documentUtil.extractCaseDocuments(caseInstances, resultMap);
		setMediaActions(getActionsForInstances(documentInstance, USER_MEDIA_DASHLET_ACTIONS));
		notifyForLoadedData();
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
	public void updateSelectedFilterField(String selectedItemId) {
		// TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArgumentsMap<CaseInstance, List<DocumentInstance>> getSearchArguments() {
		SearchArgumentsMap<CaseInstance, List<DocumentInstance>> searchArguments = null;
		Context<String, Object> context = new Context<>(1);
		context.put(SearchFilterProperties.USER_ID, userId);
		context.put(DocumentProperties.MIMETYPE, "image/*");
		searchArguments = searchService.getFilter("listEditedDocumentsByUser", CaseInstance.class,
				context);
		return searchArguments;
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
	 * Get method from resultMap
	 * 
	 * @return resultMap
	 */
	public Map<CaseInstance, List<DocumentInstance>> getResultMap() {
		return resultMap;
	}

	/**
	 * Get method for case instance.
	 * 
	 * @return case instance
	 */
	public List<CaseInstance> getCaseInstances() {
		waitForDataToLoad();
		return caseInstances;
	}

	/**
	 * Set method for case instance.
	 * 
	 * @param caseInstances
	 *            case instance
	 */
	public void setCaseInstances(List<CaseInstance> caseInstances) {
		this.caseInstances = caseInstances;
	}

	/**
	 * Getter for document instances.
	 * 
	 * @return list with document instance
	 */
	public List<DocumentInstance> getDocumentInstance() {
		return documentInstance;
	}

	/**
	 * Setter for document instances.
	 * 
	 * @param documentInstance
	 *            list with document instances
	 */
	public void setDocumentInstance(List<DocumentInstance> documentInstance) {
		this.documentInstance = documentInstance;
	}

	/**
	 * Setter for media panel actions.
	 * 
	 * @param mediaActions
	 *            media panel actions
	 */
	public void setMediaActions(Map<Serializable, List<Action>> mediaActions) {
		this.mediaActions = mediaActions;
	}

	/**
	 * Getter for media panel actions based on current instance.
	 * 
	 * @param instance
	 *            current instance(object)
	 * @return supported actions
	 */
	public List<Action> getMediaActions(Instance instance) {
		if ((instance == null) || (mediaActions == null)) {
			return new ArrayList<Action>();
		}
		return mediaActions.get(instance.getId());
	}
}