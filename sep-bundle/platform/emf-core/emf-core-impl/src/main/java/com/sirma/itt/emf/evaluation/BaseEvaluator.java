package com.sirma.itt.emf.evaluation;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.PropertiesService;
import com.sirma.itt.emf.properties.model.PropertyModel;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.User;

/**
 * Base evaluator implementation.
 *
 * @author BBonev
 */
public abstract class BaseEvaluator implements ExpressionEvaluator {

	public static final String FROM_GROUP = "from";
	public static final String MATCHES_GROUP = "matches";
	/**
	 * The Constant FROM_PATTERN. TODO: move to separate class/interface with common expression
	 * constants
	 */
	public static final String FROM_PATTERN = "(?:\\.?from\\((?<" + FROM_GROUP
			+ ">current|context|rootContext|owningInstance|parent|link\\((?<link>[\\w:]+?)\\))\\))?";

	/**
	 * The Constant MATCHES. TODO: move to separate class/interface with common expression constants
	 */
	public static final String MATCHES = "(?:\\.?matches\\((?<" + MATCHES_GROUP + ">.+?)\\))?";

	private static final ExpressionContext NO_CONTEXT = new ExpressionContext();

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -590499492537863134L;

	/** The logger. */
	@Inject
	protected Logger logger;

	@Inject
	protected javax.enterprise.inject.Instance<ExpressionsManager> expressionManager;

	@Inject
	protected TypeConverter converter;

	@Inject
	protected PropertiesService propertiesService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canHandle(String expression) {
		Matcher matcher = getPattern().matcher(expression);
		return matcher.matches();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable evaluate(String expression, Serializable... values) {
		NO_CONTEXT.clear();
		return evaluate(expression, NO_CONTEXT, values);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable evaluate(String expression, ExpressionContext context,
			Serializable... values) {
		Matcher matcher = getPattern().matcher(expression);
		if (matcher.matches()) {
			return evaluateInternal(matcher, context, values);
		}
		return null;
	}

	/**
	 * Gets the pattern used by the concrete evaluator
	 *
	 * @return the pattern
	 */
	protected abstract Pattern getPattern();

	/**
	 * Evaluate internal. The actual evaluation goes here
	 *
	 * @param matcher
	 *            the matcher for the expression passed
	 * @param context
	 *            the context the evaluation context if any
	 * @param values
	 *            the values the optional arguments
	 * @return the serializable result
	 */
	protected abstract Serializable evaluateInternal(Matcher matcher, ExpressionContext context,
			Serializable... values);

	/**
	 * Gets the typed parameter. The value is searched by the given type.
	 *
	 * @param <E>
	 *            the element type
	 * @param values
	 *            the values to check
	 * @param targetClass
	 *            the target class
	 * @return the typed parameter
	 */
	protected <E> E getTypedParameter(Serializable[] values, Class<E> targetClass) {
		if (values == null) {
			return null;
		}
		for (Serializable serializable : values) {
			if (targetClass.isInstance(serializable)) {
				return targetClass.cast(serializable);
			}
		}
		return null;
	}

	/**
	 * Gets the property from current instance.
	 *
	 * @param key
	 *            the key
	 * @param matcher
	 *            the matcher
	 * @param context
	 *            the context
	 * @param values
	 *            the values
	 * @return the property from current instance
	 */
	protected Serializable getPropertyFrom(String key, Matcher matcher,
			ExpressionContext context, Serializable... values) {
		String fromExpression = matcher.group(FROM_GROUP);

		Serializable serializable = evaluateFrom(fromExpression, context, values);
		if (serializable instanceof PropertyModel) {
			String local = key;
			if (isPropertyKey(local)) {
				local = extractProperty(local);
			}
			PropertyModel propertyModel = (PropertyModel) serializable;
			if (propertyModel.getProperties() == null) {
				Map<String, Serializable> properties = propertiesService.getEntityProperties(
						(Entity) propertyModel, propertyModel.getRevision(), propertyModel);
				propertyModel.setProperties(properties);
			}
			return propertyModel.getProperties().get(local);
		}
		return null;
	}

	/**
	 * Checks if is result valid.
	 * 
	 * @param value
	 *            the value
	 * @param matcher
	 *            the matcher
	 * @return true, if is result valid
	 */
	protected boolean isResultValid(Serializable value, Matcher matcher) {
		if (value != null) {
			String matchesExpression = matcher.group(MATCHES_GROUP);
			if (StringUtils.isNotNullOrEmpty(matchesExpression)) {
				try {
					return Pattern.matches(matchesExpression, value.toString());
				} catch (PatternSyntaxException e) {
					logger.warn("Matches expression " + matchesExpression + " is not valid: "
							+ e.getMessage());
					// not valid expression so we cannot say if it matches or not
					return false;
				}
			}
			// if there is not match pattern but the value is not null the result is still valid
			return true;
		}
		// if there is not match pattern but the value is not null the result is still valid
		return false;
	}

	/**
	 * Validate and return.
	 * 
	 * @param result
	 *            the result
	 * @param valueIfInvalid
	 *            the value if invalid
	 * @param matcher
	 *            the matcher
	 * @return the serializable
	 */
	protected Serializable validateAndReturn(Serializable result, Serializable valueIfInvalid,
			Matcher matcher) {
		if (isResultValid(result, matcher)) {
			return result;
		}
		return valueIfInvalid;
	}

	/**
	 * Find property with the specified key from the given list of arguments
	 *
	 * @param key
	 *            the key
	 * @param values
	 *            the values
	 * @return the serializable
	 */
	protected Serializable findProperty(String key, Serializable... values) {
		Serializable value = null;
		for (Serializable serializable : values) {
			if (serializable instanceof PropertyModel) {
				Map<String, Serializable> properties = ((PropertyModel) serializable)
						.getProperties();
				if (properties != null) {
					value = properties.get(key);
					if (value != null) {
						break;
					}
				}
			}
		}
		return value;
	}

	/**
	 * Evaluates the given from expression and returns the result if any.
	 *
	 * @param key
	 *            the key
	 * @param context
	 *            the context
	 * @param values
	 *            the values
	 * @return the serializable instance or <code>null</code>
	 */
	protected Serializable evaluateFrom(String key, ExpressionContext context,
			Serializable... values) {
		String local = key;
		if (StringUtils.isNullOrEmpty(local)) {
			// the default behavior
			local = "current";
		}
		return expressionManager.get().evaluateRule("${from(" + local + ")}", Serializable.class,
				context, values);
	}

	/**
	 * Gets the current instance.
	 *
	 * @param context
	 *            the context
	 * @param values
	 *            the values
	 * @return the current instance
	 */
	protected Serializable getCurrentInstance(ExpressionContext context, Serializable... values) {
		Serializable serializable = context.get(ExpressionContextProperties.CURRENT_INSTANCE);
		if (serializable == null) {
			serializable = getTypedParameter(values, Instance.class);
		}
		return serializable;
	}

	/**
	 * Extract property.
	 *
	 * @param property
	 *            the property
	 * @return the string
	 */
	protected String extractProperty(String property) {
		return property.substring(1, property.length() - 1);
	}

	/**
	 * Checks if is property key.
	 *
	 * @param property
	 *            the property
	 * @return true, if is property key
	 */
	protected boolean isPropertyKey(String property) {
		return StringUtils.isNotNullOrEmpty(property) && property.startsWith("[");
	}

	/**
	 * Gets the current language.
	 *
	 * @param context
	 *            the context
	 * @return the current language
	 */
	protected String getCurrentLanguage(ExpressionContext context) {
		String string = (String) context.get(ExpressionContextProperties.LANGUAGE);
		if (string == null)  {
			User user = SecurityContextManager.getFullAuthentication();
			if (user == null) {
				user = SecurityContextManager.getAdminUser();
			}
			string = user.getUserInfo().getLanguage();
		}
		return string;
	}
}
