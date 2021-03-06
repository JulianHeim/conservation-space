/*
 * Copyright (C) 2005-2011 Alfresco Software Limited. This file is part of Alfresco Alfresco is free
 * software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with Alfresco. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.alfresco.repo.workflow.activiti;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.deployer.BpmnDeployer;
import org.activiti.engine.impl.bpmn.diagram.ProcessDiagramGenerator;
import org.activiti.engine.impl.form.DefaultTaskFormHandler;
import org.activiti.engine.impl.form.TaskFormHandler;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.workflow.BPMEngine;
import org.alfresco.repo.workflow.WorkflowAuthorityManager;
import org.alfresco.repo.workflow.WorkflowConstants;
import org.alfresco.repo.workflow.WorkflowEngine;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.WorkflowNodeConverter;
import org.alfresco.repo.workflow.WorkflowObjectFactory;
import org.alfresco.repo.workflow.WorkflowReportServiceProvider;
import org.alfresco.repo.workflow.WorkflowServiceConstants;
import org.alfresco.repo.workflow.activiti.properties.ActivitiPropertyConverter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowDeployment;
import org.alfresco.service.cmr.workflow.WorkflowException;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowInstanceQuery;
import org.alfresco.service.cmr.workflow.WorkflowInstanceQuery.DatePosition;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskDefinition;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery.OrderBy;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.cmr.workflow.WorkflowTimer;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.collections.Function;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.parsers.DOMParser;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springmodules.workflow.jbpm31.JbpmFactoryLocator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

// TODO: Auto-generated Javadoc
/**
 * The Class ActivitiWorkflowEngine.
 *
 * @author Nick Smith
 * @author Frederik Heremans
 * @since 3.4.e
 */
public class ActivitiWorkflowEngine extends BPMEngine implements WorkflowEngine {
	// Workflow Component Messages
	/** The Constant ERR_DEPLOY_WORKFLOW. */
	private static final String ERR_DEPLOY_WORKFLOW = "activiti.engine.deploy.workflow.error";

	/** The Constant ERR_IS_WORKFLOW_DEPLOYED. */
	private static final String ERR_IS_WORKFLOW_DEPLOYED = "activiti.engine.is.workflow.deployed.error";

	/** The Constant ERR_UNDEPLOY_WORKFLOW. */
	private static final String ERR_UNDEPLOY_WORKFLOW = "activiti.engine.undeploy.workflow.error";

	/** The Constant ERR_UNDEPLOY_WORKFLOW_UNEXISTING. */
	private static final String ERR_UNDEPLOY_WORKFLOW_UNEXISTING = "activiti.engine.undeploy.workflow.unexisting.error";

	/** The Constant ERR_GET_WORKFLOW_DEF. */
	private static final String ERR_GET_WORKFLOW_DEF = "activiti.engine.get.workflow.definition.error";

	/** The Constant ERR_GET_WORKFLOW_DEF_BY_ID. */
	private static final String ERR_GET_WORKFLOW_DEF_BY_ID = "activiti.engine.get.workflow.definition.by.id.error";

	/** The Constant ERR_GET_WORKFLOW_DEF_BY_NAME. */
	private static final String ERR_GET_WORKFLOW_DEF_BY_NAME = "activiti.engine.get.workflow.definition.by.name.error";

	/** The Constant ERR_GET_ALL_DEFS_BY_NAME. */
	private static final String ERR_GET_ALL_DEFS_BY_NAME = "activiti.engine.get.all.workflow.definitions.by.name.error";

	/** The Constant ERR_GET_DEF_IMAGE. */
	private static final String ERR_GET_DEF_IMAGE = "activiti.engine.get.workflow.definition.image.error";

	/** The Constant ERR_GET_DEF_UNEXISTING_IMAGE. */
	private static final String ERR_GET_DEF_UNEXISTING_IMAGE = "activiti.engine.get.workflow.definition.unexisting.image.error";
	// private static final String ERR_GET_TASK_DEFS =
	// "activiti.engine.get.task.definitions.error";
	// private static final String ERR_GET_PROCESS_DEF =
	// "activiti.engine.get.process.definition.error";
	/** The Constant ERR_START_WORKFLOW. */
	private static final String ERR_START_WORKFLOW = "activiti.engine.start.workflow.error";

	/** The Constant ERR_GET_WORKFLOW_INSTS. */
	private static final String ERR_GET_WORKFLOW_INSTS = "activiti.engine.get.workflows.error";

	/** The Constant ERR_GET_ACTIVE_WORKFLOW_INSTS. */
	private static final String ERR_GET_ACTIVE_WORKFLOW_INSTS = "activiti.engine.get.active.workflows.error";

	/** The Constant ERR_GET_COMPLETED_WORKFLOW_INSTS. */
	private static final String ERR_GET_COMPLETED_WORKFLOW_INSTS = "activiti.engine.get.completed.workflows.error";
	// private static final String ERR_GET_WORKFLOW_INST_BY_ID =
	// "activiti.engine.get.workflow.instance.by.id.error";
	// private static final String ERR_GET_PROCESS_INSTANCE =
	// "activiti.engine.get.process.instance.error";
	/** The Constant ERR_GET_WORKFLOW_PATHS. */
	private static final String ERR_GET_WORKFLOW_PATHS = "activiti.engine.get.workflow.paths.error";
	// private static final String ERR_GET_PATH_PROPERTIES =
	// "activiti.engine.get.path.properties.error";
	/** The Constant ERR_CANCEL_WORKFLOW. */
	private static final String ERR_CANCEL_WORKFLOW = "activiti.engine.cancel.workflow.error";

	/** The Constant ERR_CANCEL_UNEXISTING_WORKFLOW. */
	private static final String ERR_CANCEL_UNEXISTING_WORKFLOW = "activiti.engine.cancel.unexisting.workflow.error";

	/** The Constant ERR_DELETE_WORKFLOW. */
	private static final String ERR_DELETE_WORKFLOW = "activiti.engine.delete.workflow.error";

	/** The Constant ERR_DELETE_UNEXISTING_WORKFLOW. */
	private static final String ERR_DELETE_UNEXISTING_WORKFLOW = "activiti.engine.delete.unexisting.workflow.error";
	// private static final String ERR_SIGNAL_TRANSITION =
	// "activiti.engine.signal.transition.error";
	/** The Constant ERR_FIRE_EVENT_NOT_SUPPORTED. */
	protected static final String ERR_FIRE_EVENT_NOT_SUPPORTED = "activiti.engine.event.unsupported";
	// private static final String ERR_FIRE_EVENT =
	// "activiti.engine.fire.event.error";
	/** The Constant ERR_GET_TASKS_FOR_PATH. */
	private static final String ERR_GET_TASKS_FOR_PATH = "activiti.engine.get.tasks.for.path.error";
	// private static final String ERR_GET_TASKS_FOR_UNEXISTING_PATH =
	// "activiti.engine.get.tasks.for.unexisting.path.error";
	/** The Constant ERR_GET_TIMERS. */
	private static final String ERR_GET_TIMERS = "activiti.engine.get.timers.error";

	/** The Constant ERR_FIND_COMPLETED_TASK_INSTS. */
	protected static final String ERR_FIND_COMPLETED_TASK_INSTS = "activiti.engine.find.completed.task.instances.error";
	// private static final String ERR_COMPILE_PROCESS_DEF_zip =
	// "activiti.engine.compile.process.definition.zip.error";
	// private static final String ERR_COMPILE_PROCESS_DEF_XML =
	// "activiti.engine.compile.process.definition.xml.error";
	// private static final String ERR_COMPILE_PROCESS_DEF_UNSUPPORTED =
	// "activiti.engine.compile.process.definition.unsupported.error";
	// private static final String ERR_GET_activiti_ID =
	// "activiti.engine.get.activiti.id.error";
	/** The Constant ERR_GET_WORKFLOW_TOKEN_INVALID. */
	private static final String ERR_GET_WORKFLOW_TOKEN_INVALID = "activiti.engine.get.workflow.token.invalid";

	/** The Constant ERR_GET_WORKFLOW_TOKEN_NULL. */
	private static final String ERR_GET_WORKFLOW_TOKEN_NULL = "activiti.engine.get.workflow.token.is.null";

	/** The Constant ERR_GET_COMPANY_HOME_INVALID. */
	private static final String ERR_GET_COMPANY_HOME_INVALID = "activiti.engine.get.company.home.invalid";

	/** The Constant ERR_GET_COMPANY_HOME_MULTIPLE. */
	private static final String ERR_GET_COMPANY_HOME_MULTIPLE = "activiti.engine.get.company.home.multiple";

	// Task Component Messages
	/** The Constant ERR_GET_ASSIGNED_TASKS. */
	private static final String ERR_GET_ASSIGNED_TASKS = "activiti.engine.get.assigned.tasks.error";

	/** The Constant ERR_GET_POOLED_TASKS. */
	private static final String ERR_GET_POOLED_TASKS = "activiti.engine.get.pooled.tasks.error";
	// private static final String ERR_QUERY_TASKS =
	// "activiti.engine.query.tasks.error";
	// private static final String ERR_GET_TASK_INST =
	// "activiti.engine.get.task.instance.error";
	/** The Constant ERR_UPDATE_TASK. */
	private static final String ERR_UPDATE_TASK = "activiti.engine.update.task.error";

	/** The Constant ERR_UPDATE_TASK_UNEXISTING. */
	private static final String ERR_UPDATE_TASK_UNEXISTING = "activiti.engine.update.task.unexisting.error";

	/** The Constant ERR_UPDATE_START_TASK. */
	private static final String ERR_UPDATE_START_TASK = "activiti.engine.update.starttask.illegal.error";
	// private static final String ERR_END_TASK =
	// "activiti.engine.end.task.error";
	/** The Constant ERR_END_UNEXISTING_TASK. */
	private static final String ERR_END_UNEXISTING_TASK = "activiti.engine.end.task.unexisting.error";

	/** The Constant ERR_GET_TASK_BY_ID. */
	private static final String ERR_GET_TASK_BY_ID = "activiti.engine.get.task.by.id.error";

	/** The Constant ERR_END_TASK_INVALID_TRANSITION. */
	private static final String ERR_END_TASK_INVALID_TRANSITION = "activiti.engine.end.task.invalid.transition";

	/** The Constant QNAME_INITIATOR. */
	public static final QName QNAME_INITIATOR = QName.createQName(NamespaceService.DEFAULT_URI,
			WorkflowConstants.PROP_INITIATOR);

	/** The Constant WORKFLOW_TOKEN_SEPERATOR. */
	private final static String WORKFLOW_TOKEN_SEPERATOR = "\\$";

	/** The repo service. */
	private RepositoryService repoService;

	/** The runtime service. */
	private RuntimeService runtimeService;

	/** The task service. */
	private TaskService taskService;

	/** The history service. */
	private HistoryService historyService;

	/** The management service. */
	private ManagementService managementService;

	/** The form service. */
	private FormService formService;

	/** The activiti util. */
	private ActivitiUtil activitiUtil;

	/** The dictionary service. */
	private DictionaryService dictionaryService;

	/** The node service. */
	private NodeService nodeService;

	/** The unprotected search service. */
	private SearchService unprotectedSearchService;

	/** The person service. */
	private PersonService personService;

	/** The type converter. */
	private ActivitiTypeConverter typeConverter;

	/** The property converter. */
	private ActivitiPropertyConverter propertyConverter;

	/** The authority manager. */
	private WorkflowAuthorityManager authorityManager;

	/** The node converter. */
	private WorkflowNodeConverter nodeConverter;

	/** The factory. */
	private WorkflowObjectFactory factory;

	/** The message service. */
	private MessageService messageService;

	/** The tenant service. */
	private TenantService tenantService;

	/** The namespace service. */
	private NamespaceService namespaceService;

	/** The company home path. */
	private String companyHomePath;

	/** The company home store. */
	private StoreRef companyHomeStore;

	/** The service registry. */
	private ServiceRegistry serviceRegistry;

	/** The workflow report service provider. */
	private WorkflowReportServiceProvider workflowReportServiceProvider;

	/**
	 * Instantiates a new activiti workflow engine.
	 */
	public ActivitiWorkflowEngine() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		repoService = activitiUtil.getRepositoryService();
		runtimeService = activitiUtil.getRuntimeService();
		taskService = activitiUtil.getTaskService();
		formService = activitiUtil.getFormService();
		historyService = activitiUtil.getHistoryService();
		managementService = activitiUtil.getManagementService();
	}

	// /* (non-Javadoc)
	// * @see org.alfresco.repo.workflow.WorkflowComponent#cancelWorkflow(java.lang.String,
	// java.lang.String)
	// */
	// @Override
	// public WorkflowInstance cancelWorkflow(String workflowId, String comment) {
	// try {
	// WorkflowDefinition workflowDefinition = null;
	// String localId = createLocalId(workflowId);
	// WorkflowTaskQuery query = new WorkflowTaskQuery();
	// query.setTaskState(WorkflowTaskState.IN_PROGRESS);
	// query.setProcessId(workflowId);
	// List<WorkflowTask> activeTasks = queryTasks(query, true);
	// ProcessInstance processInstance = runtimeService
	// .createProcessInstanceQuery().processInstanceId(localId)
	// .singleResult();
	// for (WorkflowTask workflowTask : activeTasks) {
	// workflowDefinition = workflowTask.getPath()
	// .getInstance().getDefinition();
	// if (getWorkflowReportServiceProvider()
	// .getApplicableReportService(
	// workflowDefinition
	// .getId()) != null) {
	// workflowTask.state = WorkflowTaskState.COMPLETED;
	// workflowTask.getProperties().put(
	// WorkflowModel.PROP_OUTCOME, "Cancelled");
	// workflowTask.getProperties().put(WorkflowModel.PROP_STATUS,
	// "Cancelled");
	// workflowTask.getProperties().put(
	// WorkflowModel.PROP_COMMENT, comment);
	// updateTask(workflowTask.getId(),
	// workflowTask.getProperties(), null, null);
	// getWorkflowReportServiceProvider()
	// .getApplicableReportService(
	// workflowDefinition.getId())
	// .updateTask(workflowTask);
	// }
	// }
	// if (processInstance == null) {
	// throw new WorkflowException(
	// messageService
	// .getMessage(ERR_CANCEL_UNEXISTING_WORKFLOW));
	// }
	// // TODO: Cancel VS delete?
	// // Delete the process instance
	// runtimeService.deleteProcessInstance(processInstance.getId(),
	// ActivitiConstants.DELETE_REASON_CANCELLED);
	//
	// // Convert historic process instance
	// HistoricProcessInstance deletedInstance = historyService
	// .createHistoricProcessInstanceQuery()
	// .processInstanceId(processInstance.getId()).singleResult();
	// WorkflowInstance result = typeConverter.convert(deletedInstance);
	// if (getWorkflowReportServiceProvider().getApplicableReportService(
	// workflowDefinition.getId()) != null) {
	// getWorkflowReportServiceProvider().getApplicableReportService(
	// workflowDefinition.getId()).cancelWorkflow(result);
	// }
	// // Delete the historic process instance
	// // historyService.deleteHistoricProcessInstance(deletedInstance
	// // .getId());
	//
	// return result;
	// } catch (ActivitiException ae) {
	// String msg = messageService.getMessage(ERR_CANCEL_WORKFLOW);
	// throw new WorkflowException(msg, ae);
	// }
	// }

	/*
	 * (non-Javadoc)
	 * @see org.alfresco.repo.workflow.WorkflowComponent#cancelWorkflows(java.util .List)
	 */
	@Override
	public List<WorkflowInstance> cancelWorkflows(List<String> workflowIds) {
		List<WorkflowInstance> result = new ArrayList<WorkflowInstance>(workflowIds.size());
		for (String workflowId : workflowIds) {
			result.add(cancelWorkflow(workflowId));
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowInstance cancelWorkflow(String workflowId) {
		String localId = createLocalId(workflowId);
		WorkflowTaskQuery query = new WorkflowTaskQuery();
		query.setTaskState(WorkflowTaskState.IN_PROGRESS);
		query.setProcessId(workflowId);
		List<WorkflowTask> activeTasks = queryTasks(query, true);
		try {
			ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(localId).singleResult();
			if (processInstance == null) {
				throw new WorkflowException(
						messageService.getMessage(ERR_CANCEL_UNEXISTING_WORKFLOW));
			}

			// TODO: Cancel VS delete?
			// Delete the process instance
			runtimeService.deleteProcessInstance(processInstance.getId(),
					WorkflowConstants.PROP_CANCELLED);

			// Convert historic process instance
			HistoricProcessInstance deletedInstance = historyService
					.createHistoricProcessInstanceQuery()
					.processInstanceId(processInstance.getId()).singleResult();
			WorkflowInstance result = typeConverter.convert(deletedInstance);

			// CMF dont delete the process instance
			// historyService.deleteHistoricProcessInstance(deletedInstance.getId());
			for (WorkflowTask workflowTask : activeTasks) {
				if (getWorkflowReportServiceProvider().getApplicableReportService(workflowId) != null) {
					workflowTask.state = WorkflowTaskState.COMPLETED;
					getWorkflowReportServiceProvider().getApplicableReportService(workflowId)
							.updateTask(workflowTask);
				}
			}
			return result;
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_CANCEL_WORKFLOW);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowInstance deleteWorkflow(String workflowId) {
		String localId = createLocalId(workflowId);
		try {
			// Delete the runtime process instance if still running, this calls
			// the end-listeners if any
			ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(localId).singleResult();
			if (processInstance != null) {
				runtimeService.deleteProcessInstance(processInstance.getId(),
						ActivitiConstants.DELETE_REASON_DELETED);
			}

			// Convert historic process instance
			HistoricProcessInstance deletedInstance = historyService
					.createHistoricProcessInstanceQuery().processInstanceId(localId).singleResult();

			if (deletedInstance == null) {
				throw new WorkflowException(messageService.getMessage(
						ERR_DELETE_UNEXISTING_WORKFLOW, localId));
			}

			WorkflowInstance result = typeConverter.convert(deletedInstance);

			// Delete the historic process instance
			historyService.deleteHistoricProcessInstance(deletedInstance.getId());
			if (getWorkflowReportServiceProvider().getApplicableReportService(workflowId) != null) {
				getWorkflowReportServiceProvider().getApplicableReportService(workflowId)
						.deleteWorkflow(workflowId);
			}
			return result;
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_DELETE_WORKFLOW);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowDeployment deployDefinition(InputStream workflowDefinition, String mimetype) {
		return deployDefinition(workflowDefinition, mimetype, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowDeployment deployDefinition(InputStream workflowDefinition, String mimetype,
			String name) {
		try {
			String resourceName = GUID.generate() + BpmnDeployer.BPMN_RESOURCE_SUFFIXES[0];

			if (tenantService.isEnabled()) {
				name = tenantService.getName(name);
			}

			Deployment deployment = repoService.createDeployment()
					.addInputStream(resourceName, workflowDefinition).name(name).deploy();

			// No problems can be added to the WorkflowDeployment, warnings are
			// not exposed
			return typeConverter.convert(deployment);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_DEPLOY_WORKFLOW);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowPath fireEvent(String pathId, String event) {
		String message = messageService.getMessage(ERR_FIRE_EVENT_NOT_SUPPORTED);
		throw new WorkflowException(message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowInstance> getActiveWorkflows() {
		try {
			return getWorkflows(new WorkflowInstanceQuery(true));
		} catch (ActivitiException ae) {
			String message = messageService.getMessage(ERR_GET_ACTIVE_WORKFLOW_INSTS, "");
			throw new WorkflowException(message, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowInstance> getCompletedWorkflows() {
		try {
			return getWorkflows(new WorkflowInstanceQuery(false));
		} catch (ActivitiException ae) {
			String message = messageService.getMessage(ERR_GET_COMPLETED_WORKFLOW_INSTS, "");
			throw new WorkflowException(message, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowInstance> getWorkflows() {
		try {
			return getWorkflows(new WorkflowInstanceQuery());
		} catch (ActivitiException ae) {
			String message = messageService.getMessage(ERR_GET_WORKFLOW_INSTS, "");
			throw new WorkflowException(message, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowInstance> getActiveWorkflows(String workflowDefinitionId) {
		try {
			return getWorkflows(new WorkflowInstanceQuery(workflowDefinitionId, true));
		} catch (ActivitiException ae) {
			String message = messageService.getMessage(ERR_GET_ACTIVE_WORKFLOW_INSTS,
					workflowDefinitionId);
			throw new WorkflowException(message, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowDefinition> getAllDefinitions() {
		try {
			List<ProcessDefinition> definitions = repoService.createProcessDefinitionQuery().list();
			return getValidWorkflowDefinitions(definitions);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_WORKFLOW_DEF);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowDefinition> getAllDefinitionsByName(String workflowName) {
		try {
			String key = factory.getProcessKey(workflowName);
			List<ProcessDefinition> definitions = repoService.createProcessDefinitionQuery()
					.processDefinitionKey(key).list();
			return typeConverter.convert(definitions);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_ALL_DEFS_BY_NAME, workflowName);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowInstance> getCompletedWorkflows(String workflowDefinitionId) {
		try {
			return getWorkflows(new WorkflowInstanceQuery(workflowDefinitionId, false));
		} catch (ActivitiException ae) {
			String message = messageService.getMessage(ERR_GET_COMPLETED_WORKFLOW_INSTS,
					workflowDefinitionId);
			throw new WorkflowException(message, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowDefinition getDefinitionById(String workflowDefinitionId) {
		try {
			String localId = createLocalId(workflowDefinitionId);
			ProcessDefinition procDef = repoService.createProcessDefinitionQuery()
					.processDefinitionId(localId).singleResult();
			return typeConverter.convert(procDef);
		} catch (ActivitiException ae) {
			String msg = messageService
					.getMessage(ERR_GET_WORKFLOW_DEF_BY_ID, workflowDefinitionId);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowDefinition getDefinitionByName(String workflowName) {
		try {
			String key = factory.getDomainProcessKey(workflowName);
			ProcessDefinition definition = repoService.createProcessDefinitionQuery()
					.processDefinitionKey(key).latestVersion().singleResult();
			return typeConverter.convert(definition);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_WORKFLOW_DEF_BY_NAME, workflowName);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getDefinitionImage(String workflowDefinitionId) {
		try {
			String processDefinitionId = createLocalId(workflowDefinitionId);
			ProcessDefinition processDefinition = repoService.createProcessDefinitionQuery()
					.processDefinitionId(processDefinitionId).singleResult();

			if (processDefinition == null) {
				throw new WorkflowException(messageService.getMessage(ERR_GET_DEF_UNEXISTING_IMAGE,
						workflowDefinitionId));
			}

			String diagramResourceName = ((ReadOnlyProcessDefinition) processDefinition)
					.getDiagramResourceName();
			if (diagramResourceName != null) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream resourceInputStream = repoService.getResourceAsStream(
						processDefinitionId, diagramResourceName);
				// Write the resource to a ByteArrayOutpurStream
				IOUtils.copy(resourceInputStream, out);
				return out.toByteArray();
			}
			// No image was found for the process definition
			return null;
		} catch (IOException ioe) {
			String msg = messageService.getMessage(ERR_GET_DEF_IMAGE, workflowDefinitionId);
			throw new WorkflowException(msg, ioe);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_DEF_IMAGE, workflowDefinitionId);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowDefinition> getDefinitions() {
		try {
			List<ProcessDefinition> definitions = repoService.createProcessDefinitionQuery()
					.latestVersion().list();
			return getValidWorkflowDefinitions(definitions);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_WORKFLOW_DEF);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<QName, Serializable> getPathProperties(String pathId) {
		String executionId = createLocalId(pathId);
		return propertyConverter.getPathProperties(executionId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowTaskDefinition> getTaskDefinitions(String workflowDefinitionId) {
		List<WorkflowTaskDefinition> defs = new ArrayList<WorkflowTaskDefinition>();
		String processDefinitionId = createLocalId(workflowDefinitionId);

		// This should return all task definitions, including the start-task
		ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repoService)
				.getDeployedProcessDefinition(processDefinitionId);

		String processName = ((ProcessDefinition) processDefinition).getKey();
		factory.checkDomain(processName);

		// Process start task definition
		PvmActivity startEvent = processDefinition.getInitial();

		String startTaskName = null;
		StartFormData startFormData = formService.getStartFormData(processDefinition.getId());
		if (startFormData != null) {
			startTaskName = startFormData.getFormKey();
		}

		// Add start task definition
		defs.add(typeConverter.getTaskDefinition(startEvent, startTaskName,
				processDefinition.getId(), true));

		// Now, continue through process, finding all user-tasks
		Collection<PvmActivity> taskActivities = findUserTasks(startEvent);
		for (PvmActivity act : taskActivities) {
			String formKey = getFormKey(act, processDefinition);
			defs.add(typeConverter.getTaskDefinition(act, formKey, processDefinition.getId(), false));
		}

		return defs;
	}

	/**
	 * Gets the form key.
	 *
	 * @param act
	 *            the act
	 * @param processDefinition
	 *            the process definition
	 * @return the form key
	 */
	private String getFormKey(PvmActivity act, ReadOnlyProcessDefinition processDefinition) {
		if (act instanceof ActivityImpl) {
			ActivityImpl actImpl = (ActivityImpl) act;
			if (actImpl.getActivityBehavior() instanceof UserTaskActivityBehavior) {
				UserTaskActivityBehavior uta = (UserTaskActivityBehavior) actImpl
						.getActivityBehavior();
				return getFormKey(uta.getTaskDefinition());
			} else if (actImpl.getActivityBehavior() instanceof MultiInstanceActivityBehavior) {
				// Get the task-definition from the process-definition
				if (processDefinition instanceof ProcessDefinitionEntity) {
					// Task definition id is the same the the activity id
					TaskDefinition taskDef = ((ProcessDefinitionEntity) processDefinition)
							.getTaskDefinitions().get(act.getId());
					if (taskDef != null) {
						return getFormKey(taskDef);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Gets the form key.
	 *
	 * @param taskDefinition
	 *            the task definition
	 * @return the form key
	 */
	private String getFormKey(TaskDefinition taskDefinition) {
		TaskFormHandler handler = taskDefinition.getTaskFormHandler();
		if ((handler != null) && (handler instanceof DefaultTaskFormHandler)) {
			// We cast to DefaultTaskFormHandler since we do not configure our
			// own
			return ((DefaultTaskFormHandler) handler).getFormKey();
		}
		return null;
	}

	/**
	 * Checks if is receive task.
	 *
	 * @param act
	 *            the act
	 * @return true, if is receive task
	 */
	private boolean isReceiveTask(PvmActivity act) {
		if (act instanceof ActivityImpl) {
			ActivityImpl actImpl = (ActivityImpl) act;
			return (actImpl.getActivityBehavior() instanceof ReceiveTaskActivityBehavior);
		}
		return false;
	}

	/**
	 * Find user tasks.
	 *
	 * @param startEvent
	 *            the start event
	 * @return the collection
	 */
	private Collection<PvmActivity> findUserTasks(PvmActivity startEvent) {
		// Use a linked hashmap to get the task defs in the right order
		Map<String, PvmActivity> userTasks = new LinkedHashMap<String, PvmActivity>();
		Set<String> processedActivities = new HashSet<String>();

		// Start finding activities recursively
		findUserTasks(startEvent, userTasks, processedActivities);

		return userTasks.values();
	}

	/**
	 * Checks if is first activity.
	 *
	 * @param activity
	 *            the activity
	 * @param procDef
	 *            the proc def
	 * @return true, if is first activity
	 */
	private boolean isFirstActivity(PvmActivity activity, ReadOnlyProcessDefinition procDef) {
		if (procDef.getInitial().getOutgoingTransitions().size() == 1) {
			if (procDef.getInitial().getOutgoingTransitions().get(0).getDestination()
					.equals(activity)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Find user tasks.
	 *
	 * @param currentActivity
	 *            the current activity
	 * @param userTasks
	 *            the user tasks
	 * @param processedActivities
	 *            the processed activities
	 */
	private void findUserTasks(PvmActivity currentActivity, Map<String, PvmActivity> userTasks,
			Set<String> processedActivities) {
		// Only process activity if not already processed, to prevent endless
		// loops
		if (!processedActivities.contains(currentActivity.getId())) {
			processedActivities.add(currentActivity.getId());
			if (isUserTask(currentActivity)) {
				userTasks.put(currentActivity.getId(), currentActivity);
			}

			// Process outgoing transitions
			if (currentActivity.getOutgoingTransitions() != null) {
				for (PvmTransition transition : currentActivity.getOutgoingTransitions()) {
					if (transition.getDestination() != null) {
						findUserTasks(transition.getDestination(), userTasks, processedActivities);
					}
				}
			}
		}
	}

	/**
	 * Checks if is user task.
	 *
	 * @param currentActivity
	 *            the current activity
	 * @return true, if is user task
	 */
	private boolean isUserTask(PvmActivity currentActivity) {
		// TODO: Validate if this is the best way to find out an activity is a
		// usertask
		String type = (String) currentActivity.getProperty(ActivitiConstants.NODE_TYPE);
		if ((type != null) && type.equals(ActivitiConstants.USER_TASK_NODE_TYPE)) {
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowTask> getTasksForWorkflowPath(String pathId) {
		try {
			// Extract the Activiti ID from the path
			String executionId = getExecutionIdFromPath(pathId);
			if (executionId == null) {
				throw new WorkflowException(messageService.getMessage(
						ERR_GET_WORKFLOW_TOKEN_INVALID, pathId));
			}

			// Check if the execution exists
			Execution execution = runtimeService.createExecutionQuery().executionId(executionId)
					.singleResult();
			if (execution == null) {
				throw new WorkflowException(messageService.getMessage(ERR_GET_WORKFLOW_TOKEN_NULL,
						pathId));
			}

			// Check if workflow's start task has been completed. If not, return
			// the virtual task
			// Otherwise, just return the runtime Activiti tasks
			String processInstanceId = execution.getProcessInstanceId();
			ArrayList<WorkflowTask> resultList = new ArrayList<WorkflowTask>();
			if (typeConverter.isStartTaskActive(processInstanceId)) {
				resultList.add(typeConverter.getVirtualStartTask(processInstanceId, true));
			} else {
				List<Task> tasks = taskService.createTaskQuery().executionId(executionId).list();
				for (Task task : tasks) {
					resultList.add(typeConverter.convert(task));
				}
			}
			return resultList;
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_TASKS_FOR_PATH, pathId);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * Gets the execution id from path.
	 *
	 * @param workflowPath
	 *            the workflow path
	 * @return the execution id from path
	 */
	protected String getExecutionIdFromPath(String workflowPath) {
		if (workflowPath != null) {
			String[] parts = workflowPath.split(WORKFLOW_TOKEN_SEPERATOR);
			if (parts.length != 2) {
				throw new WorkflowException(messageService.getMessage(
						ERR_GET_WORKFLOW_TOKEN_INVALID, workflowPath));
			}
			return parts[1];
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowTimer> getTimers(String workflowId) {
		try {
			List<WorkflowTimer> timers = new ArrayList<WorkflowTimer>();

			String processInstanceId = createLocalId(workflowId);
			List<Job> timerJobs = managementService.createJobQuery()
					.processInstanceId(processInstanceId).timers().list();

			// Only fetch process-instance when timers are available, to prevent
			// extra unneeded query
			ProcessInstance jobsProcessInstance = null;
			if (timerJobs.size() > 0) {
				// Reuse the process-instance, is used from WorkflowPath
				// creation
				jobsProcessInstance = runtimeService.createProcessInstanceQuery()
						.processInstanceId(processInstanceId).singleResult();
			}

			// Convert the timerJobs to WorkflowTimers
			for (Job job : timerJobs) {
				Execution jobExecution = runtimeService.createExecutionQuery()
						.executionId(job.getExecutionId()).singleResult();

				WorkflowPath path = typeConverter.convert(jobExecution, jobsProcessInstance);
				WorkflowTask workflowTask = getTaskForTimer(job, jobsProcessInstance, jobExecution);

				WorkflowTimer workflowTimer = factory.createWorkflowTimer(job.getId(), job.getId(),
						job.getExceptionMessage(), job.getDuedate(), path, workflowTask);
				timers.add(workflowTimer);
			}

			return timers;

		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_TIMERS, workflowId);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * Gets the task for timer.
	 *
	 * @param job
	 *            the job
	 * @param processInstance
	 *            the process instance
	 * @param jobExecution
	 *            the job execution
	 * @return the task for timer
	 */
	private WorkflowTask getTaskForTimer(Job job, ProcessInstance processInstance,
			Execution jobExecution) {
		if (job instanceof TimerEntity) {
			ReadOnlyProcessDefinition def = activitiUtil
					.getDeployedProcessDefinition(processInstance.getProcessDefinitionId());
			List<String> activeActivityIds = runtimeService.getActiveActivityIds(jobExecution
					.getId());

			if (activeActivityIds.size() == 1) {
				PvmActivity targetActivity = def.findActivity(activeActivityIds.get(0));
				if (targetActivity != null) {
					// Only get tasks of active activity is a user-task
					String activityType = (String) targetActivity
							.getProperty(ActivitiConstants.NODE_TYPE);
					if (ActivitiConstants.USER_TASK_NODE_TYPE.equals(activityType)) {
						Task task = taskService.createTaskQuery().executionId(job.getExecutionId())
								.singleResult();
						return typeConverter.convert(task);
					}
				}
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowInstance getWorkflowById(String workflowId) {
		try {
			WorkflowInstance instance = null;

			String processInstanceId = createLocalId(workflowId);
			ProcessInstance processIntance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();

			if (processIntance != null) {
				instance = typeConverter.convert(processIntance);
			} else {
				// The process instance can be finished
				HistoricProcessInstance historicInstance = historyService
						.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId)
						.singleResult();

				if (historicInstance != null) {
					instance = typeConverter.convert(historicInstance);
				}
			}
			return instance;
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_WORKFLOW_DEF);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowPath> getWorkflowPaths(String workflowId) {
		try {
			String processInstanceId = createLocalId(workflowId);

			List<Execution> executions = runtimeService.createExecutionQuery()
					.processInstanceId(processInstanceId).list();

			return typeConverter.convertExecution(executions);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_WORKFLOW_PATHS);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowInstance> getWorkflows(String workflowDefinitionId) {
		try {
			return getWorkflows(new WorkflowInstanceQuery(workflowDefinitionId));
		} catch (ActivitiException ae) {
			String message = messageService
					.getMessage(ERR_GET_WORKFLOW_INSTS, workflowDefinitionId);
			throw new WorkflowException(message, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDefinitionDeployed(InputStream workflowDefinition, String mimetype) {
		String key = null;
		try {
			key = getProcessKey(workflowDefinition);
		} catch (Exception e) {
			throw new WorkflowException(e.getMessage(), e);
		}

		try {
			long count = repoService.createProcessDefinitionQuery().processDefinitionKey(key)
					.count();
			return count > 0;
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_IS_WORKFLOW_DEPLOYED);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * Gets the process key.
	 *
	 * @param workflowDefinition
	 *            the workflow definition
	 * @return the process key
	 * @throws Exception
	 *             the exception
	 */
	private String getProcessKey(InputStream workflowDefinition) throws Exception {
		try {
			InputSource inputSource = new InputSource(workflowDefinition);
			DOMParser parser = new DOMParser();
			parser.parse(inputSource);
			Document document = parser.getDocument();
			NodeList elemnts = document.getElementsByTagName("process");
			if (elemnts.getLength() < 1) {
				throw new IllegalArgumentException(
						"The input stream does not contain a process definition!");
			}
			NamedNodeMap attributes = elemnts.item(0).getAttributes();
			Node idAttrib = attributes.getNamedItem("id");
			if (idAttrib == null) {
				throw new IllegalAccessError("The process definition does not have an id!");
			}
			String key = idAttrib.getNodeValue();
			return factory.getDomainProcessKey(key);
		} finally {
			workflowDefinition.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowPath signal(String pathId, String transitionId) {
		String execId = createLocalId(pathId);
		Execution oldExecution = activitiUtil.getExecution(execId);
		runtimeService.signal(execId);
		Execution execution = activitiUtil.getExecution(execId);
		if (execution != null) {
			return typeConverter.convert(execution);
		}
		return typeConverter.buildCompletedPath(execId, oldExecution.getProcessInstanceId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowPath startWorkflow(String workflowDefinitionId,
			Map<QName, Serializable> parameters) {
		try {
			String currentUserName = AuthenticationUtil.getFullyAuthenticatedUser();
			Authentication.setAuthenticatedUserId(currentUserName);

			String processDefId = createLocalId(workflowDefinitionId);

			// Set start task properties. This should be done before instance is
			// started, since it's id will be used
			parameters.put(WorkflowServiceConstants.PROP_ACTIVITI_START_TASK_OUTCOME,
					"Start Workflow");
			Map<String, Object> variables = propertyConverter.getStartVariables(processDefId,
					parameters);
			variables.put(WorkflowConstants.PROP_CANCELLED, Boolean.FALSE);
			// Add company home
			Object companyHome = nodeConverter.convertNode(getCompanyHome());
			variables.put(WorkflowConstants.PROP_COMPANY_HOME, companyHome);
			// Add the initiator
			NodeRef initiator = getPersonNodeRef(currentUserName);
			if (initiator != null) {
				variables.put(WorkflowConstants.PROP_INITIATOR,
						nodeConverter.convertNode(initiator));
				// Also add the initiator home reference, if one exists
				NodeRef initiatorHome = (NodeRef) nodeService.getProperty(initiator,
						ContentModel.PROP_HOMEFOLDER);
				if (initiatorHome != null) {
					variables.put(WorkflowConstants.PROP_INITIATOR_HOME,
							nodeConverter.convertNode(initiatorHome));
				}
			}

			// Start the process-instance
			ProcessInstance instance = runtimeService.startProcessInstanceById(processDefId,
					variables);
			if (instance.isEnded()) {
				return typeConverter.buildCompletedPath(instance.getId(), instance.getId());
			} else {
				WorkflowPath path = typeConverter.convert((Execution) instance);
				endStartTaskAutomatically(path, instance);
				return path;
			}
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_START_WORKFLOW, workflowDefinitionId);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * End start task automatically.
	 *
	 * @param path
	 *            the path
	 * @param instance
	 *            the instance
	 */
	private void endStartTaskAutomatically(WorkflowPath path, ProcessInstance instance) {
		// Check if StartTask Needs to be ended automatically
		WorkflowDefinition definition = path.getInstance().getDefinition();
		TypeDefinition metadata = definition.getStartTaskDefinition().getMetadata();
		Set<QName> aspects = metadata.getDefaultAspectNames();
		if (aspects.contains(WorkflowModel.ASPECT_END_AUTOMATICALLY)) {
			String taskId = ActivitiConstants.START_TASK_PREFIX + instance.getId();
			endStartTask(taskId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void undeployDefinition(String workflowDefinitionId) {
		try {
			String procDefId = createLocalId(workflowDefinitionId);
			ProcessDefinition procDef = repoService.createProcessDefinitionQuery()
					.processDefinitionId(procDefId).singleResult();
			if (procDef == null) {
				String msg = messageService.getMessage(ERR_UNDEPLOY_WORKFLOW_UNEXISTING,
						workflowDefinitionId);
				throw new WorkflowException(msg);
			}
			String deploymentId = procDef.getDeploymentId();
			repoService.deleteDeployment(deploymentId);
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_UNDEPLOY_WORKFLOW, workflowDefinitionId);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasWorkflowImage(String workflowInstanceId) {
		boolean hasImage = false;

		String processInstanceId = createLocalId(workflowInstanceId);
		ExecutionEntity pi = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();

		// If the process is finished, there is no diagram available
		if (pi != null) {
			// Fetch the process-definition. Not using query API, since the
			// returned
			// processdefinition isn't initialized with all activities
			ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repoService)
					.getDeployedProcessDefinition(pi.getProcessDefinitionId());

			hasImage = ((processDefinition != null) && processDefinition
					.isGraphicalNotationDefined());
		}

		return hasImage;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getWorkflowImage(String workflowInstanceId) {
		String processInstanceId = createLocalId(workflowInstanceId);
		ExecutionEntity pi = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();

		// If the process is finished, there is no diagram available
		if (pi != null) {
			// Fetch the process-definition. Not using query API, since the
			// returned
			// processdefinition isn't initialized with all activities
			ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repoService)
					.getDeployedProcessDefinition(pi.getProcessDefinitionId());

			if ((processDefinition != null) && processDefinition.isGraphicalNotationDefined()) {
				return ProcessDiagramGenerator.generateDiagram(processDefinition,
						ActivitiConstants.PROCESS_INSTANCE_IMAGE_FORMAT,
						runtimeService.getActiveActivityIds(processInstanceId));
			}
		}
		return null;
	}

	/**
	 * Converts the given list of {@link ProcessDefinition}s to a list of.
	 *
	 * @param definitions
	 *            the definitions
	 * @return the valid workflow definitions {@link WorkflowDefinition}s that have a valid domain.
	 */
	private List<WorkflowDefinition> getValidWorkflowDefinitions(List<ProcessDefinition> definitions) {
		return typeConverter.filterByDomainAndConvert(definitions,
				new Function<ProcessDefinition, String>() {
					@Override
					public String apply(ProcessDefinition value) {
						return value.getKey();
					}
				});
	}

	/**
	 * Converts the given list of {@link Task}s to a list of.
	 *
	 * @param tasks
	 *            the tasks
	 * @return the valid workflow tasks {@link WorkflowTask}s that have a valid domain.
	 */
	private List<WorkflowTask> getValidWorkflowTasks(List<Task> tasks) {
		return typeConverter.filterByDomainAndConvert(tasks, new Function<Task, String>() {
			@Override
			public String apply(Task task) {
				String defId = task.getProcessDefinitionId();
				if (defId == null) {
					return task.getTaskDefinitionKey();
				}
				ProcessDefinition definition = repoService.createProcessDefinitionQuery()
						.processDefinitionId(defId).singleResult();
				return definition.getKey();
			}
		});
	}

	/**
	 * Converts the given list of {@link Task}s to a list of.
	 *
	 * @param tasks
	 *            the tasks
	 * @return the valid historic tasks {@link WorkflowTask}s that have a valid domain.
	 */
	private List<WorkflowTask> getValidHistoricTasks(List<HistoricTaskInstance> tasks) {
		return typeConverter.filterByDomainAndConvert(tasks,
				new Function<HistoricTaskInstance, String>() {
					@Override
					public String apply(HistoricTaskInstance task) {
						String defId = task.getProcessDefinitionId();
						if (defId == null) {
							return task.getTaskDefinitionKey();
						}
						ProcessDefinition definition = repoService.createProcessDefinitionQuery()
								.processDefinitionId(defId).singleResult();
						return definition.getKey();
					}
				});
	}

	/**
	 * Gets the Company Home.
	 *
	 * @return company home node ref
	 */
	private NodeRef getCompanyHome() {
		if (tenantService.isEnabled()) {
			try {
				return tenantService.getRootNode(nodeService, unprotectedSearchService,
						namespaceService, companyHomePath,
						nodeService.getRootNode(companyHomeStore));
			} catch (RuntimeException re) {
				String msg = messageService.getMessage(ERR_GET_COMPANY_HOME_INVALID,
						companyHomePath);
				throw new IllegalStateException(msg, re);
			}
		} else {
			List<NodeRef> refs = unprotectedSearchService.selectNodes(
					nodeService.getRootNode(companyHomeStore), companyHomePath, null,
					namespaceService, false);
			if (refs.size() != 1) {
				String msg = messageService.getMessage(ERR_GET_COMPANY_HOME_MULTIPLE,
						companyHomePath, refs.size());
				throw new IllegalStateException(msg);
			}
			return refs.get(0);
		}
	}

	/**
	 * Gets an Alfresco Person reference for the given name.
	 *
	 * @param name
	 *            the person name
	 * @return the Alfresco person. Returns null, if no person is found with the given name.
	 */
	private NodeRef getPersonNodeRef(String name) {
		NodeRef authority = null;
		if (name != null) {
			if (personService.personExists(name)) {
				authority = personService.getPerson(name);
			}
		}
		return authority;
	}

	/**
	 * Sets the property converter.
	 *
	 * @param propertyConverter
	 *            the propertyConverter to set
	 */
	public void setPropertyConverter(ActivitiPropertyConverter propertyConverter) {
		this.propertyConverter = propertyConverter;
	}

	/**
	 * Sets the Dictionary Service.
	 *
	 * @param dictionaryService
	 *            the new dictionary service
	 */
	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	/**
	 * Sets the Node Service.
	 *
	 * @param nodeService
	 *            the new node service
	 */
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * Sets the Company Home Path.
	 *
	 * @param companyHomePath
	 *            the new company home path
	 */
	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	/**
	 * Sets the Company Home Store.
	 *
	 * @param companyHomeStore
	 *            the new company home store
	 */
	public void setCompanyHomeStore(String companyHomeStore) {
		this.companyHomeStore = new StoreRef(companyHomeStore);
	}

	/**
	 * Set the unprotected search service - so we can find the node ref for company home when folk
	 * do not have read access to company home TODO: review use with DC.
	 *
	 * @param unprotectedSearchService
	 *            the new unprotected search service
	 */
	public void setUnprotectedSearchService(SearchService unprotectedSearchService) {
		this.unprotectedSearchService = unprotectedSearchService;
	}

	/**
	 * Sets the Person Service.
	 *
	 * @param personService
	 *            the new person service
	 */
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	/**
	 * Sets the Authority DAO /**.
	 *
	 * @param authorityManager
	 *            the authorityManager to set
	 */
	public void setAuthorityManager(WorkflowAuthorityManager authorityManager) {
		this.authorityManager = authorityManager;
	}

	// /////////// Task Component //////////

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowTask endTask(String taskId, String transition) {
		String localTaskId = createLocalId(taskId);
		// Check if the task is a virtual start task
		if (localTaskId.startsWith(ActivitiConstants.START_TASK_PREFIX)) {
			return endStartTask(localTaskId);
		}

		return endNormalTask(taskId, localTaskId, transition);
	}

	/**
	 * End normal task.
	 *
	 * @param taskId
	 *            the task id
	 * @param localTaskId
	 *            the local task id
	 * @param transition
	 *            the transition
	 * @return the workflow task
	 */
	private WorkflowTask endNormalTask(String taskId, String localTaskId, String transition) {
		// Retrieve task
		Task task = taskService.createTaskQuery().taskId(localTaskId).singleResult();

		if (task == null) {
			String msg = messageService.getMessage(ERR_END_UNEXISTING_TASK, taskId);
			throw new WorkflowException(msg);
		}

		// Check if the assignee is equal to the current logged-in user. If not,
		// assign task before ending
		String currentUserName = AuthenticationUtil.getFullyAuthenticatedUser();
		if ((task.getAssignee() == null) || !task.getAssignee().equals(currentUserName)) {
			taskService.setAssignee(localTaskId, currentUserName);
			// Also update pojo used to set the outcome, this will read assignee
			// as wel
			task.setAssignee(currentUserName);
		}

		setOutcome(task, transition);
		taskService.complete(localTaskId);
		// The task should have a historicTaskInstance
		HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
				.taskId(task.getId()).singleResult();
		return typeConverter.convert(historicTask);
	}

	/**
	 * Sets the outcome.
	 *
	 * @param task
	 *            the task
	 * @param transition
	 *            the transition
	 */
	private void setOutcome(Task task, String transition) {
		String outcomeValue = ActivitiConstants.DEFAULT_TRANSITION_NAME;
		HashMap<QName, Serializable> updates = new HashMap<QName, Serializable>();

		boolean isDefaultTransition = (transition == null)
				|| ActivitiConstants.DEFAULT_TRANSITION_NAME.equals(transition);

		Map<QName, Serializable> properties = propertyConverter.getTaskProperties(task);
		QName outcomePropName = (QName) properties.get(WorkflowModel.PROP_OUTCOME_PROPERTY_NAME);
		if (outcomePropName != null) {
			if (isDefaultTransition == false) {
				outcomeValue = transition;
				Serializable transitionValue = propertyConverter.convertValueToPropertyType(task,
						transition, outcomePropName);
				updates.put(outcomePropName, transitionValue);
			} else {
				Serializable rawOutcome = properties.get(outcomePropName);
				if (rawOutcome != null) {
					outcomeValue = DefaultTypeConverter.INSTANCE.convert(String.class, rawOutcome);
				}
			}
		} else if (isDefaultTransition == false) {
			// Only 'Next' is supported as transition.
			String taskId = createGlobalId(task.getId());
			String msg = messageService.getMessage(ERR_END_TASK_INVALID_TRANSITION, transition,
					taskId, ActivitiConstants.DEFAULT_TRANSITION_NAME);
			throw new WorkflowException(msg);
		}
		updates.put(WorkflowModel.PROP_OUTCOME, outcomeValue);
		propertyConverter.updateTask(task, updates, null, null);
	}

	/**
	 * End start task.
	 *
	 * @param localTaskId
	 *            the local task id
	 * @return the workflow task
	 */
	private WorkflowTask endStartTask(String localTaskId) {
		// We don't end a task, we set a variable on the process-instance
		// to indicate that it's started
		String processInstanceId = localTaskId.replace(ActivitiConstants.START_TASK_PREFIX, "");
		if (false == typeConverter.isStartTaskActive(processInstanceId)) {
			return typeConverter.getVirtualStartTask(processInstanceId, false);
		}

		// Set start task end date on the process
		runtimeService.setVariable(processInstanceId, ActivitiConstants.PROP_START_TASK_END_DATE,
				new Date());

		// Check if the current activity is a signalTask and the first activity
		// in the process,
		// this is a workaround for processes without any task/waitstates that
		// should otherwise end
		// when they are started.
		ProcessInstance processInstance = activitiUtil.getProcessInstance(processInstanceId);
		String currentActivity = ((ExecutionEntity) processInstance).getActivityId();

		ReadOnlyProcessDefinition procDef = activitiUtil
				.getDeployedProcessDefinition(processInstance.getProcessDefinitionId());
		PvmActivity activity = procDef.findActivity(currentActivity);
		if (isReceiveTask(activity) && isFirstActivity(activity, procDef)) {
			// Signal the process to start flowing, beginning from the recieve
			// task
			runtimeService.signal(processInstanceId);

			// It's possible the process has ended after signalling the receive
			// task
		}
		// Return virtual start task for the execution, it's safe to use the
		// processInstanceId
		return typeConverter.getVirtualStartTask(processInstanceId, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowTask> getAssignedTasks(String authority, WorkflowTaskState state) {
		try {
			if (state == WorkflowTaskState.IN_PROGRESS) {
				List<Task> tasks = taskService.createTaskQuery().taskAssignee(authority).list();
				return typeConverter.convert(tasks);
			} else {
				List<HistoricTaskInstance> historicTasks = historyService
						.createHistoricTaskInstanceQuery().taskAssignee(authority).finished()
						.list();
				return typeConverter.convert(historicTasks);
			}
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_ASSIGNED_TASKS);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowTask> getPooledTasks(List<String> authorities) {
		try {
			if ((authorities != null) && (authorities.size() > 0)) {
				// As an optimisation, we assume the first authority CAN be a
				// user. All the
				// others are groups to which the user (or group) belongs. This
				// way, we don't have to
				// check for the type of the authority.

				String firstAuthority = authorities.get(0);
				// Use a map, can be that a task has multiple candidate-groups,
				// which are inside the list
				// of authorities
				Map<String, Task> resultingTasks = new HashMap<String, Task>();
				if (authorityManager.isUser(firstAuthority)) {
					// Candidate user
					addTasksForCandidateUser(firstAuthority, resultingTasks);
					if (authorities.size() > 1) {
						List<String> remainingAuthorities = authorities.subList(1,
								authorities.size());
						addTasksForCandidateGroups(remainingAuthorities, resultingTasks);
					}
				} else {
					// Candidate group
					addTasksForCandidateGroups(authorities, resultingTasks);
				}

				List<Task> tasks = new ArrayList<Task>();
				// Only tasks that have NO assignee, should be returned
				for (Task task : resultingTasks.values()) {
					if (task.getAssignee() == null) {
						tasks.add(task);
					}
				}
				return typeConverter.convert(tasks);
			}

			return Collections.emptyList();
		} catch (ActivitiException ae) {
			String authorityString = null;
			if (authorities != null) {
				authorityString = StringUtils.join(authorities.iterator(), ", ");
			}
			String msg = messageService.getMessage(ERR_GET_POOLED_TASKS, authorityString);
			throw new WorkflowException(msg, ae);
		}
	}

	/**
	 * Adds the tasks for candidate groups.
	 *
	 * @param groupNames
	 *            the group names
	 * @param resultingTasks
	 *            the resulting tasks
	 */
	private void addTasksForCandidateGroups(List<String> groupNames,
			Map<String, Task> resultingTasks) {
		if ((groupNames != null) && (groupNames.size() > 0)) {
			List<Task> tasks = taskService.createTaskQuery().taskCandidateGroupIn(groupNames)
					.list();
			for (Task task : tasks) {
				resultingTasks.put(task.getId(), task);
			}
		}
	}

	/**
	 * Adds the tasks for candidate user.
	 *
	 * @param userName
	 *            the user name
	 * @param resultingTasks
	 *            the resulting tasks
	 */
	private void addTasksForCandidateUser(String userName, Map<String, Task> resultingTasks) {
		List<Task> tasks = taskService.createTaskQuery().taskCandidateUser(userName).list();
		for (Task task : tasks) {
			resultingTasks.put(task.getId(), task);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowTask getTaskById(String taskId) {
		try {
			String localId = createLocalId(taskId);
			if (localId.startsWith(ActivitiConstants.START_TASK_PREFIX)) {
				String processInstanceId = localId.replace(ActivitiConstants.START_TASK_PREFIX, "");
				return typeConverter.getVirtualStartTask(processInstanceId, null);
			} else {
				Task task = activitiUtil.getTaskInstance(localId);
				if (task != null) {
					return typeConverter.convert(task);
				}
				HistoricTaskInstance historicTask = activitiUtil.getHistoricTaskInstance(localId);
				return typeConverter.convert(historicTask);
			}
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_GET_TASK_BY_ID);
			throw new WorkflowException(msg, ae);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.alfresco.repo.workflow.TaskComponent#queryTasks(org.alfresco.service
	 * .cmr.workflow.WorkflowTaskQuery, boolean)
	 */
	@Override
	public List<WorkflowTask> queryTasks(WorkflowTaskQuery query, boolean sameSession) {
		return queryTasks(query);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<WorkflowTask> queryTasks(WorkflowTaskQuery query) {
		ArrayList<WorkflowTask> result = new ArrayList<WorkflowTask>();
		WorkflowTaskState taskState = query.getTaskState();
		if (WorkflowTaskState.COMPLETED.equals(taskState) == false) {
			result.addAll(queryRuntimeTasks(query));
		}

		// Depending on the state, history should be included/excluded as wel
		if (WorkflowTaskState.IN_PROGRESS.equals(taskState) == false) {
			result.addAll(queryHistoricTasks(query));
			result.addAll(queryStartTasks(query));
		}
		return result;
	}

	/**
	 * Query runtime tasks.
	 *
	 * @param query
	 *            the query
	 * @return the list
	 */
	private List<WorkflowTask> queryRuntimeTasks(WorkflowTaskQuery query) {
		// Runtime-tasks only exist on process-instances that are active
		// so no use in querying runtime tasks if not active
		if (!Boolean.FALSE.equals(query.isActive())) {
			// Add task name
			TaskQuery taskQuery = taskService.createTaskQuery();
			if (query.getTaskName() != null) {
				// Task 'key' is stored as variable on task
				String formKey = query.getTaskName().toPrefixString(namespaceService);
				taskQuery.taskVariableValueEquals(ActivitiConstants.PROP_TASK_FORM_KEY, formKey);
			}

			if (query.getProcessId() != null) {
				String processInstanceId = createLocalId(query.getProcessId());
				taskQuery.processInstanceId(processInstanceId);
			}

			if (query.getProcessName() != null) {
				String processName = getProcessNameMTSafe(query.getProcessName());
				taskQuery.processDefinitionKey(processName);
			}

			if (query.getWorkflowDefinitionName() != null) {
				String processName = factory.getProcessKey(query.getWorkflowDefinitionName());
				taskQuery.processDefinitionKey(processName);
			}

			if (query.getActorId() != null) {
				taskQuery.taskAssignee(query.getActorId());
			}

			if (query.getTaskId() != null) {
				String taskId = createLocalId(query.getTaskId());
				taskQuery.taskId(taskId);
			}

			// Custom task properties
			if (query.getTaskCustomProps() != null) {
				addTaskPropertiesToQuery(query.getTaskCustomProps(), taskQuery);
			}

			if (query.getProcessCustomProps() != null) {
				addProcessPropertiesToQuery(query.getProcessCustomProps(), taskQuery);
			}
			// Add ordering
			if (query.getOrderBy() != null) {
				WorkflowTaskQuery.OrderBy[] orderBy = query.getOrderBy();
				orderQuery(taskQuery, orderBy);
			}

			List<Task> results;
			int limit = query.getLimit();
			if (limit > 0) {
				results = taskQuery.listPage(0, limit);
			} else {
				results = taskQuery.list();
			}
			List<WorkflowTask> validWorkflowTasks = getValidWorkflowTasks(results);
			for (WorkflowTask workflowTask : validWorkflowTasks) {
				if (workflowTask.getPath() == null) {
					continue;
				}
				WorkflowDefinition workflowDefinition = workflowTask.getPath().getInstance()
						.getDefinition();
				if (getWorkflowReportServiceProvider().getApplicableReportService(
						workflowDefinition.getId()) != null) {
					getWorkflowReportServiceProvider().getApplicableReportService(
							workflowDefinition.getId()).createIfNotExists(workflowTask);
				}
			}
			return validWorkflowTasks;
		}
		return new ArrayList<WorkflowTask>();
	}

	/**
	 * Adds the process properties to query.
	 *
	 * @param processCustomProps
	 *            the process custom props
	 * @param taskQuery
	 *            the task query
	 */
	private void addProcessPropertiesToQuery(Map<QName, Object> processCustomProps,
			TaskQuery taskQuery) {
		for (Entry<QName, Object> customProperty : processCustomProps.entrySet()) {
			String name = factory.mapQNameToName(customProperty.getKey());

			// Perform minimal property conversions
			Object converted = propertyConverter.convertPropertyToValue(customProperty.getValue());
			taskQuery.processVariableValueEquals(name, converted);
		}
	}

	/**
	 * Gets the process name mt safe.
	 *
	 * @param processNameQName
	 *            the process name q name
	 * @return the process name mt safe
	 */
	private String getProcessNameMTSafe(QName processNameQName) {
		String key = processNameQName.toPrefixString(namespaceService);
		return factory.getProcessKey(key);
	}

	/**
	 * Order query.
	 *
	 * @param taskQuery
	 *            the task query
	 * @param orderBy
	 *            the order by
	 */
	private void orderQuery(TaskQuery taskQuery, OrderBy[] orderBy) {
		for (WorkflowTaskQuery.OrderBy orderByPart : orderBy) {
			if (orderByPart == WorkflowTaskQuery.OrderBy.TaskActor_Asc) {
				taskQuery.orderByTaskAssignee().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskActor_Desc) {
				taskQuery.orderByTaskAssignee().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskCreated_Asc) {
				taskQuery.orderByTaskCreateTime().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskCreated_Desc) {
				taskQuery.orderByTaskCreateTime().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskDue_Asc) {
				// TODO: order by dueDate? It's a task-variable
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskDue_Desc) {
				// TODO: order by duedate? It's a task-variable
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskId_Asc) {
				taskQuery.orderByTaskId().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskId_Desc) {
				taskQuery.orderByTaskId().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskName_Asc) {
				taskQuery.orderByTaskName().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskName_Desc) {
				taskQuery.orderByTaskName().desc();
			}
			// All workflows are active, no need to order on
			// WorkflowTaskQuery.OrderBy.TaskState_Asc
		}
	}

	/**
	 * Order query.
	 *
	 * @param taskQuery
	 *            the task query
	 * @param orderBy
	 *            the order by
	 */
	private void orderQuery(HistoricTaskInstanceQuery taskQuery, OrderBy[] orderBy) {
		for (WorkflowTaskQuery.OrderBy orderByPart : orderBy) {
			if (orderByPart == WorkflowTaskQuery.OrderBy.TaskActor_Asc) {
				taskQuery.orderByTaskAssignee().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskActor_Desc) {
				taskQuery.orderByTaskAssignee().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskCreated_Asc) {
				taskQuery.orderByHistoricActivityInstanceStartTime().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskCreated_Desc) {
				taskQuery.orderByHistoricActivityInstanceStartTime().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskDue_Asc) {
				// TODO: order by dueDate? It's a task-variable
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskDue_Desc) {
				// TODO: order by duedate? It's a task-variable
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskId_Asc) {
				taskQuery.orderByTaskId().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskId_Desc) {
				taskQuery.orderByTaskId().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskName_Asc) {
				taskQuery.orderByTaskName().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskName_Desc) {
				taskQuery.orderByTaskName().desc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskState_Asc) {
				taskQuery.orderByHistoricTaskInstanceEndTime().asc();
			} else if (orderByPart == WorkflowTaskQuery.OrderBy.TaskState_Desc) {
				taskQuery.orderByHistoricTaskInstanceEndTime().asc();
			}
		}
	}

	/**
	 * Adds the task properties to query.
	 *
	 * @param taskCustomProps
	 *            the task custom props
	 * @param taskQuery
	 *            the task query
	 */
	private void addTaskPropertiesToQuery(Map<QName, Object> taskCustomProps, TaskQuery taskQuery) {
		for (Entry<QName, Object> customProperty : taskCustomProps.entrySet()) {
			String name = factory.mapQNameToName(customProperty.getKey());

			// Perform minimal property conversions
			Object converted = propertyConverter.convertPropertyToValue(customProperty.getValue());
			taskQuery.taskVariableValueEquals(name, converted);
		}
	}

	/**
	 * Query historic tasks.
	 *
	 * @param query
	 *            the query
	 * @return the list
	 */
	private List<WorkflowTask> queryHistoricTasks(WorkflowTaskQuery query) {
		HistoricTaskInstanceQuery historicQuery = historyService.createHistoricTaskInstanceQuery()
				.finished();

		if (query.getTaskId() != null) {
			String taskId = createLocalId(query.getTaskId());
			historicQuery.taskId(taskId);
		}

		if (query.getProcessId() != null) {
			String processInstanceId = createLocalId(query.getProcessId());
			historicQuery.processInstanceId(processInstanceId);
		}

		if (query.getTaskName() != null) {
			historicQuery.taskDefinitionKey(query.getTaskName().toPrefixString());
		}

		if (query.getActorId() != null) {
			historicQuery.taskAssignee(query.getActorId());
		}

		if (query.getProcessName() != null) {
			String processName = getProcessNameMTSafe(query.getProcessName());
			historicQuery.processDefinitionKey(processName);
		}

		if (query.getWorkflowDefinitionName() != null) {
			String processName = factory.getProcessKey(query.getWorkflowDefinitionName());
			historicQuery.processDefinitionKey(processName);
		}

		if (query.getTaskCustomProps() != null) {
			addTaskPropertiesToQuery(query.getTaskCustomProps(), historicQuery);
		}

		if (query.getProcessCustomProps() != null) {
			addProcessPropertiesToQuery(query.getProcessCustomProps(), historicQuery);
		}

		if (query.isActive() != null) {
			if (query.isActive()) {
				historicQuery.processUnfinished();
			} else {
				historicQuery.processFinished();
			}
		}

		// Order query
		if (query.getOrderBy() != null) {
			orderQuery(historicQuery, query.getOrderBy());
		}

		List<HistoricTaskInstance> results;
		int limit = query.getLimit();
		if (limit > 0) {
			results = historicQuery.listPage(0, limit);
		} else {
			results = historicQuery.list();
		}
		return getValidHistoricTasks(results);
	}

	/**
	 * Adds the task properties to query.
	 *
	 * @param taskCustomProps
	 *            the task custom props
	 * @param taskQuery
	 *            the task query
	 */
	private void addTaskPropertiesToQuery(Map<QName, Object> taskCustomProps,
			HistoricTaskInstanceQuery taskQuery) {
		for (Entry<QName, Object> customProperty : taskCustomProps.entrySet()) {
			String name = factory.mapQNameToName(customProperty.getKey());

			// Perform minimal property conversions
			Object converted = propertyConverter.convertPropertyToValue(customProperty.getValue());
			taskQuery.taskVariableValueEquals(name, converted);
		}
	}

	/**
	 * Adds the process properties to query.
	 *
	 * @param processCustomProps
	 *            the process custom props
	 * @param taskQuery
	 *            the task query
	 */
	private void addProcessPropertiesToQuery(Map<QName, Object> processCustomProps,
			HistoricTaskInstanceQuery taskQuery) {
		for (Entry<QName, Object> customProperty : processCustomProps.entrySet()) {
			String name = factory.mapQNameToName(customProperty.getKey());

			// Perform minimal property conversions
			Object converted = propertyConverter.convertPropertyToValue(customProperty.getValue());
			taskQuery.processVariableValueEquals(name, converted);
		}
	}

	/**
	 * Query start tasks.
	 *
	 * @param query
	 *            the query
	 * @return the list
	 */
	private List<WorkflowTask> queryStartTasks(WorkflowTaskQuery query) {
		List<WorkflowTask> startTasks = new ArrayList<WorkflowTask>();

		String processInstanceId = null;
		String taskId = query.getTaskId();
		if (taskId != null) {
			String localTaskId = createLocalId(taskId);
			if (localTaskId.startsWith(ActivitiConstants.START_TASK_PREFIX)) {
				processInstanceId = localTaskId.substring(ActivitiConstants.START_TASK_PREFIX
						.length());
			}
		} else {
			String processId = query.getProcessId();
			if (processId != null) {
				// Start task for a specific process
				processInstanceId = createLocalId(processId);
			}
		}

		// Only return start-task when a process or task id is set
		if (processInstanceId != null) {
			WorkflowTask workflowTask = typeConverter.getVirtualStartTask(processInstanceId, null);
			if (workflowTask != null) {
				boolean startTaskMatches = isStartTaskMatching(workflowTask, query);
				if (startTaskMatches) {
					startTasks.add(workflowTask);
				}
			}
		}
		return startTasks;
	}

	/**
	 * Checks if is start task matching.
	 *
	 * @param workflowTask
	 *            the workflow task
	 * @param query
	 *            the query
	 * @return true, if is start task matching
	 */
	private boolean isStartTaskMatching(WorkflowTask workflowTask, WorkflowTaskQuery query) {
		if (query.isActive() != null) {
			if (query.isActive() && !workflowTask.getPath().isActive()) {
				return false;
			}
			if (!query.isActive() && workflowTask.getPath().isActive()) {
				return false;
			}
		}

		if ((query.getActorId() != null)
				&& !query.getActorId().equals(
						workflowTask.getProperties().get(ContentModel.PROP_OWNER))) {
			return false;
		}

		if (query.getProcessCustomProps() != null) {
			// Get properties for process instance, based on path of start task,
			// which is process-instance
			Map<QName, Serializable> props = getPathProperties(workflowTask.getPath().getId());
			if (!checkPropertiesPresent(query.getProcessCustomProps(), props)) {
				return false;
			}
		}

		if (query.getProcessId() != null) {
			if (!query.getProcessId().equals(workflowTask.getPath().getInstance().getId())) {
				return false;
			}
		}

		// Query by process name deprecated, but still implemented.
		if (query.getProcessName() != null) {
			String processName = factory.mapQNameToName(query.getProcessName());
			if (!processName.equals(workflowTask.getPath().getInstance().getDefinition().getName())) {
				return false;
			}
		}

		if (query.getWorkflowDefinitionName() != null) {
			if (!query.getWorkflowDefinitionName().equals(
					workflowTask.getPath().getInstance().getDefinition().getName())) {
				return false;
			}
		}

		if (query.getTaskCustomProps() != null) {
			if (!checkPropertiesPresent(query.getTaskCustomProps(), workflowTask.getProperties())) {
				return false;
			}
		}

		if (query.getTaskId() != null) {
			if (!query.getTaskId().equals(workflowTask.getId())) {
				return false;
			}
		}

		if (query.getTaskName() != null) {
			if (!query.getTaskName().equals(workflowTask.getDefinition().getMetadata().getName())) {
				return false;
			}
		}

		if (query.getTaskState() != null) {
			if (!query.getTaskState().equals(workflowTask.getState())) {
				return false;
			}
		}

		// If we fall through, start task matches the query
		return true;
	}

	/**
	 * Check properties present.
	 *
	 * @param expectedProperties
	 *            the expected properties
	 * @param props
	 *            the props
	 * @return true, if successful
	 */
	private boolean checkPropertiesPresent(Map<QName, Object> expectedProperties,
			Map<QName, Serializable> props) {
		for (Map.Entry<QName, Object> entry : expectedProperties.entrySet()) {
			if (props.containsKey(entry.getKey())) {
				Object requiredValue = entry.getValue();
				Object actualValue = props.get(entry.getKey());

				if (requiredValue != null) {
					if (!requiredValue.equals(actualValue)) {
						return false;
					}
					break;
				} else {
					if (actualValue != null) {
						return false;
					}
					break;
				}
			}
			if (entry.getValue() != null) {
				// If variable is not found and required value is non null,
				// start-task doesn't match
				return false;
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.alfresco.repo.workflow.TaskComponent#getStartTasks(java.util.List, boolean)
	 */
	@Override
	public List<WorkflowTask> getStartTasks(List<String> workflowInstanceIds, boolean sameSession) {
		List<WorkflowTask> result = new ArrayList<WorkflowTask>(workflowInstanceIds.size());
		for (String workflowInstanceId : workflowInstanceIds) {
			WorkflowTask startTask = getStartTask(workflowInstanceId);
			if (startTask != null) {
				result.add(startTask);
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowTask getStartTask(String workflowInstanceId) {
		String instanceId = createLocalId(workflowInstanceId);
		return typeConverter.getVirtualStartTask(instanceId, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowTask startTask(String taskId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */

	@Override
	public WorkflowTask suspendTask(String taskId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowTask updateTask(String taskId, Map<QName, Serializable> properties,
			Map<QName, List<NodeRef>> add, Map<QName, List<NodeRef>> remove) {
		try {
			if (taskId.startsWith(ActivitiConstants.START_TASK_PREFIX)) {
				// Known limitation, start-tasks cannot be updated
				String msg = messageService.getMessage(ERR_UPDATE_START_TASK, taskId);
				throw new WorkflowException(msg);
			}
			String createLocalId = createLocalId(taskId);
			Task task = taskService.createTaskQuery().taskId(createLocalId).singleResult();
			if (task != null) {
				Task updatedTask = propertyConverter.updateTask(task, properties, add, remove);
				WorkflowTask convertedTask = typeConverter.convert(updatedTask);
				WorkflowDefinition workflowDefinition = convertedTask.getPath().getInstance()
						.getDefinition();
				if (getWorkflowReportServiceProvider().getApplicableReportService(
						workflowDefinition.getId()) != null) {
					getWorkflowReportServiceProvider().getApplicableReportService(
							workflowDefinition.getId()).updateTask(convertedTask);
				}
				return convertedTask;
			} else {
				String msg = messageService.getMessage(ERR_UPDATE_TASK_UNEXISTING, taskId);
				throw new WorkflowException(msg);
			}
		} catch (ActivitiException ae) {
			String msg = messageService.getMessage(ERR_UPDATE_TASK, taskId);
			throw new WorkflowException(msg, ae);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.alfresco.repo.workflow.WorkflowComponent#getWorkflows(org.alfresco
	 * .service.cmr.workflow.WorkflowInstanceQuery)
	 */
	@Override
	public List<WorkflowInstance> getWorkflows(WorkflowInstanceQuery workflowInstanceQuery) {
		LinkedList<WorkflowInstance> results = new LinkedList<WorkflowInstance>();
		if (Boolean.FALSE.equals(workflowInstanceQuery.getActive()) == false) {
			// Add active.
			results.addAll(getWorkflowsInternal(workflowInstanceQuery, true));
		}
		if (Boolean.TRUE.equals(workflowInstanceQuery.getActive()) == false) {
			// Add complete
			results.addAll(getWorkflowsInternal(workflowInstanceQuery, false));
		}

		return results;
	}

	/**
	 * Gets the workflows internal.
	 *
	 * @param workflowInstanceQuery
	 *            the workflow instance query
	 * @param isActive
	 *            the is active
	 * @return the workflows internal
	 */
	@SuppressWarnings("unchecked")
	private List<WorkflowInstance> getWorkflowsInternal(
			WorkflowInstanceQuery workflowInstanceQuery, boolean isActive) {
		String processDefId = workflowInstanceQuery.getWorkflowDefinitionId() == null ? null
				: createLocalId(workflowInstanceQuery.getWorkflowDefinitionId());
		LinkedList<WorkflowInstance> results = new LinkedList<WorkflowInstance>();

		HistoricProcessInstanceQuery query;
		if (isActive) {
			// Don't use ProcessInstanceQuery here because in any case they will
			// be converted to WorkflowInstance thro HistoricProcessInstance.
			query = historyService.createHistoricProcessInstanceQuery().unfinished();
		} else {
			query = historyService.createHistoricProcessInstanceQuery().finished();
		}

		if (processDefId != null) {
			query = query.processDefinitionId(processDefId);
		}

		if (workflowInstanceQuery.getExcludedDefinitions() != null) {
			List<String> exDefIds = new ArrayList<String>();
			for (String excludedDef : workflowInstanceQuery.getExcludedDefinitions()) {
				String exDef = createLocalId(excludedDef);
				exDef = exDef.replaceAll("\\*", "%");
				exDefIds.add(exDef);
			}

			if (exDefIds.size() > 0) {
				query.processDefinitionKeyNotIn(exDefIds);
			}
		}

		// Check start range
		if (workflowInstanceQuery.getStartBefore() != null) {
			query.startedBefore(workflowInstanceQuery.getStartBefore());

		}
		if (workflowInstanceQuery.getStartAfter() != null) {
			query.startedAfter(workflowInstanceQuery.getStartAfter());
		}

		// check end range
		if (workflowInstanceQuery.getEndBefore() != null) {
			query.finishedBefore(workflowInstanceQuery.getEndBefore());
		}
		if (workflowInstanceQuery.getEndAfter() != null) {
			query.finishedAfter(workflowInstanceQuery.getEndAfter());
		}

		if (workflowInstanceQuery.getCustomProps() != null) {
			Map<QName, Object> customProps = workflowInstanceQuery.getCustomProps();

			// CLOUD-667: Extract initiator-property and use 'startedBy' instead
			Object initiatorObject = customProps.get(QNAME_INITIATOR);
			if ((initiatorObject != null) && (initiatorObject instanceof NodeRef)) {
				// Extract username from person-node
				NodeRef initiator = (NodeRef) initiatorObject;
				if (nodeService.exists(initiator)) {
					String initiatorUserName = (String) nodeService.getProperty(initiator,
							ContentModel.PROP_USERNAME);
					query.startedBy(initiatorUserName);

					// Clone properties map and remove initiator
					customProps = new HashMap<QName, Object>();
					customProps.putAll(workflowInstanceQuery.getCustomProps());
					customProps.remove(QNAME_INITIATOR);
				}
			}

			for (Map.Entry<QName, Object> prop : customProps.entrySet()) {
				String propertyName = factory.mapQNameToName(prop.getKey());
				if (prop.getValue() == null) {
					query.variableValueEquals(propertyName, null);
				} else {
					PropertyDefinition propertyDefinition = dictionaryService.getProperty(prop
							.getKey());
					if (propertyDefinition == null) {
						Object converted = propertyConverter
								.convertPropertyToValue(prop.getValue());
						query.variableValueEquals(propertyName, converted);
					} else {
						String propertyType = propertyDefinition.getDataType().getJavaClassName();
						if (propertyType.equals("java.util.Date")) {
							Map<DatePosition, Date> dateProps = (Map<DatePosition, Date>) prop
									.getValue();
							for (Map.Entry<DatePosition, Date> dateProp : dateProps.entrySet()) {
								if (dateProp.getValue() != null) {
									if (dateProp.getKey() == DatePosition.BEFORE) {
										query.variableValueLessThanOrEqual(propertyName,
												dateProp.getValue());
									}
									if (dateProp.getKey() == DatePosition.AFTER) {
										query.variableValueGreaterThanOrEqual(propertyName,
												dateProp.getValue());
									}
								}
							}
						} else {
							Object convertedValue = DefaultTypeConverter.INSTANCE.convert(
									propertyDefinition.getDataType(), prop.getValue());
							query.variableValueEquals(propertyName, convertedValue);
						}
					}
				}
			}
		}

		List<HistoricProcessInstance> completedInstances = query.list();
		List<WorkflowInstance> completedResults = typeConverter.convert(completedInstances);
		results.addAll(completedResults);
		return results;
	}

	/**
	 * Sets the node converter.
	 *
	 * @param nodeConverter
	 *            the nodeConverter to set
	 */
	public void setNodeConverter(WorkflowNodeConverter nodeConverter) {
		this.nodeConverter = nodeConverter;
	}

	/**
	 * Sets the factory.
	 *
	 * @param factory
	 *            the factory to set
	 */
	public void setFactory(WorkflowObjectFactory factory) {
		this.factory = factory;
	}

	/**
	 * Sets the message service.
	 *
	 * @param messageService
	 *            the messageService to set
	 */
	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}

	/**
	 * Sets the tenant service.
	 *
	 * @param tenantService
	 *            the tenantService to set
	 */
	public void setTenantService(TenantService tenantService) {
		this.tenantService = tenantService;
	}

	/**
	 * Sets the type converter.
	 *
	 * @param typeConverter
	 *            the typeConverter to set
	 */
	public void setTypeConverter(ActivitiTypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Sets the activiti util.
	 *
	 * @param activitiUtil
	 *            the activitiUtil to set
	 */
	public void setActivitiUtil(ActivitiUtil activitiUtil) {
		this.activitiUtil = activitiUtil;
	}

	/**
	 * Sets the namespace service.
	 *
	 * @param namespaceService
	 *            the namespaceService to set
	 */
	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	/**
	 * Gets the workflow report service provider.
	 *
	 * @return the workflow report service provider
	 */
	public WorkflowReportServiceProvider getWorkflowReportServiceProvider() {
		if (workflowReportServiceProvider == null) {
			BeanFactoryLocator instance = new JbpmFactoryLocator();
			BeanFactoryReference useBeanFactory = instance.useBeanFactory(null);
			workflowReportServiceProvider = (WorkflowReportServiceProvider) useBeanFactory
					.getFactory().getBean("workflowReportServiceProvider");
		}
		return workflowReportServiceProvider;
	}

	/**
	 * Sets the service registry.
	 *
	 * @param serviceRegistry
	 *            the new service registry
	 */
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
