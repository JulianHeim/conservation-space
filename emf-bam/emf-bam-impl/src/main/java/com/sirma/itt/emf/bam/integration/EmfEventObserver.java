/**
 * 
 */
package com.sirma.itt.emf.bam.integration;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.StandaloneTaskInstance;
import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.bam.agent.BAMAgent;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.evaluation.ExpressionContext;
import com.sirma.itt.emf.evaluation.ExpressionsManager;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.OwnedModel;
import com.sirma.itt.emf.link.LinkInstance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.properties.event.PropertiesChangeEvent;
import com.sirma.itt.emf.provider.ProviderRegistry;
import com.sirma.itt.emf.security.event.UserAuthenticatedEvent;
import com.sirma.itt.emf.security.event.UserLogoutEvent;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.state.StateService;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * Observes actions related to user account like login and logout and property
 * changes upon objects in EMF. Filters incoming events and sends them to a BAM
 * agent. This is an application scoped EJB so it doesn't instantiate every time
 * an event is observed.
 * 
 * @author Mihail Radkov
 */
@ApplicationScoped
public class EmfEventObserver {

	/** Logger used for displaying messages related to this class. */
	private final Logger logger = LoggerFactory
			.getLogger(EmfEventObserver.class);

	/** Date formatter for creating a formatted time stamp. */
	private final DateFormat dateFormat = new SimpleDateFormat(
			"YYYYMMddHHmmssSSS");

	/** Agent that handles events by sending them to a BAM server. */
	@Inject
	private BAMAgent bamAgent;

	/** Manager used when creating URLs to EMF instances. */
	@Inject
	private ExpressionsManager expressionsManager;

	/**
	 * Action registry used for extracting label names depending on the system's
	 * language.
	 */
	@Inject
	private ProviderRegistry<Pair<Class<?>, String>, Action> actionRegistry;

	/** Service used for extracting the state of an instance. */
	@Inject
	private StateService stateService;

	/** Configuration property indicating the system's language. */
	@Inject
	@Config(name = EmfConfigurationProperties.SYSTEM_LANGUAGE)
	private String language;

	/**
	 * Called when a property change occurs. Checks if the event's entity is
	 * instance of instance and if so process it further. If the event is
	 * filtered additional payload is added and sent to an BAM agent.
	 * 
	 * @param event
	 *            the event's payload
	 */
	public void onPropertyChange(
			@Observes(during = TransactionPhase.AFTER_SUCCESS) PropertiesChangeEvent event) {
		// TODO: Asynchronous!!!! If something from here hangs, the UI also
		// hangs.
		// TODO: Move it in another method.
		// Checking if the event's entity is an instance of Instance so a cast
		// can be safely done.

		if (event.getEntity() instanceof Instance) {
			Instance instance = (Instance) event.getEntity();
			List<Object> payload = handleInstanceProperties(instance);
			// Checking if a payload is constructed for the event. If not this
			// means the instance is
			// not to be handled further. If it is constructed, additional
			// payload is added.
			if (payload != null) {
				// Adding the entity's id.
				payload.add(1, getObjectID(event.getEntity()));
				// Adding the operation label based on the system language.
				payload.add(0,
						getOperationLabel(event.getOperationId(), instance));
				// Adding a formatted timestamp.
				payload.add(0, getFormattedTimestamp(event.getTimestamp()));
				// Adding a bookmark link to the instance.
				payload.add(createBookmarkLink(instance));
				// Adding the operation id.
				payload.add(event.getOperationId());
				// Adding the instance state.
				payload.add(getState(instance));

				payload.add(getProjectID(instance));

				payload.add(getCaseID(instance));
				// Sending the constructed payload to the agent.
				bamAgent.addEvent(payload);
			}
		}
	}

	/**
	 * Extracts recursively an instance's project id.
	 * 
	 * @param instance
	 *            the instance
	 * @return the project's id if there is any
	 */
	public String getProjectID(Instance instance) {
		if (instance instanceof OwnedModel) {
			Instance ownedInstance = ((OwnedModel) instance)
					.getOwningInstance();
			if (ownedInstance instanceof ProjectInstance) {
				return getProperties(ownedInstance).get(
						DefaultProperties.UNIQUE_IDENTIFIER).toString();
			} else {
				return getProjectID(ownedInstance);
			}
		}
		return "";
	}

	/**
	 * Extracts recursively an instance's case id if there is any.
	 * 
	 * @param instance
	 *            the instance
	 * @return the case's id if there is any
	 */
	public String getCaseID(Instance instance) {
		if (instance instanceof CaseInstance) {
			return getProperties(instance).get(
					DefaultProperties.UNIQUE_IDENTIFIER).toString();
		}
		if (instance instanceof OwnedModel) {
			Instance ownedInstance = ((OwnedModel) instance)
					.getOwningInstance();
			if (ownedInstance instanceof CaseInstance) {
				return getProperties(ownedInstance).get(
						DefaultProperties.UNIQUE_IDENTIFIER).toString();
			} else {
				return getProjectID(ownedInstance);
			}
		}
		return "";
	}

	/**
	 * Handles EMF object instances by filtering them and returns a constructed
	 * payload from their properties. If the passed instance is not filtered,
	 * the method returns null.
	 * 
	 * @param instance
	 *            EMF object instance
	 * @return constructed payload represented in list of objects or null if the
	 *         instance is not handled
	 */
	public List<Object> handleInstanceProperties(Instance instance) {
		// TODO: Which properties?
		if (instance instanceof CaseInstance) {
			return constructPayload(getProperties(instance), instance.getClass().getSimpleName().toLowerCase(),
					DefaultProperties.TITLE);
		} else if (instance instanceof DocumentInstance) {
			return constructPayload(getProperties(instance), instance.getClass().getSimpleName().toLowerCase(),
					DefaultProperties.NAME);
		} else if (instance instanceof StandaloneTaskInstance) {
			// TODO: Assigned?
			return constructPayload(getProperties(instance), instance.getClass().getSimpleName().toLowerCase(),
					DefaultProperties.TITLE);
		} else if (instance instanceof TaskInstance) {
			// TODO: Assigned?
			return constructPayload(getProperties(instance), instance.getClass().getSimpleName().toLowerCase(),
					DefaultProperties.TITLE);
		} else if (instance instanceof ProjectInstance) {
			return constructPayload(getProperties(instance), instance.getClass().getSimpleName().toLowerCase(),
					DefaultProperties.TITLE);
		} else if (instance instanceof ObjectInstance) {
			return constructPayload(getProperties(instance), instance.getClass().getSimpleName().toLowerCase(),
					DefaultProperties.TITLE);
		} else if (instance instanceof LinkInstance) {
			// TODO: Will be handled later.
			return null;
		} else {
			return null;
		}
	}

	/**
	 * Constructs a list of objects from properties related to an EMF object
	 * instance.
	 * 
	 * @param properties
	 *            the properties of an EMF object instance
	 * @param object
	 *            the type of the instance
	 * @param params
	 *            variadic parameter specifying which properties to be extracted
	 * @return a payload represented by list of objects
	 */
	public List<Object> constructPayload(Map<String, Serializable> properties,
			String object, String... params) {
		List<Object> payload = new LinkedList<Object>();
		if (properties != null && object != null && params != null) {
			// Retrieves the last user who modified the object.
			payload.add(properties.get(DefaultProperties.MODIFIED_BY));
			payload.add(object);
			for (String param : params) {
				payload.add(properties.get(param));
			}
			payload.add(properties.get(DefaultProperties.TYPE));
			payload.add(properties.get(DefaultProperties.UNIQUE_IDENTIFIER));
		}
		return payload;
	}

	/**
	 * Retrieves the state of an instance of Instance. Performs a null check.
	 * 
	 * @param instance
	 *            the provided instance
	 * @return the state of the instance or null if it is null
	 */
	private String getState(Instance instance) {
		if (instance != null) {
			return stateService.getPrimaryState(instance);
		}
		return null;
	}

	/**
	 * Retrieves labels for operations based on the provided instance and the
	 * system language. If the operationID is null or empty, nothing is
	 * retrieved.
	 * 
	 * @param operationID
	 *            the operation id
	 * @param instance
	 *            the provided instance
	 * @return the retrieved label or if the operationID is null or empty then
	 *         returns null or empty string accordingly
	 */
	private String getOperationLabel(String operationID, Instance instance) {
		String label = operationID;
		if (StringUtils.isNotNullOrEmpty(label)) {
			Action action = actionRegistry.find(new Pair<Class<?>, String>(
					instance.getClass(), operationID));
			if (action != null) {
				label = action.getLabel();
			} else {
				// When an action doesn't exist for some instance. (like
				// 'suspend' for a document)
				logger.warn("Action " + operationID + " is null for "
						+ instance.getClass().getSimpleName());
			}
		}
		return label;
	}

	/**
	 * Retrieves an entity's id. If the entity is null then a null value is
	 * returned.
	 * 
	 * @param entity
	 *            the entity
	 * @return the entity's id as string or null if the entity is null
	 */
	public String getObjectID(Entity entity) {
		if (entity != null) {
			return entity.getId().toString();
		}
		return null;
	}

	/**
	 * Creates an URL bookmark of an EMF object. The host and port are not
	 * included to the bookmark. Performs null check for the provided instance.
	 * 
	 * @param instance
	 *            the object
	 * @return the URL bookmark or null if the provided instance is null
	 */
	private String createBookmarkLink(Instance instance) {
		if (instance != null) {
			ExpressionContext context = expressionsManager
					.createDefaultContext(instance, null, null);
			return expressionsManager.evaluateRule("${link(currentInstance)}",
					String.class, context, instance);
		}
		return null;
	}

	/**
	 * Fired when an user has logged in.
	 * 
	 * @param event
	 *            event object.
	 */
	public void onLoggedIn(
			@Observes(during = TransactionPhase.AFTER_SUCCESS) UserAuthenticatedEvent event) {
		// TODO: Null check for the event?
		User user = event.getAuthenticatedUser();
		if (user != null) {
			bamAgent.addEvent(constructPayload("login", user.getName(),
					System.currentTimeMillis()));
		}
	}

	/**
	 * Fired when an user has logged out.
	 * 
	 * @param event
	 *            event object.
	 */
	public void onLoggedOut(
			@Observes(during = TransactionPhase.AFTER_SUCCESS) UserLogoutEvent event) {
		// TODO: Null check for the event?
		User user = event.getAuthenticatedUser();
		if (user != null) {
			bamAgent.addEvent(constructPayload("logout", user.getName(),
					System.currentTimeMillis()));
		}
	}

	/**
	 * Handles an user action by creating a payload containing an information
	 * related to the action.
	 * 
	 * @param action
	 *            the action
	 * @param user
	 *            the user who did the action
	 * @param timestamp
	 *            provided timestamp
	 * @return the constructed payload related to the user action
	 */
	public List<Object> constructPayload(String action, String user,
			long timestamp) {
		List<Object> payload = new LinkedList<Object>();
		payload.add(getFormattedTimestamp(timestamp));
		payload.add(action);
		payload.add(user);
		return payload;
	}

	/**
	 * Extract properties from an instance. Performs null check.
	 * 
	 * @param instance
	 *            the instance
	 * @return the properties or null if the instance is null
	 */
	public Map<String, Serializable> getProperties(Instance instance) {
		if (instance != null) {
			return instance.getProperties();
		}
		return null;
	}

	/**
	 * Checks if two maps have different content or not.
	 * 
	 * @param added
	 *            the first map
	 * @param removed
	 *            the second map
	 * @return true if their content is different and false otherwise
	 */

	public boolean isChanged(Map<String, Serializable> added,
			Map<String, Serializable> removed) {
		return !added.equals(removed);
	}

	/**
	 * Creates a formatted time stamp from a provided long value.
	 * 
	 * @param timestamp
	 *            - the provided timestamp as long
	 * @return the formatted time stamp
	 */
	private String getFormattedTimestamp(long timestamp) {
		return dateFormat.format(timestamp);
	}

	/**
	 * Called after an instantiation of an object of this class.
	 */
	@PostConstruct
	public void postConstruct() {
		logger.info("EMF event observer constructed");
	}

	/**
	 * Called before destroying an instance of this class.
	 */
	@PreDestroy
	public void preDestroy() {
		logger.info("EMF event observer destroyed.");
	}
}
