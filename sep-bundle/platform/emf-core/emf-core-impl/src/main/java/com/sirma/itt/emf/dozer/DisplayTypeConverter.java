package com.sirma.itt.emf.dozer;

import org.dozer.CustomConverter;
import org.dozer.MappingException;

import com.sirma.itt.emf.domain.DisplayType;

/**
 * The Class DisplayType converter for dozer mappings.
 *
 * @author BBonev
 */
public class DisplayTypeConverter implements CustomConverter{

	@Override
	public Object convert(Object destination, Object source, Class<?> destClass,
			Class<?> sourceClass) {
		if (source == null) {
			if (destClass.equals(DisplayType.class)) {
				return DisplayType.parse(null);
			}
			return null;
		}
		if (source instanceof String) {
			String s = (String) source;
			DisplayType displayType = DisplayType.parse(s);
			return displayType;
		} else if (source instanceof DisplayType) {
			return source.toString().toLowerCase();
		} else {
			throw new MappingException(
					"Converter CodelistConverter used incorrectly. Arguments passed in were:"
							+ destination + " and " + source);
		}
	}

}
