package com.sirma.itt.cmf.services.adapters;

import javax.enterprise.context.ApplicationScoped;

import com.sirma.itt.cmf.services.adapter.CMFContentAdapterService;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.instance.model.DMSInstance;

/**
 * The Class CMFContentAdapterServiceMock.
 */
@ApplicationScoped
public class CMFContentAdapterServiceMock implements CMFContentAdapterService {

	@Override
	public DMSFileDescriptor getContentDescriptor(DMSInstance instance) throws DMSException {
		return null;
	}

	@Override
	public DMSFileDescriptor getContentDescriptor(String dmsId) throws DMSException {
		return null;
	}

	@Override
	public DMSFileDescriptor getContentDescriptor(String dmsId, String containerId)
			throws DMSException {
		return null;
	}

}
