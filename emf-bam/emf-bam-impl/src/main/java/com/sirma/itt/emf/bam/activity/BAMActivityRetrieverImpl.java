package com.sirma.itt.emf.bam.activity;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.bam.configuration.BAMConfigurationProperties;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.time.DateRange;

/**
 * Class that retrieves activities from a specified data source. Obtains connection to that data
 * source and then upon calling one if the class's implemented methods executes queries to it.
 * 
 * @author Mihail Radkov
 * @author Nikolay Velkov
 * @author BBonev
 */
public class BAMActivityRetrieverImpl implements BAMActivityRetriever, Serializable {

	/** The constant serial version. */
	private static final long serialVersionUID = 4889608707185146547L;

	/** Logger used for logging events related to this class. */
	private final Logger logger = LoggerFactory.getLogger(BAMActivityRetrieverImpl.class);

	/** Date formatter used when working with dates. */
	private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	/** The constant DATE_RECEIVED */
	//TODO: consider move all literals as constants.
	private static final String DATE_RECEIVED = "date_received";

	/** Configuration property for table's name in the database where events are stored. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_ACTIVITY_TABLE, defaultValue = "emf_events")
	private String activityTable;

	@Inject
	@Config(name = BAMConfigurationProperties.BAM_ENABLED, defaultValue = "false")
	private Boolean bamEnabled;

	/**
	 * Open connection.
	 * 
	 * @return the connection
	 */
	protected Connection openConnection() {
		try {
			// FIXME: add connection pooling
			InitialContext context = new InitialContext();
			Object lookup = context.lookup("java:jboss/datasources/bamDS");
			if (lookup instanceof DataSource) {
				DataSource bamDatasource = (DataSource) lookup;
				return obtainConnection(bamDatasource);
			}
		} catch (RuntimeException e) {
			logger.error("Failed to read BAM data source due to : " + e.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("", e);
			}
		} catch (NamingException e) {
			logger.error("Failed to read BAM data source due to : " + e.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("", e);
			}
		}
		return null;
	}

	/**
	 * Obtains a connection from a provided {@link DataSource}. Performs a null check for it.
	 * 
	 * @param datasource
	 *            the provided data source
	 * @return a {@link Connection} or null if the obtaining could not be done
	 */
	public Connection obtainConnection(DataSource datasource) {
		Connection conn = null;
		if (datasource != null) {
			try {
				// set maximum connection timeout to 5 seconds
				datasource.setLoginTimeout(5);
				conn = datasource.getConnection();
				logger.debug("Data source connection obtained.");
			} catch (SQLException e) {
				logger.error("Couldn't obtain a data source connection. ", e);
			}
		}
		return conn;
	}

	/**
	 * Executes a given query from a provided {@link Connection} and returns the results. Performs
	 * parameters checks.
	 * <p>
	 * Converts the rows of a given {@link ResultSet} to a list of {@link BAMActivity} objects. If
	 * the given {@link ResultSet} is null, an empty list of {@link BAMActivity} is returned.
	 * 
	 * @param conn
	 *            the conn
	 * @param query
	 *            the query
	 * @return the activities
	 */
	@SuppressWarnings("resource")
	public List<BAMActivity> buildActivities(Connection conn, String query) {
		List<BAMActivity> activities = new LinkedList<BAMActivity>();
		if (StringUtils.isNotNullOrEmpty(query) && (conn != null)) {
			ResultSet dbSet = null;
			PreparedStatement stm = null;
			try {
				stm = conn.prepareStatement(query);
				dbSet = stm.executeQuery();

				if (dbSet.getMetaData().getColumnCount() == 11) {
					while (dbSet.next()) {
						BAMActivity record = new BAMActivity();
						record.setId(removeQuotes(dbSet.getString(1)));
						record.setTitle(removeQuotes(dbSet.getString(2)));
						record.setUsername(removeQuotes(dbSet.getString(3)));
						record.setAction(removeQuotes(dbSet.getString(4)));
						record.setProjectid(removeQuotes(dbSet.getString(5)));
						record.setCaseid(removeQuotes(dbSet.getString(6)));
						record.setObjectsubtype(removeQuotes(dbSet.getString(7)));
						record.setObjectsysid(removeQuotes(dbSet.getString(8)));
						record.setObjecttype(removeQuotes(dbSet.getString(9)));
						record.setDatereceived(removeQuotes(dbSet.getString(10)));
						record.setObjecturl(removeQuotes(dbSet.getString(11)));
						activities.add(record);
					}
				} else {
					logger.warn("Unexpected column size in ResultSet.");
				}
				return activities;
			} catch (SQLException e) {
				logger.warn("Couldn't execute query.", e);
			} finally {
				if (dbSet != null) {
					try {
						dbSet.close();
					} catch (SQLException e) {
						// not interested
					}
				}
				if (stm != null) {
					try {
						stm.close();
					} catch (SQLException e) {
						// not interested
					}
				}
			}
		}
		return Collections.emptyList();
	}

	/**
	 * If a provided string starts and/or ends with quotes, then they are removed. If the string is
	 * null or empty then the same parameter is returned.
	 * 
	 * @param input
	 *            the provided string
	 * @return the trimmed string
	 */
	public String removeQuotes(String input) {
		if (StringUtils.isNotNullOrEmpty(input)) {
			return input.replaceAll("^\"|\"$", "");
		}
		return input;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("resource")
	public List<BAMActivity> getActivities(BAMActivityCriteria activityCriteria) {
		if (!bamEnabled) {
			return Collections.emptyList();
		}
		String query = constructCriteriaQuery(activityCriteria);
		Connection connection = null;
		try {
			connection = openConnection();
			if (connection == null) {
				logger.warn("Failed to open connection to BAM server!");
				return Collections.emptyList();
			}
			return buildActivities(connection, query.toString());
		} finally {
			closeConnection(connection);
		}
	}

	/**
	 * Constructs a query from a provided criteria.
	 * 
	 * @param activityCriteria
	 *            the provided criteria
	 * @return the constructed query
	 */
	public String constructCriteriaQuery(BAMActivityCriteria activityCriteria) {
		StringBuilder query = new StringBuilder(
				"Select objectid,objecttitle,username,actionid,projectid,caseid,objectsubtype,objectsysid,objecttype,date_received, objecturl from "
						+ activityTable + " where");
		constructDateRangeCriteria(query, activityCriteria.getDateRange());
		constructIds(query, activityCriteria.getIds(), activityCriteria.getCriteriaType());
		constructUser(query, activityCriteria.getIncludedUsername(), true);
		constructUser(query, activityCriteria.getExcludedUsername(), false);
		sortBy(query, DATE_RECEIVED, false);
		query.append(" ;");
		return query.toString();
	}

	/**
	 * Constructs part of a query in a string builder for extracting activities from a specified
	 * date range.
	 * 
	 * @param queryBuilder
	 *            the provided builder
	 * @param dateRange
	 *            the specified date range
	 */
	public void constructDateRangeCriteria(StringBuilder queryBuilder, DateRange dateRange) {
		if ((queryBuilder != null) && (dateRange != null)) {
			queryBuilder.append(" replace(eventdate,'\"','')::bigint>=");
			queryBuilder.append(dateFormat.format(dateRange.getFirst()));
			queryBuilder.append(" and replace(eventdate,'\"','')::bigint<=");
			queryBuilder.append(dateFormat.format(dateRange.getSecond()));
		}
	}

	/**
	 * Constructs query in a string builder for extracting activities by specified ids and criteria
	 * type.
	 * 
	 * @param query
	 *            the builder
	 * @param ids
	 *            the specified ids
	 * @param criteriaType
	 *            the specified criteria type
	 */
	public void constructIds(StringBuilder query, List<String> ids, CriteriaType criteriaType) {
		if ((query != null) && (ids != null) && !ids.isEmpty()) {
			String type = "";
			// TODO: Extract the type directly from the enum ?
			if (CriteriaType.CASE.equals(criteriaType)) {
				type = "caseid";
			} else if (CriteriaType.PROJECT.equals(criteriaType)) {
				type = "projectid";
			} else {
				logger.warn("Criteria type is not provided.");
				return;
			}
			query.append(" and ( ");
			query.append(type).append("='\"").append(ids.get(0)).append("\"'");
			for (int i = 1; i < ids.size(); i++) {
				query.append(" or ").append(type).append("='\"").append(ids.get(i)).append("\"'");
			}
			query.append(" )");
		}
	}

	/**
	 * Constructs a query in a string builder that extracts activities depending on the included
	 * and/or excluded username. This is specified by a boolean value.
	 * 
	 * @param query
	 *            the builder
	 * @param user
	 *            the provided username
	 * @param include
	 *            specifies if the username is to be or not to be searched for
	 */
	public void constructUser(StringBuilder query, String user, boolean include) {
		if ((query != null) && StringUtils.isNotNullOrEmpty(user)) {
			if (include) {
				query.append(" and username='\"");
			} else {
				query.append(" and username!='\"");
			}
			query.append(user).append("\"'");
		}
	}

	/**
	 * Sort activities results by field name.
	 * 
	 * @param query
	 *            base query
	 * @param sortField
	 *            sort field
	 * @param asc
	 *            ascending
	 */
	public void sortBy(StringBuilder query, String sortField, boolean asc){
		if ((query != null) && StringUtils.isNotNullOrEmpty(sortField)) {
			query.append(" order by ").append(sortField);
			if(asc){
				query.append(" asc");
			}else{
				query.append(" desc");
			}
		}
	}

	/**
	 * Close DB connection
	 * 
	 * @param connection
	 *            the connection
	 */
	public void closeConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
			logger.trace("BAM connection closed.");
		} catch (SQLException e) {
			logger.warn("Failed to close the connection to BAM DB due to " + e.getMessage());
			logger.debug("", e);
		}
	}
}
