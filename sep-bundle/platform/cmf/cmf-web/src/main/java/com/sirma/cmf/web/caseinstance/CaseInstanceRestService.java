package com.sirma.cmf.web.caseinstance;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.services.CaseService;
import com.sirma.itt.cmf.services.SectionService;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.emf.web.header.InstanceHeaderBuilder;
import com.sirma.itt.emf.web.treeHeader.Size;

/**
 * Provides functionality to work with the case or to provide some case data.
 * 
 * @author svelikov
 */
@Path("/caseInstance")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
@ApplicationScoped
public class CaseInstanceRestService extends EmfRestService {

	@Inject
	private SectionService sectionService;

	@Inject
	private CaseService caseService;

	/** The rendition service. */
	@Inject
	private InstanceHeaderBuilder treeHeaderBuilder;

	/**
	 * Search for objects sections for given case for which current user has edit permissions.
	 * Provided id can be a section id or case id. If is a section id, then case
	 * 
	 * @param id
	 *            Can be a section id or case id.
	 * @param instanceType
	 *            target instance type
	 * @param purpose
	 *            the section purpose (for documents we have no purpose)
	 * @return the object sections
	 */
	@GET
	@Path("/sections")
	public Response getObjectSections(@QueryParam("id") String id,
			@QueryParam("instanceType") String instanceType, @QueryParam("purpose") String purpose) {

		if (debug) {
			log.debug("CaseInstanceRestService.caseSections request: id=" + id + ", instanceType="
					+ instanceType + ", purpose=" + purpose);
		}

		if (StringUtils.isNullOrEmpty(id) || StringUtils.isNullOrEmpty(instanceType)) {
			return buildResponse(Response.Status.BAD_REQUEST,
					"Missing required arguments 'id' or 'instanceType'!");
		}

		// try to load the case instance
		Instance caseInstance = null;
		if (SectionInstance.class.getSimpleName().equalsIgnoreCase(instanceType)) {
			SectionInstance sectionInstance = sectionService.loadByDbId(id);
			if (sectionInstance != null) {
				caseInstance = sectionInstance.getOwningInstance();
			}
		} else if (CaseInstance.class.getSimpleName().equalsIgnoreCase(instanceType)) {
			caseInstance = caseService.load(id);
		}

		if (caseInstance == null) {
			if (debug) {
				log.debug("CaseInstanceRestService.caseSections can't find case instance!");
			}
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Can't find case instance!");
		}

		// if case is loaded, we build the response using its sections
		JSONObject result = new JSONObject();
		JsonUtil.addToJson(result, "caseId", caseInstance.getId());
		JsonUtil.addToJson(result, "icon", treeHeaderBuilder.getIcon(caseInstance,
				DefaultProperties.HEADER_COMPACT, Size.MEDIUM.getSize(),false));
		JsonUtil.addToJson(result, "header",
				caseInstance.getProperties().get(DefaultProperties.HEADER_COMPACT));
		JSONArray sectionsAsJson = getSectionsAsJson(((CaseInstance) caseInstance).getSections(),
				purpose);
		JsonUtil.addToJson(result, "sections", sectionsAsJson);

		return buildResponse(Response.Status.OK, result.toString());
	}

	/**
	 * Build a sections json array.
	 * 
	 * @param sections
	 *            the section instance list
	 * @param purpose
	 *            the purpose of the section to be used as filter
	 * @return the sections as json
	 */
	private JSONArray getSectionsAsJson(List<SectionInstance> sections, String purpose) {
		// filter sections by purpose
		List<SectionInstance> sectionsByPurpose = new ArrayList<SectionInstance>(sections.size());
		if (StringUtils.isNotNullOrEmpty(purpose)) {
			for (SectionInstance section : sections) {
				if (purpose.equalsIgnoreCase(section.getPurpose())) {
					sectionsByPurpose.add(section);
				}
			}
		} else {
			// get only those with no purpose
			for (SectionInstance section : sections) {
				if (StringUtils.isNullOrEmpty(section.getPurpose())) {
					sectionsByPurpose.add(section);
				}
			}
		}
		// build json array
		JSONArray result = new JSONArray();
		for (SectionInstance section : sectionsByPurpose) {
			JSONObject sectionData = new JSONObject();
			JsonUtil.addToJson(sectionData, "instanceId", section.getId());
			JsonUtil.addToJson(sectionData, "data",
					section.getProperties().get(DefaultProperties.HEADER_COMPACT));
			JsonUtil.addToJson(
					sectionData,
					"icon",
					treeHeaderBuilder.getIcon(section, DefaultProperties.HEADER_COMPACT,
							Size.SMALL.getSize(), false));
			result.put(sectionData);
		}

		return result;
	}

}
