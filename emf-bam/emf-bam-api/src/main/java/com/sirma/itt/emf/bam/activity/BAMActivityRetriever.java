/**
 * 
 */
package com.sirma.itt.emf.bam.activity;

import java.util.List;

/**
 * Allows retrieval of recorded activities.
 * 
 * @author Nikolay Velkov
 * 
 */
public interface BAMActivityRetriever {

	/**
	 * Retrieves BAM activities by a provided criteria.
	 * 
	 * @param activityCriteria
	 *            the provided criteria
	 * @return a list of {@link BAMActivity}. If the list is empty then no
	 *         activity is present for the criteria
	 */
	List<BAMActivity> getActivities(BAMActivityCriteria activityCriteria);
}
