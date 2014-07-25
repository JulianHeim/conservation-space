package com.sirma.itt.cmf.script;

import com.sirma.itt.cmf.constants.LinkConstantsCmf;
import com.sirma.itt.emf.script.ScriptNode;

/**
 * Script node implementation that has specific workflow methods
 * 
 * @author BBonev
 */
public class WorkflowScriptNode extends ScriptNode {

	/**
	 * Gets the processing documents.
	 * 
	 * @return the processing documents
	 */
	public ScriptNode[] getProcessingDocuments() {
		if (target == null) {
			return new ScriptNode[0];
		}
		return getLinks(LinkConstantsCmf.PROCESSES);
	}
}
