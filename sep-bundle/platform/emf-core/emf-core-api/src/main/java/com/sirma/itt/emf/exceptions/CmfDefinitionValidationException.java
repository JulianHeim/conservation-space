package com.sirma.itt.emf.exceptions;

/**
 * Thrown when validating the definitions model.
 *
 * @author BBonev
 */
public class CmfDefinitionValidationException extends EmfRuntimeException {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 7510675472677763684L;

	/**
	 * Instantiates a new cmf definition validation exception.
	 */
	public CmfDefinitionValidationException() {
		// nothing to do here
	}

	/**
	 * Instantiates a new cmf definition validation exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public CmfDefinitionValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new cmf definition validation exception.
	 *
	 * @param message
	 *            the message
	 */
	public CmfDefinitionValidationException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new cmf definition validation exception.
	 *
	 * @param cause
	 *            the cause
	 */
	public CmfDefinitionValidationException(Throwable cause) {
		super(cause);
	}

}
