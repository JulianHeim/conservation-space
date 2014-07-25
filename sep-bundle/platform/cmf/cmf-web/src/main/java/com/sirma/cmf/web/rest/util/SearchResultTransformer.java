package com.sirma.cmf.web.rest.util;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.emf.converter.TypeConverterUtil;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.web.header.InstanceHeaderBuilder;
import com.sirma.itt.emf.web.treeHeader.Size;

/**
 * @author yasko
 */
@Named
public class SearchResultTransformer {
	
	private static final Pattern HTML_A_TAG = Pattern.compile("<(/?)(a[^>]*)>", Pattern.CANON_EQ);

	/** The rendition service. */
	@Inject
	private InstanceHeaderBuilder treeHeaderBuilder;
	
	/**
	 * Transforms a list on {@link Instance} to a json array and sets it as 'values' property of a
	 * specified json object.
	 * 
	 * @param total Total number items that match the search criteria.
	 * @param instances
	 *            Instances to transform.
	 * @param fields
	 *            Fields to pluck from the instances
	 * @param result
	 *            Json object in which to set the transformed result
	 * @param dictionaryService
	 *            Used for retrieving the domain class of the instance holding the property.
	 * @throws JSONException
	 *             error during transformation
	 */
	public void transformResult(int total, List<Instance> instances, List<String> fields, JSONObject result,
			DictionaryService dictionaryService) throws JSONException {
		Map<Instance, Boolean> instancesWithPermissions = new LinkedHashMap<>(instances.size());
		for (Instance instance : instances) {
			instancesWithPermissions.put(instance, true);
		}
		transformResult(total, instancesWithPermissions, fields, result, dictionaryService);
	}

	/**
	 * Transforms a list on {@link Instance} to a json array and sets it as 'values' property of a
	 * specified json object.
	 * 
	 * @param total Total number items that match the search criteria.
	 * @param instances
	 *            Instances to transform. A map with an {@link Instance} as a key and a boolean value indicating does the user has permissions over the {@link Instance} 
	 * @param fields
	 *            Fields to pluck from the instances
	 * @param result
	 *            Json object in which to set the transformed result
	 * @param dictionaryService
	 *            Used for retrieving the domain class of the instance holding the property.
	 * @throws JSONException
	 *             error during transformation
	 */
	public void transformResult(int total, Map<Instance, Boolean> instances, List<String> fields, JSONObject result,
			DictionaryService dictionaryService) throws JSONException {
		// TODO: Move this method in some util class
		JSONArray values = new JSONArray();
		for (Entry<Instance, Boolean> instanceEntry : instances.entrySet()) {
			Instance instance = instanceEntry.getKey();
			DataTypeDefinition dataTypeDefinition = dictionaryService
					.getDataTypeDefinition(instance.getClass().getName());

			Map<String, Serializable> properties = instance.getProperties();
			if (properties == null) {
				// for some reason some of the objects does not have properties
				// this should not happen
				continue;
			}
			// TODO REVIEW: See what what do we always need to be included
			// or rely solely on the fields from the search args
			JSONObject item = new JSONObject();
			// REVIEW FIXME: What is the difference between the next three properties?
			item.put("identifier", properties.get(DefaultProperties.UNIQUE_IDENTIFIER));
			item.put("dbId", instance.getId());
			item.put("name", instance.getId());
			item.put("title", properties.get(DefaultProperties.TITLE));
			item.put("type", dataTypeDefinition.getName());
			item.put("emfType", dataTypeDefinition.getName());
			item.put("disabled", !instanceEntry.getValue());
			
			item.put("domainClass", dataTypeDefinition.getFirstUri());
			
			String defaultHeader = (String) properties.get(DefaultProperties.HEADER_DEFAULT);
			String breadCrumbHeader = (String) properties.get(DefaultProperties.HEADER_BREADCRUMB);
			String compactHeader = (String) properties.get(DefaultProperties.HEADER_COMPACT);
			if (!instanceEntry.getValue()) {
				defaultHeader = HTML_A_TAG.matcher(defaultHeader).replaceAll("<$1span>");
				breadCrumbHeader = HTML_A_TAG.matcher(breadCrumbHeader).replaceAll("<$1span>");
				compactHeader = HTML_A_TAG.matcher(compactHeader).replaceAll("<$1span>");
			}
			item.put(DefaultProperties.HEADER_DEFAULT, defaultHeader);
			item.put(DefaultProperties.HEADER_BREADCRUMB, breadCrumbHeader);
			item.put(DefaultProperties.HEADER_COMPACT, compactHeader);
			item.put("icon", treeHeaderBuilder.getIcon(instance, DefaultProperties.HEADER_DEFAULT,
					Size.BIGGER.getSize(),false));
			if ((fields != null) && !fields.isEmpty()) {
				for (String field : fields) {
					Serializable fieldValue = properties.get(field);
					if (fieldValue instanceof Date) {
						// result to the call for help from bellow
						// this could be moved to upper level to convert
						// everything to string
						fieldValue = TypeConverterUtil.getConverter().convert(String.class,
								fieldValue);
					}
					item.put(field, fieldValue);
				}
			}
			values.put(item);
		}
		result.put("values", values);
		result.put("resultSize", total);
		result.put("success", true);
		result.put("idProperty", "dbId");

		JSONObject meta4extjs = new JSONObject();
		meta4extjs.put("root", "values");
		result.put("metaData", meta4extjs);
	}
}
