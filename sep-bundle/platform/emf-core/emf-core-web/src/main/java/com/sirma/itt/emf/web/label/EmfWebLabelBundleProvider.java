package com.sirma.itt.emf.web.label;

import com.sirma.itt.emf.label.ClasspathLabelBundleProvider;
import com.sirma.itt.emf.label.LabelBundleProvider;
import com.sirma.itt.emf.plugin.Extension;

/**
 * Provides labels for resources located in emf-web module.
 * 
 * @author Adrian Mitev
 */
@Extension(target = LabelBundleProvider.TARGET_NAME, order = 0)
public class EmfWebLabelBundleProvider extends ClasspathLabelBundleProvider {

	@Override
	protected String getBaseName() {
		return "com.sirma.itt.emf.i18n.i18n";
	}

}
