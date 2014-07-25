/**
 * Copyright (c) 2013 18.07.2013 , Sirma ITT.
 */
package com.sirma.itt.comment.web.rest;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.converter.TypeConverterUtil;
import com.sirma.itt.emf.db.SequenceEntityGenerator;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.forum.CommentService;
import com.sirma.itt.emf.forum.ForumProperties;
import com.sirma.itt.emf.forum.event.CommentUpdatedEvent;
import com.sirma.itt.emf.forum.event.TopicReadEvent;
import com.sirma.itt.emf.forum.model.CommentInstance;
import com.sirma.itt.emf.forum.model.ImageAnnotation;
import com.sirma.itt.emf.forum.model.TopicInstance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.properties.model.PropertyModel;
import com.sirma.itt.emf.resources.ResourceProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.CurrentUser;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.EqualsHelper;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.emf.web.header.InstanceHeaderBuilder;
import com.sirma.itt.emf.web.util.DateUtil;

/**
 * @deprecated Use com.sirma.itt.comment.web.rest.TopicsRestService
 * Rest service for operating with comments on objects.
 * 
 * @author Adrian Mitev
 */
@Deprecated
@Path("/comment")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
public class CommentRestService {

	/*
	 * This whole service needs a redesign
	 */

	private static final Operation EDIT_DETAILS = new Operation(ActionTypeConstants.EDIT_DETAILS);

	/** The comment service. */
	@Inject
	private CommentService commentService;

	/** The resource service. */
	@Inject
	private ResourceService resourceService;

	/** The user. */
	@Inject
	@CurrentUser
	private User user;

	/** The event service. */
	@Inject
	private EventService eventService;

	/** The date util. */
	@Inject
	private DateUtil dateUtil;

	/** The log. */
	private static final Logger LOG = Logger.getLogger(CommentRestService.class);

	/** The debug. */
	private static boolean debug = LOG.isDebugEnabled();

	@Inject
	private InstanceHeaderBuilder treeHeaderBuilder;

	@Inject
	private AuthorityService authorityService;

	/**
	 * Removes comment.
	 * 
	 * @param commentId
	 *            id of the comment
	 * @return removed comment id
	 */
	@DELETE
	@Path("/remove/{commentId}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String remove(@PathParam("commentId") String commentId) {
		commentService.deleteById(commentId);
		return "{\"commentId\":\"" + commentId + "\"}";
	}

	/**
	 * Loads a list of comments for a given object.
	 * 
	 * @param objectId
	 *            id of the object for which the comments should be fetched.
	 * @return fetched comments.
	 * @throws JSONException
	 *             when json conversion fails.
	 */
	@GET
	@Path("/{objectId}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String load(@PathParam("objectId") String objectId) throws JSONException {
		if (StringUtils.isNullOrEmpty(objectId)) {
			return "[]";
		}
		TimeTracker tracker = null;
		if (debug) {
			tracker = new TimeTracker().begin();
			LOG.debug("CommentRestService.load comments for object id [" + objectId + "]");
		}

		List<TopicInstance> topics = commentService.getTopics(objectId, null, -1, true, null, null);

		JSONArray result = buildTopicResponse(topics, true);

		if (debug) {
			LOG.debug("CommentRestService.load found " + result.length()
					+ " comments for object id [" + objectId + "] in " + tracker.stopInSeconds()
					+ " s");
		}

		return result.toString();
	}

	/**
	 * Builds the topic response.
	 * 
	 * @param topics
	 *            the topics
	 * @param evalActions
	 *            to eval actions or not
	 * @return the JSON array
	 */
	private JSONArray buildTopicResponse(List<TopicInstance> topics, Boolean evalActions) {
		JSONArray result = new JSONArray();
		for (TopicInstance instance : topics) {
			JSONObject object = convert(instance, evalActions);
			if (object.length() > 0) {
				result.put(object);
			}
		}
		return result;
	}

	/**
	 * Loads a list of comments for a given object.
	 * 
	 * @param instanceType
	 *            the instance type
	 * @param instanceId
	 *            id of the object for which the comments should be fetched.
	 * @return fetched comments.
	 * @throws JSONException
	 *             when json conversion fails.
	 */
	@GET
	@Path("/{instanceType}/{instanceId}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String loadForInstance(@PathParam("instanceType") String instanceType,
			@PathParam("instanceId") String instanceId) throws JSONException {
		if (StringUtils.isNullOrEmpty(instanceId) || StringUtils.isNullOrEmpty(instanceType)) {
			return "[]";
		}
		TimeTracker tracker = null;
		if (debug) {
			tracker = new TimeTracker().begin();
			LOG.debug("CommentRestService.load comments for instance type/id [" + instanceType
					+ "/" + instanceId + "]");
		}
		InstanceReference reference = TypeConverterUtil.getConverter().convert(
				InstanceReference.class, instanceType);
		reference.setIdentifier(instanceId);

		List<TopicInstance> topics = commentService.getTopics(reference, null, -1, true, null, null);

		JSONArray result = buildTopicResponse(topics, true);

		if (debug) {
			LOG.debug("CommentRestService.load " + topics.size() + " topics for instance type/id ["
					+ instanceType + "/" + instanceId + "] in " + tracker.stopInSeconds() + " s");
		}

		return result.toString();
	}

	/**
	 * Loads comments based on a filter criteria.
	 * 
	 * @param data
	 *            filter data
	 * @return fetched comments
	 * @throws JSONException
	 *             the jSON exception
	 */
	@POST
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String filter(String data) throws JSONException {
		JSONArray result = new JSONArray();
		try {
			JSONObject criteriaJson = new JSONObject(data);
			String instanceId = criteriaJson.has("instanceId") ? criteriaJson
					.getString("instanceId") : null;
			String instanceType = criteriaJson.has("instanceType") ? criteriaJson
					.getString("instanceType") : null;
			Boolean userRelated = criteriaJson.has("userRelated") ? criteriaJson
					.getBoolean("userRelated") : false;
			Boolean includeChildren = criteriaJson.has("includeChildren") ? criteriaJson
					.getBoolean("includeChildren") : false;
			Boolean evalActions = criteriaJson.has("evalActions") ? criteriaJson
					.getBoolean("evalActions") : true;
			Integer limit = criteriaJson.has("limit") ? criteriaJson.getInt("limit") : 100;

			List<TopicInstance> topics = null;

			TimeTracker tracker = null;
			if (debug) {
				tracker = new TimeTracker().begin();
				LOG.debug("CommentRestService.filter comments by criteria: " + data);
			}

			if (userRelated) {
				if (user == null) {
					return "[]";
				}
				topics = commentService.getTopicsByUser(user);
			} else if (includeChildren) {
				if (StringUtils.isNullOrEmpty(instanceId)) {
					return "[]";
				}
				topics = commentService.getInstanceSuccessorsTopics(instanceId);
			} else {
				if (StringUtils.isNullOrEmpty(instanceType)) {
					if (StringUtils.isNullOrEmpty(instanceId)) {
						return "[]";
					}
					topics = commentService.getTopics(instanceId, null, -1, true, null, null);
				} else {
					if (StringUtils.isNullOrEmpty(instanceId)) {
						return "[]";
					}
					InstanceReference reference = TypeConverterUtil.getConverter().convert(
							InstanceReference.class, instanceType);
					reference.setIdentifier(instanceId);
					topics = commentService.getTopics(reference, null, -1, true, null, null);
				}
			}

			if (debug) {
				LOG.debug("CommentRestService.filter returned " + topics.size()
						+ " topics by criteria: " + data + " in " + tracker.stopInSeconds() + " s");
			}

			if (topics != null) {
				if (topics.size() > limit.intValue()) {
					result = buildTopicResponse(topics.subList(0, limit), evalActions);
				} else {
					result = buildTopicResponse(topics, evalActions);
				}
			}

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return result.toString();
	}

	/**
	 * Adds/Updates a comment to an object.
	 * 
	 * @param objectId
	 *            Identifier of the object/section that is commented.
	 * @param parentId
	 *            Identifier of the Topic/Thread (should be provided if the comment is a reply).
	 * @param instanceId
	 *            Identifier of the instance to which topics/threads are attached to.
	 * @param instanceType
	 *            Type of the instance to which topics/threads are attached to.
	 * @param id
	 *            Identifier of the topic/thread/comment.
	 * @param title
	 *            title of the comment.
	 * @param status
	 *            status of the comment.
	 * @param content
	 *            content of the comment.
	 * @param category
	 *            category of the comment.
	 * @param createdOn
	 *            is the time of creation.
	 * @param createdByUsername
	 *            is the creator.
	 * @param tags
	 *            the tags
	 * @param svgTag
	 *            the structure of the shape.
	 * @param imageId
	 *            client side ID.
	 * @param viewbox
	 *            the view box during annotation creation.
	 * @param zoomLevel
	 *            level of zooming where image was annotated.
	 * @return the added comment as JSON object
	 */
	@POST
	@Path("/{objectId}{parentId:(/[^/]+?)?}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String add(@PathParam("objectId") String objectId,
			@PathParam("parentId") String parentId, @FormParam("instanceId") String instanceId,
			@FormParam("instanceType") String instanceType, @FormParam("id") String id,
			@FormParam("title") String title, @FormParam("status") String status,
			@FormParam("content") String content, @FormParam("category") String category,
			@FormParam("createdOn") String createdOn,
			@FormParam("createdByUsername") String createdByUsername,
			@FormParam("tags") String tags, @FormParam("shape[svgTag]") String svgTag,
			@FormParam("shape[imageId]") String imageId,
			@FormParam("shape[viewbox]") String viewbox,
			@FormParam("shape[zoomLevel]") String zoomLevel) {

		// if there is no logged in user
		if (user == null) {
			return "{}";
		}

		TimeTracker tracker = null;
		if (debug) {
			tracker = new TimeTracker().begin();
			LOG.debug("CommentRestService.add comment start");
		}

		String parentIdentifier = null;
		if (StringUtils.isNotNullOrEmpty(parentId)) {
			// remove the starting '/' of the parent parameter
			parentIdentifier = parentId.substring(1);
		}
		TopicInstance topicInstance = null;
		if (parentIdentifier == null) {
			// new topic is going to be created
			topicInstance = new TopicInstance();
			topicInstance.getProperties().put(DefaultProperties.TITLE, title);
			
			topicInstance.setSubSectionId(objectId);
			
			InstanceReference reference = TypeConverterUtil.getConverter().convert(
					InstanceReference.class, instanceType);
			reference.setIdentifier(instanceId);
			topicInstance.setTopicAbout(reference);
			
			if (StringUtils.isNotNull(createdOn)) {
				Date date = dateUtil.getISODateTime(createdOn);
				topicInstance.setPostedDate(date);
			} else {
				topicInstance.setPostedDate(new Date());
			}
			topicInstance.setFrom(user.getId().toString());
			if (StringUtils.isNotNullOrEmpty(status)) {
				topicInstance.getProperties().put(DefaultProperties.STATUS, status);
			} else {
				topicInstance.getProperties().put(DefaultProperties.STATUS, "IN_PROGRESS");
			}
			topicInstance.getProperties().put(DefaultProperties.TYPE, category);
			if (org.apache.commons.lang.StringUtils.isBlank(tags)) {
				// CS-520 - remove "default" tag
				// tags = "default";
			}
			topicInstance.setTags(tags);
		} else {
			CommentInstance instance = commentService.loadByDbId(parentIdentifier);
			if (instance instanceof TopicInstance) {
				topicInstance = (TopicInstance) instance;
			} else {
				// we have invalid parent here
				// for future implementations to convert the comment instance as
				// new topic
				return "{}";
			}
		}

		if ((imageId != null) || (zoomLevel != null) || (svgTag != null) || (viewbox != null)) {
			ImageAnnotation shape = new ImageAnnotation();
			SequenceEntityGenerator.generateStringId(shape, false);
			topicInstance.setImageAnnotation(shape);
			// shape.setImageId(imageId);
			shape.setSvgValue(svgTag);
			shape.setViewBox(viewbox);
			if (zoomLevel != null) {
				shape.setZoomLevel(Integer.valueOf(zoomLevel).intValue());
			}
		}

		CommentInstance comment = null;
		if (StringUtils.isNullOrEmpty(id)) {
			comment = new CommentInstance();
			comment.setSubSectionId(objectId);
		} else {
			for (CommentInstance commentInstance : topicInstance.getComments()) {
				if (commentInstance.getId().equals(id)) {
					comment = commentInstance;
				}
			}
			if (comment == null) {
				if (id.equals(parentIdentifier)) {
					// update topic
					boolean updated = false;
					updated |= setPropertyIfChanged(topicInstance, DefaultProperties.TITLE, title);
					updated |= setPropertyIfChanged(topicInstance, DefaultProperties.TYPE, category);
					updated |= setPropertyIfChanged(topicInstance, DefaultProperties.STATUS, status);
					updated |= setPropertyIfChanged(topicInstance, ForumProperties.TAGS, tags);
					if (updated) {
						commentService.save(topicInstance, EDIT_DETAILS);
					}
				}
				// comment was deleted so the user edited old/stale comment
				return convert(topicInstance, true).toString();
			}
		}

		// Old comment is needed for auto remove links
		CommentInstance oldCommnet = new CommentInstance();
		oldCommnet.setComment(comment.getComment());

		comment.setComment(content);
		comment.setFrom(user.getId().toString());

		if (StringUtils.isNotNull(createdOn)) {
			Date date = dateUtil.getISODateTime(createdOn);
			comment.setPostedDate(date);
		} else {
			comment.setPostedDate(new Date());
		}
		sanitize(comment);

		if (comment.getId() == null) {
			commentService.postComment(topicInstance, comment);
		} else {
			commentService.save(comment, EDIT_DETAILS);
		}

		if (debug) {
			LOG.debug("CommentRestService.add comment in " + tracker.stopInSeconds() + " s");
		}

		eventService.fire(new CommentUpdatedEvent(oldCommnet, comment));

		// if we have a new topic then we should return it, otherwise return
		// only the added comment
		return (parentIdentifier == null ? convert(topicInstance, true) : convert(comment, true))
				.toString();
	}

	/**
	 * Updates a topic and it's replies.
	 * 
	 * @param topicId
	 *            The id of the topic.
	 * @param topicJson
	 *            Topic data serialized as JSON
	 * @return The updated topic as JSON
	 */
	@PUT
	@Path("/topics/{topicId}")
	public String updateTopic(@PathParam("topicId") String topicId, String topicJson) {
		try {
			JSONObject topicAsJsonObject = new JSONObject(topicJson);
			CommentInstance instance = commentService.loadByDbId(topicId);
			if (instance instanceof TopicInstance) {
				TopicInstance topicInstance = (TopicInstance) instance;

				ImageAnnotation imageAnnotation = topicInstance.getImageAnnotation();
				if ((imageAnnotation != null) && topicAsJsonObject.has("shape")) {
					imageAnnotation.setSvgValue(topicAsJsonObject.getJSONObject("shape").getString(
							"svgTag"));
				}

				if (topicAsJsonObject.has("children")) {
					JSONArray children = topicAsJsonObject.getJSONArray("children");
					int length = children.length();
					for (int i = 0; i < length; i++) {
						JSONObject replyJsonObject = children.getJSONObject(i);
						CommentInstance reply = getReply(topicInstance,
								replyJsonObject.getString("id"));
						reply.setComment(replyJsonObject.getString("content"));
					}
				}
				commentService.save(topicInstance, EDIT_DETAILS);
				String result = convert(topicInstance, true).toString();
				return result;
			}
		} catch (JSONException e) {
			LOG.error("Can not convert given string to JSON object", e);
		}

		return null;
	}

	/**
	 * Finds a reply to a topic by it's id.
	 * 
	 * @param topicInstance
	 *            Topic to search in.
	 * @param replyId
	 *            Id of the reply we are searching for.
	 * @return The {@link CommentInstance} matching the reply id, or {@code null} if not found.
	 */
	private CommentInstance getReply(TopicInstance topicInstance, String replyId) {
		List<CommentInstance> comments = topicInstance.getComments();
		CommentInstance reply = null;
		for (CommentInstance commentInstance : comments) {
			if (commentInstance.getId().equals(replyId)) {
				reply = commentInstance;
				break;
			}
		}
		return reply;
	}

	/**
	 * Sets the property identified by the given key if the old value differs from the new given
	 * value
	 * <p>
	 * REVIEW: probably good idea to move to utility class - but witch one
	 * 
	 * @param model
	 *            the model
	 * @param key
	 *            the key
	 * @param newValue
	 *            the new value
	 * @return true, if the model has been updated due to change
	 */
	private boolean setPropertyIfChanged(PropertyModel model, String key, Serializable newValue) {
		Serializable oldValue = model.getProperties().get(key);
		boolean changed = !EqualsHelper.nullSafeEquals(oldValue, newValue);
		if ((((newValue instanceof String) && org.apache.commons.lang.StringUtils
				.isBlank((String) newValue)) && changed) || changed) {
			model.getProperties().put(key, newValue);
			return true;
		}
		return false;
	}

	/**
	 * Notify for read topic.
	 * 
	 * @param userId
	 *            the user id
	 * @param topicId
	 *            the topic id
	 */
	@GET
	@Path("/{user}/read/{topicId}")
	public void notifyForReadTopic(@PathParam("user") String userId,
			@PathParam("topicId") String topicId) {
		if (StringUtils.isNullOrEmpty(topicId) || (user == null)) {
			return;
		}
		eventService.fire(new TopicReadEvent(topicId, user));
	}

	/**
	 * Gets the unread count.
	 * 
	 * @param userId
	 *            the user id
	 * @param topics
	 *            the topics
	 * @return the unread count
	 */
	@POST
	@Path("/unread/{user}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String getUnreadCount(@QueryParam("user") String userId,
			@FormParam("topics") String topics) {
		return "{}";
	}

	/**
	 * Sanitizes comment data.
	 * 
	 * @param comment
	 *            is the comment
	 */
	private void sanitize(CommentInstance comment) {
		// comment.setComment(HTMLPolicy.sanitize(comment.getComment()));
	}

	/**
	 * Converts a Comment into JSON object and recursively traverses the children comments.
	 * 
	 * @param comment
	 *            comment to convert.
	 * @param evalActions
	 *            to eval actions or not
	 * @return produces json object.
	 */
	private JSONObject convert(CommentInstance comment, Boolean evalActions) {
		JSONObject object = new JSONObject();
		JsonUtil.addToJson(object, "id", comment.getId());
		JsonUtil.addToJson(object, "objectId", comment.getTopic().getIdentifier());

		String creator = comment.getFrom();
		Resource resource = convertResource(object, creator);

		JsonUtil.addToJson(object, "createdOn",
				dateUtil.getISOFormattedDateTime(comment.getPostedDate()));
		JsonUtil.addToJson(object, "content",
				new StringBuilder("<div>").append(comment.getComment()).append("</div>").toString());
		// load user image from user settings.
		setUserAvatar(object, resource);

		JsonUtil.addToJson(object, "parentId", comment.getTopic().getId());

		Collection<Action> actions = Collections.emptyList();
		if (Boolean.TRUE.equals(evalActions)) {
			actions = authorityService.getAllowedActions(user.getIdentifier(),
					comment, "");
		}
		JsonUtil.addActions(object, actions);
		return object;
	}

	/**
	 * Convert.
	 * 
	 * @param topic
	 *            the topic
	 * @param evalActions
	 *            to eval actions or not
	 * @return the jSON object
	 */
	private JSONObject convert(TopicInstance topic, Boolean evalActions) {
		JSONObject object = new JSONObject();
		if (topic.getTopicAbout() == null) {
			return object;
		}
		JsonUtil.addToJson(object, "id", topic.getId());
		JsonUtil.addToJson(object, "objectId", topic.getSubSectionId());
		JsonUtil.addToJson(object, "instanceId", topic.getTopicAbout().getIdentifier());
		String typeName = topic.getTopicAbout().getReferenceType().getName();
		JsonUtil.addToJson(object, "instanceType", typeName);

		JsonUtil.addToJson(object, "icon", treeHeaderBuilder.getBreadcrumbIcon(typeName));

		Serializable creator = topic.getProperties().get(ForumProperties.CREATED_BY);
		Resource resource = convertResource(object, creator);

		JsonUtil.addToJson(
				object,
				"createdOn",
				dateUtil.getISOFormattedDateTime((Date) topic.getProperties().get(
						ForumProperties.CREATED_ON)));
		JsonUtil.addToJson(object, "status", topic.getProperties().get(DefaultProperties.STATUS));
		JsonUtil.addToJson(object, "title", topic.getProperties().get(ForumProperties.TITLE));
		JsonUtil.addToJson(object, "category", topic.getProperties().get(ForumProperties.TYPE));
		JsonUtil.addToJson(object, "tags", topic.getTags());
		// TODO - load user image from user settings.
		setUserAvatar(object, resource);

		if (topic.getImageAnnotation() != null) {
			ImageAnnotation annotation = topic.getImageAnnotation();
			JSONObject shape = new JSONObject();

			JsonUtil.addToJson(shape, "imageId", annotation.getIdentifier());
			JsonUtil.addToJson(shape, "imageUri", annotation.getIdentifier());
			JsonUtil.addToJson(shape, "zoomLevel", annotation.getZoomLevel());
			JsonUtil.addToJson(shape, "svgTag", annotation.getSvgValue());
			JsonUtil.addToJson(shape, "viewbox", annotation.getViewBox());

			Collection<Action> actions = authorityService.getAllowedActions(user.getIdentifier(),
					annotation, "");
			JsonUtil.addActions(shape, actions);

			JsonUtil.addToJson(object, "shape", shape);
		}

		Collection<Action> actions = Collections.emptyList();
		if (Boolean.TRUE.equals(evalActions)) {
			actions = authorityService.getAllowedActions(user.getIdentifier(),
					topic, "");
		}
		JsonUtil.addActions(object, actions);

		JSONArray children = new JSONArray();
		if (topic.getComments() != null) {
			for (CommentInstance child : topic.getComments()) {
				children.put(convert(child, evalActions));
			}
		}
		// we have to put an entry no mater if we have or no children
		JsonUtil.addToJson(object, "children", children);

		return object;
	}

	/**
	 * Sets the user avatar.
	 * 
	 * @param object
	 *            the object
	 * @param resource
	 *            the resource
	 */
	private void setUserAvatar(JSONObject object, Resource resource) {
		if ((resource == null) || (resource.getProperties().get(ResourceProperties.AVATAR) == null)) {
			JsonUtil.addToJson(object, "userImage", "/emf/document/images/user.png");
		} else {
			JsonUtil.addToJson(object, "userImage",
					resource.getProperties().get(ResourceProperties.AVATAR));
		}
	}

	/**
	 * Convert resource.
	 * 
	 * @param object
	 *            the object
	 * @param creator
	 *            the creator
	 * @return the resource
	 */
	private Resource convertResource(JSONObject object, Serializable creator) {
		Resource resource = null;
		if (creator != null) {
			if (creator.toString().contains(":")) {
				resource = resourceService.loadByDbId(creator);
			} else {
				resource = resourceService.getResource(creator.toString(), ResourceType.USER);
			}
			if (resource == null) {
				JsonUtil.addToJson(object, "createdBy", "Unknown");
				JsonUtil.addToJson(object, "createdByUsername", "");
			} else {
				JsonUtil.addToJson(object, "createdBy", resource.getDisplayName());
				JsonUtil.addToJson(object, "createdByUsername", resource.getIdentifier());
			}
		}
		return resource;
	}
}