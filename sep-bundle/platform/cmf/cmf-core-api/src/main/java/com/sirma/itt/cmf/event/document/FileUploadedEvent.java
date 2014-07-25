package com.sirma.itt.cmf.event.document;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.emf.event.AbstractInstanceEvent;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.util.Documentation;

/**
 * The FileUploadedEvent is fired when a document is uploaded but before the current instance is
 * persisted. This event holds the uploaded document as and the current instance object.
 * 
 * @author svelikov
 */
@Documentation("The FileUploadedEvent is fired when a document is uploaded but before the current instance is persisted. This event holds the uploaded document as and the current instance object.")
public class FileUploadedEvent extends AbstractInstanceEvent<DocumentInstance> {

	/** The current instance during this event was fired. */
	private Instance currentInstance;

	/**
	 * Instantiates a new file uploaded event.
	 * 
	 * @param instance
	 *            the instance
	 * @param currentInstance
	 *            the current instance during this event was fired.
	 */
	public FileUploadedEvent(DocumentInstance instance, Instance currentInstance) {
		super(instance);
		this.currentInstance = currentInstance;
	}

	/**
	 * Getter method for currentInstance.
	 * 
	 * @return the currentInstance
	 */
	public Instance getCurrentInstance() {
		return currentInstance;
	}

	/**
	 * Setter method for currentInstance.
	 * 
	 * @param currentInstance
	 *            the currentInstance to set
	 */
	public void setCurrentInstance(Instance currentInstance) {
		this.currentInstance = currentInstance;
	}

}
