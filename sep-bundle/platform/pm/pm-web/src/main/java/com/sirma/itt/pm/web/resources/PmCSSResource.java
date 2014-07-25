package com.sirma.itt.pm.web.resources;

import javax.inject.Named;

import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.web.plugin.PageFragment;
import com.sirma.itt.emf.web.resources.StylesheetResourceExtensionPoint;

/**
 * PM module stylesheet files.
 * 
 * @author svelikov
 */
@Named
public class PmCSSResource implements StylesheetResourceExtensionPoint {

	/**
	 * The Class PMStylesheetsPlugin.
	 */
	@Extension(target = EXTENSION_POINT, enabled = true, order = 20, priority = 1)
	public static class PMStylesheetsPlugin implements PageFragment {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPath() {
			return "/common/pm-stylesheets.xhtml";
		}
	}

}