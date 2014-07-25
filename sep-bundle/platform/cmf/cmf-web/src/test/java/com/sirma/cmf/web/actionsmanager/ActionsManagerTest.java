package com.sirma.cmf.web.actionsmanager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sirma.cmf.CMFTest;
import com.sirma.cmf.web.DocumentContext;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.NullInstance;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.EmfAction;
import com.sirma.itt.emf.web.action.event.EMFActionEvent;

/**
 * Allowed action test.
 * 
 * @author svelikov
 */
@Test
public class ActionsManagerTest extends CMFTest {

	private static final String DASHBOARD = "dashboard";

	private static final String CREATE_CASE = "createCase";

	/** The manager. */
	private final ActionsManager manager;

	private final List<EMFActionEvent> firedEvents = new ArrayList<EMFActionEvent>();

	private AuthorityService authorityService;

	/**
	 * Constructor initializes the class under test.
	 */
	public ActionsManagerTest() {

		manager = new ActionsManager() {

			private DocumentContext docContext = new DocumentContext();

			@Override
			protected EMFActionEvent fireActionEvent(Action action, String actionId,
					Instance instance, String navigation) {
				EMFActionEvent event = new EMFActionEvent(instance, navigation, actionId, action);
				firedEvents.add(event);
				return event;
			}

			@Override
			public DocumentContext getDocumentContext() {
				return docContext;
			}

			@Override
			public void setDocumentContext(DocumentContext documentContext) {
				docContext = documentContext;
			}
		};

		authorityService = Mockito.mock(AuthorityService.class);

		ReflectionUtils.setField(manager, "log", LOG);
		ReflectionUtils.setField(manager, "authorityService", authorityService);
	}

	/**
	 * Reset test.
	 */
	@BeforeMethod
	public void resetTest() {
		manager.getDocumentContext().clear();
	}

	/**
	 * Execute context action test.
	 */
	public void executeContextActionTest() {
		String navigation = manager.executeContextAction(null, null, null);
		assertNull(navigation);
		assertNull(manager.getDocumentContext().getContextInstance());
		assertNull(manager.getDocumentContext().getSelectedAction());

		//
		navigation = manager.executeContextAction(CREATE_CASE, DASHBOARD, null);
		assertEquals(navigation, DASHBOARD);
		assertNull(manager.getDocumentContext().getContextInstance());
		assertNull(manager.getDocumentContext().getSelectedAction());
		String currentOperation = manager.getDocumentContext().getCurrentOperation(
				NullInstance.class.getSimpleName());
		assertEquals(currentOperation, CREATE_CASE);
	}

	/**
	 * Execute allowed action test.
	 */
	public void executeAllowedActionTest() {
		String operation = "delete";
		Instance caseInstance = createCaseInstance(Long.valueOf(1L));
		EmfAction action = new EmfAction(operation);
		String navigation = manager.executeAllowedAction(action, caseInstance);

		// test navigation - should be null
		assertNull(navigation);

		// check if an event is fired as result of this method call
		assertFalse(firedEvents.isEmpty());

		// check fired event attributes
		EMFActionEvent event = firedEvents.get(0);
		assertEquals(event.getActionId(), operation);
		assertEquals(event.getInstance(), caseInstance);
		assertEquals(event.getNavigation(), null);
		assertEquals(event.getAction(), action);

		// check if executed action is stored in the context
		assertEquals(manager.getDocumentContext().getSelectedAction(), action);

		// check if in context is set current operation
		assertEquals(
				manager.getDocumentContext()
						.getCurrentOperation(CaseInstance.class.getSimpleName()), operation);
	}

	/**
	 * Gets the actions test.
	 */
	public void getActionsTest() {
		List<Action> actualActions = manager.getActions(null, null);
		assertTrue(actualActions.isEmpty());

		//
		Instance caseInstance = createCaseInstance(Long.valueOf(1L));
		Set<Action> actions = new HashSet<>();
		Action action = new EmfAction(CREATE_CASE);
		actions.add(action);
		Mockito.when(authorityService.getAllowedActions(caseInstance, "my-cases-panel"))
				.thenReturn(actions);
		actualActions = manager.getActions(caseInstance, "my-cases-panel");
		assertTrue(actualActions.size() == 1);
		assertEquals(actualActions.get(0), action);
	}

	/**
	 * Test for getActionStyleClass method.
	 */
	public void getActionStyleClassTest() {
		String actual = manager.getActionStyleClass(null, null, null, null);
		assertEquals(actual, "allowed-action-button");

		actual = manager.getActionStyleClass("", null, null, "");
		assertEquals(actual, "allowed-action-button");

		actual = manager.getActionStyleClass("case-delete-btn", null, null, null);
		assertEquals(actual, "case-delete-btn");

		CaseInstance caseInstance = createCaseInstance(null);
		actual = manager.getActionStyleClass("case-delete-btn", caseInstance, null, null);
		assertEquals(actual, "caseinstance case-delete-btn");

		Action action = new EmfAction("deleteCase");
		actual = manager.getActionStyleClass("case-delete-btn", caseInstance, action, null);
		assertEquals(actual, "caseinstance case-delete-btn deleteCase");

		actual = manager.getActionStyleClass("case-delete-btn", caseInstance, action, "compact");
		assertEquals(actual, "caseinstance case-delete-btn deleteCase has-tooltip");
	}
}
