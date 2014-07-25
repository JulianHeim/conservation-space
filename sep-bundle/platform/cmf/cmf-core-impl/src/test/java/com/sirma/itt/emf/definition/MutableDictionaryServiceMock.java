package com.sirma.itt.emf.definition;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Alternative;

import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.definition.model.PrototypeDefinition;
import com.sirma.itt.emf.domain.DisplayType;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.PathElement;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.instance.model.Instance;

/**
 * The Class MutableDictionaryServiceMock.
 */
@Alternative
public class MutableDictionaryServiceMock implements MutableDictionaryService {

	/**
	 *
	 */
	private static final long serialVersionUID = 5146679217285809841L;

	@Override
	public <E extends DefinitionModel> List<E> getAllDefinitions(Class<E> ref) {
		return new LinkedList<>();
	}

	@Override
	public <E extends DefinitionModel> E getDefinition(Class<E> ref,
			String defId) {

		return null;
	}

	@Override
	public <E extends DefinitionModel> E getDefinition(Class<E> ref,
			String defId, Long version) {

		return null;
	}

	@Override
	public <E extends DefinitionModel> List<E> getDefinitionVersions(
			Class<E> ref, String defId) {

		return null;
	}

	@Override
	public PropertyDefinition getProperty(String currentQName, Long revision,
			PathElement pathElement) {

		return null;
	}

	@Override
	public PrototypeDefinition getPrototype(String currentQName, Long revision,
			PathElement pathElement) {

		return null;
	}

	@Override
	public Long getPropertyId(String propertyName, Long revision,
			PathElement pathElement, Serializable value) {

		return null;
	}

	@Override
	public String getPropertyById(Long propertyId) {

		return null;
	}

	@Override
	public PrototypeDefinition getProperty(Long propertyId) {

		return null;
	}

	@Override
	public DataTypeDefinition getDataTypeDefinition(String name) {

		return null;
	}

	@Override
	public Map<String, Serializable> filterProperties(DefinitionModel model,
			Map<String, Serializable> properties, DisplayType displayType) {

		return null;
	}

	@Override
	public DefinitionModel getInstanceDefinition(Instance instance) {

		return null;
	}

	@Override
	public PrototypeDefinition getDefinitionByValue(String propertyName,
			Serializable serializable) {

		return null;
	}

	@Override
	public void initializeBasePropertyDefinitions() {


	}

	@Override
	public <E extends DefinitionModel> boolean isDefinitionEquals(E case1,
			E case2) {

		return false;
	}

	@Override
	public <E extends TopLevelDefinition> E saveDefinition(E definition) {

		return null;
	}

	@Override
	public <E extends TopLevelDefinition> E saveTemplateDefinition(
			E definitionModel) {

		return null;
	}

	@Override
	public DataTypeDefinition saveDataTypeDefinition(
			DataTypeDefinition typeDefinition) {

		return null;
	}

	@Override
	public PropertyDefinition savePropertyIfChanged(
			PropertyDefinition newProperty, PropertyDefinition oldProperty) {

		return null;
	}

	@Override
	public List<Pair<String, String>> removeDefinitionsWithoutInstances(
			Set<String> definitionsToCheck) {

		return null;
	}

}
