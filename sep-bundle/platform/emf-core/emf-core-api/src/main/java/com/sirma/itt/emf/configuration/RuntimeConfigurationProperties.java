package com.sirma.itt.emf.configuration;

/**
 * Configuration properties for runtime configuration use.
 *
 * @author BBonev
 */
public interface RuntimeConfigurationProperties {

	/** Tells the properties service to save properties without definition. */
	String SAVE_PROPERTIES_WITHOUT_DEFINITION = "SAVE_PROPERTIES_WITHOUT_DEFINITION";

	/** The use container filtering when searching for properties and definitions. */
	String DO_NOT_USE_CONTAINER_FILTERING = "USE_CONTAINER_FILTERING";

	/**
	 * The called method that supports this should only update the internal cache but not the
	 * database if connected.
	 */
	String CACHE_ONLY_OPERATION = "CACHE_ONLY_OPERATION";

	/** Provides current language code needed in a non session context. */
	String CURRENT_LANGUAGE_CODE = "CURRENT_LANGUAGE_CODE";

	/**
	 * The Constant DO_NO_CALL_DMS. If this constant is present in the current context then the
	 * current executing operation will not call DMS sub system
	 */
	String DO_NO_CALL_DMS = "DO_NO_CALL_DMS";

	/**
	 * The use recursive conversion. When executing type conversion if the converted object has
	 * supports a nested conversion (like tree elements) if the converter implementation should
	 * process the child elements or not. The default behavior is not to process child elements so
	 * this should be explicitly set.
	 */
	String USE_RECURSIVE_CONVERSION = "USE_RECURSIVE_CONVERSION";

	/** Disables firing of the instance persist event */
	String DO_NOT_FIRE_PERSIST_EVENT = "DO_NOT_FIRE_PERSIST_EVENT";

	/** The Constant SERIALIZATION_ENGINE used for thread local store of the default engines. */
	String SERIALIZATION_ENGINE = "$SERIALIZATION_ENGINE$";

	/**
	 * When saving complex objects sometimes if the default behavior is not desired the tree save
	 * could be disabled using this configuration.
	 */
	String DO_NOT_SAVE_CHILDREN = "DO_NOT_SAVE_CHILDREN";

	/** The do not load children override. */
	String DO_NOT_LOAD_CHILDREN_OVERRIDE = "DO_NOT_LOAD_CHILDREN_OVERRIDE";

	/** The currently executed operation by the current user if any. */
	String CURRENT_OPERATION = "CURRENT_OPERATION";

	/** The disable automatic links creation. */
	String DISABLE_AUTOMATIC_LINKS = "DISABLE_AUTOMATIC_LINKS";

	/** Audit disabled is configuration to stop automatic update of modified/modifier properties. */
	String AUDIT_MODIFICATION_DISABLED = "AUDIT_MODIFICATION_DISABLED";
}
