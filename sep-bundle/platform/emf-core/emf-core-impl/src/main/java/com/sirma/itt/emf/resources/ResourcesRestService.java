package com.sirma.itt.emf.resources;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.resources.event.ResourceSynchronizationEvent;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.rest.RestServiceConstants;

/**
 * Rest service for accessing and managint resource instances via rest service
 * 
 * @author BBonev
 */
@Path("/resources")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
public class ResourcesRestService {

	/** The event service. */
	@Inject
	private EventService eventService;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;

	/** The resource service. */
	@Inject
	private ResourceService resourceService;

	/**
	 * Refresh.
	 * 
	 * @param force
	 *            the force
	 */
	@GET
	@Path("/refresh")
	public void refresh(@QueryParam("force") String force) {
		boolean forced = StringUtils.isNotNullOrEmpty(force) && Boolean.valueOf(force);
		eventService.fire(new ResourceSynchronizationEvent(forced));
	}

	/**
	 * Gets the resources.
	 * 
	 * @param type
	 *            the type
	 * @return the resources
	 */
	@GET
	@Path("/get/{param}")
	public String getResources(@PathParam("param") String type) {
		List<Resource> result = null;
		if (type == null) {
			return "[]";
		}
		ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
		result = resourceService.getAllResources(resourceType, null);
		Collection<JSONObject> collection = typeConverter.convert(JSONObject.class, result);
		JSONArray array = new JSONArray(collection);
		return array.toString();
	}
}
