package com.sirma.cmf.web.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.cmf.web.rest.util.SearchResultTransformer;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.rest.EmfRestService;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.search.Query;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.search.Sorter;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.SecurityModel;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.services.PeopleService;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.emf.web.config.EmfWebConfigurationProperties;

/**
 * REST service for performing basic search operations.
 *
 * @author yasko
 */
@Path("/search")
@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
public class SearchRestService extends EmfRestService {

	/** The people service. */
	@Inject
	private PeopleService peopleService;

	/** The search service. */
	@Inject
	private SearchService searchService;

	/** The converter date format pattern. */
	@Inject
	@Config(name = EmfWebConfigurationProperties.CONVERTER_DATE_FORMAT, defaultValue = "dd.MM.yyyy")
	private String converterDateFormatPattern;

	/** The converter datetime format pattern. */
	@Inject
	@Config(name = EmfWebConfigurationProperties.CONVERTER_DATETIME_FORMAT, defaultValue = "dd.MM.yyyy, HH:mm")
	private String converterDatetimeFormatPattern;

	@Inject
	private SearchResultTransformer searchResultTransformer;

	@Inject
	private AuthorityService authorityService;

	/**
	 * Searches for users containing the provided search term in their names or usernames.
	 *
	 * @deprecated Use UsersRestService
	 * @param searchTerm
	 *            Search term.
	 * @return List of users.
	 */
	@Deprecated
	@GET
	@Path("/users")
	public String searchUsers(@QueryParam("term") String searchTerm) {
		TimeTracker tracker = null;
		if (debug) {
			tracker = new TimeTracker().begin();
			log.debug("SearchRestService.searchForUsers term [" + searchTerm + "]");
		}
		List<User> users = peopleService.getFilteredUsers("*" + searchTerm + "*");
		JSONArray result = new JSONArray();

		for (User user : users) {
			JSONObject jsonObject = new JSONObject();
			JsonUtil.addToJson(jsonObject, "value", user.getId());
			JsonUtil.addToJson(jsonObject, "label", user.getDisplayName());
			result.put(jsonObject);
		}

		if (debug) {
			log.debug("SearchRestService.searchForUsers response: " + result.length() + " in "
					+ tracker.stopInSeconds() + " s");
			if (trace) {
				log.trace("SearchRestService.searchForUsers response:" + result);
			}
		}
		return result.toString();
	}

	/**
	 * Performs basic object search.
	 *
	 * @param uriInfo
	 *            Contains search params.
	 * @return JSON object as string containing the results.
	 */
	@GET
	@Path("/basic")
	public String performBasicSearch(@Context UriInfo uriInfo) {
		TimeTracker tracker = null;
		if (debug) {
			tracker = new TimeTracker().begin();
			log.debug("SearchRestService.performBasicSearch uriInfo:" + uriInfo);
		}
		JSONObject result = new JSONObject();
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			SearchArguments<Instance> searchArgs = new SearchArguments<>();
			searchArgs.setQuery(Query.getEmpty());

			String queryText = queryParams.getFirst("queryText");
			if (StringUtils.isNotBlank(queryText)) {
				searchArgs.setStringQuery(queryText);
			} else {

				List<String> location = queryParams.get("location[]");
				if ((location != null) && !location.isEmpty()) {
					Set<String> locationValues = new HashSet<>();
					for (String locationValue : location) {
						locationValues.add(locationValue);
					}
					searchArgs.getArguments().put("context", new ArrayList<String>(locationValues));
				}

				List<String> objectRelationship = queryParams.get("objectRelationship[]");
				if ((objectRelationship != null) && !objectRelationship.isEmpty()) {
					Set<String> relations = new HashSet<>();
					for (String relation : objectRelationship) {
						relations.add(relation);
					}
					searchArgs.getArguments().put("relations", new ArrayList<String>(relations));
				}

				List<String> subType = queryParams.get("subType[]");
				if ((subType != null) && !subType.isEmpty()) {
					Set<String> subTypes = new HashSet<>();
					for (String subTypeString : subType) {
						subTypes.add(subTypeString);
					}
					searchArgs.getArguments().put("emf:type", new ArrayList<String>(subTypes));
				}

				String metaText = queryParams.getFirst("metaText");
				if (StringUtils.isNotBlank(metaText)) {

					// searchArgs.setQuery(Query.getEmpty().or("dcterms:title",
					// metaText).or("dcterms:description", metaText));
					searchArgs.getArguments().put("fts", metaText);
					// searchArgs.getArguments().put("description", metaText);
				}

				String mimeType = queryParams.getFirst("mimetype");
				if (StringUtils.isNotBlank(mimeType)) {
					searchArgs.getArguments().put("emf:mimetype", mimeType);
				}

				SimpleDateFormat sdf = new SimpleDateFormat(converterDateFormatPattern,
						Locale.ENGLISH);
				Date start = null;
				Date end = null;

				String identifier = queryParams.getFirst("identifier");
				if (StringUtils.isNotBlank(identifier)) {
					searchArgs.getArguments().put("dcterms:identifier", identifier);
				}
				String createdFrom = queryParams.getFirst("createdFromDate");
				if (StringUtils.isNotBlank(createdFrom)) {
					start = sdf.parse(createdFrom);
				}
				String createdTo = queryParams.getFirst("createdToDate");
				if (StringUtils.isNotBlank(createdTo)) {
					Calendar calendar = GregorianCalendar.getInstance();
					calendar.setTime(sdf.parse(createdTo));
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					end = calendar.getTime();
				}
				if ((start != null) || (end != null)) {
					searchArgs.getArguments().put("emf:createdOn", new DateRange(start, end));
				}
				List<String> objectTypes = queryParams.get("objectType[]");
				if ((objectTypes != null) && !objectTypes.isEmpty()) {
					ArrayList<String> objectTypeValues = new ArrayList<>();
					for (String objectType : objectTypes) {
						objectTypeValues.add(objectType);
					}

					searchArgs.getArguments().put("rdf:type", objectTypeValues);
				}

				List<String> createdByList = queryParams.get("createdBy[]");
				if ((createdByList != null) && !createdByList.isEmpty()) {
					Set<String> users = new HashSet<>();
					for (String createdBy : createdByList) {
						users.add(createdBy);
					}
					searchArgs.getArguments().put("emf:createdBy", new ArrayList<>(users));
				}
			}
			String pageNumber = queryParams.getFirst("pageNumber");
			if (StringUtils.isNotBlank(pageNumber)) {
				searchArgs.setPageNumber(Integer.parseInt(pageNumber));
			}
			String pageSize = queryParams.getFirst("pageSize");
			if (StringUtils.isNotBlank(pageSize)) {
				searchArgs.setPageSize(Integer.parseInt(pageSize));
			}

			String orderBy = queryParams.getFirst("orderBy");
			if (StringUtils.isNotBlank(orderBy)) {
				searchArgs.setOrdered(true);

				String orderDirection = queryParams.getFirst("orderDirection");
				if (StringUtils.isNotBlank(orderDirection)) {
					searchArgs.setSorter(new Sorter(orderBy,
							"asc".equalsIgnoreCase(orderDirection) ? Sorter.SORT_ASCENDING
									: Sorter.SORT_DESCENDING));
				}
			} else {
				orderBy = "emf:modifiedOn";
				searchArgs.setOrdered(true);
				searchArgs.setSorter(new Sorter(orderBy, Sorter.SORT_DESCENDING));
			}

			// Class<? extends Instance> instanceTypeClass = getInstanceClass();
			// searchService.search(instanceTypeClass, searchArgs);
			searchService.search(Instance.class, searchArgs);

			// TODO Implement search by fields
			List<String> selectedFields = queryParams.get("fields[]");

			User currentUser = authenticationService.get().getCurrentUser();
			if ((searchArgs.getResult() != null) && (currentUser != null)) {
				Map<Instance, Boolean> resultsByPermission = authorityService.hasPermission(
						SecurityModel.PERMISSION_READ, searchArgs.getResult(), currentUser);

				searchResultTransformer.transformResult(searchArgs.getTotalItems(),
						resultsByPermission, selectedFields, result, dictionaryService);
			}

			if (debug) {
				log.debug("SearchRestService.performBasicSearch response results : "
						+ searchArgs.getResult().size() + " in " + tracker.stopInSeconds() + " s");
				if (trace) {
					log.trace("SearchRestService.performBasicSearch response:" + result);
				}
			}
		} catch (ParseException e) {
			log.error("", e);
		} catch (JSONException e) {
			log.error("", e);
		}

		return result.toString();
	}

}
