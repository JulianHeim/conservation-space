package com.sirma.itt.idoc.web.document.actions;

import com.sirma.itt.emf.security.model.EmfAction;

/**
 * @author yasko
 */
public class IdocAction extends EmfAction {

	private int displayOrder;

	/**
	 * @param id
	 *            asd
	 * @param label
	 *            asd
	 */
	public IdocAction(IdocActionDefinition id, String label) {
		super(id.getId());
		setOnclick(id.getOnClick());
		setIconImagePath(id.getIcon());
		setLabel(label);
		setDisplayOrder(id.ordinal());
	}

	/**
	 * Getter method for displayOrder.
	 * 
	 * @return the displayOrder
	 */
	public int getDisplayOrder() {
		return displayOrder;
	}

	/**
	 * Setter method for displayOrder.
	 * 
	 * @param displayOrder
	 *            the displayOrder to set
	 */
	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Override
	public String toString() {
		return getClass().getName() + " " + getActionId();
	}
}
