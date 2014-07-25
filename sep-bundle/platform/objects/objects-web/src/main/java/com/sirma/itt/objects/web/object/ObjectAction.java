package com.sirma.itt.objects.web.object;

import java.io.Serializable;

import javax.enterprise.event.Observes;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.EntityAction;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.security.action.EMFAction;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.web.action.event.EMFActionEvent;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;
import com.sirma.itt.objects.web.constants.ObjectNavigationConstants;

/**
 * The Class ObjectAction handles object actions and observers.
 * 
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class ObjectAction extends EntityAction implements Serializable {

	private static final long serialVersionUID = 4107319526384382508L;

	/**
	 * Create object observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void createObject(
			@Observes @EMFAction(value = ObjectActionTypeConstants.CREATE_OBJECT, target = SectionInstance.class) final EMFActionEvent event) {
		log.debug("ObjectsWeb: Executing observer ObjectAction.createObject");

		Instance objectInstance = event.getInstance();
		if (objectInstance != null) {
			getDocumentContext().addInstance(objectInstance);
			getDocumentContext().addContextInstance(objectInstance);

			getDocumentContext().remove(ObjectInstance.class.getSimpleName());
			getDocumentContext().resetCurrentOperation(ObjectInstance.class.getSimpleName());
			event.setNavigation(ObjectNavigationConstants.OBJECT);
		}
	}

	/**
	 * Attach object observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void attachObject(
			@Observes @EMFAction(value = ObjectActionTypeConstants.ATTACH_OBJECT, target = SectionInstance.class) final EMFActionEvent event) {
		log.debug("ObjectsWeb: Executing observer SectionObjectsAction.attachObject");

		getDocumentContext().addInstance(event.getInstance());
	}

	/**
	 * Clone object observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void cloneObject(
			@Observes @EMFAction(value = ObjectActionTypeConstants.CLONE, target = ObjectInstance.class) final EMFActionEvent event) {
		log.debug("ObjectsWeb: Executing observer ObjectAction.clone");

		getDocumentContext().addInstance(event.getInstance());
	}

	/**
	 * Edit object observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void editObject(
			@Observes @EMFAction(value = ObjectActionTypeConstants.EDIT_DETAILS, target = ObjectInstance.class) final EMFActionEvent event) {
		log.debug("ObjectsWeb: Executing observer ObjectAction.editObject");

		Instance objectInstance = event.getInstance();
		if (objectInstance != null) {
			getDocumentContext().addInstance(objectInstance);
			getDocumentContext().addContextInstance(objectInstance);
			event.setNavigation(ObjectNavigationConstants.OBJECT);
		}
	}

	/**
	 * Delete object observer.
	 * 
	 * @param event
	 *            the event
	 */
	public void deleteObject(
			@Observes @EMFAction(value = ObjectActionTypeConstants.DELETE, target = ObjectInstance.class) final EMFActionEvent event) {
		log.debug("ObjectsWeb: Executing observer ObjectAction.delete");

		ObjectInstance objectInstance = (ObjectInstance) event.getInstance();
		String currentOperation = getDocumentContext().getCurrentOperation(
				ObjectInstance.class.getSimpleName());
		if (objectInstance != null) {
			Operation operation = new Operation(currentOperation);
			instanceService.delete(objectInstance, operation, false);
		}
	}

}
