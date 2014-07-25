package com.sirma.itt.emf.plugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.emf.exceptions.EmfConfigurationException;
import com.sirma.itt.emf.util.CollectionUtils;

/**
 * Provides a utility function for working with plugins
 * 
 * @author BBonev
 */
public class PluginUtil {

	/** The Constant logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtil.class);

	/**
	 * Parses the supported objects and creates a mapping of them by class. If class is already
	 * defined as supported by other extension the method could thrown an exception if needed.
	 * 
	 * @param <S>
	 *            the plugin type
	 * @param plugins
	 *            the plugin instances to iterate
	 * @param allowedDuplicates
	 *            the allowed duplicates is <code>true</code> the method will NOT throw an exception
	 *            if a class is defined by two or more extensions and will override the supported
	 *            extension with the latest in the list.
	 * @return a mapping of extensions by supported class.
	 * @throws EmfConfigurationException
	 *             if the allowedDuplicates is <code>false</code> and is found a class that is
	 *             provided in more then one extension. The exception will be thrown even the same
	 *             extension provides the class more then once also.
	 */
	public static <S extends SupportablePlugin> Map<Class<?>, S> parseSupportedObjects(
			Iterable<S> plugins, boolean allowedDuplicates) throws EmfConfigurationException {
		Map<Class<?>, S> mapping = CollectionUtils.createLinkedHashMap(200);
		for (S s : plugins) {
			List<Class<?>> list = s.getSupportedObjects();
			if ((list != null) && !list.isEmpty()) {
				for (Class<?> clazz : list) {
					if (mapping.containsKey(clazz)) {
						String msg = "Overriding the handling extension for "
								+ clazz.getCanonicalName() + " with " + s;
						if (allowedDuplicates) {
							LOGGER.warn(msg);
						} else {
							throw new EmfConfigurationException(msg);
						}
					}
					mapping.put(clazz, s);
				}
			} else {
				LOGGER.warn("Provided invalid extension " + s.getClass()
						+ ". Should return supported objects!");
			}
		}
		return mapping;
	}

	/**
	 * Parses the supported objects and creates a mapping of them by class. If class is already
	 * defined the new extension is added to the set for this class.
	 * 
	 * @param <S>
	 *            the plugin type
	 * @param <T>
	 *            the class type
	 * @param plugins
	 *            the plugin instances to iterate
	 * @return a map with set of extensions for supported class.
	 */
	@SuppressWarnings("unchecked")
	public static <S extends SupportablePlugin, T> Map<Class<T>, Set<S>> parseSupportedObjects(
			Iterable<S> plugins) {
		Map<Class<T>, Set<S>> mapping = CollectionUtils.createLinkedHashMap(200);
		for (S s : plugins) {
			List<Class<?>> list = s.getSupportedObjects();
			if ((list != null) && !list.isEmpty()) {
				for (Class<?> clazz : list) {
					if (!mapping.containsKey(clazz)) {
						mapping.put((Class<T>) clazz, new LinkedHashSet<S>());
					}
					if (mapping.get(clazz).contains(s)) {
						LOGGER.warn("Overriding " + s);
					}
					mapping.get(clazz).add(s);
				}
			} else {
				LOGGER.warn("Provided invalid extension " + s.getClass()
						+ ". Should return supported objects!");
			}
		}
		return mapping;
	}
}
