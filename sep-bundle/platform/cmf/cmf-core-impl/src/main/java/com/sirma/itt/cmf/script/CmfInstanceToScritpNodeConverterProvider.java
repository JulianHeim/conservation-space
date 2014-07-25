package com.sirma.itt.cmf.script;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.script.InstanceToScriptNodeConverterProvider;
import com.sirma.itt.emf.script.ScriptNode;

/**
 * Cmf converter register for specific converters for
 * {@link com.sirma.itt.emf.instance.model.Instance} to {@link ScriptNode}.
 * 
 * @author BBonev
 */
@ApplicationScoped
public class CmfInstanceToScritpNodeConverterProvider extends InstanceToScriptNodeConverterProvider {

	/** The nodes. */
	@Inject
	private Instance<WorkflowScriptNode> workflowNodes;
	/** The nodes. */
	@Inject
	private Instance<DocumentScriptNode> documentNodes;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void register(TypeConverter converter) {
		converter.addConverter(WorkflowInstanceContext.class, ScriptNode.class,
				new InstanceToScriptNodeConverter<WorkflowInstanceContext>(workflowNodes));
		converter.addConverter(DocumentInstance.class, ScriptNode.class,
				new InstanceToScriptNodeConverter<DocumentInstance>(documentNodes));
	}
}
