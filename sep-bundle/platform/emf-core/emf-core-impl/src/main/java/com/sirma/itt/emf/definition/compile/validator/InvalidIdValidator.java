package com.sirma.itt.emf.definition.compile.validator;

import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.definition.load.DefinitionValidator;
import com.sirma.itt.emf.definition.model.Condition;
import com.sirma.itt.emf.definition.model.Conditional;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinitionModel;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.Identity;
import com.sirma.itt.emf.label.LabelDefinition;
import com.sirma.itt.emf.util.ValidationLoggingUtil;

/**
 * Validator class that checks the identifiers.
 *
 * @author BBonev
 */
public class InvalidIdValidator implements DefinitionValidator {
	/** The Constant ASCII_CHAR. */
	public static final String ASCII_CHAR = "[\\x20-\\x7E\\r\\n\\t:]";
	/** Pattern used for ID validation. */
	private static final Pattern ID_PATTERN = Pattern.compile("[\\w$:]+");
	/** The Constant ASCII_CHARACTER_PATTERN. */
	private static final Pattern ASCII_CHARACTER_PATTERN = Pattern.compile("^" + ASCII_CHAR + "+$");
	/** The Constant ASCII_CHAR_PATTERN. */
	private static final Pattern ASCII_CHAR_PATTERN = Pattern.compile(ASCII_CHAR);
	@Inject
	private Logger logger;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(RegionDefinitionModel model) {
		if (model  == null) {
			return true;
		}
		boolean valid = true;
		valid &= validate((DefinitionModel) model);
		for (RegionDefinition regionDefinition : model.getRegions()) {
			valid &= validate(regionDefinition);
			if (regionDefinition.getControlDefinition() != null) {
				valid &= validate(regionDefinition.getControlDefinition());
			}
			valid &= validateCondition(regionDefinition);
		}
		return valid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(DefinitionModel model) {
		if (model  == null) {
			return true;
		}
		boolean valid = true;
		valid &= validate((Identity) model);
		for (PropertyDefinition propertyDefinition : model.getFields()) {
			valid &= validate(propertyDefinition);
			if (propertyDefinition.getControlDefinition() != null) {
				valid &= validate(propertyDefinition.getControlDefinition());
			}
			valid &= validateCondition(propertyDefinition);
		}
		return valid;
	}

	/**
	 * Validate condition.
	 *
	 * @param conditional
	 *            the conditional
	 * @return true, if successful
	 */
	protected boolean validateCondition(Conditional conditional) {
		boolean valid = true;
		if ((conditional.getConditions() != null) && !conditional.getConditions().isEmpty()) {
			for (Condition condition : conditional.getConditions()) {
				valid &= validate(condition);
			}
		}
		return valid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(Identity model) {
		if (model == null) {
			return true;
		}
		// check for non ASCII characters
		String identifier = model.getIdentifier();
		if (!ASCII_CHARACTER_PATTERN.matcher(identifier).matches()) {
			StringBuilder builder = new StringBuilder("Found non ASCII character in ID='").append(
					identifier).append("'. The invalid characters are: ");
			for (int i = 0; i < identifier.length(); i++) {
				char c = identifier.charAt(i);
				if (!ASCII_CHAR_PATTERN.matcher("" + c).matches()) {
					builder.append(c).append(", ");
				}
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.deleteCharAt(builder.length() - 1);
			logger.error(builder);
			ValidationLoggingUtil.addErrorMessage(builder.toString());
			return false;
		}
		// disable validation for labels of the label id for word characters
		if (model instanceof LabelDefinition) {
			return true;
		}
		// the ID should have only word characters [a-zA-Z0-9_:.]
		if (!ID_PATTERN.matcher(identifier).matches()) {
			String message = "Found non word character in ID='" + identifier + "'";
			logger.error(message);
			ValidationLoggingUtil.addErrorMessage(message);
			return false;
		}
		return true;
	}

}
