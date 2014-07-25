package com.sirma.itt.emf.search;

import java.util.List;
import java.util.Map;

import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.SupportablePlugin;

/**
 * Extension for {@link SearchService}. Realizes the concrete search implementations per object type
 * and query language.
 * 
 * @param <I>
 *            the type of the main search type
 * @author BBonev
 */
public interface SearchServiceFilterExtension<I extends Instance> extends SupportablePlugin {

	/** The target name. */
	String TARGET_NAME = "searchServiceExtension";

	/**
	 * Gets the filter by name and initialized using the given context if any.
	 * 
	 * @param <S>
	 *            the result arguments type
	 * @param filterName
	 *            the filter name
	 * @param context
	 *            the context if any, could be <code>null</code>.
	 * @return the filter or <code>null</code> if not found/supported.
	 */
	<S extends SearchArguments<I>> S getFilter(String filterName, Context<String, Object> context);

	/**
	 * Gets the filters for the given place holder.
	 * 
	 * @param <S>
	 *            the result arguments type
	 * @param placeHolder
	 *            the place holder
	 * @param context
	 *            the context if any, could be <code>null</code>.
	 * @return the filters mapping the placeholder, as filter name -> search arguments.
	 */
	<S extends SearchArguments<I>> Map<String, S> getFilters(String placeHolder,
			Context<String, Object> context);

	/**
	 * Gets the list of supported filters for the given placeholder.
	 * 
	 * @param placeHolder
	 *            the place holder
	 * @return the filters
	 */
	List<String> getFilters(String placeHolder);
}
