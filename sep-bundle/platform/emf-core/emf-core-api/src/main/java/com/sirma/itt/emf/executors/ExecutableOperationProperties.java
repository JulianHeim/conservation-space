package com.sirma.itt.emf.executors;


/**
 * List of base constants used to describe data for {@link ExecutableOperation}s
 * 
 * @author BBonev
 */
public interface ExecutableOperationProperties {

	/*
	 * Request keys.
	 */
	/** The list of operations to be executed. */
	String OPERATIONS = "operations";
	/** The operation id that should be executed. */
	String OPERATION = "operation";
	/** The target instance id. */
	String ID = "id";
	/** The target instance type. */
	String TYPE = "type";
	/** The parent instance id. */
	String PARENT_ID = "parentId";
	/** The parent instance type. */
	String PARENT_TYPE = "parentType";
	/** A properties mapping for the target instance. */
	String PROPERTIES = "properties";
	/** The target definition id or other definition. */
	String DEFINITION = "definition";
	/** The revision for the provided definition id via {@link #DEFINITION}. */
	String REVISION = "revision";
	/** The dms id. */
	String DMS_ID = "dmsId";

	/*
	 * Response keys
	 */
	/**
	 * The response from the execution of the operation. The content could be missing if the
	 * operation does returns anything or could be JSON object or array. It's up to the operation
	 * implementation do decide what to return for his successful execution.<br>
	 * NOTE: The content will be placed only on successful execution.
	 */
	String RESPONSE = "response";
	/**
	 * The response state object that contains information about the execution of the current
	 * operation. Required field is {@link #STATUS} property and optional are {@link #MESSAGE}.
	 */
	String RESPONSE_STATE = "responseState";
	/** The response status code for the particular response defined in {@link #RESPONSE_STATE}. */
	String STATUS = "status";
	/**
	 * The message placed in the {@link #RESPONSE_STATE} object to describe any problem or to give
	 * information about the current operation execution.
	 */
	String MESSAGE = "message";
	/**
	 * The exception stack trace placed in the {@link #RESPONSE_STATE} object to list the full
	 * exception trace if any.
	 */
	String STACK_TRACE = "stacktrace";

	/*
	 * Context variables
	 */
	/**
	 * Context variable. A temporary key that holds any exceptions or information message that need
	 * to be returned to the user for the given operation. When building the response the content of
	 * the property will be transfered to {@link #MESSAGE} property inside of
	 * {@link #RESPONSE_STATE}.
	 */
	String CTX_STATUS_MESSAGE = "$statusMessage$";
	/**
	 * Context variable. A temporary key that holds any exceptions that need to be returned to the
	 * user for the given operation. When building the response the content of the property will be
	 * transfered to {@link #STACK_TRACE} property inside of {@link #RESPONSE_STATE}.
	 */
	String CTX_STACK_TRACE = "$stacktrace$";
	/** Context variable. Internal property to define a restore point for rollback. */
	String CTX_ROLLBACK = "rollback";
	/**
	 * Context variable. The key at which the target instance information could be found (most
	 * likely a {@link com.sirma.itt.emf.instance.model.InstanceReference}).
	 */
	String CTX_TARGET = "target";
	/**
	 * Context variable. The key at which the parent instance information could be found if present
	 * (most likely a {@link com.sirma.itt.emf.instance.model.InstanceReference}).
	 */
	String CTX_PARENT = "parent";
}
