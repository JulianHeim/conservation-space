package com.sirma.cmf.web.help;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.javacrumbs.jsonunit.JsonAssert;

import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.sirma.cmf.CMFTest;
import com.sirma.cmf.web.task.TaskRestService;
import com.sirma.itt.cmf.beans.model.AbstractTaskInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskState;
import com.sirma.itt.cmf.services.TaskService;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * Tests for TaskRestService.
 * 
 * @author svelikov
 */
@Test
public class TaskRestServiceTest extends CMFTest {

	private final TaskRestService service;
	private TaskService taskService;
	private StandaloneTaskInstance standaloneTaskInstance;

	/**
	 * Instantiates a new task rest service test.
	 */
	public TaskRestServiceTest() {
		service = new TaskRestService() {

			@Override
			public Instance fetchInstance(String instanceId, String instanceType) {
				if ("standalonetaskinstance".equals(instanceType)) {
					return standaloneTaskInstance;
				}
				return null;
			}

			@Override
			public JSONObject convertInstanceToJSON(Instance instance) {
				JSONObject object = new JSONObject();
				JsonUtil.addToJson(object, "dbId", instance.getId());
				JsonUtil.addToJson(object, "type", instance.getClass().getSimpleName()
						.toLowerCase());
				return object;
			}
		};

		taskService = Mockito.mock(TaskService.class);
		ReflectionUtils.setField(service, "log", SLF4J_LOG);
		ReflectionUtils.setField(service, "taskService", taskService);
	}

	/**
	 * Test for getting subtasks for given task.
	 */
	@SuppressWarnings("boxing")
	public void subtasksTest() {
		Response response = service.subtasks(null, null, true);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		response = service.subtasks("instance1", null, true);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		response = service.subtasks(null, "taskinstance", true);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		//
		response = service.subtasks("instance1", "standalonetaskinstance-notfound", true);
		assertEquals(response.getStatus(), Status.INTERNAL_SERVER_ERROR.getStatusCode());

		//
		standaloneTaskInstance = createStandaloneTaskInstance(Long.valueOf(1L));
		Mockito.when(taskService.hasSubTasks(standaloneTaskInstance, TaskState.IN_PROGRESS))
				.thenReturn(Boolean.FALSE);
		response = service.subtasks("instance1", "standalonetaskinstance", true);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		String responseData = response.getEntity().toString();
		JsonAssert.assertJsonEquals("{\"hasSubtasks\":false}", responseData);

		//
		Mockito.when(taskService.hasSubTasks(standaloneTaskInstance, TaskState.IN_PROGRESS))
				.thenReturn(Boolean.TRUE);
		// if checkonly=true, then no data is expected
		response = service.subtasks("instance1", "standalonetaskinstance", true);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		responseData = response.getEntity().toString();
		JsonAssert.assertJsonEquals("{\"hasSubtasks\":true}", responseData);

		// if checkonly=false, then data is expected but service doesn't return any
		response = service.subtasks("instance1", "standalonetaskinstance", false);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		responseData = response.getEntity().toString();
		JsonAssert.assertJsonEquals("{\"hasSubtasks\":true,\"subtasks\":[]}", responseData);

		// service returns some task instances as subtasks
		List<AbstractTaskInstance> subtasks = new ArrayList<>();
		subtasks.add(createStandaloneTaskInstance(Long.valueOf(1L)));
		subtasks.add(createStandaloneTaskInstance(Long.valueOf(2L)));
		Mockito.when(taskService.getSubTasks(standaloneTaskInstance)).thenReturn(subtasks);
		response = service.subtasks("instance1", "standalonetaskinstance", false);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		responseData = response.getEntity().toString();
		JsonAssert
				.assertJsonEquals(
						"{\"hasSubtasks\":true,\"subtasks\":[{\"dbId\":1,\"type\":\"standalonetaskinstance\"},{\"dbId\":2,\"type\":\"standalonetaskinstance\"}]}",
						responseData);
	}
}
