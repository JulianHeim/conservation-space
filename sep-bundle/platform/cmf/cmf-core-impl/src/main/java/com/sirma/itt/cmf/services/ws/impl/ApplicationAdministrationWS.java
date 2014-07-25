package com.sirma.itt.cmf.services.ws.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.ws.soap.MTOM;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sirma.itt.cmf.event.LoadTemplates;
import com.sirma.itt.cmf.services.ws.ApplicationAdministration;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.cache.lookup.EntityLookupCache;
import com.sirma.itt.emf.cache.lookup.EntityLookupCacheContext;
import com.sirma.itt.emf.codelist.event.LoadCodelists;
import com.sirma.itt.emf.codelist.event.ResetCodelistEvent;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.event.LoadAllDefinitions;
import com.sirma.itt.emf.definition.event.LoadSemanticDefinitions;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.label.LabelService;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.serialization.XStreamConvertableWrapper;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * Web service for remote administration of the application.
 *
 * @author BBonev
 */
@Path("/administration")
@Consumes({ "application/json" })
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
@MTOM
public class ApplicationAdministrationWS extends EmfRestService implements
		ApplicationAdministration {

	/** The logger. */
	@Inject
	private Logger logger;

	/** The event. */
	@Inject
	private EventService eventService;

	/** The cache context. */
	@Inject
	private EntityLookupCacheContext cacheContext;
	@Inject
	private DictionaryService dictionaryService;
	@Inject
	TypeConverter converter;
	/** The label service. */
	@Inject
	private LabelService labelService;
	/**
	 * {@inheritDoc}
	 */
	@Override
	@GET
	@Path("reloadDefinitions")
	public void reloadDefinitions() {
		logger.info("Called reloadDefinitions from WS port");
		eventService.fire(new LoadAllDefinitions(true));
	}

	@Override
	@GET
	@Path("reloadTemplates")
	public void reloadTemplates() {
		logger.info("Called reloadTemplates from WS port");
		eventService.fire(new LoadTemplates());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@GET
	@Path("reloadSemanticDefinitions")
	public void reloadSemanticDefinitions() {
		logger.info("Called reloadSemanticDefinitions from WS port");
		eventService.fire(new LoadSemanticDefinitions());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@GET
	@Path("resetCodelists")
	public void resetCodelists() {
		logger.info("Called resetCodelists from WS port");
		eventService.fire(new ResetCodelistEvent());

		eventService.fire(new LoadCodelists());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@GET
	@Path("clearInternalCache")
	public void clearInternalCache() {
		logger.info("Called clearInternalCache from WS port");
		Set<String> activeCaches = cacheContext.getActiveCaches();
		for (String cacheName : activeCaches) {
			if (cacheName.toLowerCase().contains("entity")) {
				logger.info("Clearing " + cacheName);
				EntityLookupCache<Serializable, Object, Serializable> cache = cacheContext
						.getCache(cacheName);
				cache.clear();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@GET
	@Path("clearDefinitionsCache")
	public void clearDefinitionsCache() {
		logger.info("Called clearDefinitionsCache from WS port");
		Set<String> activeCaches = cacheContext.getActiveCaches();
		for (String cacheName : activeCaches) {
			if (cacheName.toLowerCase().contains("definition")) {
				logger.info("Clearing " + cacheName);
				EntityLookupCache<Serializable, Object, Serializable> cache = cacheContext
						.getCache(cacheName);
				cache.clear();
			}
		}
	}

	/**
	 * Clear cache.
	 */
	@GET
	@Path("clear-label-cache")
	public void clearLabelCache() {
		labelService.clearCache();
	}

	/**
	 * Clear cache.
	 * 
	 * @param cacheName
	 *            the cache name
	 * @return the response
	 */
	@GET
	@Path("{cacheId}/clear")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public Response clearCache(@PathParam("cacheId") String cacheName) {
		if (StringUtils.isNullOrEmpty(cacheName)) {
			return buildResponse(Status.NOT_FOUND, "{}");
		}
		EntityLookupCache<Serializable, Object, Serializable> cache = cacheContext
				.getCache(cacheName);
		JSONObject object = new JSONObject();
		buildCacheStatistics(object, cacheName, cache);
		if (cache != null) {
			cache.clear();
		}
		return buildResponse(Status.OK, object.toString());
	}

	/**
	 * Gets the cache statistics.
	 * 
	 * @return the cache statistics
	 */
	@GET
	@Path("cacheStatistics")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public Response getCacheStatistics() {
		Set<String> activeCaches = cacheContext.getActiveCaches();
		JSONObject jsonObject = new JSONObject();
		for (String cacheName : activeCaches) {
			EntityLookupCache<Serializable, Object, Serializable> cache = cacheContext
					.getCache(cacheName);
			buildCacheStatistics(jsonObject, cacheName, cache);
		}
		return buildResponse(Status.OK, jsonObject.toString());
	}

	/**
	 * Builds the cache statistics.
	 * 
	 * @param jsonObject
	 *            the json object
	 * @param cacheName
	 *            the cache name
	 * @param cache
	 *            the cache
	 */
	private void buildCacheStatistics(JSONObject jsonObject, String cacheName,
			EntityLookupCache<Serializable, Object, Serializable> cache) {
		if (cache == null) {
			JsonUtil.addToJson(jsonObject, cacheName, JSONObject.NULL);
		} else {
			JSONObject values = new JSONObject();
			JsonUtil.addToJson(values, "primary", Integer.toString(cache.primaryKeys().size()));
			JsonUtil.addToJson(values, "secondary", Integer.toString(cache.secondaryKeys().size()));
			JsonUtil.addToJson(jsonObject, cacheName, values);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@GET
	@Path("definition")
	@Produces({ "application/xml" })
	public String getDefinition(@QueryParam("type") String type, @QueryParam("id") String id) {
		if (StringUtils.isNullOrEmpty(type)) {
			return "<error>Missing required argument: type -> possible values: casedefinition, workflowdefinition, etc</error>";
		}

		DataTypeDefinition typeDefinition = dictionaryService.getDataTypeDefinition(type
				.toLowerCase());
		if (typeDefinition == null) {
			return "<error>Invalid type: \"" + type
					+ "\". Possible values: casedefinition, workflowdefinition, etc</error>";
		}
		String className = typeDefinition.getJavaClassName();
		DefinitionModel model = null;
		try {
			Class<? extends DefinitionModel> forName = (Class<? extends DefinitionModel>) Class
					.forName(className);
			if (StringUtils.isNullOrEmpty(id)) {
				List<? extends DefinitionModel> definitions = dictionaryService
						.getAllDefinitions(forName);
				StringBuilder builder = new StringBuilder();
				builder.append("<definitions>");
				for (DefinitionModel definitionModel : definitions) {
					builder.append("\n\t<definition revision=\"")
							.append(definitionModel.getRevision()).append("\" >")
							.append(definitionModel.getIdentifier())
							.append("</definition>");
				}
				builder.append("</definitions>");
				return builder.toString();
			}

			model = dictionaryService.getDefinition(forName, id);
		} catch (Exception e) {
			return "<error>Failed to retrive definition with type: " + type + " and id " + id
					+ ": " + e.getMessage() + "</error>";
		}
		if (model == null) {
			return "<error>Definition \"" + id + "\" not found</error>";
		}
		return converter.convert(String.class, new XStreamConvertableWrapper(model));
	}

}
