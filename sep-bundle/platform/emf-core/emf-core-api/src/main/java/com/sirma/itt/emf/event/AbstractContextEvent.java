package com.sirma.itt.emf.event;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base implementation for the context event
 * 
 * @author BBonev
 */
public abstract class AbstractContextEvent implements ContextEvent {

	/** The context. Context is lazy initialized only when needed */
	protected Map<String, Object> context;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> getContext() {
		if (context == null) {
			context = new LinkedHashMap<String, Object>();
		}
		return context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addToContext(String key, Object value) {
		getContext().put(key, value);
	}

}
