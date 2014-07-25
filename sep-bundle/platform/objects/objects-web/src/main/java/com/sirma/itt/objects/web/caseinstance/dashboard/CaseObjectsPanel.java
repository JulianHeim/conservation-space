package com.sirma.itt.objects.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.Sorter;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * This class integrate object into case dashboard. <b>NOTE: In this class we use workaround for
 * retrieving object, this allow to use action for current object that need section instance
 * inside.</b>
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseObjectsPanel extends
		DashboardPanelActionBase<ObjectInstance, SearchArguments<ObjectInstance>> implements
		Serializable, DashboardPanelController {

	/** The constant serial version identifier. */
	private static final long serialVersionUID = -3402542641853854668L;

	/** The splitter identifier constant. */
	private static final String STRING_SPLITTER = ",";

	/** The object type property for retrieving the full URI. */
	private static final String OBJECT_TYPE = "objectinstance";

	/** The semantic object type required for searching arguments. */
	private static final String TYPE = "rdf:type";

	/** The context. */
	private static final String CONTEXT = "context";

	/** The semantic object modified on, required for searching arguments. */
	private static final String MODIFIED_ON = "emf:modifiedOn";

	/** The object instance list. */
	private List<ObjectInstance> objectInstaceList;

	/** The available object actions. */
	private Map<Serializable, List<Action>> objectActions;

	/** The current case instance. */
	private CaseInstance caseInstance;

	/** The object panel placeholder, needed when retrieving actions. */
	private static final String CASE_OBJECT_DASHLET_PLACEHOLDER = "case-object-dashlet-actions";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		caseInstance = getDocumentContext().getInstance(CaseInstance.class);
	}

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
	public void executeDefaultFilter() {
		objectInstaceList = new ArrayList<ObjectInstance>();
		if (caseInstance == null) {
			notifyForLoadedData();
			return;
		}
		// filter(getSearchArguments());
		CaseInstance reloadedInstance = caseInstanceService.loadByDbId(caseInstance.getId());
		List<SectionInstance> sections = reloadedInstance.getSections();
		if (sections != null) {
			for (SectionInstance section : sections) {
				List<Instance> content = section.getContent();
				if ((content != null) && !content.isEmpty()) {
					objectInstaceList.addAll(retrieveObjectInstances(section.getContent()));
				}
			}
		}
		setObjectActions(getActionsForInstances(objectInstaceList, CASE_OBJECT_DASHLET_PLACEHOLDER));
		if (!objectInstaceList.isEmpty()) {
			Collections.sort(objectInstaceList, Collections.reverseOrder(new ObjectComparator()));
		}
		notifyForLoadedData();
	}

	/**
	 * Retrieve object instance from section.
	 * 
	 * @param content
	 *            list with objects from current section
	 * @return list with instance of type {@link ObjectInstance}
	 */
	private List<ObjectInstance> retrieveObjectInstances(List<Instance> content) {
		List<ObjectInstance> result = new ArrayList<ObjectInstance>();
		for (Instance instance : content) {
			if (instance instanceof ObjectInstance) {
				result.add((ObjectInstance) instance);
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<ObjectInstance> searchArguments) {
		// searchService.search(Instance.class, searchArguments);
		// objectInstaceList = searchArguments.getResult();
		// notifyForLoadedData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<ObjectInstance> getSearchArguments() {

		SearchArguments<ObjectInstance> searchArguments = null;
		Context<String, Object> context = new Context<>(1);

		// check for available case instance
		if (caseInstance == null) {
			objectInstaceList = new ArrayList<ObjectInstance>();
			return searchArguments;
		}

		context.put(SearchFilterProperties.USER_ID, userId);
		searchArguments = searchService.getFilter("listAllObjects", ObjectInstance.class, null);

		String caseInstanceUri = getCaseUri(caseInstance);
		String dataTypeUriList = getDomainObjectType();
		String[] splittedUris = dataTypeUriList.split(STRING_SPLITTER);
		// appending only domain object URI
		searchArguments.getArguments().put(TYPE, uriWrapper(splittedUris[0]));
		searchArguments.getArguments().put(CONTEXT, uriWrapper(caseInstanceUri));
		searchArguments.setSorter(new Sorter(MODIFIED_ON, Sorter.SORT_DESCENDING));

		return searchArguments;
	}

	/**
	 * Get data type definition.
	 * 
	 * @return combination of URIs as string
	 */
	protected String getDomainObjectType() {
		return dictionaryService.getDataTypeDefinition(OBJECT_TYPE).getFirstUri();
	}

	/**
	 * Get project URI.
	 * 
	 * @param instance
	 *            current context instance(Case instance)
	 * @return instance uri
	 */
	protected String getCaseUri(Instance instance) {
		return (String) instance.getId();
	}

	/**
	 * This method will construct URI holder needed for the semantic query NOTE: REG(STR(...)) if we
	 * use and return as String.
	 * 
	 * @param objectTypeUris
	 *            the object type URI
	 * @return wrapped URI
	 */
	protected ArrayList<String> uriWrapper(String objectTypeUris) {
		ArrayList<String> wrappedUriList = null;

		if (StringUtils.isNotNullOrEmpty(objectTypeUris)) {
			wrappedUriList = new ArrayList<String>(1);
			wrappedUriList.add(objectTypeUris);
		}
		return wrappedUriList;
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
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> dashletActionIds() {
		return CollectionUtils.EMPTY_SET;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return "myobjects-dashlet";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return null;
	}

	/**
	 * Getter for object instance list.
	 * 
	 * @return object instance list
	 */
	public List<ObjectInstance> getObjectInstaceList() {
		waitForDataToLoad();
		return objectInstaceList;
	}

	/**
	 * Setter for object instance list.
	 * 
	 * @param objectInstaceList
	 *            object instance list
	 */
	public void setObjectInstaceList(List<ObjectInstance> objectInstaceList) {
		this.objectInstaceList = objectInstaceList;
	}

	/**
	 * Setter for object actions.
	 * 
	 * @param objectActions
	 *            current object actions
	 */
	public void setObjectActions(Map<Serializable, List<Action>> objectActions) {
		this.objectActions = objectActions;
	}

	/**
	 * Getter for object actions based on current instance.
	 * 
	 * @param instance
	 *            current object instance
	 * @return list with actions for current object
	 */
	public List<Action> getObjectActions(Instance instance) {
		return objectActions.get(instance.getId());
	}
}
