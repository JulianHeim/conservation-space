package com.sirma.itt.pm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sirma.itt.emf.definition.model.AllowedChildDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.TransitionDefinition;
import com.sirma.itt.emf.hash.HashCalculator;
import com.sirma.itt.emf.hash.HashCalculatorExtension;
import com.sirma.itt.emf.hash.HashHelper;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.state.transition.StateTransition;
import com.sirma.itt.pm.domain.definitions.ProjectDefinition;
import com.sirma.itt.pm.domain.definitions.impl.ProjectDefinitionImpl;

/**
 * The Class PmHashCalculatorExtension.
 * 
 * @author BBonev
 */
@Extension(target = HashCalculatorExtension.TARGET_NAME, order = 30)
public class PmHashCalculatorExtension implements HashCalculatorExtension {

	/** The Constant prime. */
	private static final int PRIME = HashHelper.PRIME;

	/** The Constant SUPPORTED_OBJECTS. */
	private static final List<Class<?>> SUPPORTED_OBJECTS = new ArrayList<Class<?>>(Arrays.asList(
ProjectDefinitionImpl.class));

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
	public Integer computeHash(HashCalculator calculator, Object o) {
		if (o instanceof ProjectDefinition) {
			return computeHash((ProjectDefinition) o, calculator);
		}
		return null;
	}

	/**
	 * Compute hash.
	 * 
	 * @param definition
	 *            the definition
	 * @param calculator
	 *            the calculator
	 * @return the int
	 */
	public static int computeHash(ProjectDefinition definition, HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "Identifier");
		result = (PRIME * result) + (definition.isAbstract() ? 1231 : 1237);
		result = HashHelper.computeHash(result, definition.getDmsId(), "DmsId");
		result = HashHelper.computeHash(result, definition.getParentDefinitionId(),
				"ParentDefinitionId");
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

}
