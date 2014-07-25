package com.sirma.cmf.mock.service;

import java.io.File;
import java.io.OutputStream;

import javax.enterprise.inject.Alternative;

import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.io.ContentService;

/**
 * The Class ContentServiceMock.
 *
 * @author svelikov
 */
@Alternative
public class ContentServiceMock implements ContentService {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File getContent(DMSFileDescriptor location) {
		return new File("");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File getContent(DMSFileDescriptor location, String fileName) {
		return null;
	}

	@Override
	public long getContent(DMSFileDescriptor location, OutputStream output) {
		// TODO Auto-generated method stub
		return -1L;
	}

}
