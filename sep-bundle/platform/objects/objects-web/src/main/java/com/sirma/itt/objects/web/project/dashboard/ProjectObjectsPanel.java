package com.sirma.itt.objects.web.project.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.scheduler.SchedulerEntry;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.Sorter;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * <b>ProjectObjectsPanel</b> manage functionality for dashlet, located in project dashboard. The
 * content is represented as object records, actions and filters.
 * 
 * @author cdimitrov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectObjectsPanel extends
		DashboardPanelActionBase<ObjectInstance, SearchArguments<ObjectInstance>> implements
		Serializable, DashboardPanelController {

	/** The constant serial version identifier. */
	private static final long serialVersionUID = -3402542641853854668L;

	/** The constant for URI splitter. */
	private static final String STRING_SPLITTER = ",";

	/** The constant for object type. */
	private static final String OBJECT_TYPE = "objectinstance";

	/** Semantic property(current type) that will be used in the query for retrieving objects. */
	private static final String TYPE = "rdf:type";

	/** The context needed for the sematic query that will hold the object URI. */
	private static final String CONTEXT = "context";

	/** The modified on constant. Used for sorting of the result */
	private static final String MODIFIED_ON = "emf:modifiedOn";

	/** Constant that will holds actions for the dashlet. */
	private static final Set<String> dashletActions = new HashSet<String>(Arrays.asList(
			ActionTypeConstants.CREATE_OBJECT, ActionTypeConstants.ATTACH_OBJECT));

	/** The list with object instance that will be displayed into the dashlet. */
	private List<ObjectInstance> objectInstaceList;

	/** The current project instance retrieved from the document context. */
	private Instance projectInstance;

	/** The available object actions. */
	private Map<Serializable, List<Action>> objectActions;

	/** The placeholder for object actions. */
	private static final String PROJECT_OBJECT_ACTIONS_PLACEHOLDER = "project-object-actions";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeForAsynchronousInvocation() {
		projectInstance = getDocumentContext().getCurrentInstance();
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
	protected void beforeAsyncLoading(SchedulerEntry entry) {
		entry.getConfiguration().setInSameTransaction(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		SearchArguments<ObjectInstance> arguments = getSearchArguments();
		filter(arguments);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<ObjectInstance> searchArguments) {
		objectActions = new HashMap<Serializable, List<Action>>();
		if (searchArguments != null) {
			searchService.search(Instance.class, searchArguments);
			objectInstaceList = searchArguments.getResult();
			setObjectActions(getActionsForInstances(objectInstaceList,
					PROJECT_OBJECT_ACTIONS_PLACEHOLDER));
		}
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
		Context<String, Object> context = new Context<>(1);

		// check for available project instance
		if (projectInstance == null) {
			objectInstaceList = new ArrayList<ObjectInstance>();
			return searchArguments;
		}

		context.put(SearchFilterProperties.USER_ID, userId);
		searchArguments = searchService.getFilter("listAllObjects", ObjectInstance.class, null);

		String projectInstanceUri = getProjectUri(projectInstance);
		String dataTypeUriList = getDomainObjectType();
		String[] splittedUris = dataTypeUriList.split(STRING_SPLITTER);
		// appending only domain object URI
		searchArguments.getArguments().put(TYPE, uriWrapper(splittedUris[0]));
		searchArguments.getArguments().put(CONTEXT, uriWrapper(projectInstanceUri));
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
	 *            current context instance(Project instance)
	 * @return instance uri
	 */
	protected String getProjectUri(Instance instance) {
		return (String) instance.getId();
	}

	/**
	 * This method will construct URI holder needed for the semantic query<br>
	 * <b>NOTE:</b> REG(STR(...)) if we use and return as String.
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
		return dashletActions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return "project-objects-dashlet";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		if (projectInstance == null) {
			// if this is called before initialization
			projectInstance = getDocumentContext().getCurrentInstance();
		}
		return projectInstance;
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
