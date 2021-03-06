package com.sirma.itt.menu.main;

import javax.inject.Named;

import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.web.menu.main.MainMenu;
import com.sirma.itt.emf.web.plugin.PageFragment;
import com.sirma.itt.emf.web.plugin.Plugable;

/**
 * CSMainMenuExtensions. Override some of the plugins in order to disbale them.
 * 
 * @author svelikov
 */
public class CSMainMenuExtensions extends MainMenu {

	/**
	 * ObjectsLibrary extension for main menu.
	 */
	@Extension(target = EXTENSION_POINT, enabled = false, order = 700, priority = 2)
	public static class ObjectsLibraryMenuExtensionDisabled implements PageFragment {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPath() {
			return "/menu/main/objects-library.xhtml";
		}
	}

	/**
	 * The Class AdminMenuExtension.
	 */
	@Named
	@Extension(target = EXTENSION_POINT, enabled = false, order = 1000, priority = 11)
	public static class AdminMenuExtensionDisabled implements PageFragment, Plugable {

		/** The Constant EXTENSION_POINT. */
		public static final String EXTENSION_POINT = "adminMenu";

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPath() {
			return "/menu/main/admin.xhtml";
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getExtensionPoint() {
			return EXTENSION_POINT;
		}
	}
}
