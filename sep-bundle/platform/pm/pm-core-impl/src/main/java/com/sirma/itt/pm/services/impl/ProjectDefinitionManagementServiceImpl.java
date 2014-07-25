package com.sirma.itt.pm.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.adapter.DMSDefintionAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.definition.DefinitionManagementServiceExtension;
import com.sirma.itt.emf.definition.model.DataType;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.io.descriptor.ResourceFileDescriptor;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.pm.domain.definitions.ProjectDefinition;
import com.sirma.itt.pm.domain.definitions.impl.ProjectDefinitionImpl;
import com.sirma.itt.pm.xml.schema.PmSchemaBuilder;

/**
 * Adds definitions loading for project specific definitions.
 * 
 * @author BBonev
 */
@ApplicationScoped
@Extension(target = DefinitionManagementServiceExtension.TARGET_NAME, order = 30)
public class ProjectDefinitionManagementServiceImpl implements DefinitionManagementServiceExtension {

	/** The adapter service. */
	@Inject
	private DMSDefintionAdapterService adapterService;

	/** The logger. */
	@Inject
	private Logger logger;

	/** The Constant SUPPORTED_OBJECTS. */
	private static final List<Class<?>> SUPPORTED_OBJECTS = new ArrayList<Class<?>>(Arrays.asList(
			DataTypeDefinition.class, DataType.class, ProjectDefinition.class,
			ProjectDefinitionImpl.class));

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Class<?>> getSupportedObjects() {
		return SUPPORTED_OBJECTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<DMSFileDescriptor> getDefinitions(Class<?> definitionClass) {
		if (ProjectDefinition.class.isAssignableFrom(definitionClass)) {
			return getProjectDefinitions();
		} else if (DataTypeDefinition.class.isAssignableFrom(definitionClass)) {
			return getTypeDefinitions();
		}
		return null;
	}

	/**
	 * Gets the type definitions.
	 * 
	 * @return the type definitions
	 */
	public List<DMSFileDescriptor> getTypeDefinitions() {
		List<DMSFileDescriptor> result = new ArrayList<>(1);
		// add the specific types
		result.add(new ResourceFileDescriptor("../types.xml", PmSchemaBuilder.class));
		return Collections.unmodifiableList(result);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<DMSFileDescriptor> getProjectDefinitions() {
		try {
			return adapterService.getDefinitions(ProjectDefinition.class);
		} catch (DMSException e) {
			logger.warn("Failed to retrieve project definitions from DMS", e);
		}
		return Collections.emptyList();
	}

}
