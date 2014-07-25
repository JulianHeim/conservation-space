package com.sirma.cmf.web.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.Action;
import com.sirma.cmf.web.form.picklist.event.ItemsFilter;
import com.sirma.cmf.web.form.picklist.event.ItemsFilterBinding;
import com.sirma.cmf.web.form.picklist.event.LoadItemsEvent;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.User;

/**
 * The Class WorkflowSelectItemAction.
 *
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class WorkflowSelectItemAction extends Action implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4260065824606186697L;

	/** The people service. */
	@Inject
	private ResourceService resourceService;

	/** The cmf users. */
	private List<User> emfUsers;

	/** The load items event. */
	@Inject
	private Event<LoadItemsEvent> loadItemsEvent;

	/**
	 * Load items. Constructs and fires an event for loading of items.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public List<User> loadItems() {

		log.debug("CMFWeb: Executing WorkflowSelectItemAction.loadItems");

		UIComponent currentComponent = UIComponent.getCurrentComponent(FacesContext
				.getCurrentInstance());
		Map<String, Object> attributes = currentComponent.getAttributes();
		String filterName = (String) attributes.get("filtername");
		Map<String, List<String>> keywords = (Map<String, List<String>>) attributes.get("keywords");

		LoadItemsEvent event = new LoadItemsEvent();
		boolean loadAll = true;
		if (StringUtils.isNotNullOrEmpty(filterName)) {

			if (keywords != null) {
				event.setKeywords(keywords);
			}

			ItemsFilterBinding itemsFilterBinding = new ItemsFilterBinding(filterName);
			loadItemsEvent.select(itemsFilterBinding).fire(event);

			log.debug("CMFWeb: Fired LoadItemsEvent with filterName [" + filterName
					+ "] with keywords: " + keywords);

			// Users may be set but not populated if the filtering had filtered all items.
			// So we need to know that the event is actually handled in order to know that there is
			// no need to load all items.
			if ((event.getItems() != null) && event.isHandled()) {
				loadAll = false;
			}

		}

		if (loadAll) {
			log.debug("CMFWeb: LoadItemsEvent was not handled or no filters were provided! All items will be loaded instead.");
			List<User> list = resourceService.getAllResources(ResourceType.USER, null);
			emfUsers = list;
		} else {
			emfUsers = (List<User>) event.getItems();
			log.debug("CMFWeb: LoadItemsEvent is handled, [" + emfUsers.size()
					+ "] items was found");
		}

		return emfUsers;
	}

	/**
	 * Load users observer.
	 *
	 * @param event
	 *            the event
	 */
	// TODO: used from picklist
	public void loadUsers(@Observes @ItemsFilter("users") LoadItemsEvent event) {

		log.debug("CMFWeb: Executing Observer WorkflowSelectItemAction.loadUsers");

		List<PicklistItem> items = new ArrayList<PicklistItem>();

		List<Resource> allCMFUsers = resourceService.getAllResources(ResourceType.USER, null);

		for (Resource emfUser : allCMFUsers) {
			String name = emfUser.getIdentifier();
			items.add(new PicklistItem(name));
		}

		FacesContext.getCurrentInstance().getExternalContext().getRequestMap()
				.put("picklistItems", items);
	}

	/**
	 * Getter method for cmfUsers.
	 *
	 * @return the cmfUsers
	 */
	public List<User> getCmfUsers() {
		return emfUsers;
	}

	/**
	 * Setter method for cmfUsers.
	 *
	 * @param emfUsers
	 *            the cmfUsers to set
	 */
	public void setCmfUsers(List<User> emfUsers) {
		this.emfUsers = emfUsers;
	}
}
