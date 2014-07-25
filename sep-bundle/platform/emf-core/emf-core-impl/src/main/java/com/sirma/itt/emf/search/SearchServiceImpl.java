package com.sirma.itt.emf.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.plugin.PluginUtil;
import com.sirma.itt.emf.security.Secure;

/**
 * Default implementation for the search service.
 *
 * @author BBonev
 */
@ApplicationScoped
public class SearchServiceImpl implements SearchService, Serializable {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 7435785571309548636L;

	/** The extension. */
	@Inject
	@ExtensionPoint(SearchServiceFilterExtension.TARGET_NAME)
	private Iterable<SearchServiceFilterExtension<Instance>> extension;

	/** The engines. */
	@Inject
	@ExtensionPoint(SearchEngine.TARGET_NAME)
	private Iterable<SearchEngine> engines;

	/** The mapping. */
	private Map<Class<?>, SearchServiceFilterExtension<Instance>> mapping;

	/**
	 * Initialize mappings.
	 */
	@PostConstruct
	public void initializeMappings() {
		mapping = PluginUtil.parseSupportedObjects(extension, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends Instance, S extends SearchArguments<E>> S getFilter(String filterName,
			Class<E> resultType, Context<String, Object> context) {
		return getExtension(resultType).getFilter(filterName, context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends Instance, S extends SearchArguments<E>> Map<String, S> getFilters(
			String placeHolder, Class<E> resultType, Context<String, Object> context) {
		return getExtension(resultType).getFilters(placeHolder, context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends Instance> List<String> getFilters(String placeHolder, Class<E> resultType) {
		return getExtension(resultType).getFilters(placeHolder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Secure
	@Override
	public <E extends Instance, S extends SearchArguments<E>> void search(Class<?> target,
			S arguments) {
		for (SearchEngine engine : engines) {
			if (engine.isSupported(target, arguments)) {
				engine.search(target, arguments);
				return;
			}
		}
		// REVIEW or throw an exception
	}

	/**
	 * Gets the extension.
	 * 
	 * @param <I>
	 *            the generic type
	 * @param target
	 *            the target
	 * @return the extension
	 */
	@SuppressWarnings("unchecked")
	protected <I extends Instance> SearchServiceFilterExtension<I> getExtension(Class<I> target) {
		SearchServiceFilterExtension<Instance> serviceExtension = mapping.get(target);
		return (SearchServiceFilterExtension<I>) serviceExtension;
	}

}
