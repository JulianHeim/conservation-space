package com.sirma.cmf.web.form.picklist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sirma.cmf.web.Action;
import com.sirma.cmf.web.form.picklist.event.ItemsFilterBinding;
import com.sirma.cmf.web.form.picklist.event.LoadItemsEvent;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.resources.ResourceProperties;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.JsonUtil;

/**
 * PicklistController is responsible for operations in picklist component.
 *
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class PicklistController extends Action implements Serializable {

	private static final long serialVersionUID = 6246028109995287580L;

	private static final String ASSIGNEE_LIST_ITEMS_FILTER = "assigneeListItemsFilter";

	@Inject
	private ResourceService resourceService;

	@Inject
	private Event<LoadItemsEvent> loadItemsEvent;

	/**
	 * Load items.
	 *
	 * @param fromRequest
	 *            the from request
	 * @param itemType
	 *            the item type
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public List<?> loadItems(boolean fromRequest, String itemType) {
		String filterName = null;
		Map<String, List<String>> keywords = null;
		if (fromRequest) {
			Map<String, Object> requestMap = getRequestMap();
			filterName = (String) requestMap.get(PicklistConstants.FILTERNAME_ATTR);
			keywords = (Map<String, List<String>>) requestMap.get(PicklistConstants.KEYWORDS_ATTR);
		} else {
			UIComponent currentComponent = getCurrentComponent();
			Map<String, Object> attributes = currentComponent.getAttributes();
			filterName = (String) attributes.get(PicklistConstants.FILTERNAME_ATTR);
			keywords = (Map<String, List<String>>) attributes.get(PicklistConstants.KEYWORDS_ATTR);
		}
		return loadItems(fromRequest, itemType, filterName, keywords);
	}

	/**
	 * Load the items using the given arguments
	 *
	 * @param fromRequest
	 *            the from request
	 * @param itemType
	 *            the item type
	 * @param filterName
	 *            is the filter to fire on load - might be null
	 * @param keywords
	 *            are the keywords to use - might be null
	 * @return the list of items
	 */
	public List<?> loadItems(boolean fromRequest, String itemType, String filterName,
			Map<String, List<String>> keywords) {
		log.debug("CMFWeb: Executing PicklistController.loadItems");
		String filterNameInternal = filterName;
		// this is default filter name and if any other is provided, this should be overriden
		if (StringUtils.isNullOrEmpty(filterNameInternal)) {
			filterNameInternal = ASSIGNEE_LIST_ITEMS_FILTER;
		}

		TimeTracker timeTracker = TimeTracker.createAndStart();
		List<?> list = loadItemsList(filterNameInternal, keywords, ResourceType.getByType(itemType));
		double midTime = timeTracker.stopInSeconds();
		timeTracker.begin();

		outjectItems(list);
		log.debug("CMFWeb: Loading resources of type " + itemType + " took " + midTime
				+ " s and outject of " + list.size() + " took " + timeTracker.stopInSeconds()
				+ " s");

		return list;
	}

	/**
	 * Gets the current component.
	 *
	 * @return the current component
	 */
	private UIComponent getCurrentComponent() {
		return UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
	}

	/**
	 * Gets the request map.
	 *
	 * @return the request map
	 */
	private Map<String, Object> getRequestMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
	}

	/**
	 * Retrieve items by resource type optionally filtered by provided filter.
	 *
	 * @param filterName
	 *            the filter name
	 * @param keywords
	 *            the keywords
	 * @param resourceType
	 *            the resource type
	 * @return the items list
	 */
	@SuppressWarnings("unchecked")
	private List<?> loadItemsList(String filterName, Map<String, List<String>> keywords,
			ResourceType resourceType) {
		LoadItemsEvent event = fireEvent(filterName, keywords);

		// Users may be set but not populated if the filtering had filtered all items.
		// So we need to know that the event is actually handled in order to know that there is
		// no need to load them all.
		boolean loadAll = true;
		if ((event.getItems() != null) && event.isHandled()) {
			loadAll = false;
		}
		List<Resource> resources = new LinkedList<>();
		if (loadAll) {
			log.debug("CMFWeb: LoadItemsEvent was not handled or no filters were provided! All items of type ["
					+ resourceType + "] will be loaded instead.");
			if (resourceType == ResourceType.ALL) {
				resources.addAll(resourceService.getAllResources(ResourceType.USER, null));
				resources.addAll(resourceService.getAllResources(ResourceType.GROUP, null));
			} else {
				resources = resourceService.getAllResources(resourceType, null);
			}
		} else {
			resources = (List<Resource>) event.getItems();
			log.debug("CMFWeb: LoadItemsEvent is handled, [" + resources.size()
					+ "] items was found");
		}
		return resources;
	}

	/**
	 * Fire event for populating and filtering of the items.
	 *
	 * @param filterName
	 *            the filter name
	 * @param keywords
	 *            the keywords
	 * @return the load items event
	 */
	protected LoadItemsEvent fireEvent(String filterName, Map<String, List<String>> keywords) {

		LoadItemsEvent event = new LoadItemsEvent();

		if (StringUtils.isNotNullOrEmpty(filterName)) {

			if (keywords != null) {
				event.setKeywords(keywords);
			}

			ItemsFilterBinding itemsFilterBinding = new ItemsFilterBinding(filterName);
			loadItemsEvent.select(itemsFilterBinding).fire(event);

			log.debug("CMFWeb: Fired LoadItemsEvent with filterName [" + filterName
					+ "] with keywords: " + keywords);
		}

		return event;
	}

	/**
	 * Gets the list of items as display names. Ids are split by '|'.
	 *
	 * @param value
	 *            the list/value for user/s
	 * @param itemType
	 *            is the current item type
	 * @return the list of items display names as string
	 */
	@SuppressWarnings("unchecked")
	public String getAsString(Object value, String itemType) {
		try {
			Collection<String> iterable = null;
			if (value instanceof String) {
				if (StringUtils.isNullOrEmpty((String) value)) {
					return "";
				}
				String[] users = ((String) value).split(ItemsConverter.DELIMITER);
				iterable = new ArrayList<>(users.length);
				for (String user : users) {
					iterable.add(user);
				}
			} else if (value instanceof Collection) {
				iterable = (Collection<String>) value;
			}
			StringBuilder resulted = new StringBuilder();
			if (iterable != null) {
				Iterator<String> iterator = iterable.iterator();
				while (iterator.hasNext()) {
					String object = iterator.next();
					resulted.append(getDisplayValue(object, itemType));
					if (iterator.hasNext()) {
						resulted.append(",");
					}
				}
			}
			return resulted.toString();
		} catch (final Exception e) {
			final String errorMessage = "Item conversion error!";
			log.error(errorMessage, e);
			throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					errorMessage, errorMessage));
		}
	}

	/**
	 * Picklist retrieving of value for field.
	 *
	 * @param value
	 *            is the value
	 * @param itemType
	 *            is the item type
	 * @param multivalued
	 *            whether is multivalue
	 * @return the value for current picker
	 */
	public Object getAsValue(Object value, String itemType, boolean multivalued) {

		return value;

	}

	/**
	 * Gets the display value for item.
	 *
	 * @param value
	 *            the value
	 * @param itemType
	 *            current item type
	 * @return the display value for item
	 */
	private String getDisplayValue(String value, String itemType) {
		String passedValue = value;
		String displayName = "";
		ResourceType resourceType = ResourceType.getByType(itemType);
		Resource resource = null;
		if (resourceType == ResourceType.ALL) {
			resource = resourceService.getResource(passedValue, ResourceType.UNKNOWN);
		} else {
			resource = resourceService.getResource(passedValue, resourceType);
		}
		if (resource != null) {
			displayName = resource.getDisplayName();
		}
		return displayName;
	}

	/**
	 * Outject items list to request scope to be accessible for the clientside api.
	 *
	 * @param items
	 *            the users
	 */
	private void outjectItems(List<?> items) {

		JSONArray itemsOutjected = new JSONArray();

		int index = 0;
		for (Object object : items) {
			index++;
			JSONObject item = new JSONObject();
			if (object instanceof Resource) {
				Resource resource = (Resource) object;
				JsonUtil.addToJson(item, "id", index);
				JsonUtil.addToJson(item, "itemLabel", resource.getDisplayName());
				JsonUtil.addToJson(item, "itemValue", resource.getIdentifier());
				JsonUtil.addToJson(item, "type", resource.getType().getName());

				if (resource instanceof User) {
					User emfUser = (User) resource;
					JsonUtil.addToJson(item, "iconPath",
							emfUser.getUserInfo().get(ResourceProperties.AVATAR));
				}
			}
			itemsOutjected.put(item);
		}

		getRequestMap().put("picklistItems", itemsOutjected.toString());
	}
}
