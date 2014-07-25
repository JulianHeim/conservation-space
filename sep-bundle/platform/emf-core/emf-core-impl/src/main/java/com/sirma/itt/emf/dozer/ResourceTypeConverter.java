package com.sirma.itt.emf.dozer;

import org.dozer.CustomConverter;
import org.dozer.MappingException;

import com.sirma.itt.emf.resources.ResourceType;

/**
 * Dozer converter to support the {@link ResourceType} conversions
 * 
 * @author BBonev
 */
public class ResourceTypeConverter implements CustomConverter {

	@Override
	public Object convert(Object destination, Object source, Class<?> destClass,
			Class<?> sourceClass) {
		if (source == null) {
			return null;
		}
		if (source instanceof Integer) {
			ResourceType displayType = ResourceType.getById((Integer) source);
			return displayType;
		} else if (source instanceof String) {
			ResourceType displayType = ResourceType.getByType((String) source);
			return displayType;
		} else if (source instanceof ResourceType) {
			if (destClass.equals(Integer.class)) {
				return ((ResourceType) source).getType();
			} else {
				return ((ResourceType) source).getName();
			}
		} else {
			throw new MappingException(
					"Converter CodelistConverter used incorrectly. Arguments passed in were:"
							+ destination + " and " + source);
		}
	}

}
