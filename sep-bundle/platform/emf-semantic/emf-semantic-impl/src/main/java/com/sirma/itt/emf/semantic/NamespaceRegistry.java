package com.sirma.itt.emf.semantic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.definition.event.LoadSemanticDefinitions;
import com.sirma.itt.emf.domain.model.Uri;
import com.sirma.itt.emf.semantic.configuration.SemanticConfigurationProperties;
import com.sirma.itt.semantic.ConnectionFactory;
import com.sirma.itt.semantic.NamespaceRegistryService;

/**
 * <p>
 * Namespace registry service implementation holding a cache of all defined namespaces and their
 * prefixes in the underlying semantic repository
 * </p>
 * <p>
 * The cache is eagerly loaded during application initialization. Subsequent updates of the
 * namespace cache are defined by the configuration setting
 * {@link com.sirma.itt.semantic.configuration.SemanticSyncConfigurationProperties#NAMESPACE_REGISTRY_REINIT_PERIOD}
 * </p>
 * <p>
 * Depends on PatchDbService, because the semantic repository has to be initialized for the registry
 * to work properly.
 * </p>
 *
 * @see {@link com.sirma.itt.semantic.NamespaceRegistryService}
 * @author Valeri Tishev
 */
@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Lock(LockType.READ)
@DependsOn(value = "PatchDbService")
public class NamespaceRegistry implements NamespaceRegistryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceRegistry.class);

	@Resource
	private TimerService timerService;

	@Inject
	private ConnectionFactory connectionFactory;

	private AtomicReference<Map<String, String>> namespaces;
	private AtomicReference<Map<String, String>> prefixes;

	private AtomicReference<String> namespacePrefixes;

	@Inject
	@Config(name = SemanticConfigurationProperties.NAMESPACE_REGISTRY_REINIT_PERIOD, defaultValue = "15")
	private int namespaceReinitPeriod;

	/** Pattern that is used to split URIs by the short and/or full delimiter */
	private static final Pattern URI_SPLIT_PATTERN = Pattern.compile(SHORT_URI_DELIMITER + "|"
			+ FULL_URI_DELITIMER);

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Initializes and schedules the re-initialization of the namespace registry cache
	 */
	@PostConstruct
	protected void initAndSchedule() {
		initNamespaces();

		ScheduleExpression schedule = new ScheduleExpression();
		schedule.hour("*");
		schedule.minute("*/" + namespaceReinitPeriod);

		// create non-persistent timer configuration
		// and schedule the re-initialization of the registry
		final TimerConfig timerConfig = new TimerConfig(this.getClass().getName(), false);
		timerService.createCalendarTimer(schedule, timerConfig);
		LOGGER.info("Schedule expression for re-initialization of namespace registry: {}", schedule);
	}

	/**
	 * The method uses {@link org.openrdf.repository.RepositoryConnection} in order to get all
	 * declared namespaces in the underlying semantic repository and cache them
	 */
	private void initNamespaces() {
		if (namespaces == null) {
			namespaces = new AtomicReference<>();
		}
		if (prefixes == null) {
			prefixes = new AtomicReference<>();
		}
		if (namespacePrefixes == null) {
			namespacePrefixes = new AtomicReference<>();
		}
		try {
			RepositoryConnection connection = connectionFactory.produceConnection();
			RepositoryResult<Namespace> currentNamespaces = connection.getNamespaces();


			Map<String, String> updatedNamespaces = new HashMap<>();
			Map<String, String> updatedPrefixes = new HashMap<>();
			StringBuilder updatedNamespacePrefixes = new StringBuilder();
			while (currentNamespaces.hasNext()) {
				Namespace namespace = currentNamespaces.next();
				updatedNamespaces.put(namespace.getPrefix(), namespace.getName());
				if (updatedPrefixes.containsKey(namespace.getName())) {
					LOGGER.warn(
							"\n===========================================\n    Found namespace with multiple prefixes: namespace={} {} and {} "
									+ "\n===========================================",
							namespace.getName(), namespace.getPrefix(),
							updatedPrefixes.get(namespace.getName()));
					continue;
				}
				updatedPrefixes.put(namespace.getName(), namespace.getPrefix());

				updatedNamespacePrefixes.append("PREFIX ").append(namespace.getPrefix())
						.append(SHORT_URI_DELIMITER).append("<").append(namespace.getName())
						.append(">").append("\n");

			}

			currentNamespaces.close();
			connectionFactory.disposeConnection(connection);

			// lock for write while update is executing
			lock.writeLock().lock();
			try {
				namespaces.set(updatedNamespaces);
				prefixes.set(updatedPrefixes);
				namespacePrefixes.set(updatedNamespacePrefixes.toString());
			} finally {
				lock.writeLock().unlock();
			}
		} catch (RepositoryException e) {
			LOGGER.error("Failed initializing namespace registry", e);
		}
	}

	/**
	 * Re-initializes the namespace registry cache
	 *
	 * @param timer
	 *            the timer
	 */
	@Timeout
	protected void reinitCache(Timer timer) {
		initNamespaces();
		LOGGER.debug("Next re-initialization of namespace registry scheduled for: {}",
				timer.getNextTimeout());
	}

	/**
	 * Gets the namespaces holder map.
	 *
	 * @return the namespaces holder map
	 */
	private Map<String, String> getNamespacesHolderMap() {
		lock.readLock().lock();
		try {
			Map<String, String> map = namespaces.get();
			if (map == null) {
				LOGGER.error("The namespace registry has not been initialized so far.");
				return Collections.emptyMap();
			}
			return map;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Gets the prefixes holder map.
	 *
	 * @return the prefixes holder map
	 */
	private Map<String, String> getPrefixesHolderMap() {
		lock.readLock().lock();
		try {
			Map<String, String> map = prefixes.get();
			if (map == null) {
				LOGGER.error("The prefixes registry has not been initialized so far.");
				return Collections.emptyMap();
			}
			return map;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public String getNamespace(String prefix) {
		return getNamespacesHolderMap().get(prefix);
	}

	@Override
	public String buildFullUri(String shortUri) {
		if (StringUtils.isNullOrEmpty(shortUri)) {
			throw new IllegalArgumentException("Undefined uri");
		}
		// probably full
		if (shortUri.startsWith("http")) {
			return shortUri;
		}
		String[] namespacePrefixAndPropertyName = URI_SPLIT_PATTERN.split(shortUri, -1);

		if ((namespacePrefixAndPropertyName == null)
				|| (namespacePrefixAndPropertyName.length != 2)) {
			throw new IllegalArgumentException("Malformed uri [" + shortUri + "]");
		}
		if (StringUtils.isNullOrEmpty(getNamespacesHolderMap().get(
				namespacePrefixAndPropertyName[0]))) {
			throw new IllegalStateException("Unknown namespace with prefix ["
					+ namespacePrefixAndPropertyName[0] + "] for URI [" + shortUri + "]");
		}
		return getNamespacesHolderMap().get(namespacePrefixAndPropertyName[0])
				+ namespacePrefixAndPropertyName[1];
	}

	@Override
	public String getShortUri(URI fullUri) {
		if (fullUri == null) {
			throw new IllegalArgumentException("Undefined uri");
		}
		String namespacePreffix = getPrefixesHolderMap().get(fullUri.getNamespace());
		if (namespacePreffix == null) {
			throw new IllegalStateException("Unknown prefix of namespace ["
					+ fullUri.getNamespace() + "] for URI [" + fullUri.toString() + "]");
		}
		return namespacePreffix + SHORT_URI_DELIMITER + fullUri.getLocalName();
	}

	@Override
	public String getShortUri(Uri fullUri) {
		if (fullUri == null) {
			throw new IllegalArgumentException("Undefined uri");
		}
		String namespacePreffix = getPrefixesHolderMap().get(fullUri.getNamespace());
		if (namespacePreffix == null) {
			throw new IllegalStateException("Unknown prefix of namespace ["
					+ fullUri.getNamespace() + "] for URI [" + fullUri.toString() + "]");
		}
		return namespacePreffix + SHORT_URI_DELIMITER + fullUri.getLocalName();
	}

	@Override
	public String getShortUri(String fullUri) {
		if (fullUri == null) {
			throw new IllegalArgumentException("Undefined uri");
		}
		int lastIndexOf = fullUri.lastIndexOf(FULL_URI_DELITIMER);
		if (lastIndexOf < 0) {
			// probably not full uri
			return fullUri;
		}
		String namespaceUri = fullUri.substring(0, lastIndexOf + 1);
		String namespacePreffix = getPrefixesHolderMap().get(namespaceUri);
		if (namespacePreffix == null) {
			throw new IllegalStateException("Unknown prefix of namespace [" + namespaceUri
					+ "] for URI [" + fullUri + "]");
		}
		return namespacePreffix + SHORT_URI_DELIMITER + fullUri.substring(lastIndexOf + 1);
	}

	@Override
	public String getNamespaces() {
		try {
			lock.readLock().lock();
			return new String(namespacePrefixes.get());
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Observes for event for reloading of the cache of namespaces. Reloads the namespaces from the
	 * repository
	 *
	 * @param event
	 *            The event
	 */
	@Override
	public void observeReloadDefinitionEvent(@Observes LoadSemanticDefinitions event) {
		initNamespaces();
	}

}
