package com.sirma.cmf.web.task;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskState;
import com.sirma.itt.cmf.beans.model.WorkLogEntry;
import com.sirma.itt.cmf.constants.TaskProperties;
import com.sirma.itt.cmf.services.TaskService;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.instance.InstanceUtil;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.emf.web.util.DateUtil;

/**
 * Rest service for working with tasks.
 */
@Path("/task")
@Consumes("application/json")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
public class TaskRestService extends EmfRestService {

	@Inject
	private TaskService taskService;

	@Inject
	private DateUtil dateUtil;

	/**
	 * Service for getting of subtasks of given task instance. If checkonly=true, then no instances
	 * are loaded and returned with the response but only a flag that indicates the existence of
	 * subtasks.
	 * 
	 * @param instanceId
	 *            the instance id
	 * @param instanceType
	 *            the instance type
	 * @param checkonly
	 *            if this service should return the subtasks in response
	 * @return the response
	 */
	@GET
	@Path("subtasks")
	public Response subtasks(@QueryParam("instanceId") String instanceId,
			@QueryParam("instanceType") String instanceType,
			@QueryParam("checkonly") Boolean checkonly) {
		if (debug) {
			log.debug(
					"TaskInstanceRestService.subtasks request: instanceId[{}] instanceType[{}] checkonly[{}]",
					new Object[] { instanceId, instanceType, checkonly });
		}
		if (StringUtils.isNullOrEmpty(instanceId) || StringUtils.isNullOrEmpty(instanceType)) {
			return buildResponse(Response.Status.BAD_REQUEST,
					"Missing required arguments 'instanceId' or 'instanceType'!");
		}

		// for new instances we have no persisted instance, so we just return
		boolean idPersisted = InstanceUtil.isIdPersisted(instanceId);
		if (!idPersisted) {
			JSONObject skipResponse = new JSONObject();
			JsonUtil.addToJson(skipResponse, "skip", true);
			return buildResponse(Response.Status.OK, skipResponse.toString());
		}
		// load the instance and check if there are subtasks
		Instance instance = fetchInstance(instanceId, instanceType);
		if (instance == null) {
			return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Can't load task instance!");
		}
		boolean notExpectingData = (checkonly == null) ? false : checkonly;
		boolean hasSubTasks = taskService.hasSubTasks((AbstractTaskInstance) instance,
				TaskState.IN_PROGRESS);
		JSONObject response = new JSONObject();
		if (hasSubTasks) {
			JsonUtil.addToJson(response, "hasSubtasks", true);

			if (notExpectingData) {
				return buildResponse(Status.OK, response.toString());
			}

			JSONArray subtasks = new JSONArray();
			JsonUtil.addToJson(response, "subtasks", subtasks);
			List<AbstractTaskInstance> subTasksList = taskService
					.getSubTasks((AbstractTaskInstance) instance);
			for (AbstractTaskInstance abstractTaskInstance : subTasksList) {
				subtasks.put(convertInstanceToJSON(abstractTaskInstance));
			}

			return buildResponse(Status.OK, response);
		}
		JsonUtil.addToJson(response, "hasSubtasks", false);
		return buildResponse(Status.OK, response.toString());
	}

	/**
	 * Gets the logged work for a task.
	 * 
	 * @param taskId
	 *            the task id
	 * @param taskType
	 *            the task type
	 * @param id
	 *            the id of the logged work record. Use for single select
	 * @param page
	 *            the page
	 * @param start
	 *            the start
	 * @param limit
	 *            the limit
	 * @return the logged work
	 */
	@Path("logWork")
	@GET
	public Response getLoggedWork(@QueryParam("taskId") String taskId,
			@QueryParam("taskType") String taskType, @QueryParam("id") String id,
			@QueryParam("page") String page, @QueryParam("start") String start,
			@QueryParam("limit") String limit) {

		if ((taskId == null) || taskId.isEmpty()) {
			return buildJSONErrorResponse(null);
		}

		User currentUser = authenticationService.get().getCurrentUser();
		JSONObject response = new JSONObject();
		try {
			if (currentUser != null) {
				JSONArray result = new JSONArray();
				AbstractTaskInstance task = taskService.loadByDbId(taskId);
				List<WorkLogEntry> loggedData = taskService.getLoggedData(task);
				for (WorkLogEntry workLogEntry : loggedData) {
					result.put(convertWorkLogEntryToJSON(workLogEntry, currentUser));
				}
				response.put("success", true);
				response.put("data", result);

			} else {
				return buildJSONErrorResponse("Error saving logged work.");
			}
		} catch (JSONException e) {
			log.error("", e);
			return buildJSONErrorResponse(null);
		}
		return buildResponse(Response.Status.OK, response.toString());
	}

	/**
	 * Create logged work.
	 * 
	 * @param taskId
	 *            the task id
	 * @param taskType
	 *            the task type
	 * @param data
	 *            the data
	 * @return the response
	 */
	@Path("logWork")
	@POST
	public Response postLoggedWork(@QueryParam("taskId") String taskId,
			@QueryParam("taskType") String taskType, String data) {

		if ((taskId == null) || taskId.isEmpty()) {
			return buildJSONErrorResponse(null);
		}

		try {
			JSONObject loggedWorkRecord = new JSONObject(data);

			Map<String, Serializable> loggedData = convertJSONToLoggedWorkData(loggedWorkRecord);

			AbstractTaskInstance task = taskService.loadByDbId(taskId);
			User currentUser = authenticationService.get().getCurrentUser();

			JSONObject response = new JSONObject();
			if (currentUser != null) {
				InstanceReference instanceReference = typeConverter.convert(
						InstanceReference.class, task);

				Serializable recordId = taskService.logWork(instanceReference,
						currentUser.getName(), loggedData);

				if (recordId != null) {
					loggedWorkRecord.put("id", recordId);
					loggedWorkRecord.put("userName", currentUser.getName());
					loggedWorkRecord.put("userDisplayName", currentUser.getDisplayName());
					loggedWorkRecord.put("editDetails", true);
					loggedWorkRecord.put("delete", true);

					response.put("success", true);
					response.put("data", loggedWorkRecord);
				} else {
					return buildJSONErrorResponse("Error saving logged work.");
				}
			} else {
				return buildJSONErrorResponse("Error saving logged work.");
			}

			return buildResponse(Response.Status.OK, response.toString());
		} catch (JSONException e) {
			log.error("", e);
			return buildJSONErrorResponse(null);
		}
	}

	/**
	 * Update logged work.
	 * 
	 * @param taskId
	 *            the task id
	 * @param taskType
	 *            the task type
	 * @param id
	 *            the id
	 * @param data
	 *            the data
	 * @return the response
	 */
	@Path("logWork/{id}")
	@PUT
	public Response putLoggedWork(@QueryParam("taskId") String taskId,
			@QueryParam("taskType") String taskType, @PathParam("id") String id, String data) {
		// Check permissions
		if ((id == null) || id.isEmpty()) {
			return buildJSONErrorResponse(null);
		}
		try {
			JSONObject loggedWorkRecord = new JSONObject(data);
			Map<String, Serializable> loggedData = convertJSONToLoggedWorkData(loggedWorkRecord);

			User currentUser = authenticationService.get().getCurrentUser();
			JSONObject response = new JSONObject();
			if ((currentUser != null) && taskService.updateLoggedWork(id, loggedData)) {
				loggedWorkRecord.put("id", id);
				loggedWorkRecord.put("userName", currentUser.getName());
				loggedWorkRecord.put("userDisplayName", currentUser.getDisplayName());
				loggedWorkRecord.put("editDetails", true);
				loggedWorkRecord.put("delete", true);

				response.put("success", true);
				response.put("data", loggedWorkRecord);
			} else {
				return buildJSONErrorResponse("Error saving logged work.");
			}
			return buildResponse(Response.Status.OK, response.toString());
		} catch (JSONException e) {
			log.error("", e);
			return buildJSONErrorResponse(null);
		}
	}

	/**
	 * Delete logged work.
	 * 
	 * @param taskId
	 *            the task id
	 * @param taskType
	 *            the task type
	 * @param id
	 *            the id
	 * @return the response
	 */
	@Path("logWork/{id}")
	@DELETE
	public Response deleteLoggedWork(@QueryParam("taskId") String taskId,
			@QueryParam("taskType") String taskType, @PathParam("id") String id) {
		// Check permissions
		if ((id == null) || id.isEmpty()) {
			return buildJSONErrorResponse(null);
		}

		if (taskService.deleteLoggedWork(id)) {
			JSONObject response = new JSONObject();
			try {
				response.put("success", true);
				response.put("data", new JSONArray());
			} catch (JSONException e) {
				log.error("", e);
			}
			return buildResponse(Response.Status.OK, response.toString());
		}
		return buildJSONErrorResponse(null);
	}

	/**
	 * Converts json to logged work data.
	 * 
	 * @param json
	 *            json request object
	 * @return map with logged work properties
	 */
	private Map<String, Serializable> convertJSONToLoggedWorkData(JSONObject json) {
		Map<String, Serializable> loggedData = new HashMap<String, Serializable>(3);
		try {
			loggedData.put(TaskProperties.TIME_SPENT,
					Integer.valueOf(json.getInt(TaskProperties.TIME_SPENT)));
			loggedData.put(TaskProperties.WORK_DESCRIPTION,
					json.getString(TaskProperties.WORK_DESCRIPTION));
			loggedData.put(TaskProperties.START_DATE,
					dateUtil.getISODateTime(json.getString(TaskProperties.START_DATE)));
		} catch (JSONException e) {
			log.error("", e);
		}
		return loggedData;
	}

	/**
	 * Convert work log entry to json.
	 * 
	 * @param entry
	 *            logged work entry
	 * @param currentUser
	 *            the current user
	 * @return the jSON object
	 */
	private JSONObject convertWorkLogEntryToJSON(WorkLogEntry entry, User currentUser) {
		JSONObject json = new JSONObject();
		try {
			json.put("id", entry.getId());
			json.put("userName", entry.getUser());
			json.put("userDisplayName", entry.getUserDisplayName());

			if (currentUser.getName().equals(entry.getUser())) {
				json.put("editDetails", true);
				json.put("delete", true);
			}

			Map<String, Serializable> properties = entry.getProperties();
			if (properties != null) {
				json.put(TaskProperties.TIME_SPENT, properties.get(TaskProperties.TIME_SPENT));
				json.put(TaskProperties.WORK_DESCRIPTION,
						properties.get(TaskProperties.WORK_DESCRIPTION));
				Serializable startDate = properties.get(TaskProperties.START_DATE);
				if (startDate instanceof Date) {
					json.put(TaskProperties.START_DATE,
							dateUtil.getISOFormattedDateTime((Date) startDate));
				}
			}
		} catch (JSONException e) {
			log.error("", e);
		}
		return json;
	}

	/**
	 * Builds error response for ExtJS CRUD operations.
	 * 
	 * @param msg
	 *            error message
	 * @return the response
	 */
	private Response buildJSONErrorResponse(String msg) {
		JSONObject response = new JSONObject();
		try {
			response.put("success", false);
			if ((msg != null) && !msg.isEmpty()) {
				response.put("msg", msg);
			}
		} catch (JSONException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return buildResponse(Response.Status.OK, response.toString());
	}

}
