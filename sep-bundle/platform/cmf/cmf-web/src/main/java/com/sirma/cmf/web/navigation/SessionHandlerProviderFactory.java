package com.sirma.cmf.web.navigation;

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

/**
 * A factory for creating SessionHandlerProvider objects.
 * 
 * @author svelikov
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
@DependsOn(SecurityContextManager.SERVICE_NAME)
public class SessionHandlerProviderFactory {

	@Inject
	private Instance<SessionHandlerProvider> factory;

	private static SessionHandlerProvider sessionHandlerProvider;

	/**
	 * Initialize.
	 */
	@PostConstruct
	public void initialize() {
		if (!factory.isUnsatisfied()) {
			sessionHandlerProvider = factory.get();
		}
	}

	/**
	 * Gets the SessionHandlerProvider.
	 * 
	 * @return the SessionHandlerProvider
	 */
	public static SessionHandlerProvider getSessionHandlerProvider() {
		if (sessionHandlerProvider == null) {
			throw new EmfConfigurationException("SessionHandlerProvider implementation not found!");
		}
		return sessionHandlerProvider;
	}
}
