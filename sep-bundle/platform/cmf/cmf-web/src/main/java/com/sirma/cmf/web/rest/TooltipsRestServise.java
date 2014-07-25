package com.sirma.cmf.web.rest;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * This class represent REST controller that will handle tooltip management.
 * 
 * @author cdimitrov
 */
@Path("/tooltip")
@Consumes("application/json")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
@Stateless
public class TooltipsRestServise extends EmfRestService {

	private static final String HEADER_TYPE = "headerType";

	/**
	 * This method will retrieve instance based on received type and id and will send the tooltip
	 * data from requested header type.
	 * 
	 * @param instanceId
	 *            the current instance id
	 * @param instanceType
	 *            the current instance type
	 * @param headerType
	 *            the headerType
	 * @return tooltip for the instance
	 */
	@Path("/")
	@GET
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Response getTooltip(@QueryParam(INSTANCE_ID) String instanceId,
			@QueryParam(INSTANCE_TYPE) String instanceType,
			@QueryParam(HEADER_TYPE) String headerType) {
		// extract instance based on id and type
		Instance instance = fetchInstance(instanceId, instanceType);
		if (instance == null) {
			log.warn("Cannot extract instance with id={} and type={}", instanceId, instanceType);
			return buildResponse(Status.BAD_REQUEST, null);
		}
		// extract header from the instance
		String calculatedHeaderType = DefaultProperties.HEADER_DEFAULT;
		if (StringUtils.isNotNullOrEmpty(headerType)
				&& DefaultProperties.HEADERS.contains(headerType)) {
			calculatedHeaderType = DefaultProperties.HEADERS.iterator().next();
		}
		String instanceHeader = instance.getProperties().get(calculatedHeaderType).toString();
		JSONObject response = new JSONObject();
		JsonUtil.addToJson(response, "tooltip", instanceHeader);
		return buildResponse(Status.OK, response.toString());
	}

}
