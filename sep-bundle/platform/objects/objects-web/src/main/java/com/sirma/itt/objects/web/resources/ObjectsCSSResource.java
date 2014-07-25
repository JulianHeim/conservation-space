package com.sirma.itt.objects.web.resources;

import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.web.plugin.PageFragment;
import com.sirma.itt.emf.web.resources.StylesheetResourceExtensionPoint;

/**
 * Objects module stylesheet files.
 * 
 * @author svelikov
 */
public class ObjectsCSSResource implements StylesheetResourceExtensionPoint {

	/**
	 * ObjectsStylesheetsPlugin.
	 */
	@Extension(target = EXTENSION_POINT, enabled = true, order = 60, priority = 1)
	public static class ObjectsStylesheetsPlugin implements PageFragment {

		@Override
		public String getPath() {
			return "/common/objects-stylesheets.xhtml";
		}
	}
}