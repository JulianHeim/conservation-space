package com.sirma.itt.objects.web.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.sirma.cmf.web.rest.SearchQueryParameters;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.search.NamedQueries;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.CurrentUser;
import com.sirma.itt.emf.security.model.User;

/**
 * Provider for the object libraries main menu retrieving allowed libraries from the semantic db.
 * 
 * @author svelikov
 */
public class SemanticObjectLibrariesMenuProvider implements ObjectLibrariesMenuProvider {

	@Inject
	private SearchService searchService;

	@Inject
	@CurrentUser
	private User currentUser;

	@Inject
	@Config(name = EmfConfigurationProperties.SYSTEM_LANGUAGE, defaultValue = "bg")
	private String defaultLanguage;

	private List<ObjectLibraryMenuItem> objectLibraryMenuItems;

	/**
	 * Inits the provider.
	 */
	@PostConstruct
	public void init() {
		objectLibraryMenuItems = buildObjectLibrariesMenuItems();
	}

	@Override
	public List<ObjectLibraryMenuItem> buildObjectLibrariesMenuItems() {
		List<ObjectLibraryMenuItem> libraries = new ArrayList<>();

		SearchArguments<Instance> filter = searchService.getFilter(
				NamedQueries.QUERY_CLASSES_PART_OF_OBJECT_LIBRARY, Instance.class, null);
		// TODO: defaultLanguage should be used here when we have i18n in semantic db
		filter.getArguments().put("lang", "en");
		searchService.search(Instance.class, filter);
		List<Instance> result = filter.getResult();

		for (Instance instance : result) {
			Map<String, Serializable> properties = instance.getProperties();
			ObjectLibraryMenuItem menuItem = new ObjectLibraryMenuItem(
					(String) properties.get(DefaultProperties.TITLE), (String) instance.getId());
			menuItem.addParameter(SearchQueryParameters.OBJECT_TYPE, (String) instance.getId());
			libraries.add(menuItem);
		}

		return libraries;
	}

	@Override
	public List<ObjectLibraryMenuItem> getObjectLibraryMenuItems() {
		return objectLibraryMenuItems;
	}

}
