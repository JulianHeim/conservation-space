package com.sirma.itt.objects.web.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.cmf.web.instance.AttachInstanceAction;
import com.sirma.cmf.web.rest.util.SearchResultTransformer;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.annotation.Proxy;
import com.sirma.itt.emf.codelist.CodelistService;
import com.sirma.itt.emf.concurrent.TaskExecutor;
import com.sirma.itt.emf.converter.TypeConverterUtil;
import com.sirma.itt.emf.definition.DefinitionUtil;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinitionModel;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.domain.DisplayType;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.instance.dao.BatchEntityLoader;
import com.sirma.itt.emf.instance.dao.InstanceDao;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.instance.dao.ServiceRegister;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.PropertiesService;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.security.SecurityModel;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;
import com.sirma.itt.objects.services.ObjectService;

/**
 * Rest service for manipulating and managing objects.
 * <p>
 * REVIEW: the link itself implies it's a rest service. No need for the '-rest' in the url
 */
@Secure
@Path("/object-rest")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
public class ObjectRestService extends EmfRestService {

	private static final String MULTI = "multi";
	private static final String NAME = "name";
	private static final String VALUE = "value";
	private static final String LINK_ID = "linkId";
	private static final String RDF_TYPE = "rdf:type";

	/** The Constant SECTION_PURPOSE. */
	private static final String SECTION_PURPOSE = "objectsSection";

	@Inject
	@Proxy
	private InstanceService<Instance, DefinitionModel> instanceService;

	@Inject
	private TaskExecutor taskExecutor;

	@Inject
	private PropertiesService propertiesService;

	@Inject
	private CodelistService codelistService;

	@Inject
	private SearchResultTransformer searchResultTransformer;

	@Inject
	private AuthorityService authorityService;

	@Inject
	private ObjectService objectService;

	@Inject
	private SearchService searchService;

	@Inject
	private AttachInstanceAction attachInstanceAction;

	@Inject
	private ServiceRegister register;

	/**
	 * Checks if instances exist in the database.
	 * 
	 * @param data
	 *            JSON array containing a list of objects representing the instances that need to be
	 *            checked - [ { instanceId: "id", instanceType: "type" } ]
	 * @return Returns a JSON object with a single property 'values' which is an array of the same
	 *         format as the passed in to the service, but containing only information for the
	 *         deleted instances.
	 */
	@POST
	@Path("/exists")
	public String instancesExist(String data) {
		JSONObject result = new JSONObject();
		try {
			JSONArray typeAndIdPairs = new JSONArray(data);
			Map<String, String> instanceIdToTypeMapping = new HashMap<>();
			int length = typeAndIdPairs.length();
			for (int i = 0; i < length; i++) {
				JSONObject pair = typeAndIdPairs.getJSONObject(i);
				String id = JsonUtil.getStringValue(pair, INSTANCE_ID);
				String type = JsonUtil.getStringValue(pair, INSTANCE_TYPE);

				instanceIdToTypeMapping.put(id, type);
			}

			List<Instance> existingInstances = checkIfInstancesExist(new LinkedList<>(
					instanceIdToTypeMapping.keySet()));

			for (Instance instance : existingInstances) {
				instanceIdToTypeMapping.remove(instance.getId());
			}

			JSONArray values = new JSONArray();
			for (Map.Entry<String, String> entry : instanceIdToTypeMapping.entrySet()) {
				JSONObject value = new JSONObject();
				JsonUtil.addToJson(value, INSTANCE_ID, entry.getKey());
				JsonUtil.addToJson(value, INSTANCE_TYPE, entry.getValue());
				values.put(value);
			}

			JsonUtil.addToJson(result, "values", values);
		} catch (JSONException e) {
			log.warn("", e);
		}
		return result.toString();
	}

	/**
	 * Given a list of instance identifiers checks if they exist in the database.
	 * 
	 * @param instancesIdList
	 *            List of instance identifiers.
	 * @return List of existing instances.
	 */
	private List<Instance> checkIfInstancesExist(List<String> instancesIdList) {
		/*
		 * FIXME This should be a service method!!!
		 */

		Context<String, Object> context = new Context<String, Object>();
		context.put("URIS", instancesIdList);

		// TODO replace filter name with constant when it is implemented
		SearchArguments<Instance> filter = searchService.getFilter("CHECK_EXISTING_INSTANCE",
				Instance.class, context);
		searchService.search(Instance.class, filter);
		return filter.getResult();
	}

	/**
	 * Gets the type class.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param type
	 *            the type
	 * @return the type class
	 */
	@SuppressWarnings("unchecked")
	private <T extends Instance> Class<T> getTypeClass(String type) {
		if (type == null) {
			return null;
		}
		DataTypeDefinition selectedInstanceType = dictionaryService.getDataTypeDefinition(type);
		return (Class<T>) selectedInstanceType.getJavaClass();
	}

	/**
	 * Loads all possible relationships of type literal (properties) defined in the ontology,
	 * combined with ones from the object definition.
	 * 
	 * @param id
	 *            Instance identifier.
	 * @param type
	 *            The type of the object as defined in types.xml (caseinstance, objectinstance,
	 *            etc.)
	 * @param definitionId
	 *            The definition id
	 * @return A combined list of semantic and definition literals/properties as a json array.
	 */
	@GET
	@Path("/literals")
	public String loadLiteralsForType(@QueryParam("type") String type, @QueryParam("id") String id,
			@QueryParam("definitionId") String definitionId) {
		// fields: [ 'name', 'value', 'plainText', 'editType', 'linkId', 'cls' ]
		Instance instance = fetchInstance(id, type);

		DefinitionModel instanceDefinition;
		if (instance != null) {
			String domainClass = typeConverter.convert(String.class,
					instance.getProperties().get(RDF_TYPE));
			if (org.apache.commons.lang.StringUtils.isBlank(domainClass)) {
				DataTypeDefinition dataTypeDefinition = dictionaryService
						.getDataTypeDefinition(type);
				domainClass = dataTypeDefinition.getFirstUri();
			}

			// TODO: at some point we should get the properties from a single place
			// i.e semantic db
			instanceDefinition = dictionaryService.getInstanceDefinition(instance);

		} else {
			Class<Instance> typeClass = getTypeClass(type);
			if (typeClass == null) {
				return null;
			}
			InstanceService<Instance, DefinitionModel> instanceService = serviceRegister
					.getInstanceService(typeClass);

			// for document instance for now the default instance creation is more
			// complicated...
			if (DocumentInstance.class.equals(typeClass)) {
				DocumentInstance documentInstance = new DocumentInstance();
				documentInstance.setIdentifier(definitionId);
				documentInstance.setStandalone(true);
				documentInstance.setRevision(0L);
				instanceDefinition = dictionaryService.getInstanceDefinition(documentInstance);
			} else {
				instanceDefinition = dictionaryService.getDefinition(
						instanceService.getInstanceDefinitionClass(), definitionId);
			}

			instance = instanceService.createInstance(instanceDefinition, null, new Operation(""));

			if (instance instanceof DocumentInstance) {
				((DocumentInstance) instance).setStandalone(true);
			}
		}

		JSONArray result = new JSONArray();

		Map<String, ?> forRest = propertiesService.convertForRest(instance, instanceDefinition);

		// get properties defined in the base object definition
		List<PropertyDefinition> fields = instanceDefinition.getFields();
		for (PropertyDefinition propertyDefinition : fields) {
			addPropertyFromDefinitionToResult(result, propertyDefinition, forRest);
		}

		// if it's a region definition get the regions and the properties
		// defined in each region
		if (instanceDefinition instanceof RegionDefinitionModel) {
			List<RegionDefinition> regions = ((RegionDefinitionModel) instanceDefinition)
					.getRegions();
			for (RegionDefinition region : regions) {
				fields = region.getFields();
				for (PropertyDefinition propertyDefinition : fields) {
					addPropertyFromDefinitionToResult(result, propertyDefinition, forRest);
				}
			}
		}
		// Semantic properties will be included later on
		// List<PropertyInstance> properties =
		// semanticDefinitionService.getProperties(domainClass);
		// for (PropertyInstance propertyInstance : properties) {
		// addPropertyFromOntologyToResult(result, propertyInstance, instance);
		// }
		return result.toString();
	}

	/**
	 * Saves a property of an instance.
	 * 
	 * @param instanceId
	 *            Instance identifier.
	 * @param instanceType
	 *            Instance type.
	 * @param data
	 *            Property information such as name and value.
	 */
	@POST
	@Path("/literals")
	public void saveLiteral(@QueryParam(INSTANCE_ID) String instanceId,
			@QueryParam(INSTANCE_TYPE) String instanceType, String data) {
		if (org.apache.commons.lang.StringUtils.isBlank(data)) {
			return;
		}

		Instance instance = fetchInstance(instanceId, instanceType);
		if (instance == null) {
			return;
		}

		InstanceDao<Instance> instanceDao = register.getInstanceDao(instance);
		boolean addedValues = false;
		try {
			// could be json array when adding multiple values at the same time
			if (data.startsWith("[")) {
				JSONArray array = new JSONArray(data);
				for (int i = 0; i < array.length(); i++) {
					JSONObject object = array.getJSONObject(i);
					addedValues |= addLiteralToInstance(object, instance);
				}
			} else {
				JSONObject object = new JSONObject(data);
				addedValues = addLiteralToInstance(object, instance);
			}

			if (addedValues) {
				instanceDao.saveProperties(instance, true);
				// instanceService.save(instance, new Operation(ActionTypeConstants.EDIT_DETAILS));
			}
		} catch (JSONException e) {
			log.warn("ObjectsWeb: Failed to parse request to add literal: " + data + " due to "
					+ e.getMessage());
			log.warn("", e);
		}
	}

	/**
	 * Adds the literal to instance.
	 * 
	 * @param data
	 *            the object
	 * @param instance
	 *            the instance
	 * @return true, if successful
	 */
	@SuppressWarnings("unchecked")
	private boolean addLiteralToInstance(JSONObject data, Instance instance) {
		String propertyName = JsonUtil.getStringValue(data, LINK_ID);
		if (propertyName == null) {
			return false;
		}
		
		// convert the property name to uri if necessary
		String literal = propertyName.replace('_', ':');
		String value = JsonUtil.getStringValue(data, VALUE);
		String oldValue = JsonUtil.getStringValue(data, "oldValue");
		boolean isMultiValued = JsonUtil.getBooleanValue(data, "isMultiValued");

		if (value == null && isMultiValued) {
			return false;
		}

		if (isMultiValued) {
			Serializable currentValue = instance.getProperties().get(literal);
			Collection<Serializable> collection = null;
			// check if the current values is not a single value
			// or does not exists
			if (currentValue instanceof Collection) {
				collection = (Collection<Serializable>) currentValue;
			} else {
				collection = new LinkedList<>();
				if (currentValue != null) {
					collection.add(currentValue);
				}
			}

			if (!"".equals(oldValue)) {
				collection.remove(oldValue);
			}
			collection.add(value);

			instance.getProperties().put(literal, (Serializable) collection);
		} else {
			instance.getProperties().put(literal, value);
		}
		return true;
	}

	/**
	 * Delete selected fact value.
	 * 
	 * @param instanceId
	 *            Instance identifier.
	 * @param instanceType
	 *            Instance type.
	 * @param data
	 *            Property information such as name and value.
	 */
	@POST
	@Path("/remove")
	public void removeFactValue(@QueryParam(INSTANCE_ID) String instanceId,
			@QueryParam(INSTANCE_TYPE) String instanceType, String data) {
		if (org.apache.commons.lang.StringUtils.isBlank(data)) {
			return;
		}
		Instance instance = fetchInstance(instanceId, instanceType);
		boolean removedValues = false;
		try {

			// could be json array when removing multiple values at the same time
			if (data.startsWith("[")) {
				JSONArray array = new JSONArray(data);
				for (int i = 0; i < array.length(); i++) {
					JSONObject object = array.getJSONObject(i);
					removedValues |= removeLiteralFromInstance(object, instance);
				}
			} else {
				JSONObject object = new JSONObject(data);
				removedValues = removeLiteralFromInstance(object, instance);
			}

			if (removedValues) {
				instanceService.save(instance, new Operation(""));
			}
		} catch (JSONException e) {
			log.warn("", e);
		}
	}

	/**
	 * Remove the literal from instance.
	 * 
	 * @param data
	 *            the object
	 * @param instance
	 *            the instance
	 * @return true, if successful
	 */
	private boolean removeLiteralFromInstance(JSONObject data, Instance instance) {

		String literal = JsonUtil.getStringValue(data, LINK_ID).replace('_', ':');
		String value = JsonUtil.getStringValue(data, VALUE);

		List multiValueList = null;

		try {
			multiValueList = (List) instance.getProperties().get(literal);
			if (multiValueList == null) {
				multiValueList = new ArrayList();
			}
			multiValueList.remove(value);
		} catch (ClassCastException e) {
			log.error("", e);
		}

		if (multiValueList == null) {
			instance.getProperties().put(literal, value);
		} else {
			instance.getProperties().put(literal, (Serializable) multiValueList);
		}

		return true;
	}

	/**
	 * Adds a property to a json array.
	 * 
	 * @param result
	 *            JSON array to add to.
	 * @param definition
	 *            Property definition.
	 * @param properties
	 *            Instance containing the property.
	 * @return true if the property was added.
	 */
	@SuppressWarnings("unchecked")
	private boolean addPropertyFromDefinitionToResult(JSONArray result,
			PropertyDefinition definition, Map<String, ?> properties) {
		DisplayType displayType = definition.getDisplayType();
		// if ((displayType == DisplayType.HIDDEN) || (displayType ==
		// DisplayType.SYSTEM)) {
		// return false;
		// }

		Object value = properties.get(definition.getName());

		if (value == null) {
			return false;
		}

		String name = definition.getName();
		String label = definition.getLabel();
		if (org.apache.commons.lang.StringUtils.isBlank(name)
				|| org.apache.commons.lang.StringUtils.isBlank(label) || name.contains("header")) {
			return false;
		}

		List<Serializable> multiValueList = null;
		if (value instanceof List) {
			multiValueList = (List<Serializable>) value;
			for (int i = 0; i < multiValueList.size(); i++) {
				addSingleValueToResult(multiValueList.get(i), result, definition, displayType,
						multiValueList, i > 0);
			}
		} else {
			addSingleValueToResult(value, result, definition, displayType, multiValueList, false);
		}
		return true;
	}

	/**
	 * Adds the single value to result.
	 * 
	 * @param value
	 *            the serializable
	 * @param result
	 *            the result
	 * @param definition
	 *            the definition
	 * @param displayType
	 *            the display type
	 * @param multiValueList
	 *            the multi value list
	 * @param isPartOfMultiValue
	 *            the is part of multi value
	 */
	private void addSingleValueToResult(Object value, JSONArray result,
			PropertyDefinition definition, DisplayType displayType,
			List<Serializable> multiValueList, boolean isPartOfMultiValue) {
		JSONObject jsonObject = new JSONObject();
		if (isPartOfMultiValue) {
			JsonUtil.addToJson(jsonObject, NAME, "");
			JsonUtil.addToJson(jsonObject, MULTI, true);
		} else {
			JsonUtil.addToJson(jsonObject, NAME, definition.getLabel());
		}
		JsonUtil.addToJson(jsonObject, "isMultiValued", definition.isMultiValued());
		JsonUtil.addToJson(jsonObject, VALUE, value);
		JsonUtil.addToJson(jsonObject, "oldValue", value);

		if (definition.getCodelist() != null) {
			JsonUtil.addToJson(jsonObject, "codelistNumber", definition.getCodelist());
			String description = codelistService.getDescription(definition.getCodelist(),
					value.toString());
			JsonUtil.addToJson(jsonObject, "description", description);
			// FIXME: constant?
			JsonUtil.addToJson(jsonObject, "editType", "codelist");
		} else {
			JsonUtil.addToJson(jsonObject, "editType", definition.getDataType().getName());
		}

		JsonUtil.addToJson(jsonObject, LINK_ID, definition.getName());
		JsonUtil.addToJson(jsonObject, "cls", "");
		JsonUtil.addToJson(jsonObject, "editable", !((displayType == DisplayType.HIDDEN)
				|| (displayType == DisplayType.READ_ONLY) || (displayType == DisplayType.SYSTEM)));

		result.put(jsonObject);
	}

	/**
	 * Search for objects sections for given case for which current user has edit permissions.
	 * 
	 * @param caseId
	 *            the case id
	 * @return the object sections
	 */
	@GET
	@Path("/objectsSections")
	public String getObjectSections(@QueryParam("caseId") String caseId) {
		JSONObject result = new JSONObject();
		try {
			List<SectionInstance> caseObjectSections = null;

			CaseInstance caseInstance = (CaseInstance) serviceRegister.getInstanceService(
					CaseInstance.class).loadByDbId(caseId);

			if (caseInstance != null) {
				Resource currentUser = getCurrentUser();
				if (currentUser == null) {
					return "{}";
				}
				if (authorityService.hasPermission(SecurityModel.PERMISSION_EDIT, caseInstance,
						currentUser)) {
					List<SectionInstance> sections = caseInstance.getSections();
					caseObjectSections = DefinitionUtil.filterByPurpose(sections, SECTION_PURPOSE);
				}
			} else {
				log.error("ObjectsWeb: ObjectRestService - a case instance was not found!");
			}

			if (caseObjectSections != null) {
				JSONArray values = new JSONArray();
				for (Instance instance : caseObjectSections) {
					JSONObject item = convertInstanceToJSON(instance);
					if (item != null) {
						values.put(item);
					}
				}
				result.put("values", values);
			}
		} catch (JSONException jse) {
			log.error("", jse);
		}

		return result.toString();
	}

	/**
	 * Selects and returns section and its owning cas.
	 * 
	 * @param sectionId
	 *            the section id
	 * @return the section and case
	 */
	// TODO: move in CaseInstanceRestService
	@GET
	@Path("/caseAndSection")
	public String getSectionAndCase(@QueryParam("sectionId") String sectionId) {
		JSONObject result = new JSONObject();
		SectionInstance sectionInstance = loadInstanceInternal(SectionInstance.class, sectionId);
		if (sectionInstance == null) {
			return "{}";
		}
		Instance caseInstance = sectionInstance.getOwningInstance();

		JSONObject caseJSON = convertInstanceToJSON(caseInstance);
		JSONObject sectionJSON = convertInstanceToJSON(sectionInstance);
		JsonUtil.addToJson(result, "caseData", caseJSON);
		JsonUtil.addToJson(result, "section", sectionJSON);

		return result.toString();
	}

	/**
	 * Attach selected objects to current section.
	 * 
	 * @param data
	 *            the request content in JSON string. Contains sectionId and selectedItems fields.
	 * @return the response
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response attachObjects(String data) {
		log.debug("ObjectsWeb: ObjectRestService.attachObjects request:" + data);
		Response response = attachInstanceAction.attachDocuments(data, this, instanceService,
				ObjectActionTypeConstants.ATTACH_OBJECT);
		return response;
	}

	/**
	 * Move object in same case.
	 * 
	 * @param objectId
	 *            the object id
	 * @param sourceId
	 *            the source section id
	 * @param destId
	 *            the destination section id
	 * @return the response
	 */
	@GET
	@Path("/objectMoveSameCase")
	public Response moveObjectSameCase(@QueryParam("objectId") String objectId,
			@QueryParam("sourceId") String sourceId, @QueryParam("destId") String destId) {
		if (debug) {
			log.debug("ObjectsWeb: ObjectRestService.moveObjectSameCase objectId=" + objectId
					+ ", sourceId=" + sourceId + ", destId=" + destId);
		}
		if (StringUtils.isNullOrEmpty(objectId) || StringUtils.isNullOrEmpty(sourceId)
				|| StringUtils.isNullOrEmpty(destId)) {
			return buildResponse(Response.Status.BAD_REQUEST,
					"ObjectRestService.moveObjectSameCase is missing required arguments: objectId, sourceId or destId!");
		}

		Instance objectInstance = fetchInstance(objectId, ObjectInstance.class.getSimpleName()
				.toLowerCase());
		if (objectInstance == null) {
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
					"ObjectRestService.moveObjectSameCase can not load requested object instance!");
		}

		Instance sourceSectionInstance = fetchInstance(sourceId, SectionInstance.class
				.getSimpleName().toLowerCase());
		if (sourceSectionInstance == null) {
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
					"ObjectRestService.moveObjectSameCase can not load requested source section instance!");
		}

		Instance destSectionInstance = fetchInstance(destId, SectionInstance.class.getSimpleName()
				.toLowerCase());
		if (destSectionInstance == null) {
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
					"ObjectRestService.moveObjectSameCase can not load requested destination section instance!");
		}

		boolean isMoved = objectService.move((ObjectInstance) objectInstance,
				sourceSectionInstance, destSectionInstance);

		if (!isMoved) {
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
					"ObjectRestService.moveObjectSameCase can not move object!");
		}

		return Response.ok().build();
	}

	/**
	 * Load object by the specified object id (dbId or URI).
	 * 
	 * @param criteria
	 *            Request payload as a json object containing the objects information for the
	 *            objects to load. <b>itemList</b> Json array as string containing the type and
	 *            identifier properties for each item. <b>fields</b> Json array containing the
	 *            string names of properties to pluck from the original instances.
	 * @return Json object as string containing a 'values' array property with the loaded objects.
	 */
	@POST
	@Path("/objectsByIdentity")
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String getObjectsById(String criteria) {
		JSONObject result = new JSONObject();
		try {
			JSONObject criteriaJson = new JSONObject(criteria);
			if (criteriaJson.has("itemList")) {
				JSONArray objects2load = criteriaJson.getJSONArray("itemList");
				JSONArray fields = criteriaJson.getJSONArray("fields");

				int length = objects2load.length();
				List<Pair<Class<? extends Instance>, Serializable>> idTypePairs = new ArrayList<>();
				for (int i = 0; i < length; i++) {
					JSONObject item = objects2load.getJSONObject(i);
					DataTypeDefinition dataTypeDefinition = dictionaryService
							.getDataTypeDefinition(item.getString("type"));
					Class<?> javaClass = dataTypeDefinition.getJavaClass();

					idTypePairs.add(new Pair(javaClass, convertInstanceId(item.getString("dbId"),
							javaClass)));
				}

				List<String> fieldsList = new ArrayList<>();
				for (int i = 0; i < fields.length(); i++) {
					fieldsList.add(fields.getString(i));
				}

				List<Instance> load = BatchEntityLoader.load(idTypePairs, serviceRegister,
						taskExecutor);

				searchResultTransformer.transformResult(load.size(), load, fieldsList, result,
						dictionaryService);
			}
		} catch (JSONException e) {
			log.warn("", e);
		}
		return result.toString();
	}

	/**
	 * Updates the properties of an object.
	 * 
	 * @param properties
	 *            Properties to merge into the instance.
	 */
	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateObject(Map<String, String> properties) {
		Class<?> instanceClass = dictionaryService.getDataTypeDefinition(properties.remove("type"))
				.getJavaClass();
		Serializable id = convertInstanceId(properties.remove("dbId"), instanceClass);

		InstanceService<Instance, DefinitionModel> service = serviceRegister
				.getInstanceService(instanceClass);
		Instance instance = service.loadByDbId(id);

		DefinitionModel definitionModel = dictionaryService.getInstanceDefinition(instance);

		// TODO FIXME: ExtJs doesn't like the colon for some reason, find out
		// why and fix
		// the opposite is done in
		// .DefinitionRestService#addPropertyFromOntologyToResult(...)
		Map<String, String> propertiesWithFixedKeys = new HashMap<>();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			propertiesWithFixedKeys.put(entry.getKey().replace('_', ':'), entry.getValue());
		}
		Map<String, Serializable> fromRest = propertiesService.convertFromRest(
				propertiesWithFixedKeys, definitionModel);
		instance.getProperties().putAll(fromRest);
		instanceService.save(instance, new Operation(""));
	}

	/**
	 * Converts a object id to the type expected by the service.
	 * 
	 * @param id
	 *            Object to convert.
	 * @param instanceClass
	 *            Instance class.
	 * @return The converted serializable id.
	 */
	private Serializable convertInstanceId(Object id, Class<?> instanceClass) {
		return TypeConverterUtil.getConverter().convert(
				serviceRegister.getInstanceDao(instanceClass).getPrimaryIdType(), id);
	}

}
