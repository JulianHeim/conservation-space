package com.sirma.itt.objects.web.object;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.cmf.web.DocumentContext;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.web.action.event.EMFActionEvent;
import com.sirma.itt.objects.ObjectsTest;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;
import com.sirma.itt.objects.web.constants.ObjectNavigationConstants;

/**
 * The Class ObjectActionTest.
 * 
 * @author svelikov
 */
@Test
public class ObjectActionTest extends ObjectsTest {

	/** The action. */
	private ObjectAction action;
	private ObjectInstance objectInstance;

	/**
	 * Instantiates a new object action test.
	 */
	public ObjectActionTest() {
		action = new ObjectAction() {
			private DocumentContext documentContext = new DocumentContext();

			@Override
			public DocumentContext getDocumentContext() {
				return documentContext;
			}

			@Override
			public void setDocumentContext(DocumentContext documentContext) {
				this.documentContext = documentContext;
			}
		};

		objectInstance = createObjectInstance(1l);

		ReflectionUtils.setField(action, "log", log);
	}

	/**
	 * Creates the object test.
	 */
	public void createObjectTest() {
		EMFActionEvent event = createEventObject(null, null,
				ObjectActionTypeConstants.CREATE_OBJECT, null);

		// if there is no instance in the event: the context should not be populated and navigation
		// should be null
		action.createObject(event);
		DocumentContext documentContext = action.getDocumentContext();
		ObjectInstance actualObjectInstance = documentContext.getInstance(ObjectInstance.class);
		Assert.assertNull(actualObjectInstance);
		ObjectInstance actualContextInstance = (ObjectInstance) documentContext
				.getContextInstance();
		Assert.assertNull(actualContextInstance);
		String navigation = event.getNavigation();
		Assert.assertNull(navigation);

		// if there is an instance in the event: the context should be populated and navigation
		// should be set
		event = createEventObject(null, objectInstance, ObjectActionTypeConstants.CREATE_OBJECT,
				null);
		action.createObject(event);
		actualContextInstance = (ObjectInstance) documentContext.getContextInstance();
		Assert.assertEquals(actualContextInstance, objectInstance);
		navigation = event.getNavigation();
		Assert.assertEquals(navigation, ObjectNavigationConstants.OBJECT);
		Assert.assertTrue(documentContext.getCurrentOperation(ObjectInstance.class.getSimpleName()) == null);

		// clear context
		documentContext.clear();
	}

	/**
	 * Attach object test.
	 */
	public void attachObjectTest() {
		EMFActionEvent event = createEventObject(null, null,
				ObjectActionTypeConstants.ATTACH_OBJECT, null);

		// if there is no instance in the event: the context should not be populated and navigation
		// should be null
		action.attachObject(event);
		DocumentContext documentContext = action.getDocumentContext();
		ObjectInstance actualObjectInstance = documentContext.getInstance(ObjectInstance.class);
		Assert.assertNull(actualObjectInstance);
		String navigation = event.getNavigation();
		Assert.assertNull(navigation);

		// if there is an instance in the event: the context should be populated and navigation
		// should null because we should stay on the same page
		event = createEventObject(null, objectInstance, ObjectActionTypeConstants.ATTACH_OBJECT,
				null);
		action.attachObject(event);
		actualObjectInstance = documentContext.getInstance(ObjectInstance.class);
		Assert.assertEquals(actualObjectInstance, objectInstance);
		navigation = event.getNavigation();
		Assert.assertNull(navigation);

		// clear context
		documentContext.clear();
	}

}
