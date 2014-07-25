package com.sirma.itt.emf.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.User;

/**
 * Default implementation for the {@link TaskExecutor} service.
 * <p>
 * REVIEW: add per user perUserUploadPoolSize mapping so that the threads used by a user can be
 * limited based on the global property not to per method call.
 * 
 * @author BBonev
 */
@ApplicationScoped
public class AsyncTaskExecutor implements TaskExecutor {
	// per user configuration
	public static final Integer MAX_PARALLEL_THREADS_PER_USER = 10;
	public static final int DEFAULT_PARALLEL_THREADS_PER_USER = 5;
	// Pool configuration
	public static final Integer MIN_RUNNING_THREADS = 20;
	public static final Integer MAX_RUNNING_THREADS = 200;
	public static final int DEFAULT_RUNNING_THREADS = 100;

	private static final String SYSTEM_USER = "%SYSTEM%";

	/** The Constant LOGGER. */
	@Inject
	private Logger LOGGER;
	/** The debug. */
	private boolean debug;
	/** The trace. */
	private boolean trace;

	/** The parallel uploads pool size config. */
	@Inject
	@Config(name = EmfConfigurationProperties.ASYNCHRONOUS_TASK_POOL_SIZE, defaultValue = ""
			+ DEFAULT_RUNNING_THREADS)
	private Integer parallelUploadsPoolSizeConfig;
	/** The parallel uploads config. */
	@Inject
	@Config(name = EmfConfigurationProperties.ASYNCHRONOUS_TASK_PER_USER_POOL_SIZE, defaultValue = ""
			+ DEFAULT_PARALLEL_THREADS_PER_USER)
	private Integer parallelUploadsConfig;

	/** The Constant LOCK. */
	private final ReentrantLock LOCK = new ReentrantLock();
	/** The upload pool size. */
	private int threadPoolSize;

	private int perUserPoolSize;
	/** The upload pool. */
	private ForkJoinPool threadPool;

	private Map<String, AtomicInteger> runningUserThreads = new LinkedHashMap<String, AtomicInteger>(
			500);

	/** The thread worker factory. */
	private ForkJoinWorkerThreadFactory threadWorkerFactory = new AsyncForkJoinWorkerThreadFactory();

	/** The current pool size. */
	private AtomicInteger currentPoolSize = new AtomicInteger(0);

	/**
	 * Initialize the thread pool.
	 */
	@PostConstruct
	public void initialize() {

		debug = LOGGER.isDebugEnabled();
		trace = LOGGER.isTraceEnabled();

		// the first thread that comes here locks takes the lock all other will exit
		if (!LOCK.tryLock()) {
			return;
		}
		try {
			// if the pool is already created then we a done
			// but if the pool is stopped then we have to recreate it
			if ((threadPool != null) && !threadPool.isShutdown() && !threadPool.isTerminated()) {
				return;
			}

			if (parallelUploadsConfig > MAX_PARALLEL_THREADS_PER_USER) {
				perUserPoolSize = MAX_PARALLEL_THREADS_PER_USER;
			} else if (parallelUploadsConfig <= 0) {
				// no parallelism
				perUserPoolSize = 1;
			} else {
				perUserPoolSize = parallelUploadsConfig;
			}

			if (parallelUploadsPoolSizeConfig > MAX_RUNNING_THREADS) {
				threadPoolSize = MAX_RUNNING_THREADS;
			} else if (parallelUploadsPoolSizeConfig < MIN_RUNNING_THREADS) {
				// no parallelism
				threadPoolSize = MIN_RUNNING_THREADS;
			} else {
				threadPoolSize = parallelUploadsPoolSizeConfig;
			}
			threadPool = new ForkJoinPool(threadPoolSize, threadWorkerFactory, null, true);
			if (debug) {
				LOGGER.debug("Created upload pool for up to " + threadPoolSize + " threads and "
						+ perUserPoolSize + " uploads per user uploads");
			}
		} finally {
			LOCK.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Secure
	@Override
	public <T extends GenericAsyncTask> void execute(List<T> tasks) {
		String runFor;
		// if we have a pool task that requires a pool execution also we should create for the given
		// user new pool entry so that he could execute the tasks
		if (Thread.currentThread() instanceof AsyncForkJoinWorkerThread) {
			runFor = Thread.currentThread().getName();
			if (trace) {
				LOGGER.trace("Will initiate cascade thread pool execution for thread: " + runFor);
			}
		} else {
			User user = SecurityContextManager.getFullAuthentication();
			runFor = SYSTEM_USER;
			if (user != null) {
				runFor = user.getName();
			}
		}
		try {
			// initialize the user thread count
			LOCK.lock();
			if (!runningUserThreads.containsKey(runFor)) {
				runningUserThreads.put(runFor, new AtomicInteger(perUserPoolSize));
			}
		} finally {
			LOCK.unlock();
		}
		try {
			executeInternal(tasks, runFor, true);
		} finally {
			// if our thread remove the entry from the pool at the end
			if (Thread.currentThread() instanceof AsyncForkJoinWorkerThread) {
				LOCK.lock();
				try {
					runningUserThreads.remove(Thread.currentThread().getName());
				} finally {
					LOCK.unlock();
				}
			}
		}
	}

	/**
	 * Execute the given list of tasks for the given user.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param tasks
	 *            the tasks to execute
	 * @param user
	 *            the user the current user
	 * @param rollbackOnError
	 *            the rollback on error
	 */
	protected <T extends GenericAsyncTask> void executeInternal(List<T> tasks, String user,
			boolean rollbackOnError) {
		List<T> runningTasks = new ArrayList<T>(perUserPoolSize);
		List<T> queuedTasks = new LinkedList<T>();
		List<T> allTasks = new LinkedList<T>();

		for (T genericAsyncTask : tasks) {
			allTasks.add(genericAsyncTask);
			if (isThreadAvailable(user)) {
				acquire(user);
				if (trace) {
					LOGGER.trace("Acquired new thread for user " + user + " remaining="
							+ runningUserThreads.get(user));
				}
				threadPool.submit((ForkJoinTask<?>) genericAsyncTask);
				runningTasks.add(genericAsyncTask);
			} else {
				queuedTasks.add(genericAsyncTask);
			}
		}

		List<Throwable> errors = new LinkedList<Throwable>();
		List<GenericAsyncTask> fails = new LinkedList<GenericAsyncTask>();
		List<GenericAsyncTask> successfullTasks = new LinkedList<GenericAsyncTask>();
		// REVIEW: this probably need to be rewritten using only the running and queued tasks
		while (!allTasks.isEmpty() || !queuedTasks.isEmpty()) {
			try {
				// we will wait some time before first next check
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// not interested
			}
			for (Iterator<T> it = allTasks.iterator(); it.hasNext();) {
				GenericAsyncTask asyncTask = it.next();
				if (asyncTask.isDone()) {
					it.remove();
					release(user);
					if (trace) {
						LOGGER.trace("Releasing worker for user " + user
								+ " available threads now: " + runningUserThreads.get(user));
					}
				}
				if (asyncTask.isCompletedNormally()) {
					if (Boolean.TRUE.equals(asyncTask.getRawResult())) {
						// execution completed
					} else {
						LOGGER.error("Failed to execute task "
								+ asyncTask.getClass().getSimpleName());
					}
					// execute the success method when all are successful
					successfullTasks.add(asyncTask);
				} else if (asyncTask.isCompletedAbnormally()) {
					Throwable exception = asyncTask.getException();
					if (exception != null) {
						LOGGER.error("Failed to execute task "
								+ asyncTask.getClass().getSimpleName() + " with: "
								+ exception.getMessage());
						errors.add(exception);
					} else {
						LOGGER.error("Failed to execute task "
								+ asyncTask.getClass().getSimpleName() + " without exception");
					}
					fails.add(asyncTask);
				}

				// if we have more tasks we add them to the list
				// schedule it and reset the iterator
				if ((isThreadAvailable(user)) && !queuedTasks.isEmpty()) {
					acquire(user);
					if (trace) {
						LOGGER.trace("Acquired new thread for user " + user + " remaining="
								+ runningUserThreads.get(user));
					}
					T newTask = queuedTasks.remove(0);
					runningTasks.add(newTask);
					threadPool.submit((ForkJoinTask<?>) newTask);
					it = allTasks.iterator();
				}
			}
		}

		// if we have any errors act accordingly
		if (!fails.isEmpty()) {
			// if found any errors and rollback if required
			if (rollbackOnError) {
				LOGGER.warn("There are " + fails.size() + " failed tasks out of " + tasks.size()
						+ ". Rolling back " + successfullTasks.size() + " successful tasks.");
				for (GenericAsyncTask task : successfullTasks) {
					try {
						task.onRollback();
					} catch (Exception e) {
						errors.add(e);
					}
				}
			}
			// if any errors call the proper methods and if some throws an exception it will be
			// thrown back later.
			for (GenericAsyncTask genericAsyncTask : fails) {
				try {
					genericAsyncTask.executeOnFail();
				} catch (Exception e) {
					errors.add(e);
				}
			}

			// if we have some errors we throw the first one so we can rollback
			// transaction.
			if (!errors.isEmpty()) {
				LOGGER.warn("When executing async tasks " + errors.size()
						+ " failed with errors. Execeptions are:");
				for (Throwable throwable : errors) {
					LOGGER.error(throwable.getMessage(), throwable);
				}
				throw new EmfRuntimeException(errors.get(0));
			}
		} else {
			for (GenericAsyncTask task : successfullTasks) {
				try {
					if (trace) {
						LOGGER.trace("Executed task " + task.getClass().getSimpleName()
								+ " successfully");
					}
					task.executeOnSuccess();
				} catch (Exception e) {
					LOGGER.error("Failed to execute " + task.getClass().getName()
							+ ".executeOnSuccess() method due to " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Checks if is thread available for execution
	 * 
	 * @param user
	 *            the user
	 * @return true, if is thread available
	 */
	protected boolean isThreadAvailable(String user) {
		return (runningUserThreads.get(user).get() > 0);
	}

	/**
	 * Decrement the available threads for the user
	 * 
	 * @param user
	 *            the user
	 */
	protected void acquire(String user) {
		currentPoolSize.incrementAndGet();
		runningUserThreads.get(user).decrementAndGet();
	}

	/**
	 * Release a thread for the user
	 * 
	 * @param user
	 *            the user
	 */
	protected void release(String user) {
		currentPoolSize.decrementAndGet();
		runningUserThreads.get(user).incrementAndGet();
	}

}
