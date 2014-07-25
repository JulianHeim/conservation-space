package com.sirma.itt.emf.dozer;

import org.dozer.CustomConverter;

import com.sirma.itt.emf.converter.TypeConverterUtil;

/**
 * Custom converter that uses the internal {@link com.sirma.itt.emf.converter.TypeConverter}
 * facility to convert data types in Dozer.
 * 
 * @author BBonev
 */
public class TypeConverterForDozer implements CustomConverter {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue,
			Class<?> destinationClass, Class<?> sourceClass) {
		return TypeConverterUtil.getConverter().convert(destinationClass, sourceFieldValue);
	}

}
