package com.sirma.cmf.web.object.browser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sirma.cmf.web.CaseManagement;
import com.sirma.cmf.web.ProjectManagement;
import com.sirma.cmf.web.entity.dispatcher.InstanceContextInitializer;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.concurrent.TaskExecutor;
import com.sirma.itt.emf.instance.InstanceUtil;
import com.sirma.itt.emf.instance.dao.BatchEntityLoader;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.instance.model.RootInstanceContext;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * A controller for the Objects browser component.
 * 
 * @author svelikov
 */
@Path("/objects/browse")
@Consumes("application/json")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
@Stateless
public class ObjectsBrowserRestService extends EmfRestService {

	private static final String NAME = "name";
	private static final String OBJECTINSTANCE = "objectinstance";
	protected static final String CASEINSTANCE = "caseinstance";
	protected static final String PROJECTINSTANCE = "projectinstance";
	protected static final String TASKS = "tasks";
	protected static final String OBJECTS_SECTION = "objectsSection";
	protected static final String DOCUMENTS_SECTION = "documentsSection";

	/** The leaf instances. */
	private final Set<String> leafInstances = new HashSet<String>(Arrays.asList("taskinstance"));

	/** Pattern to match open and closing html 'a' tags. */
	private static final Pattern HTML_A_TAG = Pattern.compile("<(/?)(a[^>]*)>", Pattern.CANON_EQ);

	@Inject
	private InstanceContextInitializer instanceContextInitializer;

	/** The task executor. */
	@Inject
	private TaskExecutor taskExecutor;

	/** The link service. */
	@Inject
	private LinkService linkService;

	/** The case root provider. */
	@Inject
	@CaseManagement
	private javax.enterprise.inject.Instance<ObjectBrowserRootProvider> caseRootProvider;

	/** The project root provider. */
	@Inject
	@ProjectManagement
	private javax.enterprise.inject.Instance<ObjectBrowserRootProvider> projectRootProvider;

	/** The Constant ALLOWED_CASE_CHILDREN. */
	private static final Set<Class<?>> ALLOWED_CASE_CHILDREN = CollectionUtils
			.createLinkedHashSet(5);
	static {
		// FIXME: remove this ugly hack
		ALLOWED_CASE_CHILDREN.add(TaskInstance.class);
		ALLOWED_CASE_CHILDREN.add(SectionInstance.class);
		ALLOWED_CASE_CHILDREN.add(StandaloneTaskInstance.class);
		ALLOWED_CASE_CHILDREN.add(WorkflowInstanceContext.class);
	}

	/**
	 * Provides a json array with root instances to be displayed inside the object browser. Response
	 * is in format:
	 * 
	 * <pre>
	 * 	[
	 * 		{instanceId:'id1',header:'project 1 header', name:'project 1'},
	 *  	{instanceId:'id2',header:'project 2 header', name:'project 2'}
	 * ];
	 * </pre>
	 * 
	 * @param rootType
	 *            the root type ('projectinstance' for example)
	 * @param page
	 *            the page (passed by the extjs tree store)
	 * @param start
	 *            the start (passed by the extjs tree store)
	 * @param limit
	 *            the limit (passed by the extjs tree store)
	 * @return the roots
	 */
	@Path("roots")
	@GET
	public Response getRoots(@QueryParam("rootType") String rootType,
			@QueryParam("page") String page, @QueryParam("start") String start,
			@QueryParam("limit") String limit) {
		if (debug) {
			log.debug("EMFWeb: ObjectsBrowserRestService.roots rootType=" + rootType);
		}

		String actualRootType = PROJECTINSTANCE;
		if (StringUtils.isNotNullOrEmpty(rootType)) {
			actualRootType = rootType;
		}

		JSONArray data = new JSONArray();

		List<Instance> roots = new ArrayList<Instance>();

		if (!projectRootProvider.isUnsatisfied() && PROJECTINSTANCE.equals(actualRootType)) {
			roots = projectRootProvider.get().getRoots();
		} else {
			roots = caseRootProvider.get().getRoots();
		}

		for (Instance instance : roots) {
			// Skip deleted objects - CS-602
			if (!"DELETED".equals(instance.getProperties().get("status"))) {
				JSONObject item = new JSONObject();
				JsonUtil.addToJson(item, INSTANCE_ID, instance.getId());
				JsonUtil.addToJson(item, INSTANCE_TYPE, instance.getClass().getSimpleName()
						.toLowerCase());
				JsonUtil.addToJson(item, HEADER,
						instance.getProperties().get(DefaultProperties.HEADER_COMPACT));
				JsonUtil.addToJson(item, NAME, instance.getProperties()
						.get(DefaultProperties.TITLE));
				data.put(item);
			}
		}

		return Response.status(Response.Status.OK).entity(data.toString()).build();
	}

	/**
	 * Load the data for the objects browser.
	 * 
	 * @param node
	 *            the node is the path for the current instance
	 * @param type
	 *            the type of the node being requested
	 * @param currentInstanceId
	 *            the current instance id
	 * @param currentInstanceType
	 *            the current instance type
	 * @param allowSelection
	 *            If selection of nodes should be allowed.
	 * @param instanceFilter
	 *            The instance type filter to be applied.
	 * @param mimetypeFilter
	 *            If the result should be filtered by mimetype.
	 * @param clickableLinks
	 *            If the links inside the node text should be clickable.
	 * @param clickOpenWindow
	 *            If the links inside the node text should open in new browser tab
	 * @return Response which contains the built data store json
	 */
	@Path("tree")
	@GET
	public Response tree(@QueryParam("node") String node, @QueryParam("type") String type,
			@QueryParam("currId") String currentInstanceId,
			@QueryParam("currType") String currentInstanceType,
			@QueryParam("allowSelection") boolean allowSelection,
			@QueryParam("instanceFilter") String instanceFilter,
			@QueryParam("mimetypeFilter") String mimetypeFilter,
			@QueryParam("clickableLinks") boolean clickableLinks,
			@QueryParam("clickOpenWindow") boolean clickOpenWindow) {
		if (debug) {
			log.debug("EMFWeb: ObjectsBrowserRestService.load node=" + node + ", type=" + type
					+ ", currentInstanceId=" + currentInstanceId + ", currentInstanceType="
					+ currentInstanceType + ", allowSelection=" + allowSelection
					+ ", instanceFilter=" + instanceFilter + ", mimetypeFilter=" + mimetypeFilter
					+ ", clickableLinks=" + clickableLinks + ", clickOpenWindow=" + clickOpenWindow);
		}

		// check required arguments
		if (StringUtils.isNullOrEmpty(node) || StringUtils.isNullOrEmpty(type)) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Missing required arguments!").build();
		}

		String rootInstanceId = extractInstanceId(node);
		// load instance that should be expanded later
		Instance rootInstance = fetchInstance(rootInstanceId, type);

		if (rootInstance == null) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Can't find instance type=" + type + " with id=" + rootInstanceId)
					.build();
		}

		// load the children of the current node being expanded
		List<Instance> loadedInstances = fetchInstanceChildren(rootInstance);

		// group instance by type and purpose for section instances
		Map<String, List<Instance>> grouped = groupByType(loadedInstances);

		String currentNodePath = buildPathToCurrentNode(currentInstanceId, currentInstanceType,
				rootInstance);

		// buid JSON response
		JSONArray nodes = buildData(grouped, rootInstance, node, shouldBeGrouped(rootInstance),
				currentNodePath, allowSelection, clickableLinks, clickOpenWindow, instanceFilter,
				mimetypeFilter);

		String data = nodes.toString();

		return Response.status(Response.Status.OK).entity(data).build();
	}

	/**
	 * Builds the path to current node. The path contains instance id's separated with '/'. If given
	 * node is not instance but a group, then its name is used instead of id because they don't have
	 * one.
	 * 
	 * @param currentInstanceId
	 *            the current instance id
	 * @param currentInstanceType
	 *            the current instance type
	 * @param rootInstance
	 *            the root instance
	 * @return the string
	 */
	private String buildPathToCurrentNode(String currentInstanceId, String currentInstanceType,
			Instance rootInstance) {
		Instance currentInstanceNode = fetchInstance(currentInstanceId, currentInstanceType);
		if (currentInstanceNode == null) {
			return "";
		}
		instanceContextInitializer.restoreHierarchy(currentInstanceNode, rootInstance);
		List<Instance> parentInstances = InstanceUtil.getParentPath(currentInstanceNode, true);

		StringBuilder path = new StringBuilder();
		for (Instance parent : parentInstances) {
			String instanceType = parent.getClass().getSimpleName().toLowerCase();
			if (parent instanceof RootInstanceContext) {
				path.append(parent.getId()).append("/");
			} else if (parent instanceof CaseInstance) {
				path.append(instanceType).append("/").append(parent.getId()).append("/");
			} else if (parent instanceof SectionInstance) {
				String purpose = ((SectionInstance) parent).getPurpose();
				purpose = StringUtils.isNullOrEmpty(purpose) ? DOCUMENTS_SECTION : purpose;
				path.append(purpose).append("/").append(parent.getId()).append("/");
			} else if (parent instanceof DocumentInstance) {
				path.append(parent.getId()).append("/");
			} else {
				if (OBJECTINSTANCE.equals(instanceType)) {
					path.append(parent.getId()).append("/");
				} else if (parent instanceof WorkflowInstanceContext) {
					path.append(instanceType).append("/").append(parent.getId()).append("/");
				} else if (parent instanceof AbstractTaskInstance) {
					path.append(TASKS).append("/").append(parent.getId()).append("/");
				}
			}
		}
		if (path.length() > 0) {
			path = path.delete(path.length() - 1, path.length());
		}
		return path.toString();
	}

	/**
	 * Gets the root instance.
	 * 
	 * @param currentInstance
	 *            the current instance
	 * @return the root instance
	 */
	protected Instance getRootInstance(Instance currentInstance) {
		if (currentInstance instanceof RootInstanceContext) {
			return currentInstance;
		}
		Instance rootInstance = InstanceUtil.getRootInstance(currentInstance, true);
		return rootInstance;
	}

	/**
	 * Extract instance id.
	 * 
	 * @param node
	 *            the node
	 * @return the string
	 */
	protected String extractInstanceId(String node) {
		if (StringUtils.isNotNullOrEmpty(node)) {
			return node.substring(node.lastIndexOf("/") + 1);
		}
		return "";
	}

	/**
	 * If children should be grouped under common group node or not.
	 * 
	 * @param currentInstanceNode
	 *            the current instance node
	 * @return true, if is section
	 */
	protected boolean shouldBeGrouped(Instance currentInstanceNode) {
		return (currentInstanceNode instanceof RootInstanceContext)
				|| (currentInstanceNode instanceof CaseInstance);
	}

	/**
	 * Group instances by type.
	 * 
	 * @param loadedInstances
	 *            the loaded instances
	 * @return the map
	 */
	protected Map<String, List<Instance>> groupByType(List<Instance> loadedInstances) {
		Map<String, List<Instance>> grouped = new HashMap<String, List<Instance>>();
		if (loadedInstances != null) {
			for (Instance instance : loadedInstances) {
				if (instance instanceof SectionInstance) {
					mapSectionInstance((SectionInstance) instance, grouped);
				} else if (instance instanceof AbstractTaskInstance) {
					mapTaskInstance((AbstractTaskInstance) instance, grouped);
				} else {
					mapInstance(instance, grouped);
				}
			}
		}
		return grouped;
	}

	/**
	 * Map an instance.
	 * 
	 * @param instance
	 *            the instance
	 * @param grouped
	 *            the grouped
	 */
	protected void mapInstance(Instance instance, Map<String, List<Instance>> grouped) {
		String key = instance.getClass().getSimpleName().toLowerCase();
		createMapping(key, grouped);
		grouped.get(key).add(instance);
	}

	/**
	 * Map section instance.
	 * 
	 * @param instance
	 *            the instance
	 * @param grouped
	 *            the grouped
	 */
	protected void mapSectionInstance(SectionInstance instance, Map<String, List<Instance>> grouped) {
		String purpose = instance.getPurpose();
		if (StringUtils.isNullOrEmpty(purpose) || DOCUMENTS_SECTION.equals(purpose)) {
			createMapping(DOCUMENTS_SECTION, grouped);
			grouped.get(DOCUMENTS_SECTION).add(instance);
		} else if (OBJECTS_SECTION.equals(purpose)) {
			createMapping(OBJECTS_SECTION, grouped);
			grouped.get(OBJECTS_SECTION).add(instance);
		}
	}

	/**
	 * Map task instance.
	 * 
	 * @param instance
	 *            the instance
	 * @param grouped
	 *            the grouped
	 */
	protected void mapTaskInstance(AbstractTaskInstance instance,
			Map<String, List<Instance>> grouped) {
		createMapping(TASKS, grouped);
		grouped.get(TASKS).add(instance);
	}

	/**
	 * Creates mapping for given key if it doesn't exists.
	 * 
	 * @param key
	 *            the key
	 * @param grouped
	 *            the grouped
	 */
	protected void createMapping(String key, Map<String, List<Instance>> grouped) {
		if (StringUtils.isNullOrEmpty(key)) {
			return;
		}
		if (grouped.get(key) == null) {
			grouped.put(key, new ArrayList<Instance>());
		}
	}

	/**
	 * Fetch instance children.
	 * 
	 * @param currentInstanceNode
	 *            the current instance node
	 * @return the list
	 */
	protected List<Instance> fetchInstanceChildren(Instance currentInstanceNode) {
		// Class<? extends Instance> instanceClass = getInstanceClass(instanceFilter);
		List<LinkReference> linkReferences = linkService.getLinks(
				currentInstanceNode.toReference(), LinkConstants.PARENT_TO_CHILD);
		if (linkReferences != null) {
			List<InstanceReference> instanceReferences = new ArrayList<InstanceReference>(
					linkReferences.size());

			for (LinkReference linkReference : linkReferences) {
				if (linkReference.getFrom().getReferenceType().getJavaClass()
						.equals(CaseInstance.class)
						&& !ALLOWED_CASE_CHILDREN.contains(linkReference.getTo().getReferenceType()
								.getJavaClass())) {
					continue;
				}
				instanceReferences.add(linkReference.getTo());
			}
			List<Instance> loadedInstances = BatchEntityLoader.loadFromReferences(
					instanceReferences, serviceRegister, taskExecutor);
			return loadedInstances;
		}
		return new ArrayList<Instance>();
	}

	/**
	 * Builds the data store to be returned to the client.
	 * 
	 * @param grouped
	 *            the grouped instances
	 * @param root
	 *            the root instance for which is the request
	 * @param node
	 *            the node id represented as path
	 * @param renderGrouped
	 *            if children of the node should be rendered under common root or immediately under
	 *            the node
	 * @param currentNodePath
	 *            the current node path
	 * @param allowSelection
	 *            If selection of nodes should be allowed.
	 * @param clickableLinks
	 *            If the links inside the node text should be clickable.
	 * @param clickOpenWindow
	 *            If the links inside the node text should open in new browser tab.
	 * @param instanceFilter
	 *            the instance filter
	 * @param mimetypeFilter
	 *            the mimetype filter
	 * @return the jSON object representing the data store
	 */
	protected JSONArray buildData(Map<String, List<Instance>> grouped, Instance root, String node,
			boolean renderGrouped, String currentNodePath, boolean allowSelection,
			boolean clickableLinks, boolean clickOpenWindow, String instanceFilter,
			String mimetypeFilter) {
		JSONArray children = new JSONArray();

		if (grouped != null) {
			for (String groupId : grouped.keySet()) {
				// except for sections, all instances are grouped under group nodes
				// TODO: for objects and documents we also may have nesting but not grouped
				// TODO: for workflows we also may have nesting but not grouped
				if (renderGrouped) {
					String accumulatedWithGroupId = node + "/" + groupId;
					JSONObject group = new JSONObject();
					JsonUtil.addToJson(group, "id", accumulatedWithGroupId);
					JsonUtil.addToJson(group, "text", getGroupLabel(groupId));
					JsonUtil.addToJson(group, "cls", groupId + "-group");
					JsonUtil.addToJson(group, "leaf", Boolean.FALSE);
					JsonUtil.addToJson(group, "cnPath", currentNodePath);
					JSONArray groupChildren = new JSONArray();
					JsonUtil.addToJson(group, "children", groupChildren);
					children.put(group);
					addChildren(grouped, groupId, accumulatedWithGroupId, groupChildren,
							currentNodePath, allowSelection, clickableLinks, clickOpenWindow,
							instanceFilter, mimetypeFilter);
				} else {
					String accumulatedId = node;
					addChildren(grouped, groupId, accumulatedId, children, currentNodePath,
							allowSelection, clickableLinks, clickOpenWindow, instanceFilter,
							mimetypeFilter);
				}
			}
		}

		return children;
	}

	/**
	 * Adds children data in the data store json for any group.
	 * 
	 * @param grouped
	 *            the grouped instances
	 * @param groupId
	 *            the group id is the key under which the children for the group are mapped
	 * @param accumulatedId
	 *            the accumulated id is the current instance path with appended group id
	 * @param groupChildren
	 *            the group children is the json array inside which the children data objects should
	 *            be added
	 * @param currentNodePath
	 *            the current node path
	 * @param allowSelection
	 *            If selection of node should be allowed.
	 * @param clickableLinks
	 *            If the links inside the node text should be clickable.
	 * @param clickOpenWindow
	 *            If the links inside the node text should open in new browser tab.
	 * @param instanceFilter
	 *            the instance filter
	 * @param mimetypeFilter
	 *            the mimetype filter
	 */
	private void addChildren(Map<String, List<Instance>> grouped, String groupId,
			String accumulatedId, JSONArray groupChildren, String currentNodePath,
			boolean allowSelection, boolean clickableLinks, boolean clickOpenWindow,
			String instanceFilter, String mimetypeFilter) {
		List<Instance> instanceGroup = grouped.get(groupId);
		for (Instance instance : instanceGroup) {
			boolean isLeaf = isLeaf(instance);
			// omit the instance if it is leaf and there are some filters provided and the instance
			// is not selectable according to the fileters
			boolean isSelectable = isSelectable(instanceFilter, mimetypeFilter, instance);
			boolean hasFilter = StringUtils.isNotNullOrEmpty(instanceFilter)
					|| StringUtils.isNotNullOrEmpty(mimetypeFilter);
			if (isLeaf && (hasFilter && !isSelectable)) {
				continue;
			}
			JSONObject child = new JSONObject();
			String accumulatedWithGroupId = accumulatedId + "/" + instance.getId();
			JsonUtil.addToJson(child, "id", accumulatedWithGroupId);
			JsonUtil.addToJson(child, "dbId", instance.getId());
			String textValue = getNodeText(
					instance.getProperties().get(DefaultProperties.HEADER_COMPACT), clickableLinks,
					clickOpenWindow);
			JsonUtil.addToJson(child, "text", textValue);
			JsonUtil.addToJson(child, "cls", instance.getClass().getSimpleName().toLowerCase());
			// leaf instances are those that doesn't allow children
			JsonUtil.addToJson(child, "leaf", Boolean.valueOf(isLeaf));
			JsonUtil.addToJson(child, "cnPath", currentNodePath);
			// if selection is allowed and no filters are provided, we allow selection for all
			// instances
			if (allowSelection && !hasFilter) {
				JsonUtil.addToJson(child, "checked", Boolean.FALSE);
			}
			// if selection is allowed and a filter is provided, then we allow selection only for
			// selectable instances
			else if (allowSelection && hasFilter && isSelectable) {
				JsonUtil.addToJson(child, "checked", Boolean.FALSE);
			}
			groupChildren.put(child);
		}
	}

	/**
	 * Checks if given instance node can be selected in the ui. If mimetypeFilter is provided, we
	 * assume that the instance type should be 'documentinstance' and filter only those that have
	 * requested mimetype. If mimetypeFilter is not provided we simly check whether the
	 * instanceFilter matches the actual type of the current instance object.
	 * 
	 * @param instanceFilter
	 *            the instance filter
	 * @param mimetypeFilter
	 *            the mimetype filter
	 * @param instance
	 *            the instance
	 * @return true, if is selectable
	 */
	protected boolean isSelectable(String instanceFilter, String mimetypeFilter, Instance instance) {
		if (instance == null) {
			return false;
		}
		// if mimetype is provided we get it with precedence before the instanceFilter
		String actualInstanceFilter = null;
		if (StringUtils.isNotNullOrEmpty(mimetypeFilter)) {
			actualInstanceFilter = DocumentInstance.class.getSimpleName().toLowerCase();
		} else if (StringUtils.isNotNullOrEmpty(instanceFilter)) {
			actualInstanceFilter = instanceFilter;
		}
		String instanceType = instance.getClass().getSimpleName().toLowerCase();
		// current instance should match the requested type and mimetype if provided
		boolean isSameType = instanceType.equals(actualInstanceFilter);

		// if mimetypeFilter is present, we should check both the mimetype and the instance type
		if (StringUtils.isNotNullOrEmpty(mimetypeFilter)) {
			String mimetype = (String) instance.getProperties().get(DocumentProperties.MIMETYPE);
			boolean isSameMimetype = false;
			if (StringUtils.isNotNullOrEmpty(mimetype) && mimetype.contains(mimetypeFilter)) {
				isSameMimetype = true;
			}
			if (isSameType && isSameMimetype) {
				return true;
			}
		} else if (isSameType) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if a node is leaf.
	 * 
	 * @param instance
	 *            the instance
	 * @return true, if is leaf
	 */
	private boolean isLeaf(Instance instance) {
		return leafInstances.contains(instance.getClass().getSimpleName().toLowerCase());
	}

	/**
	 * Gets the node text modifying the text according to configuration provided.
	 * 
	 * @param value
	 *            the value
	 * @param clickableLinks
	 *            If the links inside the node text should be clickable.
	 * @param clickOpenWindow
	 *            If the links inside the node text should open in new browser tab.
	 * @return the node text
	 */
	private String getNodeText(Serializable value, boolean clickableLinks, boolean clickOpenWindow) {
		String result = (String) value;
		if (clickableLinks) {
			// if a link should be opened in a new browser tab, we append a target=blank attribute
			// to the link
			if (clickOpenWindow) {
				return result.replaceAll("<a ", "<a target=\"_blank\" ");
			}
			return result;
		}

		// if no clickbale links is required, we replace all links with span tags
		Matcher matcher = HTML_A_TAG.matcher(result);
		return matcher.replaceAll("<$1span>");
	}

	/**
	 * Gets a group label from bundle by key.
	 * 
	 * @param key
	 *            the key
	 * @return the group label
	 */
	private String getGroupLabel(String key) {
		String label = labelProvider.getValue("objects.explorer.group." + key);
		return label;
	}

}
