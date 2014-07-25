/**
 *
 */
package com.sirma.itt.emf.security;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.definition.dao.AllowedChildrenTypeProvider;
import com.sirma.itt.emf.domain.ObjectTypes;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.plugin.PluginUtil;
import com.sirma.itt.emf.security.model.Role;
import com.sirma.itt.emf.util.CollectionUtils;

/**
 * The RoleEvaluatorManagerServiceImpl.
 *
 * @author bbanchev
 */
@ApplicationScoped
public class RoleEvaluatorManagerServiceImpl implements RoleEvaluatorManagerService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = Logger.getLogger(RoleEvaluatorManagerServiceImpl.class);

	/** The type provider. */
	@Inject
	private AllowedChildrenTypeProvider typeProvider;

	/** The evaluators. */
	@Inject
	@ExtensionPoint(value = RoleEvaluator.TARGET_NAME)
	private Iterable<RoleEvaluator<Instance>> evaluators;

	/** The root evaluators. */
	private Map<Class<? extends Instance>, RoleEvaluator<? extends Instance>> rootEvaluators;

	/** The role comparator. */
	private Comparator<RoleEvaluator<Instance>> roleComparator = new RoleComparator();

	/**
	 * Initialize the root evaluators based on the registered extensions.
	 */
	@PostConstruct
	protected void init() {
		Map<Class<Instance>, Set<RoleEvaluator<Instance>>> supportedObjectsEvaluators = PluginUtil
				.parseSupportedObjects(evaluators);
		Set<RoleEvaluator<Instance>> instanceEvaluators = supportedObjectsEvaluators
				.remove(Instance.class);

		rootEvaluators = CollectionUtils.createHashMap(supportedObjectsEvaluators.size());

		for (Entry<Class<Instance>, Set<RoleEvaluator<Instance>>> entry : supportedObjectsEvaluators
				.entrySet()) {

			LinkedList<RoleEvaluator<Instance>> list = new LinkedList<RoleEvaluator<Instance>>(
					entry.getValue());
			if (instanceEvaluators != null) {
				// TODO some filter if the generic evaluator is not good for specific task
				list.addAll(instanceEvaluators);

				Collections.sort(list, roleComparator);
			}
			if (list.size() > 0) {
				RoleEvaluator<Instance> root = list.pop();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Adding root evaluator with key=" + entry.getKey()
							+ " and evluator=" + root + " and chain " + list);
				}
				root.addChainInOrder(list);
				rootEvaluators.put(entry.getKey(), root);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Role getHighestRole(Role role1, Role role2) {

		if ((role1 == null) || (role1.getRoleId() == null)) {
			return role2;
		}
		if ((role2 == null) || (role2.getRoleId() == null)) {
			return role1;
		}
		return role1.getRoleId().getGlobalPriority() > role2.getRoleId().getGlobalPriority() ? role1
				: role2;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEvaluatorType(Instance instance) {
		if (instance == null) {
			return ObjectTypes.INSTANCE;
		}
		String type = typeProvider.getTypeByInstance(instance.getClass());
		if (type != null) {
			return type;
		}
		return ObjectTypes.INSTANCE;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Instance, S extends Instance> RoleEvaluator<T> getRootEvaluator(S instance) {
		if (instance != null) {
			return (RoleEvaluator<T>) rootEvaluators.get(instance.getClass());
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Class<? extends Instance>, RoleEvaluator<? extends Instance>> getRootEvaluators() {
		return Collections.unmodifiableMap(rootEvaluators);
	}

	/**
	 * Gets the extension metadata.
	 *
	 * @param bean
	 *            the bean
	 * @return the extension metadata
	 */
	private Extension getExtensionMetadata(RoleEvaluator<Instance> bean) {
		Extension plugin = bean.getClass().getAnnotation(Extension.class);
		if (plugin == null) {
			// navigate to the actual class from proxy
			Class<?> superclass = bean.getClass().getSuperclass();
			plugin = superclass.getAnnotation(Extension.class);
		}
		return plugin;
	}

	/**
	 * Comparator for the registered role evaluators.
	 *
	 * @author BBonev
	 */
	private final class RoleComparator implements Comparator<RoleEvaluator<Instance>> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(RoleEvaluator<Instance> o1, RoleEvaluator<Instance> o2) {
			return Double.compare(getExtensionMetadata(o1).order(), getExtensionMetadata(o2)
					.order());
		}
	}
}
