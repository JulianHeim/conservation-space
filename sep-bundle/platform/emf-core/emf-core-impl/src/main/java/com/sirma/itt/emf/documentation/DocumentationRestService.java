/*
 *
 */
package com.sirma.itt.emf.documentation;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.ws.soap.MTOM;

import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * Base documentation rest service that provides information about events, plugins, configurations,
 * constants.
 * 
 * @author BBonev
 */
@ApplicationScoped
@Path("/documentation")
@Consumes({ "application/json" })
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
@MTOM
public class DocumentationRestService {

	/** The documentation extension. */
	@Inject
	@ExtensionPoint(ApplicationDocumentationExtension.TARGET_NAME)
	private Iterable<ApplicationDocumentationExtension> extensions;

	/** The mapping. */
	private Map<String, ApplicationDocumentationExtension> mapping = new LinkedHashMap<>();

	/**
	 * List options.
	 * 
	 * @return the string
	 */
	@GET
	@Path("options")
	public String listOptions() {
		JSONObject object = new JSONObject();
		JsonUtil.addToJson(object, "data", getOrCreateMapping().keySet());
		return object.toString();
	}

	/**
	 * Gets the or create mapping.
	 * 
	 * @return the or create mapping
	 */
	private Map<String, ApplicationDocumentationExtension> getOrCreateMapping() {
		if (mapping.isEmpty()) {
			for (ApplicationDocumentationExtension extension : extensions) {
				for (String option : extension.generationOptions()) {
					mapping.put(option, extension);
				}
			}
		}
		return mapping;
	}

	/**
	 * List configuration information.
	 * 
	 * @param option
	 *            the option
	 * @return the string
	 * @throws JSONException
	 *             the jSON exception
	 */
	@GET
	@Path("/{option}")
	public String executeOption(@PathParam("option") String option) throws JSONException {
		ApplicationDocumentationExtension documentationExtension = getOrCreateMapping().get(option);
		if (documentationExtension != null) {
			return documentationExtension.generate(option);
		}
		return "{}";
	}

}
