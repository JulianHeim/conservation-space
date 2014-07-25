package com.sirma.itt.emf.dozer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dozer.CustomConverter;
import org.dozer.MappingException;

/**
 * Converter class used to convert between a list of string values and single string 
 *
 * @author BBonev
 */
public class StringToListConverter implements CustomConverter {

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object convert(Object destination, Object source, Class<?> destClass, Class<?> sourceClass) {
		if (source == null) {
			return null;
		}
		if (source instanceof String) {
			List<String> dest;
			String[] split = ((String) source).split("\\s*,\\s*|\\s*;\\s*");
			if (destination == null) {
				dest = new ArrayList<String>(split.length);
			} else {
				dest = (List<String>) destination;
			}
			dest.addAll(Arrays.asList(split));
			return destination;
		} else if (source instanceof List) {
			StringBuilder builder = new StringBuilder();
			for (Object element : ((List<?>) source)) {
				builder.append(element).append(",");
			}
			if (builder.length() > 0) {
				builder.deleteCharAt(builder.length() -1);
			}
			return builder.toString();
		} else {
			throw new MappingException(
					"Converter StringToBooleanConverter used incorrectly. Arguments passed in were:"
							+ destination + " and " + source);
		}
	}

}
