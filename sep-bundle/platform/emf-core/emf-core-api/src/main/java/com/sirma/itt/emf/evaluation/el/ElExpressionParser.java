package com.sirma.itt.emf.evaluation.el;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.evaluation.ExpressionContext;
import com.sirma.itt.emf.evaluation.ExpressionEvaluator;
import com.sirma.itt.emf.evaluation.ExpressionsManager;
import com.sirma.itt.emf.exceptions.EmfConfigurationException;

/**
 * EL expression parser. The parser offers methods for building expression tree and evaluating the
 * tree.
 *
 * @author BBonev
 */
public class ElExpressionParser {

	/** The Constant CACHE. */
	private static final String CACHE = "evalCache";
	private static final Pattern CLEAR_QUOTES = Pattern.compile("(?<=\\{|\\})'|'(?=\\{|\\})");
	private static final Pattern CLEAR_NULLS = Pattern
			.compile("(?:<b>[^>\")\\w]*\\bnull\\b[^<\"\\w]*</b>[^<\"\\w]*)|(?:[^>\")\\w]*\\bnull\\b[^<\"\\w]*)");
	private static final Pattern CLEAR_MULTI_WS = Pattern.compile("\\s{2,}");

	/**
	 * Parses the given expression to {@link ElExpression} tree.
	 *
	 * @param expression
	 *            the expression
	 * @return the el expression tree
	 */
	public static ElExpression parse(String expression) {
		ElExpression elExpression = new ElExpression();
		StringBuilder builder = new StringBuilder();
		char[] data = expression.toCharArray();
		Pair<ElExpression, Integer> pair = parseInternal(data, 0);
		if (pair.getSecond().intValue() == expression.length()) {
			return pair.getFirst();
		}
		builder.append('{').append(elExpression.getSubExpressions().size()).append('}');
		elExpression.getSubExpressions().add(pair.getFirst());
		while (pair.getSecond().intValue() < expression.length()) {
			pair = parseInternal(data, pair.getSecond() + 1);
			if (pair.getFirst().getExpression().isEmpty()) {
				break;
			}
			builder.append('{').append(elExpression.getSubExpressions().size()).append('}');
			elExpression.getSubExpressions().add(pair.getFirst());
		}

		elExpression.setExpression(builder.toString());
		return elExpression;
	}

	/**
	 * Parses the internal.
	 *
	 * @param data
	 *            the data
	 * @param begin
	 *            the begin
	 * @return the el expression
	 */
	private static Pair<ElExpression, Integer> parseInternal(char[] data, int begin) {
		StringBuilder parsed = new StringBuilder();
		ElExpression expression = new ElExpression();
		boolean isEscape = false;
		int i = begin;
		int openBrackets = 0;
		for (; i < data.length; i++) {
			char c = data[i];
			if (isEscape) {
				parsed.append(c);
				isEscape = false;
			} else if (c == '\\') {
				isEscape = true;
			} else if ((c == '$') && ((i + 1) < data.length) && (data[i + 1] == '{') && (i > begin)) {
				parsed.append('{').append(expression.getSubExpressions().size()).append('}');
				Pair<ElExpression, Integer> internal = parseInternal(data, i);
				i = internal.getSecond();
				expression.getSubExpressions().add(internal.getFirst());
			} else if (c == '{') {
				openBrackets++;
				parsed.append("'{'");
			} else if (c == '}') {
				int old = Integer.valueOf(openBrackets);
				openBrackets--;
				parsed.append("'}'");
				if ((old > 0) && (openBrackets == 0)) {
					break;
				}
			} else {
				parsed.append(c);
			}
		}
		expression.setExpression(parsed.toString());
		return new Pair<ElExpression, Integer>(expression, i);
	}

	/**
	 * Evaluates the given expression tree to final result.
	 *
	 * @param exp
	 *            the expression
	 * @param manager
	 *            the manager
	 * @param converter
	 *            the converter
	 * @param context
	 *            the context
	 * @param values
	 *            the values
	 * @return the serializable result
	 */
	public static Serializable eval(ElExpression exp, ExpressionsManager manager,
			TypeConverter converter, ExpressionContext context, Serializable... values) {
		Serializable eval = evalInternal(exp, manager, converter, context, values);
		if (eval instanceof String) {
			// removes the nulls at the end of the evaluation due to if removed before that breaks
			// most of the expression evaluators due to the fact that they expect data even null data
			eval = CLEAR_NULLS.matcher(eval.toString()).replaceAll(" ");
			eval = CLEAR_MULTI_WS.matcher(eval.toString()).replaceAll(" ");
		}
		return eval;
	}

	/**
	 * Eval internal.
	 *
	 * @param exp
	 *            the exp
	 * @param manager
	 *            the manager
	 * @param converter
	 *            the converter
	 * @param context
	 *            the context
	 * @param values
	 *            the values
	 * @return the serializable
	 */
	private static Serializable evalInternal(ElExpression exp, ExpressionsManager manager,
			TypeConverter converter, ExpressionContext context, Serializable... values) {
		if (exp.getSubExpressions().isEmpty()) {
			String expression = exp.getExpression();
			if (StringUtils.isNullOrEmpty(expression)) {
				return "";
			}
			if (!isExpression(expression)) {
				return expression;
			}
			Map<String, Serializable> cache = getEvalCache(context);
			expression = CLEAR_QUOTES.matcher(expression).replaceAll("");
			exp.setExpression(expression);
			Serializable previsousEvaluated = cache.get(expression);
			if ((previsousEvaluated != null) && (!exp.getExpression().startsWith("${seq"))) {
				return previsousEvaluated;
			}

			ExpressionEvaluator evaluator = manager.getEvaluator(exp.getExpression());
			if (evaluator == null) {
				throw new EmfConfigurationException("Expression " + exp.getExpression()
						+ " not supported!");
			}
			Serializable evaluated = evaluator.evaluate(exp.getExpression(), context, values);

			cache.put(exp.getExpression(), evaluated);
			return evaluated;
		}
		List<Serializable> params = new ArrayList<Serializable>(exp.getSubExpressions().size());
		for (ElExpression subExpression : exp.getSubExpressions()) {
			Serializable serializable = evalInternal(subExpression, manager, converter, context,
					values);
			String convert = converter.convert(String.class, serializable);
			params.add(convert);
		}

		String expression = exp.getExpression();
		// String updated = OPEN_BRACKET.matcher(expression).replaceAll("'{'");
		// if (updated.length() != expression.length()) {
		// updated = CLOSE_BRACKET.matcher(updated).replaceAll("'}'");
		// }
		if (isExpression(expression)) {
			expression = MessageFormat.format(expression, params.toArray());
			exp.setExpression(expression);
			exp.getSubExpressions().clear();
			return evalInternal(exp, manager, converter, context, values);
		}
		return MessageFormat.format(expression, params.toArray());
	}

	/**
	 * Checks if is expression.
	 *
	 * @param expression
	 *            the expression
	 * @return true, if is expression
	 */
	private static boolean isExpression(String expression) {
		return expression.startsWith("$");
	}

	/**
	 * Gets the eval cache.
	 *
	 * @param context
	 *            the context
	 * @return the eval cache
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Serializable> getEvalCache(ExpressionContext context) {
		Map<String, Serializable> map = (Map<String, Serializable>) context.get(CACHE);
		if (map == null) {
			map = new LinkedHashMap<String, Serializable>();
			context.put(CACHE, (Serializable) map);
		}
		return map;
	}
}
