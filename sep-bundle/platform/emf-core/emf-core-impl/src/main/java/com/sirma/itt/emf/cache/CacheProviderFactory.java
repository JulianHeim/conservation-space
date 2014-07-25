package com.sirma.itt.emf.cache;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.exceptions.EmfConfigurationException;

/**
 * A factory for creating CacheProvider objects.
 *
 * @author BBonev
 */
@ApplicationScoped
public class CacheProviderFactory {

	/** The Constant IN_MEMORY_CACHE_PROVIDER_CLASS. */
	public static final String IN_MEMORY_CACHE_PROVIDER_CLASS = "com.sirma.itt.emf.cache.InMemoryCacheProvider";

	/** The Constant NULL_CACHE_PROVIDER_CLASS. */
	public static final String NULL_CACHE_PROVIDER_CLASS = "com.sirma.itt.emf.cache.NullCacheProvider";

	/** The Constant DEFAULT_CACHE_PROVIDER_CLASS. */
	public static final String DEFAULT_CACHE_PROVIDER_CLASS = IN_MEMORY_CACHE_PROVIDER_CLASS;

	/** The Constant LOGGER. */
	@Inject
	private Logger LOGGER;

	/** The cache provider class. */
	@Inject
	@Config(name = EmfConfigurationProperties.CACHE_PROVIDER_CLASS)
	private String cacheProviderClass;

	/** The instance. */
	private CacheProvider instance;

	/** The bean manager. */
	@Inject
	private BeanManager beanManager;

	/** The lock. */
	private Lock lock = new ReentrantLock();

	/** The failed initialization. */
	private boolean failedInitialization = false;

	/** The fail message. */
	private String failMessage;

	/**
	 * Gets the provider.
	 *
	 * @return the provider
	 */
	@Produces
	@Default
	@SuppressWarnings("unchecked")
	public CacheProvider getProvider() {
		if (instance == null) {
			if (cacheProviderClass == null) {
				LOGGER.warn("Cache provider class is not configured ("
						+ EmfConfigurationProperties.CACHE_PROVIDER_CLASS
						+ "). Using default: " + DEFAULT_CACHE_PROVIDER_CLASS);
				cacheProviderClass = DEFAULT_CACHE_PROVIDER_CLASS;
			}
			if (failedInitialization) {
				throw new EmfConfigurationException(
						"Cannot instantiate CacheProvider implementation: " + failMessage);
			}
			try {
				lock.lock();
				if (failedInitialization) {
					throw new EmfConfigurationException(
							"Cannot instantiate CacheProvider implementation: " + failMessage);
				}
				if (instance != null) {
					return instance;
				}
				Class<?> providerClass = Class.forName(cacheProviderClass);
				String cacheProviderId = "";
				CacheProviderType type = providerClass.getAnnotation(CacheProviderType.class);
				if (type != null) {
					cacheProviderId = type.value();
				}

				// lookup for the class into the list of beans and create it via
				// CDI API
				Set<Bean<?>> beans = beanManager.getBeans(providerClass,
						new AnnotationLiteralExtension(cacheProviderId));
				if (beans.isEmpty()) {
					throw new EmfConfigurationException(
							"Failed to locate cache provider: "
									+ cacheProviderClass);
				}
				Bean<CacheProvider> provider = (Bean<CacheProvider>) beans
						.iterator().next();
				CreationalContext<CacheProvider> cc = beanManager
						.createCreationalContext(provider);
				CacheProvider providerInstance = (CacheProvider) beanManager
						.getReference(provider, providerClass, cc);

				instance = providerInstance;
				// initialize provider here if needed
				LOGGER.info("Created CacheProvider instance of "
						+ cacheProviderClass);
			} catch (EmfConfigurationException e) {
				throw e;
			} catch (Exception e) {
				failedInitialization = true;
				Throwable t = e;
				while (t.getCause() != null) {
					t = t.getCause();
				}
				failMessage = t.getMessage();
				throw new EmfConfigurationException(
						"Failed to load cache provider class: "
								+ cacheProviderClass, e);
			} finally {
				lock.unlock();
			}
		}
		return instance;
	}

	/**
	 * Cache provider literal.
	 *
	 * @author BBbonev
	 */
	private final class AnnotationLiteralExtension extends AnnotationLiteral<CacheProviderType>
			implements CacheProviderType {

		/** The value. */
		private String value;

		/**
		 * Instantiates a new annotation literal extension.
		 *
		 * @param value
		 *            the value
		 */
		public AnnotationLiteralExtension(String value) {
			this.value = value;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String value() {
			return value;
		}
	}

	/**
	 * Reset status.
	 */
	public void resetStatus() {
		failedInitialization = false;
	}
}
