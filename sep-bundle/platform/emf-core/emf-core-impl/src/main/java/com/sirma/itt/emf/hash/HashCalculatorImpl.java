package com.sirma.itt.emf.hash;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.plugin.PluginUtil;

/**
 * Default {@link HashCalculator} implementation
 * 
 * @author BBonev
 */
@ApplicationScoped
public class HashCalculatorImpl implements HashCalculator {

	/** The extension. */
	@Inject
	@ExtensionPoint(HashCalculatorExtension.TARGET_NAME)
	private Iterable<HashCalculatorExtension> extensions;

	/** The mapping. */
	private Map<Class<?>, HashCalculatorExtension> mapping;

	/**
	 * Initialize mapping.
	 */
	@PostConstruct
	public void initializeMapping() {
		mapping = PluginUtil.parseSupportedObjects(extensions, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer computeHash(Object object) {
		if (object == null) {
			return 0;
		}
		if (object instanceof Collection) {
			Collection<?> collection = (Collection<?>) object;
			if (collection.isEmpty()) {
				return object.hashCode();
			}
			// if there is an extension that can handle the element objects we calculate the
			// hash code by the sum of the hash codes of all elements.
			int result = 0;
			for (Object o : collection) {
				result += computeHash(o);
			}
			return result;
		} else if (object instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) object;
			if (map.isEmpty()) {
				return object.hashCode();
			}
			int result = 0;
			for (Entry<?, ?> o : map.entrySet()) {
				result += (computeHash(o.getKey()) + computeHash(o.getValue()));
			}
			return result;
		}
		HashCalculatorExtension extension = mapping.get(object.getClass());
		if (extension != null) {
			return extension.computeHash(this, object);
		}
		// if we can't handle it, return the default
		return object.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsByHash(Object object1, Object object2) {
		if (object1 == object2) {
			// the same reference no need to check anything
			return true;
		}
		Integer hash1 = computeHash(object1);
		Integer hash2 = computeHash(object2);
		return (hash1 != null) && (hash2 != null) && (hash1.compareTo(hash2) == 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatisticsEnabled(boolean enabled) {
		HashStatistics.setStatisticsEnabled(enabled);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Pair<String, String>> getStatistics(boolean reset) {
		return HashStatistics.getStatistics(reset);
	}

}
