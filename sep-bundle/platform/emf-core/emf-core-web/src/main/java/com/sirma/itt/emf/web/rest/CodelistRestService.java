package com.sirma.itt.emf.web.rest;

import java.math.BigInteger;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.emf.codelist.CodelistService;
import com.sirma.itt.emf.codelist.model.CodeValue;
import com.sirma.itt.emf.rest.RestServiceConstants;

/**
 * REST Service for retrieving specific codelists.
 * 
 * @author yasko
 */
@Path("/codelist")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
public class CodelistRestService {

	@Inject
	private CodelistService codelistService;

	/**
	 * Retrieve codelist values by specified codelist number and optional language for the
	 * description.
	 * 
	 * @param codelist
	 *            Codelist number.
	 * @param language
	 *            Optional language. Defaults to 'en' if not specified.
	 * @return JSON object as string containing an array of codelist value code and description.
	 */
	@GET
	@Path("/{codelist}")
	public String retrieveValues(@PathParam("codelist") Integer codelist,
			@QueryParam("lang") String language) {
		
		if(codelist == null) {
			return null;
		}
		
		JSONArray result = new JSONArray();
		try {

			JSONObject value = null;
			String currentLanguage = language;
			if (StringUtils.isBlank(language)) {
				// FIXME: constant?
				currentLanguage = "en";
			}

			Map<String, CodeValue> codeValues = codelistService.getCodeValues(codelist);
			for (Map.Entry<String, CodeValue> entry : codeValues.entrySet()) {
				value = new JSONObject();
				value.put("value", entry.getKey());
				value.put("label", entry.getValue().getProperties().get(currentLanguage));
				result.put(value);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

	/**
	 * Retrieve all codelists.
	 * 
	 * @return JSON object as string containing all codelists number and description
	 */
	@GET
	@Path("/codelists")
	public String getAllCodelists() {
		JSONArray result = new JSONArray();
		Map<BigInteger, String> codelists = codelistService.getAllCodelists();

		try {
			JSONObject value = null;

			for (Map.Entry<BigInteger, String> entry : codelists.entrySet()) {
				value = new JSONObject();
				value.put("value", entry.getKey());
				value.put("label", entry.getValue());
				result.put(value);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result.toString();
	}
}
