package com.sirma.itt.emf.semantic;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.manager.RemoteRepositoryManager;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.semantic.configuration.SemanticConfigurationProperties;
import com.sirma.itt.semantic.ConnectionFactory;

/**
 * Contains a pool with connections to the Ontology repository and maintains the connections open.
 *
 * @author kirq4e
 */
@ApplicationScoped
public class ConnectionFactoryImpl implements ConnectionFactory {

	private static final Logger LOGGER = Logger.getLogger(ConnectionFactoryImpl.class);
	private static final boolean TRACE = LOGGER.isTraceEnabled();

	@Inject
	@Config(name = SemanticConfigurationProperties.SEMANTIC_DB_URL)
	private String repositoryAddress;

	@Inject
	@Config(name = SemanticConfigurationProperties.SEMANTIC_DB_REPOSITORY_NAME)
	private String repositoryName;

	@Inject
	@Config(name = SemanticConfigurationProperties.SEMANTIC_DB_CONNECTION_USER_NAME, defaultValue = "")
	private String userName;

	@Inject
	@Config(name = SemanticConfigurationProperties.SEMANTIC_DB_CONNECTION_PASSWORD, defaultValue = "")
	private String password;

	/**
	 * Repository instance
	 */
	private Repository repository;

	/**
	 * Initializes
	 */
	@PostConstruct
	public void init() {
		RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryAddress);
		if (StringUtils.isNotNullOrEmpty(userName) && StringUtils.isNotNullOrEmpty(password)) {
			repositoryManager.setUsernameAndPassword(userName, password);
		}
		try {
			repositoryManager.initialize();
			repository = repositoryManager.getRepository(repositoryName);

		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Retrieve a connection to the ontology repository and begin a transaction for it. A connection
	 * is available only for the current request.
	 *
	 * @return Active connection to the repository
	 */
	@Override
	public RepositoryConnection produceConnection() {
		try {
			if (TRACE) {
				LOGGER.trace("Producing new semantic repository connection..");
			}
			RepositoryConnection connection = repository.getConnection();
			connection.begin();

			if (TRACE) {
				LOGGER.trace("Produced connection: " + System.identityHashCode(connection));
			}
			return connection;
		} catch (RepositoryException e) {
			throw new EmfRuntimeException(e);
		}
	}

	@Override
	public RepositoryConnection produceReadOnlyConnection() {
		try {
			if (TRACE) {
				LOGGER.trace("Producing new read only semantic repository connection..");
			}
			RepositoryConnection connection = repository.getConnection();

			if (TRACE) {
				LOGGER.trace("Produced read only connection: "
						+ System.identityHashCode(connection));
			}
			return connection;
		} catch (RepositoryException e) {
			throw new EmfRuntimeException(e);
		}
	}

	/**
	 * Called when a connection should be released. Releases the connection in the pool.
	 *
	 * @param connection
	 *            connection that is released.
	 */
	@Override
	public void disposeConnection(RepositoryConnection connection) {
		if (connection == null) {
			throw new IllegalArgumentException("Parameter 'connection' cannot be NULL!");
		}

		try {
			if (TRACE) {
				LOGGER.trace("Destroying semantic repository connection: "
						+ System.identityHashCode(connection));
			}
			if (connection.isActive()) {
				connection.commit();
				if (TRACE) {
					LOGGER.trace("Committing active connection..");
				}
			}
			connection.close();
			if (TRACE) {
				LOGGER.trace("Connection closed..");
			}
		} catch (RepositoryException e) {
			throw new EmfRuntimeException(e);
		} finally {
			try {
				if (connection.isOpen()) {
					connection.close();
				}
			} catch (RepositoryException e) {
				throw new EmfRuntimeException(e);
			}
		}
	}

	/**
	 * Returns an instance of ValueFactory for creating Literals and URIs
	 *
	 * @return ValueFactory instance
	 */
	@Override
	@Produces
	@ApplicationScoped
	public ValueFactory produceValueFactory() {
		return repository.getValueFactory();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void tearDown() {
		// shutdown the repository
		if (repository.isInitialized()) {
			try {
				repository.shutDown();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
	}
}
