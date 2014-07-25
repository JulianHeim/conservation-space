package com.sirma.itt.objects.web.userdashboard.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.Sorter;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * <b>MyObjectsPanel</b> manage functionality for dashlet, located in personal/user dashboard. The
 * content is represented as object records, actions and filters.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "UserDashboard")
@ViewAccessScoped
public class MyObjectsPanel extends
		DashboardPanelActionBase<ObjectInstance, SearchArguments<ObjectInstance>> implements
		Serializable, DashboardPanelController {

	/** The constant for serial version identifier. */
	private static final long serialVersionUID = -3402542641853854668L;

	/** The list with object instance that will be displayed into the dashlet. */
	private List<ObjectInstance> objectInstaceList;

	/** The constant for URI splitter. */
	private static final String STRING_SPLITTER = ",";

	/** The constant for object type. */
	private static final String OBJECT_TYPE = "objectinstance";

	/** Semantic property(current type) that will be used in the query for retrieving objects. */
	private static final String TYPE = "rdf:type";

	/**
	 * Semantic property(the user that is created the object) that will be used in the query for
	 * retrieving objects.
	 */
	private static final String CREATED_BY = "emf:createdBy";

	/** The modified on constant. Used for sorting of the result */
	private static final String MODIFIED_ON = "emf:modifiedOn";

	/** The available object actions. */
	private Map<Serializable, List<Action>> objectActions;

	/** The constant that specify object placeholder. */
	private static final String DASHLET_ACTION_OBJECTS_PLACEHOLDER = "user-dashboard-objects-panel";

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
		filter(getSearchArguments());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<ObjectInstance> searchArguments) {
		searchService.search(Instance.class, searchArguments);
		objectInstaceList = searchArguments.getResult();
		setObjectActions(getActionsForInstances(objectInstaceList, DASHLET_ACTION_OBJECTS_PLACEHOLDER));
		notifyForLoadedData();
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
		searchArguments = searchService.getFilter("listAllObjects", ObjectInstance.class, null);
		String dataTypeUriList = getDomainObjectType();
		String[] splittedUris = dataTypeUriList.split(STRING_SPLITTER);
		// appending only domain object URI
		searchArguments.getArguments().put(TYPE, uriWrapper(splittedUris[0]));
		searchArguments.getArguments().put(CREATED_BY, userURI);
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
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		return null;
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
	 * Setter for all actions that are supported from the result of objects.
	 * 
	 * @param objectActions
	 *            supported object actions
	 */
	public void setObjectActions(Map<Serializable, List<Action>> objectActions) {
		this.objectActions = objectActions;
	}

	/**
	 * Getter for actions supported from specific object.
	 * 
	 * @param instance
	 *            current object instance
	 * @return supported object actions
	 */
	public List<Action> getObjectActions(Instance instance) {
		return objectActions.get(instance.getId());
	}

}
