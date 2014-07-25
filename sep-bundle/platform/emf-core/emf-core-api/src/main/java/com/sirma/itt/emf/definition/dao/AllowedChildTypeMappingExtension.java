package com.sirma.itt.emf.definition.dao;

import java.util.Map;

import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.plugin.Plugin;
import com.sirma.itt.emf.util.Documentation;

/**
 * Extension plugin to provide a mapping between supported
 * {@link com.sirma.itt.emf.definition.model.AllowedChildDefinition#getType()} types and the actual
 * definition classes, instances and {@link com.sirma.itt.emf.definition.model.DataTypeDefinition}
 * names.
 * 
 * @author BBonev
 */
@Documentation("Extension plugin to provide a mapping between supported "
		+ "{@link com.sirma.itt.emf.definition.model.AllowedChildDefinition#getType()} types and the actual"
		+ "definition classes, instances and {@link com.sirma.itt.emf.definition.model.DataTypeDefinition} names.")
public interface AllowedChildTypeMappingExtension extends Plugin {

	/** The target name. */
	String TARGET_NAME = "allowedChildTypeMapping";

	/**
	 * Gets type to definition class mapping.
	 * 
	 * @return the definition mapping
	 */
	Map<String, Class<? extends DefinitionModel>> getDefinitionMapping();

	/**
	 * Gets type to instance class mapping.
	 * 
	 * @return the instance mapping
	 */
	Map<String, Class<? extends Instance>> getInstanceMapping();

	/**
	 * Gets type to {@link com.sirma.itt.emf.definition.model.DataTypeDefinition#getName()} mapping.
	 * 
	 * @return the type mapping
	 */
	Map<String, String> getTypeMapping();
}
