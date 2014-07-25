/*
 *
 */
package com.sirma.itt.emf.semantic.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.sirma.itt.cmf.states.PrimaryStates;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.cache.CacheConfiguration;
import com.sirma.itt.emf.cache.Eviction;
import com.sirma.itt.emf.cache.Expiration;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.db.SemanticDb;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.exceptions.TypeConversionException;
import com.sirma.itt.emf.extension.PersistentProperties.PersistentPropertyKeys;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.link.AbstractLinkService;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.semantic.configuration.SemanticConfigurationProperties;
import com.sirma.itt.emf.semantic.exception.SemanticPersistenceException;
import com.sirma.itt.emf.semantic.queries.QueryBuilder;
import com.sirma.itt.emf.state.StateService;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.util.Documentation;
import com.sirma.itt.semantic.NamespaceRegistryService;
import com.sirma.itt.semantic.model.vocabulary.EMF;

/**
 * Semantic implementation for the {@link com.sirma.itt.emf.link.LinkService}
 *
 * @author kirq4e
 * @author Valeri Tishev
 * @author BBonev
 */
@Stateless
@SemanticDb
public class SemanticLinkServiceImpl extends AbstractLinkService {

	/** The Constant NO_LINKS. */
	private static final Set<String> NO_LINKS = Collections.<String> emptySet();
	/** The Constant LINK_ENTITY_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 10000), expiration = @Expiration(maxIdle = 600000, interval = 60000), doc = @Documentation(""
			+ "Cache used to store linked objects with a key link type/from instance reference. <br>Minimal value expression: workingInstances*averageLinkTypesPerInstance"))
	public static final String LINK_ENTITY_CACHE = "SEMANTIC_LINK_ENTITY_CACHE";

	/** The Constant LINK_ENTITY_FULL_CACHE. */
	@CacheConfiguration(container = "cmf", eviction = @Eviction(maxEntries = 2000), expiration = @Expiration(maxIdle = 600000, interval = 60000), doc = @Documentation(""
			+ "Cache used to store the link objects only by instance type ids. The cache will store only the different types of links and not the links itself. <br>Minimal value expression: workingInstances"))
	public static final String LINK_ENTITY_FULL_CACHE = "SEMANTIC_LINK_ENTITY_FULL_CACHE";

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 8909476744495202331L;

	/** The logger. */
	@Inject
	private Logger logger;

	/** The namespace registry service. */
	@Inject
	private NamespaceRegistryService namespaceRegistryService;

	/** The value factory. */
	@Inject
	private ValueFactory valueFactory;

	/** The repository connection. */
	@Inject
	private javax.enterprise.inject.Instance<RepositoryConnection> repositoryConnection;

	/** The context name. */
	@Inject
	@Config(name = SemanticConfigurationProperties.SEMANTIC_DB_CONTEXT_NAME, defaultValue = "http://ittruse.ittbg.com/data/enterpriseManagementFramework")
	private String contextName;

	/** The context. */
	private URI context;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;

	@Inject
	private DictionaryService dictionaryService;

	@Inject
	private QueryBuilder queryBuilder;

	@Inject
	private SemanticPropertiesReadConverter readConverter;

	@Inject
	private StateService stateService;

	@Inject
	private SemanticPropertiesWriteConverter writeConverter;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Pair<Serializable, Serializable> linkInternal(Object from, Object to,
			String mainLinkId, String reverseLinkId, Map<String, Serializable> properties) {
		if ((from == null) || (to == null)) {
			logger.warn("Tried to create link with a missing end point from="
					+ (from == null ? "null" : "not_null") + ", to="
					+ (to == null ? "null" : "not_null"));
			return Pair.nullPair();
		}

		TimeTracker timeTracker = null;
		if (logger.isDebugEnabled()) {
			timeTracker = new TimeTracker().begin();
			debug("=== SemanticLinkService.link ============================");
			debug("From uri = ", getInstanceResource(from), "; class = ", getType(from));
			debug("To uri = ", getInstanceResource(to), "; class = ", getType(to));
			debug("mainLinkId = ", mainLinkId);
			debug("reverseLinkId = ", reverseLinkId);
			debug("properties = ", properties);
			debug("=========================================================");
		}
		Pair<Serializable, Serializable> ids = new Pair<Serializable, Serializable>(null, null);

		Resource subject = getInstanceResource(from);

		Resource object = getInstanceResource(to);

		Model model = new LinkedHashModel(32);

		// create and add main link to model
		String uri = UUID.randomUUID().toString();
		ids.setFirst(EMF.PREFIX + ":" + uri);
		Resource predicate = createRelation(model, uri, mainLinkId, true, properties);
		model.add(subject, (URI) predicate, object);

		if (reverseLinkId != null) {
			if (mainLinkId.equals(reverseLinkId)) {
				model.add(object, (URI) predicate, subject);
				ids.setSecond(ids.getFirst());
			} else {
				uri = UUID.randomUUID().toString();
				ids.setSecond(EMF.PREFIX + ":" + uri);
				// create and add main link to model
				predicate = createRelation(model, uri, reverseLinkId, true, properties);
				model.add(object, (URI) predicate, subject);
			}
		}

		try {
			repositoryConnection.get().add(model, getContext());
			debug("Added model [", model, "] to semantic repository.");
		} catch (RepositoryException e) {
			logger.error("Failed adding model to repository", e);
			return Pair.nullPair();
		} finally {
			if (logger.isDebugEnabled()) {
				debug("Semantic db link creation took ", timeTracker.stopInSeconds(), " s");
			}
		}

		return ids;
	}

	/**
	 * Gets the type.
	 *
	 * @param object
	 *            the object
	 * @return the type
	 */
	private Object getType(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof InstanceReference) {
			InstanceReference from = (InstanceReference) object;
			if ((from.getReferenceType() == null)) {
				return null;
			}
			return from.getReferenceType().getJavaClassName();
		}
		return object.getClass();
	}

	/**
	 * Creates the relation.
	 *
	 * @param model
	 *            the model
	 * @param uriId
	 *            the uri id
	 * @param linkId
	 *            the link id
	 * @param active
	 *            the active
	 * @param properties
	 *            the properties
	 * @return the resource
	 */
	private Resource createRelation(Model model, String uriId, String linkId, boolean active,
			Map<String, Serializable> properties) {
		LinkReference linkInstance = new LinkReference();
		linkInstance.setId(EMF.PREFIX + ":" + uriId);

		String link = linkId;
		if (!linkId.startsWith("http")) {
			if (linkId.contains(":")) {
				link = namespaceRegistryService.buildFullUri(linkId);
			} else {
				link = EMF.NAMESPACE + linkId;
			}
		}
		if (properties != null) {
			linkInstance.getProperties().putAll(properties);
		}
		linkInstance.getProperties().put(EMF.RELATION_TYPE.getLocalName(), link);
		linkInstance.getProperties().put(EMF.IS_ACTIVE.getLocalName(), active);

		Model removeModel = new LinkedHashModel(32);

		return writeConverter.buildModelForInstance(linkInstance, null, model, removeModel);
	}

	/**
	 * Gets the instance resource.
	 *
	 * @param object
	 *            the object
	 * @return the instance resource
	 */
	private Resource getInstanceResource(Object object) {
		String uri = null;
		if (object instanceof Instance) {
			Instance instance = (Instance) object;
			if ((instance.getId() instanceof Long) || (instance.getId() == null)) {
				uri = (String) instance.getProperties().get(PersistentPropertyKeys.URI.getKey());
			}
			uri = instance.getId().toString();
		} else if (object instanceof InstanceReference) {
			uri = ((InstanceReference) object).getIdentifier();
		}
		if (uri == null) {
			logger.error("Invalid instance resource provided: " + object);
			return null;
		}
		return valueFactory.createURI(namespaceRegistryService.buildFullUri(uri));
	}

	/**
	 * Gets the context URI.
	 *
	 * @return the context URI
	 */
	private URI getContext() {
		if (context == null) {
			context = valueFactory.createURI(contextName);
		}
		return context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LinkReference> getLinks(InstanceReference from, Set<String> linkIds) {
		List<LinkReference> internal = getLinkReferencesInternal(from, null, linkIds);
		return internal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LinkReference> getLinksTo(InstanceReference to) {
		List<LinkReference> internal = getLinkReferencesInternal(null, to, NO_LINKS);
		return internal;
	}

	/**
	 * Gets the link references internal.
	 *
	 * @param from
	 *            the from
	 * @param to
	 *            the to
	 * @param linkIds
	 *            the link ids
	 * @return the link references internal
	 */
	private List<LinkReference> getLinkReferencesInternal(Object from, Object to,
			Collection<String> linkIds) {
		TupleQueryResult result = null;
		TimeTracker tracker = new TimeTracker().begin();
		InstanceReference fromRef = null;
		InstanceReference toRef = null;
		RepositoryConnection connection = null;
		try {
			Map<String, Value> bindings = new LinkedHashMap<>(2);
			if (from != null) {
				bindings.put("source", getInstanceResource(from));
				if (from instanceof InstanceReference) {
					fromRef = (InstanceReference) from;
				} else {
					fromRef = typeConverter.convert(InstanceReference.class, from);
				}
			}
			if (to != null) {
				bindings.put("destination", getInstanceResource(to));
				if (to instanceof InstanceReference) {
					toRef = (InstanceReference) to;
				} else {
					toRef = typeConverter.convert(InstanceReference.class, to);
				}
			}

			connection = repositoryConnection.get();
			TupleQuery preparedTupleQuery = prepareSparqlQuery(bindings, linkIds, connection);

			if (logger.isDebugEnabled()) {
				debug("Executing query for links: ", linkIds);
				debug("\n", getQueryWithBindings(preparedTupleQuery.toString(), bindings));
			}

			result = preparedTupleQuery.evaluate();

			if (!result.hasNext()) {
				debug("No results found");
				return Collections.emptyList();
			}

			return buildResultReferences(connection, result, fromRef, toRef, null, true, false);

		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			throw new SemanticPersistenceException(e);
		} finally {
			debug("Semantic Db link seach",
					(fromRef == null ? "" : " from " + fromRef.getReferenceType().getName() + "="
							+ fromRef.getIdentifier()),
					(toRef == null ? "" : " to " + toRef.getReferenceType().getName() + "="
							+ toRef.getIdentifier()), (linkIds.isEmpty() ? "" : " with links "
							+ linkIds), " took ", tracker.stopInSeconds(), " s");
			if (connection != null) {
				try {
					connection.close();
				} catch (RepositoryException e) {
					logger.error("Failed to close connection", e);
				}
			}
			if (result != null) {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					logger.error("Failed closing tuple query result.");
				}
			}
		}
	}

	/**
	 * Builds the result references.
	 *
	 * @param connection
	 *            the connection
	 * @param result
	 *            the result
	 * @param fromRef
	 *            the from ref
	 * @param toRef
	 *            the to ref
	 * @param linkId
	 *            the link id
	 * @param loadProperties
	 *            the load properties
	 * @param simpleLinks
	 *            if the method will load simple links
	 * @return the list
	 * @throws QueryEvaluationException
	 *             the query evaluation exception
	 */
	private List<LinkReference> buildResultReferences(RepositoryConnection connection,
			TupleQueryResult result, InstanceReference fromRef, InstanceReference toRef,
			String linkId, boolean loadProperties, boolean simpleLinks)
			throws QueryEvaluationException {
		List<LinkReference> linkedLinkReferences = new LinkedList<>();
		LinkReference linkReference = null;

		while (result.hasNext()) {
			BindingSet row = result.next();
			if (logger.isTraceEnabled()) {
				trace(bindingSetToString(row));
			}

			linkReference = new LinkReference();
			InstanceReference ref = buildReference(row, "subjectType", "source", fromRef);
			if (ref == null) {
				logger.warn("Invalid relation source has been returned. Ignoring result.");
				continue;
			}
			linkReference.setFrom(ref);

			ref = buildReference(row, "destType", "destination", toRef);
			if (ref == null) {
				logger.warn("Invalid relation target has been returned. Ignoring result.");
				continue;
			}
			linkReference.setTo(ref);

			Value value = row.getValue("relation");
			// when building simple references there is no ID to set
			if (value instanceof URI) {
				linkReference.setId(namespaceRegistryService.getShortUri((URI) value));
			} else {
				if (value != null) {
					logger.warn("Invalid relation id was returned " + value + " but expected URI");
				} // if null it's probably simple link
			}

			value = row.getValue("relationType");
			if (value instanceof URI) {
				linkReference.setIdentifier(namespaceRegistryService.getShortUri((URI) value));
			} else if (value == null) {
				if (linkId != null) {
					linkReference.setIdentifier(linkId);
				} else {
					logger.warn("Missing relation type from semantic query.");
				}
			} else {
				logger.warn("Invalid relation type was returned " + value + " but extected URI");
				continue;
			}
			linkedLinkReferences.add(linkReference);
		}

		if (loadProperties) {
			fetchLinkReferenceProperties(connection, linkedLinkReferences);
		}

		if (!simpleLinks) {
			Set<LinkReference> temp = CollectionUtils.createLinkedHashSet(linkedLinkReferences
					.size());

			temp.addAll(linkedLinkReferences);
			return new ArrayList<>(temp);
		}
		return linkedLinkReferences;
	}

	/**
	 * Fetch the link reference properties from the semantic repository for all links.
	 *
	 * @param connection
	 *            the connection
	 * @param linkReferences
	 *            the link references
	 */
	private void fetchLinkReferenceProperties(RepositoryConnection connection,
			Collection<LinkReference> linkReferences) {

		TimeTracker tracker = TimeTracker.createAndStart();
		List<Serializable> ids = new ArrayList<>(linkReferences.size());
		Map<Serializable, LinkReference> mapping = CollectionUtils
				.createLinkedHashMap(linkReferences.size());
		for (LinkReference reference : linkReferences) {
			ids.add(reference.getId());
			mapping.put(reference.getId(), reference);
		}

		List<Pair<String, Object>> params = new ArrayList<>(1);
		params.add(new Pair<String, Object>("URIS", ids));
		String query = queryBuilder.buildQueryByName(QueryBuilder.LOAD_PROPERTIES, params);

		TupleQuery tupleQuery;
		TupleQueryResult result = null;
		try {
			tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);

			// exclude inferred statements from the result list
			tupleQuery.setIncludeInferred(false);

			result = tupleQuery.evaluate();

			if (!result.hasNext()) {
				debug("No properties found");
				return;
			}

			Map<String, Map<Value, Set<Value>>> resultModel = readConverter
					.buildQueryResultModel(result);

			//
			String state = stateService.getState(PrimaryStates.OPENED, LinkReference.class);

			for (Entry<String, Map<Value, Set<Value>>> entry : resultModel.entrySet()) {
				LinkReference reference = mapping.get(entry.getKey());
				DefinitionModel definition = dictionaryService.getInstanceDefinition(reference);

				Map<String, Set<Value>> convertedKeys = readConverter.convertPropertiesNames(entry
						.getValue());
				if (reference.getProperties() == null) {
					Map<String, Serializable> map = CollectionUtils
							.createLinkedHashMap(convertedKeys.size());
					reference.setProperties(map);
				}
				readConverter.convertPropertiesFromSemanticToInternalModel(definition,
						convertedKeys, reference.getProperties());
				if (!reference.getProperties().containsKey(DefaultProperties.STATUS)) {
					reference.getProperties().put(DefaultProperties.STATUS, state);
				}
			}

		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			throw new SemanticPersistenceException(e);
		} finally {
			debug("Link properties fetch for ", linkReferences.size(), " entries took ",
					tracker.stopInSeconds(), " s");
			if (result != null) {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					logger.error("Failed closing tuple query result.");
				}
			}
		}
	}

	/**
	 * Builds the reference.
	 *
	 * @param row
	 *            the row
	 * @param type
	 *            the type
	 * @param id
	 *            the id
	 * @param preBuild
	 *            the pre build
	 * @return the instance reference
	 */
	InstanceReference buildReference(BindingSet row, String type, String id,
			InstanceReference preBuild) {
		if (preBuild != null) {
			return preBuild;
		}
		InstanceReference ref = null;
		Value value = row.getValue(type);
		// does not load Literal relation types
		if (!(value instanceof URI)) {
			return null;
		}
		try {
			ref = typeConverter.convert(InstanceReference.class, value.stringValue());
		} catch (TypeConversionException e) {
			logger.warn(e.getMessage());
			return null;
		}

		value = row.getValue(id);
		ref.setIdentifier(namespaceRegistryService.getShortUri((URI) value));
		return ref;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LinkReference> getLinksTo(InstanceReference to, String linkId) {
		return getLinkReferencesInternal(null, to, Arrays.asList(linkId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LinkReference> getLinksTo(InstanceReference to, Set<String> linkIds) {
		List<LinkReference> internal = getLinkReferencesInternal(null, to, linkIds);
		return internal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean removeLinksFor(InstanceReference instance) {
		// collects the link from and to the given instance
		List<LinkReference> list1 = getLinkReferencesInternal(instance, null, NO_LINKS);
		List<LinkReference> list2 = getLinkReferencesInternal(null, instance, NO_LINKS);

		List<LinkReference> links = new ArrayList<>(list1.size() + list2.size());
		links.addAll(list1);
		links.addAll(list2);
		// unlink all
		return unlinkInternal(links);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean removeLinksFor(InstanceReference instance, Set<String> linkIds) {
		List<LinkReference> list = getLinkReferencesInternal(instance, null, linkIds);
		return unlinkInternal(list);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean unlink(InstanceReference from, InstanceReference to) {
		return unlinkInternal(from, to, NO_LINKS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean unlink(InstanceReference from, InstanceReference to, String linkId,
			String reverseLinkid) {
		List<String> linkIds = new ArrayList<>(1);
		boolean unlinkInternal = false;
		if (StringUtils.isNotNullOrEmpty(linkId)) {
			linkIds.add(linkId);
			unlinkInternal = unlinkInternal(from, to, linkIds);
		}

		if (StringUtils.isNotNullOrEmpty(reverseLinkid)) {
			linkIds.clear();
			linkIds.add(reverseLinkid);
			unlinkInternal |= unlinkInternal(to, from, linkIds);
		}

		return unlinkInternal;
	}

	/**
	 * Unlink internal.
	 *
	 * @param from
	 *            the from
	 * @param to
	 *            the to
	 * @param linkIds
	 *            the link ids
	 * @return true, if successful
	 */
	private boolean unlinkInternal(InstanceReference from, InstanceReference to,
			Collection<String> linkIds) {
		// get the existing link references between the given instances
		List<LinkReference> linkReferences = getLinkReferencesInternal(from, to, linkIds);
		return unlinkInternal(linkReferences);
	}

	/**
	 * Unlink internal.
	 *
	 * @param <I>
	 *            the generic type
	 * @param links
	 *            the links
	 * @return true, if successful
	 */
	private <I extends Instance> boolean unlinkInternal(List<I> links) {
		boolean error = false;
		RepositoryConnection connection = repositoryConnection.get();
		// prepare and execute statements
		for (I linkReference : links) {
			try {
				unlinkInternalById(connection, getInstanceResource(linkReference));
				// notify for removed link
				addDeletedLinkId(linkReference.getId());
			} catch (RepositoryException e) {
				logger.error("Failed removing relation with URI [" + linkReference.getId() + "]", e);
				error = true;
			}
		}
		return !links.isEmpty() && !error;
	}

	/**
	 * Unlink internal by id.
	 *
	 * @param connection
	 *            the connection
	 * @param linkReferenceId
	 *            the link reference id
	 * @throws RepositoryException
	 *             the repository exception
	 */
	private void unlinkInternalById(RepositoryConnection connection, Serializable linkReferenceId)
			throws RepositoryException {
		// remove isActive = true statement
		Statement statement = prepareLinkStatement(linkReferenceId.toString(), true);
		connection.remove(statement, getContext());

		// add isActive = false statement
		statement = prepareLinkStatement(linkReferenceId.toString(), false);
		connection.add(statement, getContext());
	}

	/**
	 * Prepare link statement.
	 *
	 * @param resource
	 *            the LinkReference resource
	 * @param active
	 *            the active
	 * @return the prepared statement to be added or removed
	 */
	private Statement prepareLinkStatement(String resource, boolean active) {
		return valueFactory.createStatement(
		// subject
				valueFactory.createURI(namespaceRegistryService.buildFullUri(resource)),
				// predicate
				EMF.IS_ACTIVE,
				// value
				ValueConverter.createLiteral(active), getContext());
	}

	/**
	 * Dumps the passed string messages into the log if it is debug enabled.
	 *
	 * @param messages
	 *            the messages
	 */
	private void debug(Object... messages) {
		if (logger.isDebugEnabled()) {
			StringBuilder builder = new StringBuilder();
			for (Object message : messages) {
				builder.append(message == null ? "null" : message);
			}
			logger.debug(builder.toString());
		}
	}

	/**
	 * Dumps the passed string messages into the log if it is debug enabled.
	 *
	 * @param messages
	 *            the messages
	 */
	private void trace(Object... messages) {
		if (logger.isTraceEnabled()) {
			StringBuilder builder = new StringBuilder();
			for (Object message : messages) {
				builder.append(message == null ? "null" : message);
			}
			logger.trace(builder.toString());
		}
	}

	/**
	 * Prepare SPARQL query.
	 *
	 * @param bindings
	 *            the bindings
	 * @param linkIds
	 *            the link id
	 * @param connection
	 *            the connection
	 * @return the tuple query
	 * @throws RepositoryException
	 *             the repository exception
	 * @throws MalformedQueryException
	 *             the malformed query exception
	 */
	private TupleQuery prepareSparqlQuery(Map<String, Value> bindings, Collection<String> linkIds,
			RepositoryConnection connection) throws RepositoryException, MalformedQueryException {
		StringBuilder builder = new StringBuilder();

		builder.append(namespaceRegistryService.getNamespaces())
				.append("\nSELECT DISTINCT ?source ?subjectType ?relation ?relationType ?destination ?destType WHERE {")
				.append("\n\t ?source      ?relation ?destination ;")
				.append("\n\t              sesame:directType ?subjectType .")
				.append("\n\t ?relation    a emf:Relation ;")
				.append("\n\t              emf:isActive \"true\"^^xsd:boolean ; ")
				.append("\n\t              emf:relationType ?relationType.")
				.append("\n\t ?destination sesame:directType ?destType.")
				// filter deleted objects
				.append("\n\t ?source emf:isDeleted \"false\"^^xsd:boolean. ");

		if ((linkIds != null) && !linkIds.isEmpty()) {
			for (Iterator<String> it = linkIds.iterator(); it.hasNext();) {
				String linkId = it.next();

				String shortUri = linkId;
				if (linkId.startsWith("http")) {
					shortUri = namespaceRegistryService.getShortUri(linkId);
				} else if (!linkId.contains(":")) {
					shortUri = EMF.PREFIX + ":" + linkId;
				}
				builder.append("\n\t { ?relation emf:relationType ").append(shortUri).append(". }");
				if (it.hasNext()) {
					builder.append("\n UNION ");
				}
			}
		}

		builder.append("\n}");
		TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL,
				builder.toString());

		// exclude inferred statements from the result list
		tupleQuery.setIncludeInferred(false);

		// set the bindings
		for (Entry<String, Value> entry : bindings.entrySet()) {
			tupleQuery.setBinding(entry.getKey(), entry.getValue());
		}

		return tupleQuery;
	}

	/**
	 * Gets the query in text format to be used in query tab of a Sesame server TODO : To be
	 * removed.
	 *
	 * @param initialQuery
	 *            the initial query
	 * @param bindings
	 *            the bindings
	 * @return the query with bindings
	 */
	private String getQueryWithBindings(String initialQuery, Map<String, Value> bindings) {
		String query = initialQuery;

		int selectIndex = query.indexOf("SELECT");
		int whereIndex = query.indexOf("WHERE {", selectIndex);

		StringBuilder builder = new StringBuilder();
		for (Entry<String, Value> binding : bindings.entrySet()) {
			builder.append("\n BIND (<").append(binding.getValue()).append("> as ?")
					.append(binding.getKey()).append(").");
		}

		return query.substring(selectIndex, whereIndex + 7) + builder.toString()
				+ query.substring(whereIndex + 7);
	}

	/**
	 * Creates binding set's string representation with short URIs.
	 *
	 * @param row
	 *            the binding set
	 * @return the the binding set's string representation
	 */
	private String bindingSetToString(BindingSet row) {
		StringBuilder builder = new StringBuilder();
		Set<String> bindingNames = row.getBindingNames();

		Value value = null;
		for (String binding : bindingNames) {
			value = row.getValue(binding);
			if (value != null) {
				builder.append("[" + binding + " = ");
				if (value instanceof URI) {
					builder.append(namespaceRegistryService.getShortUri((URI) value));
				} else {
					builder.append(value.stringValue());
				}
				builder.append("]");
			}
		}

		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean linkSimple(InstanceReference from, InstanceReference to, String linkId) {
		if ((from == null) || StringUtils.isNullOrEmpty(linkId) || (to == null)) {
			return false;
		}
		Resource sourceUri = getInstanceResource(from);
		URI relation = valueFactory.createURI(namespaceRegistryService.buildFullUri(linkId));
		Model model = new LinkedHashModel(10);
		model.add(sourceUri, relation, getInstanceResource(to));
		try {
			repositoryConnection.get().add(model, getContext());
		} catch (RepositoryException e) {
			throw new SemanticPersistenceException("Failed creating relations.", e);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LinkReference> getSimpleLinks(InstanceReference from, String linkId) {
		if (StringUtils.isNullOrEmpty(linkId)) {
			return Collections.emptyList();
		}
		return getSimpleReferencesInternal(from, null, linkId);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LinkReference> getSimpleLinksTo(InstanceReference to, String linkId) {
		if (StringUtils.isNullOrEmpty(linkId)) {
			return Collections.emptyList();
		}
		return getSimpleReferencesInternal(null, to, linkId);
	}

	/**
	 * Gets the simple references internal.
	 *
	 * @param from
	 *            the from
	 * @param to
	 *            the to
	 * @param linkId
	 *            the link id
	 * @return the simple references internal
	 */
	private List<LinkReference> getSimpleReferencesInternal(InstanceReference from,
			InstanceReference to, String linkId) {
		if ((from == null) && (to == null)) {
			return Collections.emptyList();
		}
		StringBuilder querybuiBuilder = new StringBuilder();

		querybuiBuilder
				.append(namespaceRegistryService.getNamespaces())
				.append("SELECT DISTINCT ?source ?subjectType ?relationType ?destination ?destType WHERE {")
				.append("?source ?relationType ?destination ;")
				.append("        sesame:directType ?subjectType .")
				.append("?destination sesame:directType ?destType .");
		// check for the reversed instance not to be deleted
		if ((to != null) && (from == null)) {
			querybuiBuilder.append(" ?source emf:isDeleted \"false\"^^xsd:boolean .");
		} else {
			querybuiBuilder.append(" ?destination emf:isDeleted \"false\"^^xsd:boolean . ");
		}
		querybuiBuilder.append("}");

		URI relationType = valueFactory.createURI(namespaceRegistryService.buildFullUri(linkId));

		TupleQueryResult result = null;
		TimeTracker tracker = TimeTracker.createAndStart();
		RepositoryConnection connection = null;
		try {
			connection = repositoryConnection.get();
			TupleQuery preparedQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL,
					querybuiBuilder.toString());
			preparedQuery.setIncludeInferred(false);
			// set bindings
			if (from != null) {
				preparedQuery.setBinding("source", getInstanceResource(from));
			}
			if (to != null) {
				preparedQuery.setBinding("destination", getInstanceResource(to));
			}
			preparedQuery.setBinding("relationType", relationType);

			result = preparedQuery.evaluate();

			if (!result.hasNext()) {
				debug("No simple links results found");
				return Collections.emptyList();
			}
			// build result
			return buildResultReferences(connection, result, from, to, linkId, false, true);
		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			throw new SemanticPersistenceException(e);
		} finally {
			debug("Simple link search from=", (from != null ? from.getIdentifier() : null), " to ",
					(to != null ? to.getIdentifier() : null), " took: ", tracker.stopInSeconds(),
					" s");
			if (connection != null) {
				try {
					connection.close();
				} catch (RepositoryException e) {
					logger.error("Failed to close connection", e);
				}
			}
			if (result != null) {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					logger.error("Failed closing tuple query result.", e);
				}
			}
		}
	}

	/**
	 * Gets the simple references internal.
	 *
	 * @param from
	 *            the from
	 * @param to
	 *            the to
	 * @param linkId
	 *            the link id
	 * @return the simple references internal
	 */
	private boolean isLinkedSimpleInternal(InstanceReference from, InstanceReference to,
			String linkId) {
		if ((from == null) && (to == null)) {
			return false;
		}
		StringBuilder querybuiBuilder = new StringBuilder();

		querybuiBuilder.append(namespaceRegistryService.getNamespaces()).append("ASK WHERE {")
				.append("?source ").append(namespaceRegistryService.getShortUri(linkId))
				.append(" ?destination .")
				.append(" ?source emf:isDeleted \"false\"^^xsd:boolean .")
				.append(" ?destination emf:isDeleted \"false\"^^xsd:boolean . ").append("}");

		TimeTracker tracker = TimeTracker.createAndStart();
		RepositoryConnection connection = null;
		boolean result = false;
		try {
			connection = repositoryConnection.get();
			BooleanQuery query = connection.prepareBooleanQuery(QueryLanguage.SPARQL,
					querybuiBuilder.toString());
			query.setIncludeInferred(false);
			// set bindings
			if (from != null) {
				query.setBinding("source", getInstanceResource(from));
			}
			if (to != null) {
				query.setBinding("destination", getInstanceResource(to));
			}

			result = query.evaluate();

			return result;
		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			throw new SemanticPersistenceException(e);
		} finally {
			debug("Simple link test from=", (from != null ? from.getIdentifier() : null), " to ",
					(to != null ? to.getIdentifier() : null), " took: ", tracker.stopInSeconds(),
					" s and evaluated to ", String.valueOf(result));
			if (connection != null) {
				try {
					connection.close();
				} catch (RepositoryException e) {
					logger.error("Failed to close connection", e);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void unlinkSimple(InstanceReference from, String linkId) {
		throw new UnsupportedOperationException(
				"The method unlinkSimple(InstanceReference, String) is not implemented, yet!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void unlinkSimple(InstanceReference from, InstanceReference to, String linkId) {
		if ((from == null) || (to == null) || StringUtils.isNullOrEmpty(linkId)) {
			logger.warn("Missing arguments for unlinkSimple " + linkId);
			return;
		}
		Resource sourceUri = getInstanceResource(from);
		Resource destinationUri = getInstanceResource(to);
		URI relation = valueFactory.createURI(namespaceRegistryService.buildFullUri(linkId));
		try {
			repositoryConnection.get().remove(sourceUri, relation, destinationUri);
		} catch (RepositoryException e) {
			throw new SemanticPersistenceException("Failed removing triplet {"
					+ from.getIdentifier() + ", " + linkId + ", " + to.getIdentifier() + "} ", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean linkSimple(InstanceReference from, List<InstanceReference> tos, String linkId) {
		if ((from == null) || StringUtils.isNullOrEmpty(linkId) || (tos == null)
				|| (tos.size() == 0)) {
			return false;
		}
		Resource sourceUri = getInstanceResource(from);
		URI relation = valueFactory.createURI(namespaceRegistryService.buildFullUri(linkId));
		Model model = new LinkedHashModel();
		for (InstanceReference destination : tos) {
			model.add(sourceUri, relation, getInstanceResource(destination));
		}
		try {
			repositoryConnection.get().add(model, getContext());
		} catch (RepositoryException e) {
			throw new SemanticPersistenceException("Failed creating relations.", e);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public boolean isLinkedSimple(InstanceReference from, InstanceReference to, String linkId) {
		return isLinkedSimpleInternal(from, to, linkId);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	protected LinkReference getLinkReferenceById(Serializable id, boolean loadProperties) {
		if (id == null) {
			return null;
		}
		TimeTracker tracker = new TimeTracker().begin();

		Map<String, Value> bindings = CollectionUtils.createHashMap(2);
		TupleQueryResult result = null;
		RepositoryConnection connection = null;
		try {
			String fullUri = namespaceRegistryService.buildFullUri(id.toString());
			URI linkUri = valueFactory.createURI(fullUri);
			bindings.put("relation", linkUri);

			connection = repositoryConnection.get();
			TupleQuery preparedTupleQuery = prepareSparqlQuery(bindings, NO_LINKS, connection);

			if (logger.isDebugEnabled()) {
				debug("Executing query for id: ", id);
				debug("\n", getQueryWithBindings(preparedTupleQuery.toString(), bindings));
			}

			result = preparedTupleQuery.evaluate();

			if (!result.hasNext()) {
				debug("No results found");
				return null;
			}

			List<LinkReference> list = buildResultReferences(connection, result, null, null, null,
					loadProperties, false);
			if (list.isEmpty()) {
				debug("No results found when parsing results");
				return null;
			}
			if (list.size() > 1) {
				logger.warn("More then one result found for a single link db identifier: " + id);
			}
			return list.get(0);
		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			throw new SemanticPersistenceException(e);
		} finally {
			debug("Semantic Db link seach took ", tracker.stopInSeconds(), " s");
			if (connection != null) {
				try {
					connection.close();
				} catch (RepositoryException e) {
					logger.error("Failed to close connection", e);
				}
			}
			if (result != null) {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					logger.error("Failed closing tuple query result.");
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	protected String shrinkLinkIdentifier(String identifier) {
		return namespaceRegistryService.getShortUri(identifier);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	protected List<LinkReference> getLinksInternal(Object from, Object to,
			Collection<String> linkids) {
		return getLinkReferencesInternal(from, to, linkids);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	protected String getTopLevelCacheName() {
		return LINK_ENTITY_FULL_CACHE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	protected String expandLinkIdentifier(String identifier) {
		try {
			String id = identifier;
			if (!identifier.contains(":")) {
				id = "emf:" + identifier;
			}
			return id;
		} catch (RuntimeException e) {
			return identifier;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	protected void removeLinkInternal(LinkReference instance) {
		try {
			unlinkInternalById(repositoryConnection.get(), instance.getId());
		} catch (RepositoryException e) {
			logger.warn(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	protected boolean updatePropertiesInternal(Serializable id,
			Map<String, Serializable> properties, Map<String, Serializable> oldProperties) {
		if ((id == null) || (properties == null) || properties.isEmpty()) {
			return false;
		}

		// fetch old properties to perform a diff update
		LinkReference oldReference = new LinkReference();
		oldReference.setId(id);
		if (oldProperties != null) {
			// CMF-6497: if old properties are fetched from DB they contain more properties like
			// relation type or rdf:type and are generated remove statements for them.

			// remove all irrelevant properties and leave only the modified ones.
			oldProperties.keySet().retainAll(properties.keySet());
			oldReference.setProperties(oldProperties);
		} else {
			oldReference.setProperties(Collections.<String, Serializable> emptyMap());
		}

		LinkReference reference = new LinkReference();
		reference.setId(id);
		reference.setProperties(properties);

		Model removeModel = new LinkedHashModel();
		Model addModel = new LinkedHashModel();
		writeConverter.buildModelForInstance(reference, oldReference, addModel, removeModel);

		try {
			RepositoryConnection connection = repositoryConnection.get();

			connection.remove(removeModel);
			connection.add(addModel);
		} catch (RepositoryException e) {
			throw new SemanticPersistenceException(e);
		}
		return true;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	protected String getMiddleLevelCacheName() {
		return LINK_ENTITY_CACHE;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeLinkById(Serializable linkDbId) {
		if (linkDbId == null) {
			return;
		}
		try {
			unlinkInternalById(repositoryConnection.get(), linkDbId);
			addDeletedLinkId(linkDbId);
		} catch (RepositoryException e) {
			throw new SemanticPersistenceException(e);
		}
	}

}
