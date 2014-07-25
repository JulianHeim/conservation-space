package com.sirma.cmf.web.userdashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.sirma.cmf.web.EntityAction;
import com.sirma.cmf.web.ItemSelector;
import com.sirma.cmf.web.userdashboard.event.DashletToolbarActionBinding;
import com.sirma.cmf.web.userdashboard.event.DashletToolbarActionEvent;
import com.sirma.cmf.web.util.DashboardPanelLoaderExecutor;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.OwnedModel;
import com.sirma.itt.emf.scheduler.SchedulerConfiguration;
import com.sirma.itt.emf.scheduler.SchedulerContext;
import com.sirma.itt.emf.scheduler.SchedulerEntry;
import com.sirma.itt.emf.scheduler.SchedulerEntryType;
import com.sirma.itt.emf.scheduler.SchedulerExecuter;
import com.sirma.itt.emf.scheduler.SchedulerService;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.AuthorityService;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.web.config.EmfWebConfigurationProperties;

/**
 * The Class DashboardPanelActionBase.
 * 
 * @param <E>
 *            the element type.
 * @param <A>
 *            the generic type.
 * @author svelikov
 */
public abstract class DashboardPanelActionBase<E extends Entity, A extends SearchArguments<E>>
		extends EntityAction implements ItemSelector {

	private static final int DASHLET_WAIT_FOR_DATA_INTERVAL = 5;
	private static final String DASHLET_NOT_LOADED_DATA_MSG = "Data was not recieved in on time for ";

	protected boolean debug;

	@Inject
	protected SearchService searchService;

	@Inject
	private AuthorityService authorityService;

	private List<Action> dashletActions;

	@Inject
	private SchedulerService schedulerService;

	@Inject
	private SchedulerExecuter schedulerExecuter;

	@Inject
	private Event<DashletToolbarActionEvent> dashletToolbarActionEvent;

	/** Maximum elements to be displayed in user dashlet. */
	@Inject
	@Config(name = EmfWebConfigurationProperties.DASHLET_SEARCH_RESULT_PAGESIZE, defaultValue = "25")
	protected int maxSize = 0;

	protected String userId;

	protected String userURI;

	/** The loading flag shows if data for dashlets is still loading. */
	protected boolean loading = false;

	protected Semaphore semaphore = new Semaphore(1);

	private TimeTracker timeTracker;

	/**
	 * Initialize.
	 */
	@PostConstruct
	public void initialize() {
		User currentLoggedUser = authenticationService.getCurrentUser();
		userId = currentLoggedUser.getIdentifier();
		userURI = (String) currentLoggedUser.getId();
		debug = log.isDebugEnabled();
		timeTracker = new TimeTracker();
	}

	/**
	 * Wait for data to load when asynchronous of data is done.
	 */
	protected void waitForDataToLoad() {
		// if the asynchronous loading is in progress then we need to check if the data is present
		if (loading) {
			try {
				// REVIEW: change to trace level
				if (debug) {
					timeTracker.begin();
					log.debug(this.getClass().getSimpleName() + " checking if data is ready");
				}
				// does not enter again to block indefinitely because we have only one available
				// token and other thread enters here it could be blocked. Also it resets the state
				// for asynchronous loading so that other calls of this method does not block
				loading = false;
				// check if the data is available if so continue
				if (!semaphore.tryAcquire(DASHLET_WAIT_FOR_DATA_INTERVAL, TimeUnit.SECONDS)) {
					String msg = DASHLET_NOT_LOADED_DATA_MSG + " ["
							+ this.getClass().getSimpleName() + "]";
					log.warn(msg);
					// notificationSupport.addMessage(new NotificationMessage(msg,
					// MessageLevel.WARN));
				}
				if (debug) {
					log.debug(this.getClass().getSimpleName() + " data available in "
							+ timeTracker.stopInSeconds() + " s");
				}
			} catch (InterruptedException e) {
				log.warn("", e);
			}
		}
	}

	/**
	 * Notify for loaded data.
	 */
	protected void notifyForLoadedData() {
		// release a token to notify for data availability
		semaphore.release();
	}

	/**
	 * Called when the dashboard is opened.
	 */
	@Secure
	public void onOpen() {
		loadFilters();

		boolean filterActions = getFilterActions();
		loadDashletActions(dashletActionIds(), filterActions);

		// If asynchronous loading is supported then we will call it. If not, we call the filter.
		if (isAsynchronousLoadingSupported()) {
			executeDefaultFiltersAsync();
		} else {
			executeDefaultFilter();
		}
	}

	/**
	 * Execute default filters asynchronously.
	 */
	public void executeDefaultFiltersAsync() {
		// initialize before begin
		initializeForAsynchronousInvocation();

		// set loading start
		semaphore.drainPermits();
		loading = true;

		try {
			// create just some dummy configuration we will configure it later
			SchedulerConfiguration configuration = schedulerService
					.buildEmptyConfiguration(SchedulerEntryType.EVENT);
			// set that we need asynchronous invocation
			configuration.setSynchronous(false).setInSameTransaction(true).setPersistent(false);
			// build request entry
			SchedulerEntry entry = new SchedulerEntry();
			entry.setConfiguration(configuration);
			// instantiate new action instance
			entry.setAction(schedulerService.getActionByName(DashboardPanelLoaderExecutor.NAME));
			SchedulerContext context = new SchedulerContext();
			entry.setContext(context);
			// initialize the context with the current dashboard implementation
			context.put(DashboardPanelLoaderExecutor.DASHBOARD, (Serializable) this);

			beforeAsyncLoading(entry);
			// call asynchronously
			schedulerExecuter.execute(entry);
		} catch (RuntimeException e) {
			// if the loading failed for some reason, release a token to unblock any waiting threads
			semaphore.release();
			log.error("Dashlets data loading failed for some reason!", e);
			throw e;
		}
	}

	/**
	 * Method called before executing the given entry to load the dashboard data.
	 * 
	 * @param entry
	 *            that is going to be executed
	 */
	protected void beforeAsyncLoading(SchedulerEntry entry) {
		// nothing to do in the default impl
	}

	/**
	 * Loads all allowed actions trough authority service and converts to collection with names.
	 * 
	 * @return the actions for current instance
	 */
	protected Set<String> getActionsForCurrentInstance() {
		timeTracker.begin();
		Set<String> instanceActions = new LinkedHashSet<String>();
		Instance dashletActionsTargetInstance = dashletActionsTarget();
		// refresh instance to ensure we work with last properties version
		instanceService.refresh(dashletActionsTargetInstance);
		Set<Action> allowedActions = authorityService.getAllowedActions(
				dashletActionsTargetInstance, targetDashletName());
		for (Action action : allowedActions) {
			instanceActions.add(action.getActionId());
		}
		if (debug) {
			log.debug("Loading actions for instance type["
					+ dashletActionsTargetInstance.getClass().getSimpleName() + "] with id["
					+ dashletActionsTargetInstance.getId() + "] took "
					+ timeTracker.stopInSeconds() + " s");
		}
		return instanceActions;
	}

	/**
	 * Load dashlet actions.
	 * 
	 * @param dashletActionIds
	 *            the dashlet action ids
	 * @param filter
	 *            If actions should be filtered or not.
	 */
	private void loadDashletActions(Set<String> dashletActionIds, boolean filter) {
		timeTracker.begin();
		List<Action> actions = new ArrayList<Action>();
		Instance targetInstance = dashletActionsTarget();
		String placeholder = targetDashletName();
		if (targetInstance == null) {
			return;
		}
		if (filter) {
			if ((dashletActionIds != null) && !dashletActionIds.isEmpty()) {
				for (String actionId : dashletActionIds) {
					Set<Action> filteredAllowedActions = authorityService.filterAllowedActions(
							targetInstance, placeholder, actionId);
					actions.addAll(filteredAllowedActions);
				}
			}
		} else {
			Set<Action> allowedActions = authorityService.getAllowedActions(targetInstance,
					placeholder);
			actions.addAll(allowedActions);
		}
		dashletActions = actions;
		if (debug) {
			log.debug("Loading actions for instance type["
					+ targetInstance.getClass().getSimpleName() + "] with id["
					+ targetInstance.getId() + "] took " + timeTracker.stopInSeconds() + " s");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void itemSelectedAction(String selectedItemId, String selectedItemTitle) {

		log.debug("CMFWeb: Executing DashboardPanelActionBase.itemSelectAction: selected filter ["
				+ selectedItemId + "]");

		updateSelectedFilterField(selectedItemId);

		A searchArguments = getSearchArguments();
		searchArguments.setMaxSize(maxSize);
		searchArguments.setPageSize(maxSize);
		filter(searchArguments);
	}

	/**
	 * Gets the actions for result list instances.
	 * 
	 * @param <I>
	 *            the generic type
	 * @param loadedInstances
	 *            the loaded instances
	 * @param placeholder
	 *            the placeholder
	 * @return the actions for instances
	 */
	public <I extends Instance> Map<Serializable, List<Action>> getActionsForInstances(
			List<I> loadedInstances, String placeholder) {
		timeTracker.begin();
		Map<Serializable, List<Action>> actions = new HashMap<>();
		boolean clearTarget;
		for (Instance instance : loadedInstances) {
			clearTarget = false;
			List<Action> evaluatedActions = new ArrayList<Action>();
			// this is set to allow action evaluation in a temporary(actual) context for instances
			// that does not have currently a context but are associated with the current instance
			// (if any - such as for user dashboard panels which does not have such)
			if ((instance instanceof OwnedModel)
					&& (((OwnedModel) instance).getOwningInstance() == null)) {
				// NOTE: the method dashletActionsTarget() could throw an exception if it accesses
				// the document context, because the current method is executed asynchronously and
				// there is no user context. To fix this backup the instance in the method
				// initializeForAsynchronousInvocation(). See CaseWorkflowPanel as example.

				((OwnedModel) instance).setOwningInstance(dashletActionsTarget());
				clearTarget = true;
			}
			evaluatedActions.addAll(authorityService.getAllowedActions(userId, instance,
					placeholder));
			actions.put(instance.getId(), evaluatedActions);
			// clean the instance if needed
			if (clearTarget) {
				((OwnedModel) instance).setOwningInstance(null);
			}
		}
		if (debug) {
			log.debug("Loading actions for " + loadedInstances.size() + " instances took "
					+ timeTracker.stopInSeconds() + " s");
		}
		return actions;
	}

	/**
	 * Execute toolbar action.
	 * 
	 * @param dashlet
	 *            the dashlet
	 * @param action
	 *            the action
	 * @return the string
	 */
	public String executeToolbarAction(String dashlet, String action) {
		timeTracker.begin();
		log.debug("Executing dashlet toolbar action[" + action + "] dashlet [" + dashlet + "]");
		DashletToolbarActionBinding binding = new DashletToolbarActionBinding(dashlet, action);
		DashletToolbarActionEvent event = new DashletToolbarActionEvent();
		dashletToolbarActionEvent.select(binding).fire(event);
		log.debug("Dashlet toolbar action took " + timeTracker.stopInSeconds() + " s");
		return event.getNavigation();
	}

	/**
	 * Prepare filters list to be displayed on the page.
	 */
	public abstract void loadFilters();

	/**
	 * Dashlet action ids.
	 * 
	 * @return the sets the
	 */
	public abstract Set<String> dashletActionIds();

	/**
	 * Target dashlet name as css class.
	 * 
	 * @return the string.
	 */
	public abstract String targetDashletName();

	/**
	 * Dashlet actions target.
	 * 
	 * @return the instance.
	 */
	public abstract Instance dashletActionsTarget();

	/**
	 * Execute default filter for given panel.
	 */
	public abstract void executeDefaultFilter();

	/**
	 * Execute filter method where appropriate search service should be called to fetch the result.
	 * 
	 * @param searchArguments
	 *            the search arguments.
	 */
	public abstract void filter(A searchArguments);

	/**
	 * Update selected filter field.
	 * 
	 * @param selectedItemId
	 *            the selected item id.
	 */
	public abstract void updateSelectedFilterField(String selectedItemId);

	/**
	 * Gets the search arguments.
	 * 
	 * @return the search arguments.
	 */
	public abstract A getSearchArguments();

	/**
	 * Getter method for dashletActions.
	 * 
	 * @return the dashletActions.
	 */
	public List<Action> getDashletActions() {
		return dashletActions;
	}

	/**
	 * Setter method for dashletActions.
	 * 
	 * @param dashletActions
	 *            the dashletActions to set.
	 */
	public void setDashletActions(List<Action> dashletActions) {
		this.dashletActions = dashletActions;
	}

	/**
	 * Checks if is asynchronous loading supported. If not the overriding class should return
	 * <code>false</code>
	 * 
	 * @return true, if is asynchronous loading supported
	 */
	protected boolean isAsynchronousLoadingSupported() {
		return true;
	}

	/**
	 * Method called before beginning the asynchronous invocation.
	 */
	protected void initializeForAsynchronousInvocation() {
		// can be overridden in successors
	}

	/**
	 * This method can be overriden when dashlet actions should be filtered one by one as provided
	 * from the dashlet implementation or to be loaded all.
	 * 
	 * @return the filter actions
	 */
	protected boolean getFilterActions() {
		return true;
	}
}
