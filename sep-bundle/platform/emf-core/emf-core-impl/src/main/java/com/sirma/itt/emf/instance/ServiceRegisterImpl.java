package com.sirma.itt.emf.instance;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.sirma.itt.emf.definition.dao.DefinitionAccessor;
import com.sirma.itt.emf.definition.load.DefinitionCompilerCallback;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.instance.dao.InstanceDao;
import com.sirma.itt.emf.instance.dao.InstanceEventProvider;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.instance.dao.InstanceToServiceRegisterExtension;
import com.sirma.itt.emf.instance.dao.ServiceRegister;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.plugin.PluginUtil;
import com.sirma.itt.emf.properties.dao.PropertyModelCallback;
import com.sirma.itt.emf.properties.model.PropertyModel;

/**
 * Default implementation of the {@link ServiceRegister}. If extension is not found then
 * <code>null</code> is returned by all methods.
 * 
 * @author BBonev
 */
@ApplicationScoped
public class ServiceRegisterImpl implements ServiceRegister {

	/** The extensions. */
	@Inject
	@ExtensionPoint(InstanceToServiceRegisterExtension.TARGET_NAME)
	private Iterable<InstanceToServiceRegisterExtension> extensions;

	/** The extension mapping. */
	private Map<Class<?>, InstanceToServiceRegisterExtension> extensionMapping;

	/**
	 * Initialize extension mapping.
	 */
	@PostConstruct
	public void initializeExtensionMapping() {
		extensionMapping = PluginUtil.parseSupportedObjects(extensions, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <I extends Instance, D extends DefinitionModel> InstanceService<I, D> getInstanceService(
			Object object) {
		InstanceToServiceRegisterExtension extension = getExtension(object);
		if (extension == null) {
			return null;
		}
		return extension.getInstanceService();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <I extends Instance> InstanceDao<I> getInstanceDao(Object object) {
		InstanceToServiceRegisterExtension extension = getExtension(object);
		if (extension == null) {
			return null;
		}
		return extension.getInstanceDao();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <P extends PropertyModel> PropertyModelCallback<P> getModelCallback(Object object) {
		InstanceToServiceRegisterExtension extension = getExtension(object);
		if (extension == null) {
			return null;
		}
		return extension.getModelCallback();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DefinitionAccessor getDefinitionAccessor(Object object) {
		InstanceToServiceRegisterExtension extension = getExtension(object);
		if (extension == null) {
			return null;
		}
		return extension.getDefinitionAccessor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends TopLevelDefinition> DefinitionCompilerCallback<T> getCompilerCallback(
			Object object) {
		InstanceToServiceRegisterExtension extension = getExtension(object);
		if (extension == null) {
			return null;
		}
		return extension.getCompilerCallback();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <I extends Instance> InstanceEventProvider<I> getEventProvider(Object object) {
		InstanceToServiceRegisterExtension extension = getExtension(object);
		if (extension == null) {
			return null;
		}
		return extension.getEventProvider();
	}

	/**
	 * Gets the extension.
	 * 
	 * @param object
	 *            the object
	 * @return the extension
	 */
	private InstanceToServiceRegisterExtension getExtension(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof Class) {
			return extensionMapping.get(object);
		}
		return extensionMapping.get(object.getClass());
	}

	@Override
	public Set<Class<?>> getSupportedObjects() {
		return Collections.unmodifiableSet(extensionMapping.keySet());
	}

	@Override
	public Set<InstanceToServiceRegisterExtension> getExtensions() {
		return Collections.unmodifiableSet(new LinkedHashSet<InstanceToServiceRegisterExtension>(
				extensionMapping.values()));
	}

}
