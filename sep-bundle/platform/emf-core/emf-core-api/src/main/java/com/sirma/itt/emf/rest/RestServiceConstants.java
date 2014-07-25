package com.sirma.itt.emf.rest;

import javax.ws.rs.core.MediaType;

/**
 * Some Rest services constants
 * 
 * @author BBonev
 */
public interface RestServiceConstants {

	/** The charset=utf-8. */
	String CHARSET_UTF_8 = "charset=utf-8";

	/** The application/json utf-8 encoded. */
	String APPLICATION_JSON_UTF_ENCODED = MediaType.APPLICATION_JSON + "; " + CHARSET_UTF_8;

	/** The text html utf encoded. */
	String TEXT_HTML_UTF_ENCODED = MediaType.TEXT_HTML + "; " + CHARSET_UTF_8;
}
