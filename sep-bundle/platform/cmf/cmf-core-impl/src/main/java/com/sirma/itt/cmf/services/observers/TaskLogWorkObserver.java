package com.sirma.itt.cmf.services.observers;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.cmf.constants.TaskProperties;
import com.sirma.itt.cmf.event.task.worklog.AddedWorkLogEntryEvent;
import com.sirma.itt.cmf.event.task.worklog.DeleteWorkLogEntryEvent;
import com.sirma.itt.cmf.event.task.worklog.UpdatedWorkLogEntryEvent;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.instance.model.InitializedInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.properties.PropertiesService;

/**
 * An asynchronous update interface for receiving notifications
 * about TaskLogWork information as the TaskLogWork is constructed.
 */
public class TaskLogWorkObserver {
	
	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;
	
	/** The properties service. */
	@Inject
	private PropertiesService propertiesService;
	
	/**
	 * This method is called when a {@link AddedWorkLogEntryEvent} is observed.
	 * It accumulates task's actualEffort based on the logged work.
	 *
	 * @param event the event
	 */
	public void onAddedWorkLog(@Observes AddedWorkLogEntryEvent event) {
		Map<String, Serializable> workLogData = event.getProperties();
		if (workLogData != null) {
			Serializable timeSpent = workLogData.get(TaskProperties.TIME_SPENT);
			if (timeSpent != null && timeSpent instanceof Number) {
				InstanceReference instanceRef = event.getInstanceRef();
				accumulateWorkLogOnTask(instanceRef, ((Number)timeSpent).longValue());
			}
		}
	}
	
	/**
	 * This method is called when a {@link UpdatedWorkLogEntryEvent} is observed.
	 * It calculates task's actualEffort based on the logged work.
	 *
	 * @param event the event
	 */
	public void onUpdatedWorkLog(@Observes UpdatedWorkLogEntryEvent event) {
		Map<String, Serializable> newLoggedData = event.getNewLoggedData();
		Map<String, Serializable> oldLoggedData = event.getOldLoggedData();
		if (newLoggedData != null && oldLoggedData != null) {
			Serializable newTimeSpent = newLoggedData.get(TaskProperties.TIME_SPENT);
			Serializable oldTimeSpent = oldLoggedData.get(TaskProperties.TIME_SPENT);
			if (newTimeSpent != null && oldTimeSpent != null && 
					newTimeSpent instanceof Number && oldTimeSpent instanceof Number) {
				Long timeSpent = ((Number)newTimeSpent).longValue() - ((Number)oldTimeSpent).longValue();
				InstanceReference instanceRef = event.getInstanceRef();
				accumulateWorkLogOnTask(instanceRef, timeSpent);
			}
		}
	}
	
	/**
	 * This method is called when a {@link DeleteWorkLogEntryEvent} is observed.
	 * It calculates task's actualEffort based on the logged work.
	 *
	 * @param event the event
	 */
	public void onDeletedWorkLog(@Observes DeleteWorkLogEntryEvent event) {
		Map<String, Serializable> workLogData = event.getWorkLogData();
		if (workLogData != null) {
			Serializable timeSpent = workLogData.get(TaskProperties.TIME_SPENT);
			if (timeSpent != null && timeSpent instanceof Number) {
				InstanceReference instanceRef = event.getInstanceRef();
				accumulateWorkLogOnTask(instanceRef, -((Number)timeSpent).longValue());
			}
		}
	}
	
	/**
	 * Accumulates time in hours to task's current actualEffort.
	 *
	 * @param instanceRef the instance ref
	 * @param timeToAdd the time to add. Can be negative on log work update or delete.
	 */
	private void accumulateWorkLogOnTask(InstanceReference instanceRef, long timeToAdd) {
		InitializedInstance instance = typeConverter.convert(InitializedInstance.class, instanceRef);
		Instance taskInstance = instance.getInstance();
		Map<String, Serializable> taskProperties = taskInstance.getProperties();
		if (taskProperties != null) {
			Serializable actualEffort = taskProperties.get(TaskProperties.ACTUAL_EFFORT);
			Long updatedActualEffort = 0L;
			if (actualEffort != null && actualEffort instanceof Long) {
				updatedActualEffort = (Long)actualEffort;
			}
			updatedActualEffort += timeToAdd;
			// Actual effort couldn't be negative value
			if (updatedActualEffort >= 0) {
				taskProperties.clear();
				taskProperties.put(TaskProperties.ACTUAL_EFFORT, updatedActualEffort);
				propertiesService.saveProperties(taskInstance, true);
			}
		}
	}

}
