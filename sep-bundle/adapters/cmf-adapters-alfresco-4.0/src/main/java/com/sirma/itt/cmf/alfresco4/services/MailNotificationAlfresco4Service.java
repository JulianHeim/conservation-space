package com.sirma.itt.cmf.alfresco4.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.cmf.alfresco4.AlfrescoCommunicationConstants;
import com.sirma.itt.cmf.alfresco4.AlfrescoErroreReader;
import com.sirma.itt.cmf.alfresco4.ServiceURIRegistry;
import com.sirma.itt.cmf.alfresco4.descriptor.AlfrescoFileWithNameDescriptor;
import com.sirma.itt.emf.adapter.CMFMailNotificaionAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.remote.DMSClientException;
import com.sirma.itt.emf.remote.RESTClient;

/**
 * Adapter for mail notifications in alfresco4. Currently used to download definitions
 * <p>
 * FIXME: moved mail templates to template service!!!
 * 
 * @author bbanchev
 */
public class MailNotificationAlfresco4Service implements CMFMailNotificaionAdapterService,
		AlfrescoCommunicationConstants {
	/**
	 *
	 */
	private static final long serialVersionUID = 4184823668419062295L;
	/** The rest client. */
	@Inject
	private RESTClient restClient;

	@Override
	public List<DMSFileDescriptor> getTemplates() throws DMSException {

		try {
			JSONObject request = new JSONObject();
			List<DMSFileDescriptor> results = new ArrayList<DMSFileDescriptor>();
			request.put(KEY_PAGING, getPaging());
			request.put(KEY_SORT, getSorting());
			request.put(KEY_QUERY,
					"PATH:\"/app:company_home/app:dictionary/app:email_templates/cm:activities//*\"");

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);
			String response = restClient.request(
					ServiceURIRegistry.CMF_DEFINITIONS_NOTIFICATIONS_SEARCH, createMethod);
			if (response != null) {
				JSONObject result = new JSONObject(response);
				if (result.has(KEY_ROOT_DATA)) {
					JSONArray nodes = result.getJSONObject(KEY_ROOT_DATA).getJSONArray(
							KEY_DATA_ITEMS);
					for (int i = 0; i < nodes.length(); i++) {
						JSONObject jsonObject = (JSONObject) nodes.get(i);
						String name = jsonObject.getString("cm:name");
						String id = jsonObject.getString(KEY_NODEREF);
						String containerId = jsonObject.has(KEY_SITE_ID) ? jsonObject
								.getString(KEY_SITE_ID) : null;
						results.add(new AlfrescoFileWithNameDescriptor(id, name, containerId,
								restClient));
					}
				}
			}
			return results;
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}

	}

	/**
	 * Gets the paging arguments from request as json object.
	 *
	 * @return the pagging
	 * @throws JSONException
	 *             the jSON exception
	 */
	private JSONObject getPaging() throws JSONException {
		JSONObject paging = new JSONObject();
		paging.put(KEY_PAGING_TOTAL, 0);
		// Why so big page is used here.
		paging.put(KEY_PAGING_SIZE, 1000);
		paging.put(KEY_PAGING_SKIP, 0);
		paging.put(KEY_PAGING_MAX, 1000);
		return paging;

	}

	/**
	 * Gets the sorting.
	 *
	 * @return the sorting array of json objects
	 * @throws JSONException
	 *             the jSON exception
	 * @throws DMSException
	 *             the dMS exception
	 */
	private JSONArray getSorting() throws JSONException, DMSException {
		JSONArray sorting = null;
		Map<String, Serializable> sortArgs = new HashMap<String, Serializable>();

		sortArgs.put("cm:modified", false);
		Set<Entry<String, Serializable>> keySet = sortArgs.entrySet();
		sorting = new JSONArray();
		for (Entry<String, Serializable> string : keySet) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(string.getKey(), string.getValue());
			sorting.put(jsonObject);
		}
		return sorting;

	}

}
