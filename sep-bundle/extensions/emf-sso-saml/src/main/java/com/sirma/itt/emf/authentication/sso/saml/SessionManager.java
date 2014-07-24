package com.sirma.itt.emf.authentication.sso.saml;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpSession;

/**
 * Manager is responsible to synchronize and organize logout process during concurrent invocations.
 *
 * @author bbanchev
 */
@ApplicationScoped
public class SessionManager {
	/** List of currently processed users. */
	private Set<String> preprocessing = new HashSet<>();

	/** The sessions. */
	private Map<String, HttpSession> sessions = new WeakHashMap<>();

	/** The timer. */
	private Timer timer = new Timer();

	/**
	 * Thread to remove from cache.
	 *
	 * @author bbanchev
	 */
	private class RemoveCachedProcess extends TimerTask {
		// local copy if id
		/** The id. */
		private String id;

		/**
		 * Construct the thread for that user.
		 *
		 * @param id
		 *            is the user id.
		 */
		public RemoveCachedProcess(String id) {
			this.id = id;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			synchronized (preprocessing) {
				preprocessing.remove(id);
			}
		}
	}

	/**
	 * Invoke the user on logout started process. 5 sec after invoke user is removed automatically.
	 *
	 * @param id
	 *            is the user id.
	 */
	public void beginLogout(String id) {
		if (id != null) {
			synchronized (preprocessing) {
				boolean processing = isProcessing(id);
				if (!processing) {
					preprocessing.add(id);
					// start the process and remove the processing id, since it is expected all tabs
					// had finished the logout initiating process
					timer.schedule(new RemoveCachedProcess(id), 5000L);
				}
			}
		}
	}

	/**
	 * Is the user currently processed.
	 *
	 * @param id
	 *            is the user id
	 * @return true if it is still in local cache, false if user is not in cache
	 */
	public boolean isProcessing(String id) {
		if (id != null) {
			synchronized (preprocessing) {
				if (preprocessing.contains(id)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Explicitly remove user from cache.
	 *
	 * @param id
	 *            is the user id
	 */
	public void finishLogout(String id) {
		if (id != null) {
			synchronized (preprocessing) {
				timer.schedule(new RemoveCachedProcess(id), 2000L);
			}
		}
	}

	/**
	 * Register a session to its idp index. If the same index exists it is replaced and the old
	 * value is returned
	 *
	 * @param sessionIndex
	 *            the session index
	 * @param session
	 *            the session to link
	 * @return null if the value is not added or if the index has not been presented in the register
	 */
	public HttpSession registerSession(String sessionIndex, HttpSession session) {
		if (sessionIndex != null && session != null) {
			return sessions.put(sessionIndex, session);
		}
		return null;
	}

	/**
	 * Gets the session for the given index id.
	 *
	 * @param sessionIndex
	 *            the session index
	 * @return the session for the key or null if not found or sessionIndex is null
	 */
	public HttpSession getSession(String sessionIndex) {
		return sessionIndex != null ? sessions.get(sessionIndex) : null;
	}

	/**
	 * Unregister session from the register.
	 *
	 * @param sessionIndex
	 *            the session index
	 * @return the session that has been linked to the key or null if not found or sessionIndex is
	 *         null
	 */
	public HttpSession unregisterSession(String sessionIndex) {
		return sessionIndex != null ? sessions.remove(sessionIndex) : null;
	}
}
