package com.sirma.itt.objects.web.menu;

import java.util.List;

/**
 * Interface for providers for the object libraries menu.
 * 
 * @author svelikov
 */
public interface ObjectLibrariesMenuProvider {

	/**
	 * Builds the object libraries menu items.
	 * 
	 * @return the list
	 */
	List<ObjectLibraryMenuItem> buildObjectLibrariesMenuItems();

	/**
	 * Getter method for objectLibraryMenuItems.
	 * 
	 * @return the objectLibraryMenuItems
	 */
	List<ObjectLibraryMenuItem> getObjectLibraryMenuItems();
}
