package com.sirma.cmf.web.resources;

import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.web.plugin.PageFragment;
import com.sirma.itt.emf.web.resources.StylesheetResourceExtensionPoint;

/**
 * Contributes extesnions to StylesheetResourceExtensionPoint.
 * 
 * @author svelikov
 */
public class CmfCSSResource implements StylesheetResourceExtensionPoint {

	/**
	 * CmfMainStylesheet extesnion.
	 */
	@Extension(target = EXTENSION_POINT, enabled = true, order = 10, priority = 1)
	public static class CmfStylesheetsPlugin implements PageFragment {

		@Override
		public String getPath() {
			return "/common/cmf-stylesheets.xhtml";
		}
	}
}