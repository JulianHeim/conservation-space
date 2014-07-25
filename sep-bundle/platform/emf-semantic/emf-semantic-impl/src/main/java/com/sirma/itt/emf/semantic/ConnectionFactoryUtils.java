package com.sirma.itt.emf.semantic;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.sirma.itt.emf.exceptions.EmfConfigurationException;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.semantic.ConnectionFactory;

/**
 * * Utility class that provides a singleton/static access to {@link ConnectionFactory}
 * functionality.
 *
 * @author kirq4e
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
@DependsOn(SecurityContextManager.SERVICE_NAME)
public class ConnectionFactoryUtils {

	/** The injected converter. */
	@Inject
	private Instance<ConnectionFactory> factory;

	/** The type converter. */
	private static ConnectionFactory connectionFactory;

	/**
	 * Initialize.
	 */
	@PostConstruct
	public void initialize() {
		if (!factory.isUnsatisfied()) {
			connectionFactory = factory.get();
		}
	}

	/**
	 * Gets the connection factory. If no implementation is found then the method will throw a
	 * {@link EmfConfigurationException}
	 *
	 * @return the connection factory
	 */
	public static ConnectionFactory getConnectionFactory() {
		if (connectionFactory == null) {
			throw new EmfConfigurationException("ConnectionFactory implementation not found!");
		}
		return connectionFactory;
	}
}
