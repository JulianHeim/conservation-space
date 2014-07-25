package com.sirma.itt.emf.evaluation;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.instance.model.EmfInstance;

/**
 * The Class IdEvaluatorTest.
 * 
 * @author BBonev
 */
public class IdEvaluatorTest extends BaseEvaluatorTest {

	/**
	 * Test evaluation.
	 */
	@Test
	public void testEvaluation() {
		ExpressionsManager manager = createManager();
		EmfInstance target = new EmfInstance();
		target.setContentManagementId("cmId");
		target.setDmsId("dmsId");
		target.setIdentifier("identifier");
		target.setId("dbId");

		ExpressionContext context = manager.createDefaultContext(target, null, null);
		Assert.assertEquals("cmId", manager.evaluateRule("${id}", String.class, context));
		Assert.assertEquals("cmId", manager.evaluateRule("${id.cm}", String.class, context));
		Assert.assertEquals("dmsId", manager.evaluateRule("${id.dm}", String.class, context));
		Assert.assertEquals("identifier", manager.evaluateRule("${id.type}", String.class, context));
		Assert.assertEquals("dbId", manager.evaluateRule("${id.db}", String.class, context));

		// add matcher and from extensions
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<ExpressionEvaluator> initializeEvaluators(ExpressionEvaluatorManager manager,
			TypeConverter converter) {
		List<ExpressionEvaluator> list = super.initializeEvaluators(manager, converter);
		// add id evaluator
		IdEvaluator evaluator = new IdEvaluator();
		list.add(initEvaluator(evaluator, manager, createTypeConverter()));
		return list;
	}

}
