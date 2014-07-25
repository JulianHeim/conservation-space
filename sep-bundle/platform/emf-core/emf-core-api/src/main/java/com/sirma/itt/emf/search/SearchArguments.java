package com.sirma.itt.emf.search;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base search DTO.
 *
 * @param <E>
 *            is the type of the result
 * @author BBonev
 */
public class SearchArguments<E> {

	/** Current page number. The page numbers are starting from 1. */
	protected int pageNumber;

	/** Number of results from a given search. */
	protected int resultSize;

	/** The page size. */
	// TODO from config
	protected int pageSize = 25;

	/** The skip count. */
	protected int skipCount = 0;

	/** The total items. */
	protected int totalItems = -1;
	/** max size for total items to retrieve on first search. */
	private int maxSize = 1000;
	/** The result. */
	protected List<E> result;

	/** The ordered. */
	protected boolean ordered = true;

	/** The sorter. */
	protected Sorter sorter;// = new Sorter(DefaultProperties.MODIFIED_ON, Sorter.SORT_DESCENDING);

	/** the query. */
	protected Query query;

	/** String query. Contains Native SPARQL Query (from prepared queries) or Sirma advanced search query */
	protected String stringQuery;

	/**
	 * Name of the prepared query if the query is prepared else it is null
	 */
	protected String queryName;

	/**
	 * True if the query in stringQuery field is a SPARQL statement. Else this value is False
	 * This field is used only when the field stringQuery is NATIVE SPARQL Query
	 */
	protected boolean isSparqlQuery;

	/** The arguments. */
	protected Map<String, Serializable> arguments;

	private String context;

	/**
	 * Setter method for result.
	 *
	 * @param result
	 *            the result to set
	 */
	public void setResult(List<E> result) {
		this.result = result;
	}

	/**
	 * Getter method for result.
	 *
	 * @return the result
	 */
	public List<E> getResult() {
		return result;
	}

	/**
	 * Getter method for pageSize.
	 *
	 * @return the pageSize
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * Setter method for pageSize.
	 *
	 * @param pageSize
	 *            the pageSize to set
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * Getter method for skipCount.
	 *
	 * @return the skipCount
	 */
	public int getSkipCount() {
		return skipCount;
	}

	/**
	 * Setter method for skipCount.
	 *
	 * @param skipCount
	 *            the skipCount to set
	 */
	public void setSkipCount(int skipCount) {
		this.skipCount = skipCount;
	}

	/**
	 * Getter method for totalItems.
	 *
	 * @return the totalItems
	 */
	public int getTotalItems() {
		return totalItems;
	}

	/**
	 * Setter method for totalItems.
	 *
	 * @param totalItems
	 *            the totalItems to set
	 */
	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

	/**
	 * Setter method for ordered.
	 *
	 * @param ordered
	 *            the ordered to set
	 */
	public void setOrdered(boolean ordered) {
		this.ordered = ordered;
	}

	/**
	 * Getter method for ordered.
	 *
	 * @return the ordered
	 */
	public boolean isOrdered() {
		return ordered;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SearchArguments [getResult()=").append(getResult())
				.append(", getPageSize()=").append(getPageSize()).append(", getSkipCount()=")
				.append(getSkipCount()).append(", getTotalItems()=").append(getTotalItems())
				.append(", isOrdered()=").append(isOrdered()).append("]");
		return builder.toString();
	}

	/**
	 * Setter method for sorter.
	 *
	 * @param sorter
	 *            the sorter to set
	 */
	public void setSorter(Sorter sorter) {
		this.sorter = sorter;
	}

	/**
	 * Getter method for sorter.
	 *
	 * @return the sorter
	 */
	public Sorter getSorter() {
		return sorter;
	}

	/**
	 * Getter method for arguments.
	 *
	 * @return the arguments
	 */
	public Map<String, Serializable> getArguments() {
		if (arguments == null) {
			arguments = new LinkedHashMap<String, Serializable>();
		}
		return arguments;
	}

	/**
	 * Setter method for arguments.
	 *
	 * @param arguments
	 *            the arguments to set
	 */
	public void setArguments(Map<String, Serializable> arguments) {
		this.arguments = arguments;
	}

	/**
	 * Gets the query.
	 *
	 * @return the query
	 */
	public Query getQuery() {
		return query;
	}

	/**
	 * Sets the query.
	 *
	 * @param query
	 *            the query to set
	 */
	public void setQuery(Query query) {
		this.query = query;
	}

	/**
	 * Getter method for pageNumber.
	 *
	 * @return the pageNumber
	 */
	public int getPageNumber() {
		return pageNumber;
	}

	/**
	 * Setter method for pageNumber.
	 *
	 * @param pageNumber
	 *            the pageNumber to set
	 */
	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	/**
	 * Gets the max size for query results.
	 *
	 * @return the max size
	 */
	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * @return the context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Sets the context (site name) to search under. This is foresaw for multitenancy search
	 *
	 * @param context
	 *            the context to set
	 */
	public void setContext(String context) {
		this.context = context;
	}

	/**
	 * Getter method for stringQuery.
	 *
	 * @return the stringQuery
	 */
	public String getStringQuery() {
		return stringQuery;
	}

	/**
	 * Setter method for stringQuery.
	 *
	 * @param stringQuery
	 *            the stringQuery to set
	 */
	public void setStringQuery(String stringQuery) {
		this.stringQuery = stringQuery;
	}

	/**
	 * Getter method for isSparqltQuery.
	 *
	 * @return the isSparqltQuery
	 */
	public boolean isSparqlQuery() {
		return isSparqlQuery;
	}

	/**
	 * Setter method for isSparqltQuery.
	 *
	 * @param isSparqlQuery
	 *            the isSparqltQuery to set
	 */
	public void setSparqlQuery(boolean isSparqlQuery) {
		this.isSparqlQuery = isSparqlQuery;
	}

	/**
	 * Getter method for queryName.
	 *
	 * @return the queryName
	 */
	public String getQueryName() {
		return queryName;
	}

	/**
	 * Setter method for queryName.
	 *
	 * @param queryName the queryName to set
	 */
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

}
