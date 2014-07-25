package com.sirma.itt.emf.event.instance;

import java.io.Serializable;

import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.util.Documentation;

/**
 * Event fired after persist of every object. The event is fired just after the database persist in
 * the same transaction. If the observer modifies the object the changes will not be saved in the DB
 * unless persisted manually.
 * 
 * @param <I>
 *            the instance type
 * @author BBonev
 */
@Documentation("Event fired after persist of every object. The event is fired just after the database persist in the same transaction. If the observer modifies the object the changes will not be saved in the DB unless persisted manually.")
public class InstancePersistedEvent<I extends Instance> extends
		EntityPersistedEvent<I, Serializable> {

	/** The old version. */
	private I oldVersion;

	/**
	 * Instantiates a new instance persisted event.
	 * 
	 * @param instance
	 *            the instance
	 * @param oldVersion
	 *            the old version
	 */
	public InstancePersistedEvent(I instance, I oldVersion) {
		super(instance);
		this.oldVersion = oldVersion;
	}

	/**
	 * Gets the old version if any.
	 * 
	 * @return the old version
	 */
	public I getOldVersion() {
		return oldVersion;
	}

}
