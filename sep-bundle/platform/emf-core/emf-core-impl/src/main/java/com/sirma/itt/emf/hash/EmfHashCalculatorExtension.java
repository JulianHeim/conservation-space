package com.sirma.itt.emf.hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sirma.itt.emf.definition.model.AllowedChildConfiguration;
import com.sirma.itt.emf.definition.model.AllowedChildConfigurationImpl;
import com.sirma.itt.emf.definition.model.AllowedChildDefinition;
import com.sirma.itt.emf.definition.model.AllowedChildDefinitionImpl;
import com.sirma.itt.emf.definition.model.Condition;
import com.sirma.itt.emf.definition.model.ConditionDefinitionImpl;
import com.sirma.itt.emf.definition.model.ControlDefinitionImpl;
import com.sirma.itt.emf.definition.model.ControlParam;
import com.sirma.itt.emf.definition.model.ControlParamImpl;
import com.sirma.itt.emf.definition.model.DataType;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.definition.model.FieldDefinitionImpl;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinitionProxy;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinitionImpl;
import com.sirma.itt.emf.definition.model.StateTransitionImpl;
import com.sirma.itt.emf.definition.model.TransitionDefinition;
import com.sirma.itt.emf.definition.model.TransitionDefinitionImpl;
import com.sirma.itt.emf.definition.model.WritablePropertyDefinition;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.state.transition.StateTransition;

/**
 * Hash calculator extension to provide hash computation for the base Emf classes
 *
 * @author BBonev
 */
@Extension(target = HashCalculatorExtension.TARGET_NAME, order = 10)
public class EmfHashCalculatorExtension implements HashCalculatorExtension {

	private static final int PRIME = HashHelper.PRIME;

	/** The Constant SUPPORTED_OBJECTS. */
	private static final List<Class<?>> SUPPORTED_OBJECTS = new ArrayList<Class<?>>(Arrays.asList(
			ControlDefinitionImpl.class, ConditionDefinitionImpl.class, DataType.class,
			FieldDefinitionImpl.class, PropertyDefinitionProxy.class,
			TransitionDefinitionImpl.class, RegionDefinitionImpl.class,
			AllowedChildDefinitionImpl.class, AllowedChildConfigurationImpl.class,
			StateTransitionImpl.class));

	@Override
	public List<Class<?>> getSupportedObjects() {
		return SUPPORTED_OBJECTS;
	}

	@Override
	public Integer computeHash(HashCalculator calculator, Object object) {
		if (object instanceof ControlDefinitionImpl) {
			return computeHashCode((ControlDefinitionImpl) object);
		} else if (object instanceof ConditionDefinitionImpl) {
			return computeHashCode((ConditionDefinitionImpl) object);
		} else if (object instanceof DataType) {
			return computeHashCode((DataType) object);
		} else if (object instanceof TransitionDefinitionImpl) {
			return computeHashCode((TransitionDefinitionImpl) object);
		} else if (object instanceof RegionDefinitionImpl) {
			return computeHashCode((RegionDefinitionImpl) object);
		} else if (object instanceof WritablePropertyDefinition) {
			return computeHashCode((WritablePropertyDefinition) object);
		} else if (object instanceof AllowedChildConfigurationImpl) {
			return computeHashCode((AllowedChildConfigurationImpl) object);
		} else if (object instanceof AllowedChildDefinitionImpl) {
			return computeHashCode((AllowedChildDefinitionImpl) object);
		} else if (object instanceof StateTransitionImpl) {
			return computeHashCode((StateTransition) object, calculator);
		}
		return null;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @return the int
	 */
	protected static int computeHashCode(ControlDefinitionImpl definition) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "ControlId");
		result = HashHelper.computeHash(result, definition.getPath(), "Path(" + definition.getPath() + ")");

		if ((definition.getControlParams() != null) && !definition.getControlParams().isEmpty()) {
			for (ControlParam controlParam : definition.getControlParams()) {
				result += computeHashCode((ControlParamImpl) controlParam);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getUiParams() != null) && !definition.getUiParams().isEmpty()) {
			for (ControlParam controlParam : definition.getUiParams()) {
				result += computeHashCode((ControlParamImpl) controlParam);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += computeHashCode((WritablePropertyDefinition) fieldDefinition);

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
	 * @return the int
	 */
	protected static int computeHashCode(ControlParamImpl definition) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "ParamId");
		result = HashHelper.computeHash(result, definition.getName(), "Name");
		result = HashHelper.computeHash(result, definition.getValue(), "Value");
		result = HashHelper.computeHash(result, definition.getPath(), "Path");

		return result;
	}

	/**
	 * Compute hash code for single {@link FieldDefinitionImpl}.
	 *
	 * @param definition
	 *            the definition
	 * @return the int
	 */
	public static int computeHashCode(WritablePropertyDefinition definition) {
		int result = 1;

		/*
		 * codelist, container, displaytype, dmstype, filters, label, mandatory, mandatoryenforced,
		 * maxlength, multivalued, fieldname, indexorder, override, previewempty, rnc, tooltip,
		 * fieldtype, value, datatype_id
		 */
		result = HashHelper.computeHash(result, definition.getName(), "Name");
		result = HashHelper.computeHash(result, definition.getParentPath(), "ParentPath");
		result = HashHelper.computeHash(result, definition.getDmsType(), "DmsType");
		result = HashHelper.computeHash(result, definition.getRnc(), "Rnc");
		result = HashHelper.computeHash(result, definition.getType(), "Type");
		result = HashHelper.computeHash(result, definition.getDefaultValue(), "DefaultValue");
		result = HashHelper.computeHash(result, definition.getDisplayType(), "DisplayType");
		result = HashHelper.computeHash(result, definition.getLabelId(), "LabelId");
		result = HashHelper.computeHash(result, definition.isPreviewEnabled(), "PreviewEnabled");
		result = HashHelper.computeHash(result, definition.getTooltipId(), "TooltipId");
		result = HashHelper.computeHash(result, definition.isMandatory(), "isMandatory");
		result = HashHelper.computeHash(result, definition.isMandatoryEnforced(),
				"isMandatoryEnforced");
		result = HashHelper.computeHash(result, definition.getMaxLength(), "MaxLength");
		result = HashHelper.computeHash(result, definition.getCodelist(), "Codelist");
		result = HashHelper.computeHash(result, definition.getOrder(), "Order");
		result = HashHelper.computeHash(result, definition.isMultiValued(), "isMultiValued");
		result = HashHelper.computeHash(result, definition.isOverride(), "isOverride");
		result = HashHelper.computeHash(result, definition.getContainer(), "Container");
		result = HashHelper.computeHash(result, definition.getUri(), "Uri");
		// CMF-1547: forgot to add result * prime
		result = (result * PRIME) + computeHashCode(definition.getDataType());

		if (definition.getControlDefinition() != null) {
			result = (PRIME * result)
					+ computeHashCode((ControlDefinitionImpl) definition.getControlDefinition());
		} else {
			result = PRIME * result;
		}
		if ((definition.getFilters() != null) && !definition.getFilters().isEmpty()) {
			// changed hash code builder to iterate over the fields at the same
			// manner no matter of the order of the fields
			ArrayList<String> list = new ArrayList<String>(definition.getFilters());
			Collections.sort(list);
			for (String string : list) {
				result = HashHelper.computeHash(result, string, "Filters(" + string + ")");
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getConditions() != null) && !definition.getConditions().isEmpty()) {
			for (Condition condition : definition.getConditions()) {
				result += computeHashCode((ConditionDefinitionImpl) condition);
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
	 * @return the int
	 */
	public static int computeHashCode(DataTypeDefinition definition) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getId(), "Id");
		result = HashHelper.computeHash(result, definition.getName(), "Name");
		result = HashHelper.computeHash(result, definition.getTitle(), "Title");
		result = HashHelper.computeHash(result, definition.getDescription(), "Description");
		result = HashHelper.computeHash(result, definition.getJavaClassName(), "JavaClassName");
		if (definition instanceof DataType) {
			result = HashHelper.computeHash(result, ((DataType) definition).getUri(), "Uri");
		} else {
			result = HashHelper.computeHash(result, definition.getFirstUri(), "Uri");
		}
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @return the int
	 */
	protected static int computeHashCode(ConditionDefinitionImpl definition) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getIdentifier(), "Identifier");
		result = HashHelper.computeHash(result, definition.getRenderAs(), "RenderAs");
		result = HashHelper.computeHash(result, definition.getExpression(), "Expression");
		return result;
	}

	/**
	 * Compute hash code.
	 *
	 * @param definition
	 *            the definition
	 * @return the int
	 */
	protected static int computeHashCode(RegionDefinition definition) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getIdentifier(), "RegionId");
		result = HashHelper.computeHash(result, definition.getLabelId(), "LabelId");
		result = HashHelper.computeHash(result, definition.getTooltipId(), "Tooltip");
		result = HashHelper.computeHash(result, definition.getDisplayType(), "DisplayType");
		result = HashHelper.computeHash(result, definition.getOrder(), "Order");

		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += computeHashCode((WritablePropertyDefinition) fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getConditions() != null) && !definition.getConditions().isEmpty()) {
			for (Condition condition : definition.getConditions()) {
				result += computeHashCode((ConditionDefinitionImpl) condition);
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
	 * @return the int
	 */
	protected static int computeHashCode(TransitionDefinition definition) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getEventId(), "EventId");
		result = HashHelper.computeHash(result, definition.getLabelId(), "LabelId");
		result = HashHelper.computeHash(result, definition.getTooltipId(), "TooltipId");
		result = HashHelper.computeHash(result, definition.getIdentifier(), "TransitionId");
		result = HashHelper.computeHash(result, definition.getNextPrimaryState(),
				"NextPrimaryState");
		result = HashHelper.computeHash(result, definition.getNextSecondaryState(),
				"NextSecondaryState");
		result = HashHelper.computeHash(result, definition.getDefaultTransition(),
				"isDefaultTransition");
		result = HashHelper.computeHash(result, definition.isImmediateAction(), "isImmediate");
		result = HashHelper.computeHash(result, definition.getConfirmationMessageId(), "ConfirmationMessageId");
		result = HashHelper.computeHash(result, definition.getDisabledReasonId(), "DisabledReasonId");
		result = HashHelper.computeHash(result, definition.getPurpose(), "Purpose");
		result = HashHelper.computeHash(result, definition.getOrder(), "Order");
		result = HashHelper.computeHash(result, definition.getOwnerPrefix(), "OwnerPrefix");

		if ((definition.getConditions() != null) && !definition.getConditions().isEmpty()) {
			for (Condition condition : definition.getConditions()) {
				result += computeHashCode((ConditionDefinitionImpl) condition);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getFields() != null) && !definition.getFields().isEmpty()) {
			for (PropertyDefinition fieldDefinition : definition.getFields()) {
				result += computeHashCode((WritablePropertyDefinition) fieldDefinition);
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash code for {@link AllowedChildDefinition}.
	 *
	 * @param definition
	 *            the definition
	 * @return the int
	 */
	protected static int computeHashCode(AllowedChildDefinition definition) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getIdentifier(), "AllowedChildId");
		result = HashHelper.computeHash(result, definition.getType(), "ObjectType");

		if ((definition.getFilters() != null) && !definition.getFilters().isEmpty()) {
			for (AllowedChildConfiguration configuration : definition.getFilters()) {
				result += computeHashCode(configuration);
			}
		} else {
			result = PRIME * result;
		}
		if ((definition.getPermissions() != null) && !definition.getPermissions().isEmpty()) {
			for (AllowedChildConfiguration configuration : definition.getPermissions()) {
				result += computeHashCode(configuration);
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
	 * @return the int
	 */
	protected static int computeHashCode(AllowedChildConfiguration definition) {
		int result = 1;
		result = HashHelper.computeHash(result, definition.getProperty(), "Property");
		result = HashHelper.computeHash(result, definition.getCodelist(), "Codelist");

		if ((definition.getValues() != null) && !definition.getValues().isEmpty()) {
			// changed hash code builder to iterate over the fields at the same
			// manner no matter of the order of the fields
			ArrayList<String> list = new ArrayList<String>(definition.getValues());
			Collections.sort(list);
			for (String string : list) {
				result = HashHelper.computeHash(result, string, "Values(" + string + ")");
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
	private static int computeHashCode(StateTransition definition, HashCalculator calculator) {
		int result = 1;

		result = HashHelper.computeHash(result, definition.getFromState(), "FromState");
		result = HashHelper.computeHash(result, definition.getToState(), "ToState");
		result = HashHelper.computeHash(result, definition.getTransitionId(), "TransitionId");

		if ((definition.getConditions() != null) && !definition.getConditions().isEmpty()) {
			for (Condition condition : definition.getConditions()) {
				result += calculator.computeHash(condition);
			}
		} else {
			result = PRIME * result;

		}
		return result;
	}

}
