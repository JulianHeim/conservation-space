package com.sirma.cmf.web.object.browser;

import java.util.List;

import javax.inject.Inject;

import com.sirma.cmf.web.CaseManagement;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchFilterProperties;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.security.CurrentUser;
import com.sirma.itt.emf.security.model.User;

/**
 * Provides a list with cases accessible for current user for object browser.
 * 
 * @author svelikov
 */
@CaseManagement
public class CaseAsRootProvider implements ObjectBrowserRootProvider {

	/** The search service. */
	@Inject
	private SearchService searchService;

	/** The current user. */
	@Inject
	@CurrentUser
	private User currentUser;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Instance> getRoots() {
		Context<String, Object> context = new Context<>();
		context.put(SearchFilterProperties.USER_ID, currentUser.getIdentifier());
		SearchArguments<? extends Instance> arguments = searchService.getFilter(
				"listCaseInstancesFromUser", CaseInstance.class, context);
		searchService.search(CaseInstance.class, arguments);
		List<? extends Instance> result = arguments.getResult();
		return (List<Instance>) result;
	}

}
