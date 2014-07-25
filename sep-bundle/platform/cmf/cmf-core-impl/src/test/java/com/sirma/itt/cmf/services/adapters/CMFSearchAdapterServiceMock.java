package com.sirma.itt.cmf.services.adapters;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.sirma.itt.cmf.services.adapter.CMFSearchAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchArgumentsMap;

/**
 * The Class CMFSearchAdapterServiceMock.
 */
@ApplicationScoped
public class CMFSearchAdapterServiceMock implements CMFSearchAdapterService {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public <E extends DMSFileDescriptor> SearchArguments<E> search(
			SearchArguments<E> args, Class<? extends Instance> model)
			throws DMSException {
		RESTClientMock.checkAuthenticationInfo();
		return null;
	}

	@Override
	public <E extends DMSFileDescriptor, D extends DMSFileDescriptor> SearchArgumentsMap<E, List<D>> searchCaseAndDocuments(
			SearchArgumentsMap<E, List<D>> args) throws DMSException {
		RESTClientMock.checkAuthenticationInfo();
		return null;
	}

}
