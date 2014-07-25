package com.sirma.itt.emf.converter;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.security.context.SecurityContextManager;

/**
 * Initializer to load all type converter extensions to the type converter implementation
 *
 * @author BBonev
 */
@Singleton
@Startup
@DependsOn(SecurityContextManager.SERVICE_NAME)
public class ConverterInitializer {

	public static final String SERVICE_NAME = "ConverterInitializer";
	/** Default Type Converter. */
	@Inject
	private TypeConverter converter;

	/** The logger. */
	@Inject
	private Logger logger;

	@Inject
	@Any
	private Instance<TypeConverterProvider> converters;

	/**
	 * Initialize default set of Converters
	 */
	@PostConstruct
	public void initConverters() {
		logger.info("Initializing type converters");

		int count = 0;
		// register all available providers
		for (TypeConverterProvider provider : converters) {
			provider.register(converter);
			count++;
		}

		logger.info("Initializing type converters: COMPLETE. Processed " + count + " providers");
	}
}
