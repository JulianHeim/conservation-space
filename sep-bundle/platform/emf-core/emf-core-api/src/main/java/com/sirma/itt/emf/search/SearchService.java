package com.sirma.itt.emf.search;

import java.util.List;
import java.util.Map;

import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.model.Instance;

/**
 * Single entry point for searching data in EMF. The service provides means to get predefined search
 * filters also as executing a search under various search engines.
 * 
 * @author BBonev
 */
public interface SearchService {

	/**
	 * Gets a filter by the specified name for the given argument class type. If the filter requires
	 * any additional initializing it should be passed via the {@link Context} parameter.
	 * 
	 * @param <E>
	 *            the searched object type
	 * @param <S>
	 *            the build predefined filter arguments type
	 * @param filterName
	 *            the filter name to get
	 * @param resultType
	 *            the main result type
	 * @param context
	 *            the context used to pass arguments, could be <code>null</code>.
	 * @return the build filter filter or <code>null</code> if no such filter exists.
	 */
	<E extends Instance, S extends SearchArguments<E>> S getFilter(String filterName,
			Class<E> resultType, Context<String, Object> context);

	/**
	 * Gets a filter mapping for a location placeholder. Returns all filters applicable for the
	 * given placeholder.
	 * 
	 * @param <E>
	 *            the searched object type
	 * @param <S>
	 *            the build predefined filter arguments type
	 * @param placeHolder
	 *            the target place holder
	 * @param resultType
	 *            the expected main result type
	 * @param context
	 *            the context to use to initialize the filters
	 * @return the filters mapping
	 */
	<E extends Instance, S extends SearchArguments<E>> Map<String, S> getFilters(
			String placeHolder, Class<E> resultType, Context<String, Object> context);

	/**
	 * Gets the filters identifiers for the given placeholder and result type.
	 * 
	 * @param <E>
	 *            the searched object type
	 * @param placeHolder
	 *            the target place holder
	 * @param resultType
	 *            he expected main result type
	 * @return the filters list for the given place holder
	 */
	<E extends Instance> List<String> getFilters(String placeHolder, Class<E> resultType);

	/**
	 * Perform a search for the given arguments.
	 * 
	 * @param <E>
	 *            the searched object type
	 * @param <S>
	 *            the build predefined filter arguments type
	 * @param target
	 *            the main expected result type
	 * @param arguments
	 *            the search arguments to execute
	 */
	<E extends Instance, S extends SearchArguments<E>> void search(Class<?> target, S arguments);
}
