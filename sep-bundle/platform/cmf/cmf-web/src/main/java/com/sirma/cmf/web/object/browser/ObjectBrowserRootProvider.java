package com.sirma.cmf.web.object.browser;

import java.util.List;

import com.sirma.itt.emf.instance.model.Instance;

/**
 * The Interface ObjectBrowserRootProvider should be implemented from calsses that should provide
 * list with roots for object browser.
 * 
 * @author svelikov
 */
public interface ObjectBrowserRootProvider {

	/**
	 * Gets the roots.
	 * 
	 * @return the roots
	 */
	List<Instance> getRoots();
}
