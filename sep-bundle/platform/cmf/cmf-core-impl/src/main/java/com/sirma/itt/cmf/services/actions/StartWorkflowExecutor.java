package com.sirma.itt.cmf.services.actions;

import static com.sirma.itt.emf.executors.ExecutableOperationProperties.DMS_ID;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.json.JSONObject;

import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.cmf.services.WorkflowService;
import com.sirma.itt.emf.db.SequenceEntityGenerator;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.executors.ExecutableOperationProperties;
import com.sirma.itt.emf.executors.Operation;
import com.sirma.itt.emf.instance.actions.BaseInstanceExecutor;
import com.sirma.itt.emf.instance.model.DMSInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.scheduler.SchedulerContext;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * Operation that creates and starts a workflow.
 * 
 * <pre>
 * <code>
 * {
 * 	operation: "createWorkflow",
 * 	definition: "someWorkflowDefinition",
 * 	revision: definitionRevision,
 * 	id: "emf:someInstanceId",
 * 	type: "workflow",
 * 	parentId: "emf:caseId",
 * 	parentType: "case",
 * 	properties: {
 * 		property1: "some property value 1",
 * 		property2: "true",
 * 		property3: "2323"
 * 	},
 * 	taskId: "emf:someTaskId",
 * 	taskProperties: {
 * 		property1: "some property value 1",
 * 		property2: "true",
 * 		property3: "2323"
 * 	}
 * }
 * </code>
 * </pre>
 * 
 * @author BBonev
 */
@ApplicationScoped
@Extension(target = StartWorkflowExecutor.TARGET_NAME, order = 30)
public class StartWorkflowExecutor extends BaseInstanceExecutor {

	@Inject
	private WorkflowService workflowService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOperation() {
		return ActionTypeConstants.START_WORKFLOW;
	}

	@Override
	public SchedulerContext parseRequest(JSONObject data) {
		SchedulerContext context = super.parseRequest(data);
		// extract any specific task information
		InstanceReference reference = createTaskReference(data);
		context.put("task", reference);

		Map<String, String> properties = extractProperties(data, "taskProperties");
		if (!properties.isEmpty()) {
			context.put("taskProperties", (Serializable) properties);
		}
		return context;
	}

	/**
	 * Creates the task reference.
	 * 
	 * @param data
	 *            the data
	 * @return the instance reference
	 */
	private InstanceReference createTaskReference(JSONObject data) {
		String taskId = JsonUtil.getStringValue(data, "taskId");
		if (taskId == null) {
			taskId = SequenceEntityGenerator.generateId(true).toString();
		}
		InstanceReference reference = typeConverter.convert(InstanceReference.class,
				TaskInstance.class.getName());
		reference.setIdentifier(taskId);
		return reference;
	}

	@Override
	public Object execute(SchedulerContext data) {
		Instance instance = getOrCreateInstance(data);
		WorkflowInstanceContext context = null;
		if (instance instanceof WorkflowInstanceContext) {
			context = (WorkflowInstanceContext) instance;
		} else {
			throw new EmfRuntimeException("Expected workflow data but found " + instance.getClass());
		}
		Instance task = getOrCreateInstance(data, "task", ExecutableOperationProperties.CTX_TARGET,
				"taskProperties");

		List<TaskInstance> saved = workflowService.startWorkflow(context, (TaskInstance) task);
		// backup the dms ID so we could delete it from DMS if needed
		if (context instanceof DMSInstance) {
			data.put(DMS_ID, ((DMSInstance) saved).getDmsId());
		}
		return null;
	}

	@Override
	public Map<Serializable, Operation> getDependencies(SchedulerContext data) {
		Map<Serializable, Operation> dependancies = CollectionUtils.createHashMap(4);
		dependancies.put(
				data.getIfSameType(ExecutableOperationProperties.CTX_TARGET,
						InstanceReference.class).getIdentifier(), Operation.CREATE);
		dependancies.put(data.getIfSameType("task", InstanceReference.class).getIdentifier(),
				Operation.CREATE);
		InstanceReference reference = data.getIfSameType(ExecutableOperationProperties.CTX_PARENT,
				InstanceReference.class);
		if (reference != null) {
			dependancies.put(reference.getIdentifier(), Operation.USE);
		}
		return dependancies;
	}

}
