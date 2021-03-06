package com.sirma.itt.emf.codelist.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * The Interface CodelistFilter.
 *
 * @author BBonev
 */
@Qualifier
@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CodelistFilter {

	/**
	 * Target codelist.
	 */
	int codelist() default 0;

	/**
	 * Event filter ID.
	 */
	String filterEvent() default "";
}
