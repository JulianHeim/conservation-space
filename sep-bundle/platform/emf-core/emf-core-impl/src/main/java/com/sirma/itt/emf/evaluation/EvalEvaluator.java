package com.sirma.itt.emf.evaluation;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.sirma.itt.emf.evaluation.el.ElExpression;
import com.sirma.itt.emf.evaluation.el.ElExpressionParser;

/**
 * Expression evaluator that parses and evaluates multiple expressions.
 *
 * @author BBonev
 */
@ApplicationScoped
public class EvalEvaluator extends BaseEvaluator {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -2192750836519050927L;

	/**
	 * Matcher for EVAL expression. The expression could be on multiple lines that's why we have the
	 * DOTALL flag active.
	 */
	private static final Pattern FIELD_PATTERN = Pattern.compile(
			"\\s*\\$\\{eval\\((.+?)\\)\\}\\s*", Pattern.DOTALL);

	/** The manager. */
	@Inject
	private ExpressionsManager manager;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Pattern getPattern() {
		return FIELD_PATTERN;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Serializable evaluateInternal(Matcher matcher, ExpressionContext context,
			Serializable... values) {
		String expression = matcher.group(1);
		ElExpression elExpression = ElExpressionParser.parse(expression);
		return ElExpressionParser.eval(elExpression, manager, converter, context, values);
	}

}
