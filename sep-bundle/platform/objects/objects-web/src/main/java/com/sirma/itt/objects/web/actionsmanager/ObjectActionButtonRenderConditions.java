package com.sirma.itt.objects.web.actionsmanager;

import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.actionsmanager.ActionButtonRenderConditions;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;

/**
 * Render condition methods. Operation buttons that perform more specific tasks or has different
 * implementation in its templates should be added to noDefaultButtons set.
 * 
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class ObjectActionButtonRenderConditions extends ActionButtonRenderConditions {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8764235021793660642L;

	/**
	 * Should render move same case operation button.
	 * 
	 * @param action
	 *            current action
	 * @param instance
	 *            current instance
	 * @return true, if successful
	 */
	public boolean renderObjectMoveSameCase(Action action, Instance instance) {
		return ObjectActionTypeConstants.OBJECT_MOVE_SAME_CASE.equals(action.getActionId())
				&& isNonDefaultAction(action, instance);
	}

	/**
	 * Should render clone object operation button.
	 * 
	 * @param action
	 *            current action
	 * @param instance
	 *            current instance
	 * @return true, if successful
	 */
	public boolean renderCloneObjectButton(Action action, Instance instance) {
		return ActionTypeConstants.CLONE.equals(action.getActionId())
				&& isNonDefaultAction(action, instance);
	}

	/**
	 * Should render add thumbnail operation button.
	 * 
	 * @param action
	 *            current action
	 * @param instance
	 *            current instance
	 * @return true, if successful
	 */
	public boolean renderAddObjectThumbnail(Action action, Instance instance) {
		return ObjectActionTypeConstants.ADD_THUMBNAIL.equals(action.getActionId())
				&& isNonDefaultAction(action, instance);
	}

	/**
	 * Should render attach object operation button.
	 * 
	 * @param action
	 *            current action
	 * @param instance
	 *            current instance
	 * @return true, if successful
	 */
	public boolean renderAttachObject(Action action, Instance instance) {
		return ObjectActionTypeConstants.ATTACH_OBJECT.equals(action.getActionId())
				&& isNonDefaultAction(action, instance);
	}

}
