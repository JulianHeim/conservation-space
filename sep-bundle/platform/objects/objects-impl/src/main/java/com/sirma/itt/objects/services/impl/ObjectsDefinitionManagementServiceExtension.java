package com.sirma.itt.objects.services.impl;

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
import com.sirma.itt.objects.domain.definitions.ObjectDefinition;
import com.sirma.itt.objects.domain.definitions.impl.ObjectDefinitionImpl;
import com.sirma.itt.objects.xml.schema.ObjectsSchemaBuilder;

/**
 * Adds definitions loading for project specific definitions.
 * 
 * @author BBonev
 */
@ApplicationScoped
@Extension(target = DefinitionManagementServiceExtension.TARGET_NAME, order = 40)
public class ObjectsDefinitionManagementServiceExtension implements DefinitionManagementServiceExtension {

	/** The adapter service. */
	@Inject
	private DMSDefintionAdapterService adapterService;

	/** The logger. */
	@Inject
	private Logger logger;

	/** The Constant SUPPORTED_OBJECTS. */
	private static final List<Class<?>> SUPPORTED_OBJECTS = new ArrayList<Class<?>>(Arrays.asList(
			DataTypeDefinition.class, DataType.class, ObjectDefinition.class,
			ObjectDefinitionImpl.class));

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
		if (ObjectDefinition.class.isAssignableFrom(definitionClass)) {
			return getObjectDefinitions();
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
		List<DMSFileDescriptor> result = new ArrayList<DMSFileDescriptor>(1);
		// add the specific types
		result.add(new ResourceFileDescriptor("../types.xml", ObjectsSchemaBuilder.class));
		return Collections.unmodifiableList(result);
	}

	/**
	 * Gets the object definitions.
	 * 
	 * @return the object definitions
	 */
	public List<DMSFileDescriptor> getObjectDefinitions() {
		try {
			return adapterService.getDefinitions(ObjectDefinition.class);
		} catch (DMSException e) {
			logger.warn("Failed to retrieve object definitions from DMS", e);
		}
		return Collections.emptyList();
	}
}
