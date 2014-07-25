package com.sirma.itt.cmf.domain;

import java.util.Map;

import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionRef;
import com.sirma.itt.cmf.beans.definitions.SectionDefinition;
import com.sirma.itt.cmf.beans.definitions.TaskDefinition;
import com.sirma.itt.cmf.beans.definitions.TaskDefinitionRef;
import com.sirma.itt.cmf.beans.definitions.WorkflowDefinition;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.emf.definition.dao.AllowedChildTypeMappingExtension;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.util.CollectionUtils;

/**
 * Default type mappings for CMF classes as case, workflow and task.
 *
 * @author BBonev
 */
@Extension(target = AllowedChildTypeMappingExtension.TARGET_NAME, order = 10)
public class CmfAllowedChildTypeMappingExtension implements AllowedChildTypeMappingExtension {

	/** The Constant definitionMapping. */
	private static final Map<String, Class<? extends DefinitionModel>> definitionMapping;
	/** The Constant instanceMapping. */
	private static final Map<String, Class<? extends Instance>> instanceMapping;
	/** The Constant typeMapping. */
	private static final Map<String, String> typeMapping;

	static {
		definitionMapping = CollectionUtils.createHashMap(10);
		definitionMapping.put(ObjectTypesCmf.CASE, CaseDefinition.class);
		definitionMapping.put(ObjectTypesCmf.WORKFLOW, WorkflowDefinition.class);
		definitionMapping.put(ObjectTypesCmf.WORKFLOW_TASK, TaskDefinitionRef.class);
		definitionMapping.put(ObjectTypesCmf.STANDALONE_TASK, TaskDefinition.class);
		definitionMapping.put(ObjectTypesCmf.DOCUMENT, DocumentDefinitionRef.class);
		definitionMapping.put(ObjectTypesCmf.SECTION, SectionDefinition.class);

		instanceMapping = CollectionUtils.createHashMap(10);
		instanceMapping.put(ObjectTypesCmf.CASE, CaseInstance.class);
		instanceMapping.put(ObjectTypesCmf.WORKFLOW, WorkflowInstanceContext.class);
		instanceMapping.put(ObjectTypesCmf.WORKFLOW_TASK, TaskInstance.class);
		instanceMapping.put(ObjectTypesCmf.STANDALONE_TASK, StandaloneTaskInstance.class);
		instanceMapping.put(ObjectTypesCmf.DOCUMENT, DocumentInstance.class);
		instanceMapping.put(ObjectTypesCmf.SECTION, SectionInstance.class);

		typeMapping = CollectionUtils.createHashMap(10);
		typeMapping.put(ObjectTypesCmf.CASE, "caseInstance");
		typeMapping.put(ObjectTypesCmf.WORKFLOW, "workflowInstanceContext");
		typeMapping.put(ObjectTypesCmf.WORKFLOW_TASK, "taskInstance");
		typeMapping.put(ObjectTypesCmf.STANDALONE_TASK, "standaloneTaskInstance");
		typeMapping.put(ObjectTypesCmf.DOCUMENT, "documentInstance");
		typeMapping.put(ObjectTypesCmf.SECTION, "sectionInstance");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Class<? extends DefinitionModel>> getDefinitionMapping() {
		return definitionMapping;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Class<? extends Instance>> getInstanceMapping() {
		return instanceMapping;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getTypeMapping() {
		return typeMapping;
	}

}
