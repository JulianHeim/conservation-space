package com.sirma.itt.objects.web.object;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.annotation.Proxy;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.services.RenditionService;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;

/**
 * Rest service for manipulating and managing thumbnail images for objects.
 * 
 * @author svelikov
 */
@Secure
@Path("/thumbnail")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
@ApplicationScoped
public class ThumbnailRestService extends EmfRestService {

	@Inject
	@Proxy
	private InstanceService<Instance, DefinitionModel> instanceService;

	@Inject
	private RenditionService renditionService;

	@Inject
	private LinkService linkService;

	/**
	 * Adds the thumbnail to selected object. Expected parameters are: <br>
	 * objectId - the object id to which an image should be linked as primary <br>
	 * documentId - document id of the document with mimetype image/*
	 * 
	 * @param data
	 *            Request data.
	 * @return the response
	 */
	@POST
	@Path("/")
	public Response addThumnail(String data) {
		if (debug) {
			log.debug("ObjectsWeb: ThumbnailRestService.addThumbnail: data: " + data);
		}
		if (StringUtils.isNullOrEmpty(data)) {
			return buildResponse(Response.Status.BAD_REQUEST,
					"Missing request parameters for add thumbnail operation!");
		}

		try {
			JSONObject jsonData = new JSONObject(data);
			String objectId = JsonUtil.getStringValue(jsonData, "objectId");
			String documentId = JsonUtil.getStringValue(jsonData, "documentId");

			if (StringUtils.isNullOrEmpty(objectId) || StringUtils.isNullOrEmpty(documentId)) {
				return buildResponse(Response.Status.BAD_REQUEST,
						"Missing required arguments: objectId[" + objectId + "] or documentId["
								+ documentId + "]");
			}

			Resource currentUser = getCurrentUser();
			if (currentUser == null) {
				return buildResponse(Response.Status.UNAUTHORIZED,
						"No logged in user to add thumbnail!");
			}

			ObjectInstance objectInstance = loadInstanceInternal(ObjectInstance.class, objectId);
			if (objectInstance == null) {
				log.debug(
						"ObjectsWeb: ThumbnailRestService.addThumbnail: Can't find object with id[{}]",
						objectId);
				return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
						"Can't find object with id=" + objectId);
			}

			DocumentInstance documentInstance = loadInstanceInternal(DocumentInstance.class,
					documentId);
			if (documentInstance == null) {
				log.debug(
						"ObjectsWeb: ThumbnailRestService.addThumbnail: Can't find document with id[{}]",
						documentId);
				return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
						"Can't find document with id=" + documentId);
			}

			// If there is existing thumbnail image,
			// then deactivate the old relation and create a new one
			LinkReference hasThumbnailRelation = linkExists(objectInstance.toReference(),
					LinkConstants.HAS_THUMBNAIL);
			if (hasThumbnailRelation != null) {
				log.debug("ObjectsWeb: ThumbnailRestService.addThumbnail removing existing primary image");
				linkService.removeLinkById(hasThumbnailRelation.getId());
				LinkReference isThumbnailRelationOf = linkExists(hasThumbnailRelation.getTo(),
						LinkConstants.IS_THUMBNAIL_OF);
				if (isThumbnailRelationOf != null) {
					log.debug("ObjectsWeb: ThumbnailRestService.addThumbnail removing existing isThumbnailOf relation");
					linkService.removeLinkById(isThumbnailRelationOf.getId());
				}
			}

			// changed the thumbnail image link to system: requested with CMF-6449
			Pair<Serializable, Serializable> linkIds = linkService.link(
					objectInstance.toReference(), documentInstance.toReference(),
					LinkConstants.HAS_THUMBNAIL, LinkConstants.IS_THUMBNAIL_OF,
					LinkConstants.DEFAULT_SYSTEM_PROPERTIES);
			if ((linkIds.getFirst() != null) && (linkIds.getSecond() != null)) {
				Serializable thumbnail = renditionService.getThumbnail(documentInstance);
				if (thumbnail != null) {
					objectInstance.getProperties()
							.put(DefaultProperties.THUMBNAIL_IMAGE, thumbnail);
					instanceService.save(objectInstance, new Operation(
							ObjectActionTypeConstants.ADD_THUMBNAIL));
				}
				log.debug(
						"ObjectsWeb: ThumbnailRestService.addThumbnail: created relation of type [{}] between object[{}] and document[{}]",
						LinkConstants.HAS_THUMBNAIL, objectId, documentId);
				return Response.ok().build();
			}

			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
					"Can't create link between selected object and image");
		} catch (JSONException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Server error!")
					.build();
		}
	}

	/**
	 * Checks if an object has attached a thumbnail already.
	 * 
	 * @param objectId
	 *            the object id
	 * @return the response
	 */
	@GET
	@Path("/{objectId}")
	public Response getThumbnail(@PathParam("objectId") String objectId) {
		log.debug("ObjectsWeb: ThumbnailRestService.getThumnail: objectId[{}]", objectId);

		if (StringUtils.isNullOrEmpty(objectId)) {
			log.debug("ObjectsWeb: ThumbnailRestService.getThumnail: Missing required argument: objectId");
			return buildResponse(Response.Status.BAD_REQUEST, "Missing required argument: objectId");
		}

		Instance objectInstnace = loadInstanceInternal(ObjectInstance.class, objectId);

		if (objectInstnace == null) {
			log.debug(
					"ObjectsWeb: ThumbnailRestService.getThumnail: Can't find object with id: [{}]",
					objectId);
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
					"Can't find object with id : " + objectId);
		}

		boolean hasThumbnail = false;
		if (objectInstnace.getProperties() != null) {
			Serializable thumbnail = objectInstnace.getProperties().get(
					DefaultProperties.THUMBNAIL_IMAGE);
			if (thumbnail != null) {
				hasThumbnail = true;
			}
		}
		
		if (hasThumbnail) {
			log.debug("ObjectsWeb: ThumbnailRestService.getThumnail: Requested object already has a primary image");
			return buildResponse(Response.Status.BAD_REQUEST,
					"Requested object already has a thumbnail image");
		}

		return Response.ok().build();
	}

	/**
	 * Check if a relation exists of given type for provided object. If there is one, the first one
	 * is returned or null otherwise.
	 * 
	 * @param instance
	 *            the instance
	 * @param linkType
	 *            the link type
	 * @return the link reference
	 */
	protected LinkReference linkExists(InstanceReference instance, String linkType) {
		List<LinkReference> linkReferences = linkService.getLinks(instance, linkType);
		if (!linkReferences.isEmpty()) {
			return linkReferences.get(0);
		}
		return null;
	}

}
