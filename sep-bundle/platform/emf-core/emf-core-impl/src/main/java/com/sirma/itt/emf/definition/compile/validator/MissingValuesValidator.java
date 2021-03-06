/*
 *
 */
package com.sirma.itt.emf.definition.compile.validator;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.definition.load.DefinitionValidator;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinition;
import com.sirma.itt.emf.definition.model.RegionDefinitionModel;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.Identity;
import com.sirma.itt.emf.util.ValidationLoggingUtil;

/**
 * Validator class that checks for missing fields.
 * 
 * @author BBonev
 */
public class MissingValuesValidator implements DefinitionValidator {

	/** The logger. */
	@Inject
	private Logger logger;
	/** The trace. */
	private boolean trace;

	/**
	 * Inits the.
	 */
	@PostConstruct
	public void init() {
		trace = logger.isTraceEnabled();
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(RegionDefinitionModel model) {
		if (model == null) {
			return true;
		}
		boolean valid = true;
		List<Pair<String, PropertyDefinition>> errors = new LinkedList<Pair<String, PropertyDefinition>>();
		valid &= checkForMissingValues(model, errors);
		if ((model.getRegions() != null) && !model.getRegions().isEmpty()) {
			for (RegionDefinition regionDefinition : model.getRegions()) {
				valid &= checkForMissingValues(regionDefinition, errors);
			}
		}
		if (!valid) {
			StringBuilder builder = new StringBuilder(errors.size() * 60);
			builder.append(
					"\n=======================================================================\nFound errors in definition: ")
					.append(model.getIdentifier()).append(" (missing types) :\n");
			for (Pair<String, PropertyDefinition> pair : errors) {
				builder.append(pair.getFirst()).append("\n");
			}
			builder.append("=======================================================================");
			logger.error(builder);
			ValidationLoggingUtil.addErrorMessage(builder.toString());
			if (trace) {
				logger.trace("Found errors in " + model.getIdentifier() + " the following fields: ");
				for (Pair<String, PropertyDefinition> pair : errors) {
					logger.trace(pair.getSecond());
				}
				logger.trace("End errors for " + model.getIdentifier());
			}
		}
		return valid;
	}

	/**
	 * Internal method for checking missing values.
	 *
	 * @param model
	 *            the model
	 * @param errors
	 *            the errors
	 * @return true, if valid
	 */
	private boolean checkForMissingValues(DefinitionModel model,
			List<Pair<String, PropertyDefinition>> errors) {
		if (model == null) {
			return true;
		}
		boolean valid = true;
		if ((model.getFields() != null) && !model.getFields().isEmpty()) {
			for (PropertyDefinition propertyDefinition : model.getFields()) {
				if (!validate(propertyDefinition)) {
					errors.add(new Pair<String, PropertyDefinition>(propertyDefinition
							.getParentPath() + ":" + propertyDefinition.getName(),
							propertyDefinition));
					valid = false;
				}
				valid &= checkForMissingValues(propertyDefinition.getControlDefinition(), errors);
			}
		}
		return valid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(DefinitionModel model) {
		if (model == null) {
			return true;
		}
		boolean valid = true;
		List<Pair<String, PropertyDefinition>> errors = new LinkedList<Pair<String, PropertyDefinition>>();
		valid &= checkForMissingValues(model, errors);

		if (!valid) {
			StringBuilder builder = new StringBuilder(errors.size() * 60);
			builder.append(
					"\n=======================================================================\nFound errors in definition: ")
					.append(model.getIdentifier()).append(" (missing types) :\n");
			for (Pair<String, PropertyDefinition> pair : errors) {
				builder.append(pair.getFirst()).append("\n");
			}
			builder.append("=======================================================================");
			logger.error(builder);
			ValidationLoggingUtil.addErrorMessage(builder.toString());
			if (trace) {
				logger.trace("Found errors in " + model.getIdentifier() + " the following fields: ");
				for (Pair<String, PropertyDefinition> pair : errors) {
					logger.trace(pair.getSecond());
				}
				logger.trace("End errors for " + model.getIdentifier());
			}
		}
		return valid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(Identity model) {
		if (model instanceof PropertyDefinition) {
			PropertyDefinition propertyDefinition = (PropertyDefinition) model;
			return StringUtils.isNotNullOrEmpty(propertyDefinition.getType())
					&& (propertyDefinition.getDataType() != null);
		}
		return true;
	}

}
