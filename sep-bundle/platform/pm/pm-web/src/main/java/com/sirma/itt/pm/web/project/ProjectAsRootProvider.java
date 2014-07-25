package com.sirma.itt.pm.web.project;

import java.util.List;

import javax.inject.Inject;

import com.sirma.cmf.web.ProjectManagement;
import com.sirma.cmf.web.object.browser.ObjectBrowserRootProvider;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.CurrentUser;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * Provides a list with projects accessible for current user for object browser.
 * 
 * @author svelikov
 */
@ProjectManagement
public class ProjectAsRootProvider implements ObjectBrowserRootProvider {

	@Inject
	private SearchService searchService;

	@Inject
	@CurrentUser
	private User currentUser;

	@Override
	public List<Instance> getRoots() {
		Context<String, Object> context = new Context<>();
		context.put(SearchFilterProperties.USER_ID, currentUser.getIdentifier());
		SearchArguments<? extends Instance> arguments = searchService.getFilter("visibleProject",
				ProjectInstance.class, context);
		// all projects available for current user
		arguments.setPageSize(Integer.MAX_VALUE);
		searchService.search(ProjectInstance.class, arguments);
		return (List<Instance>) arguments.getResult();
	}

}
