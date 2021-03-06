package com.sirma.itt.cmf.services.mock;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.evaluation.BaseEvaluator;
import com.sirma.itt.emf.evaluation.ExpressionContext;
import com.sirma.itt.emf.instance.model.Instance;

/**
 * Evaluates expressions for building bookmarkable links for instance.
 * 
 * @author svelikov
 */
@ApplicationScoped
public class InstanceLinkExpressionEvaluator extends BaseEvaluator {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 838564568489566278L;

	/** The Constant FIELD_PATTERN. */
	private static final Pattern FIELD_PATTERN = Pattern.compile("\\$\\{link\\(([\\w]+)\\)\\}");

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
		String link = "#";
		String instanceKey = matcher.group(1);
		if (StringUtils.isNotNullOrEmpty(instanceKey) && context.containsKey(instanceKey)) {
			Instance instance = (Instance) context.get(instanceKey);
			link = instance.getIdentifier();
		}
		return link;
	}

}
