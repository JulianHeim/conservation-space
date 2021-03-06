package com.sirma.itt.emf.services;

import java.util.List;

import javax.enterprise.event.Observes;

import com.sirma.itt.emf.event.EmfEvent;
import com.sirma.itt.emf.scheduler.SchedulerAction;
import com.sirma.itt.emf.security.model.User;

/**
 * Defines a methods for retrieving the list of user.
 * REVIEW - service should not be dependent of Scheduler
 * @author BBonev
 */
public interface PeopleService extends SchedulerAction {

	/**
	 * Gets the all registered users.
	 *
	 * @return the all users
	 */
	List<User> getAllUsers();

	/**
	 * Gets the users that match the given filter.
	 *
	 * @param filter
	 *            the filter
	 * @return the filtered users
	 */
	List<User> getFilteredUsers(String filter);

	/**
	 * Finds the user by user Id.
	 *
	 * @param userId
	 *            the user id
	 * @return the cmf user or null if userId is null or empty.
	 */
	User findUser(String userId);

	/**
	 * Gets the sorted users by the provided userinfo atributte. See
	 * {@link com.sirma.itt.emf.security.model.UserInfo}
	 *
	 * @param sortColumn
	 *            the sort column id
	 * @return the list of users sorted by the column
	 */
	List<User> getSortedUsers(String sortColumn);

	/**
	 * Async update.
	 *
	 * @param event
	 *            the event
	 */
	void asyncUpdate(@Observes PeopleSync event);

	/**
	 * The Class PeopleSync.
	 *
	 * @author BBonev
	 */
	public class PeopleSync implements EmfEvent {

	}

}
