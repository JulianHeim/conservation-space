package com.sirma.itt.emf.evaluation.el;

import java.util.LinkedList;
import java.util.List;

/**
 * Parsed EL expression object. The objects holds a single expression and any sub expressions.
 * 
 * @author BBonev
 */
public class ElExpression {

	/** The expression. */
	private String expression;

	/** The sub expressions. */
	private List<ElExpression> subExpressions;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		if (getSubExpressions().isEmpty()) {
			return getExpression();
		}
		if (getExpression() == null) {
			return getSubExpressions().toString();
		}
		return getExpression() + " -> " + getSubExpressions();
	}

	/**
	 * Getter method for expression.
	 *
	 * @return the expression
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Setter method for expression.
	 *
	 * @param expression the expression to set
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * Getter method for subExpressions.
	 *
	 * @return the subExpressions
	 */
	public List<ElExpression> getSubExpressions() {
		if (subExpressions == null) {
			subExpressions = new LinkedList<ElExpression>();
		}
		return subExpressions;
	}

	/**
	 * Setter method for subExpressions.
	 *
	 * @param subExpressions the subExpressions to set
	 */
	public void setSubExpressions(List<ElExpression> subExpressions) {
		this.subExpressions = subExpressions;
	}
}
