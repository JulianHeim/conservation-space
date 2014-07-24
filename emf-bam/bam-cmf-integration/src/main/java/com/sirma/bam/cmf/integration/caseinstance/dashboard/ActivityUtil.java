package com.sirma.bam.cmf.integration.caseinstance.dashboard;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.beans.model.WorkflowInstanceContext;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.bam.activity.BAMActivity;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.entity.LinkSourceId;
import com.sirma.itt.emf.instance.InstanceUtil;
import com.sirma.itt.emf.instance.model.InitializedInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.Action;
import com.sirma.itt.emf.security.model.ActionRegistry;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * This class will help for retrieving activity data.
 * 
 * @author cdimitrov
 */
@ApplicationScoped
public class ActivityUtil {

	/** The logger instance. */
	private final Logger log = LoggerFactory.getLogger(ActivityUtil.class);

	/** Create action constant. */
	private final static String ACTION_CREATE = "create";

	/** Instance string fragment constant. */
	private final static String INSTANCE = "instance";

	/** The date pattern. */
	private final static String DATE_PATTERN = "yyyy-MM-dd hh:mm:ss";

	/** The dictionary service. */
	@Inject
	private DictionaryService dictionaryService;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;

	/** The resource service. */
	@Inject
	private ResourceService resourceService;

	/** The action registry. */
	@Inject
	private ActionRegistry actionRegistry;

	/**
	 * Construct recent activity data based on BAM data. TODO: Re-implement after data corrected.
	 * 
	 * @param activities
	 *            data receive from BAM server
	 * @return recent activity list
	 */
	public List<Activities> constructActivities(List<BAMActivity> activities){
		if(activities.isEmpty()){
			return new ArrayList<Activities>();
		}
		Instance objectInstance = null;
		Instance parent = null;
		Instance root = null;
		List<Activities> activityList = new ArrayList<Activities>();
		for(BAMActivity activity : activities){
			 Activities activityRecord = new Activities();
			 String objectType = activity.getObjecttype();
			 if(objectType.contains(INSTANCE)){
				 objectInstance = getInstanceByType(activity.getObjectsysid(), activity.getObjecttype());
				 String actionStr = getActionLabel(objectInstance, activity.getAction());

				 // work-around for skipping records with missing data
				 if((objectInstance!=null) && (actionStr !=null)){

					 root = getTopInstance(objectInstance, true);

					 parent = getTopInstance(objectInstance, false);

					 if((objectInstance instanceof ObjectInstance) == false){
						 if(parent!=null){
							 if(parent.equals(root)){
								 parent=null;
							 }else{
								 // generate parent data for the activity
								 activityRecord.createParentActivity((String) parent.getProperties()
										 .get(DefaultProperties.TITLE), parent.getClass().getSimpleName());
							 }
						 }
						 if(root!=null){
							 // generate root data for the activity
							 activityRecord.createRootActivity((String) root.getProperties()
									 .get(DefaultProperties.TITLE), root.getClass().getSimpleName());
						 }
					 }

					 activityRecord.setName((String) objectInstance.getProperties().get(DefaultProperties.TITLE));
					 activityRecord.setUrl(activity.getObjecturl());
					 activityRecord.setIconPath(objectInstance.getClass().getSimpleName());
					 activityRecord.setAction(actionStr);
					 activityRecord.setUser(getUserResource(activity.getUsername()));
					 activityRecord.setTimesince(timeSince(activity.getDatereceived()));
					 activityList.add(activityRecord);
				 }
			 }
		}
		return activityList;
	}

	/**
	 * This method will help us for retrieving parent or root instance based on current. If parent
	 * is {@link SectionInstance} or {@link WorkflowInstanceContext}, this method will invoke and
	 * search for next parent element.
	 * 
	 * @param instance
	 *            object instance
	 * @param root
	 *            based on this parameter will search for parent or root
	 * @return parent or root element
	 */
	private Instance getTopInstance(Instance instance, boolean root){
		if(instance!=null){
			if(root){
				return InstanceUtil.getRootInstance(instance, true);
			}else{
				Instance current = InstanceUtil.getDirectParent(instance, true);
				// proceed searching if element is section or workflow instance.
				if((current instanceof SectionInstance) || (current instanceof WorkflowInstanceContext)){
					return getTopInstance(current, root);
				}else{
					return current;
				}
			}
		}
		return null;
	}

	/**
	 * Extract instance with properties based on instance id and instance type.
	 * 
	 * @param instanceId
	 *            current instance id
	 * @param instanceType
	 *            current instance type
	 * @return extracted instance
	 */
	private Instance getInstanceByType(String instanceId, String instanceType){
		Instance instance = null;
		if (StringUtils.isNotNullOrEmpty(instanceId)
				&& StringUtils.isNotNullOrEmpty(instanceType)
				&& !"null".equals(instanceId)) {

			DataTypeDefinition dataTypeDefinition = dictionaryService
					.getDataTypeDefinition(instanceType);

			InstanceReference instanceReference = new LinkSourceId(instanceId,
					dataTypeDefinition);

			instance = typeConverter.convert(InitializedInstance.class,
					instanceReference).getInstance();

		}
		return instance;
	}

	/**
	 * Get user as resource based on user name.
	 * 
	 * @param username
	 *            user name
	 * @return user data
	 */
	private Resource getUserResource(String username){
		return resourceService.getResource(username, ResourceType.USER);
	}

	/**
	 * Retrieve action label based on current instance and operation.
	 * 
	 * @param instance
	 *            current instance
	 * @param operation
	 *            operation id
	 * @return action label
	 */
	private String getActionLabel(Instance instance, String operation){
		if(instance == null){
			return null;
		}
		Instance current = instance;
		// UPLOAD and all CREATE operations are specified for the parent not for
		// the current instance.
		if(operation.equals(ActionTypeConstants.UPLOAD) || operation.contains(ACTION_CREATE)){
			current = InstanceUtil.getDirectParent(current, true);
		}
		if((current == null) || (current instanceof ObjectInstance)){
			current = instance;
		}
		Action action = actionRegistry.find(new Pair<Class<?>, String>(current.getClass(), operation));
		if(action!=null){
			return action.getLabel().toLowerCase();
		}
		return null;
	}

	/**
	 * Calculate time since current date. TODO: This method need to be re-factor. Suffix text need
	 * to be generated by the label builder.
	 * 
	 * @param dateTime
	 *            event date
	 * @return event date
	 */
	private String timeSince(String dateTime){
		DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
    	Date date = null;

    	try {
			date = dateFormat.parse(dateTime);
		} catch (ParseException e) {
			log.error("AcitvityUtil.timeSince(dateTime) - Connot parse date ["+dateTime+"], pattern: "+dateFormat);
			return null;
		}

    	long seconds = (long) Math.floor((new Date().getTime()-date.getTime())/1000);
		long interval = (long) Math.floor(seconds / 31536000);

		if (interval >= 1) {
		    if (interval == 1) {
		    	return interval + " year ago";
		    }
		    return interval + " years ago";
		}

		interval = (long) Math.floor(seconds / 2592000);
		if (interval >= 1) {
		    if (interval == 1) {
		    	return interval + " month ago";
		    }
		    return interval + " months ago";
		}

		interval = (long) Math.floor(seconds / 86400);
		if (interval >= 1) {
		    if (interval == 1) {
		    	return interval + " day ago";
		    }
		    return interval + " days ago";
		}

		interval = (long) Math.floor(seconds / 3600);
		if (interval >= 1) {
		    if (interval == 1) {
		    	return interval + " hour ago";
		    }
		    return interval + " hours ago";
		}

		interval = (long) Math.floor(seconds / 60);
		if (interval >= 1) {
		    if (interval == 1) {
		    	return interval + " minute ago";
		    }
		    return interval + " minutes ago";
		}
		return "few seconds ago";
	}
}
