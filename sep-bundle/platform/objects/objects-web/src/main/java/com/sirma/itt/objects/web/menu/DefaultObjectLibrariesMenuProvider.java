package com.sirma.itt.objects.web.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.sirma.cmf.web.rest.SearchQueryParameters;
import com.sirma.itt.emf.security.CurrentUser;
import com.sirma.itt.emf.security.model.User;

/**
 * Default provider for the object libraries main menu.
 * 
 * @author svelikov
 */
@Named
@ViewScoped
public class DefaultObjectLibrariesMenuProvider implements ObjectLibrariesMenuProvider,
		Serializable {

	private static final long serialVersionUID = 4870783284368249073L;

	/** The current user. */
	@Inject
	@CurrentUser
	private User currentUser;

	/** The object library menu items. */
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

		ObjectLibraryMenuItem domainObjects = new ObjectLibraryMenuItem("Domain Objects", null);
		domainObjects.addParameter(SearchQueryParameters.OBJECT_TYPE, "chd:CulturalObject");
		libraries.add(domainObjects);

		return libraries;
	}

	@Override
	public List<ObjectLibraryMenuItem> getObjectLibraryMenuItems() {
		return objectLibraryMenuItems;
	}

}
