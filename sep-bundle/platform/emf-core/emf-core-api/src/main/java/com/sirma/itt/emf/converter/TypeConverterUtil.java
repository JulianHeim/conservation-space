package com.sirma.itt.emf.converter;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.sirma.itt.emf.exceptions.EmfConfigurationException;

/**
 * Utility class that provides a singleton/static access to {@link TypeConverter} functionality.
 *
 * @author BBonev
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class TypeConverterUtil {

	/** The injected converter. */
	@Inject
	private Instance<TypeConverter> converter;

	/** The type converter. */
	private static TypeConverter typeConverter;

	/**
	 * Initialize.
	 */
	@PostConstruct
	public void initialize() {
		if (!converter.isUnsatisfied()) {
			typeConverter = converter.get();
		}
	}

	/**
	 * Gets the converter. If no converter implementation is found then the method will throw a
	 * {@link EmfConfigurationException}
	 *
	 * @return the converter
	 */
	public static TypeConverter getConverter() {
		if (typeConverter == null) {
			throw new EmfConfigurationException("TypeConverter implementation not found!");
		}
		return typeConverter;
	}

}
