package com.sirma.itt.cmf.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionRef;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionTemplate;
import com.sirma.itt.cmf.beans.definitions.SectionDefinition;
import com.sirma.itt.cmf.beans.definitions.TaskDefinition;
import com.sirma.itt.cmf.beans.definitions.TaskDefinitionRef;
import com.sirma.itt.cmf.beans.definitions.TaskDefinitionTemplate;
import com.sirma.itt.cmf.beans.definitions.WorkflowDefinition;
import com.sirma.itt.cmf.beans.definitions.impl.CaseDefinitionImpl;
import com.sirma.itt.cmf.beans.definitions.impl.DocumentDefinitionImpl;
import com.sirma.itt.cmf.beans.definitions.impl.DocumentDefinitionRefImpl;
import com.sirma.itt.cmf.beans.definitions.impl.GenericDefinitionImpl;
import com.sirma.itt.cmf.beans.definitions.impl.SectionDefinitionImpl;
import com.sirma.itt.cmf.beans.definitions.impl.TaskDefinitionImpl;
import com.sirma.itt.cmf.beans.definitions.impl.TaskDefinitionRefImpl;
import com.sirma.itt.cmf.beans.definitions.impl.TaskDefinitionTemplateImpl;
import com.sirma.itt.cmf.beans.definitions.impl.WorkflowDefinitionImpl;
import com.sirma.itt.emf.definition.model.AllowedChildDefinition;
import com.sirma.itt.emf.definition.model.GenericDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.TransitionDefinition;
import com.sirma.itt.emf.hash.HashCalculator;
import com.sirma.itt.emf.hash.HashCalculatorExtension;
import com.sirma.itt.emf.hash.HashHelper;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.state.transition.StateTransition;

/**
 * Extension point that adds CMF classes that need specific hash computation to the main calculator
 * implementation.
 *
 * @author BBonev
 */
@Extension(target = HashCalculatorExtension.TARGET_NAME, priority = 20)
public class CmfHashCalculatorExtension implements HashCalculatorExtension {

	/** The Constant prime. */
	private static final int PRIME = HashHelper.PRIME;

	/** The Constant SUPPORTED_OBJECTS. */
	private static final List<Class<?>> SUPPORTED_OBJECTS = new ArrayList<Class<?>>(
			Arrays.asList(CaseDefinitionImpl.class, WorkflowDefinitionImpl.class,
					DocumentDefinitionImpl.class, DocumentDefinitionRefImpl.class,
					SectionDefinitionImpl.class, TaskDefinitionTemplateImpl.class,
					TaskDefinitionRefImpl.class, TaskDefinitionImpl.class, GenericDefinitionImpl.class));

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Class<?>> getSupportedObjects() {
		return SUPPORTED_OBJECTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer computeHash(HashCalculator calculator, Object e) {
		if (e instanceof CaseDefinition) {
			return computeHashCode((CaseDefinition) e, calculator);
		} else if (e instanceof WorkflowDefinition) {
			return computeHashCode((WorkflowDefinition) e, calculator);
		} else if (e instanceof DocumentDefinitionTemplate) {
			return computeHashCode((DocumentDefinitionTemplate) e, calculator);
		} else if (e instanceof TaskDefinitionTemplate) {
			return computeHashCode((TaskDefinitionTemplate) e, calculator);
		} else if (e instanceof SectionDefinition) {
			return computeHashCode((SectionDefinition) e, calculator);
		} else if (e instanceof TaskDefinition) {
			return computeHashCode((TaskDefinition) e, calculator);
		} else if (e instanceof TaskDefinitionRef) {
			return computeHashCode((TaskDefinitionRef) e, calculator);
		} else if (e instanceof DocumentDefinitionRef) {
			return computeHashCode((DocumentDefinitionRef) e, calculator);
		} else  if (e instanceof GenericDefinition) {
			return computeHashCode((GenericDefinition) e, calculator);
		}
		return null;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the integer
	 */
	protected Integer computeHashCode(GenericDefinition definition, HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "GenericId");
		result = HashHelper.computeHash(result, definition.getType(), "Type");
		result = HashHelper.computeHash(result, definition.getReferenceId(), "ReferenceId");
		result = (PRIME * result) + (definition.isAbstract() ? 1231 : 1237);
		result = HashHelper.computeHash(result, definition.getDmsId(), "DmsId");
		result = HashHelper.computeHash(result, definition.getParentDefinitionId(),
				"ParentDefinitionId");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");

		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition workflowIdDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(workflowIdDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;

		}
		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getSubDefinitions() != null) && !definition.getSubDefinitions().isEmpty()) {
			for (GenericDefinition child : definition.getSubDefinitions()) {
				result += computeHashCode(child, calculator);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the integer
	 */
	protected Integer computeHashCode(TaskDefinition definition, HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "TaskDefinitionId");
		result = HashHelper.computeHash(result, definition.getParentTaskId(), "ParentTaskId");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");
		result = HashHelper.computeHash(result, definition.getDmsType(), "DmsType");
		result = HashHelper.computeHash(result, definition.getDmsId(), "DmsId");
		result = HashHelper.computeHash(result, definition.getReferenceId(), "ReferenceId");
		result = HashHelper.computeHash(result, definition.isAbstract(), "isAbstract");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition childDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(childDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	protected int computeHashCode(TaskDefinitionTemplate definition,
			HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "TaskDefinitionId");
		result = HashHelper.computeHash(result, definition.getParentTaskId(), "ParentTaskId");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition childDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(childDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	protected int computeHashCode(DocumentDefinitionTemplate definition,
			HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "DocumentId");
		result = HashHelper.computeHash(result, definition.getParent(), "Parent");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);

			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition workflowIdDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(workflowIdDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	protected int computeHashCode(WorkflowDefinition definition, HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "WorkflowDefinitionId");
		result = HashHelper.computeHash(result, definition.getDmsId(), "DmsId");
		result = HashHelper.computeHash(result, definition.getParentDefinitionId(),
				"ParentDefinitionId");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTasks() != null) && !definition.getTasks().isEmpty()) {
			for (TaskDefinitionRef taskDefinitionRef : definition.getTasks()) {
				result += computeHashCode(taskDefinitionRef, calculator);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition childDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(childDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	protected int computeHashCode(TaskDefinitionRef definition, HashCalculator calculator) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getIdentifier(), "TaskDefinitionId");
		result = HashHelper.computeHash(result, definition.getPurpose(), "Purpose");
		result = HashHelper.computeHash(result, definition.getReferenceTaskId(), "ReferenceTaskId");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition childDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(childDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code for the given case definition. Method does not take into account the
	 * properties that are time or DB dependent but only once that come from the XML definition
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	protected int computeHashCode(CaseDefinition definition, HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "CaseId");
		result = (PRIME * result) + (definition.isAbstract() ? 1231 : 1237);
		result = HashHelper.computeHash(result, definition.getDmsId(), "DmsId");
		result = HashHelper.computeHash(result, definition.getParentDefinitionId(),
				"ParentDefinitionId");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");

		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition workflowIdDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(workflowIdDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;

		}
		if ((definition.getSectionDefinitions() != null)
				&& !definition.getSectionDefinitions().isEmpty()) {
			for (SectionDefinition sectionDefinition : definition.getSectionDefinitions()) {
				result += computeHashCode(sectionDefinition, calculator);
			}
		} else {
			result = PRIME * result;

		}
		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}

		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code for {@link SectionDefinition}.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	protected int computeHashCode(SectionDefinition definition, HashCalculator calculator) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getIdentifier(), "SectionId");
		result = HashHelper.computeHash(result, definition.getPurpose(), "Purpose");
		result = HashHelper.computeHash(result, definition.getReferenceId(), "ReferenceId");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getDocumentDefinitions() != null)
				&& !definition.getDocumentDefinitions().isEmpty()) {
			for (DocumentDefinitionRef documentDefinition : definition.getDocumentDefinitions()) {
				result += computeHashCode(documentDefinition, calculator);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition workflowIdDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(workflowIdDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code for {@link DocumentDefinitionRef}.
	 *
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int+
	 */
	protected int computeHashCode(DocumentDefinitionRef definition, HashCalculator calculator) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getReferenceId(), "ReferenceId");
		result = HashHelper.computeHash(result, definition.getIdentifier(), "DocumentDefinitionId");
		result = HashHelper.computeHash(result, definition.getMaxInstances(), "MaxInstances");
		result = HashHelper.computeHash(result, definition.getMandatory(), "Mandatory");
		result = HashHelper.computeHash(result, definition.getStructured(), "Structured");
		result = HashHelper.computeHash(result, definition.getPurpose(), "Purpose");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += calculator.computeHash(fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getRegions() != null) && !definition.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : definition.getRegions()) {
				result += calculator.computeHash(regionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getTransitions() != null) && !definition.getTransitions().isEmpty()) {
			for (TransitionDefinition transitionDefinition : definition.getTransitions()) {
				result += calculator.computeHash(transitionDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getStateTransitions() != null)
				&& !definition.getStateTransitions().isEmpty()) {
			for (StateTransition stateTransition : definition.getStateTransitions()) {
				result += calculator.computeHash(stateTransition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getAllowedChildren() != null) && !definition.getAllowedChildren().isEmpty()) {
			for (AllowedChildDefinition workflowIdDefinition : definition.getAllowedChildren()) {
				result += calculator.computeHash(workflowIdDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

}
