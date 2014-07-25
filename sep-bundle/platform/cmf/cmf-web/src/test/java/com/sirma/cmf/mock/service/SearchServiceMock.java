package com.sirma.cmf.mock.service;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Alternative;

import com.sirma.itt.emf.domain.Context;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;

/**
 * The Class SearchServiceMock.
 */
@Alternative
public class SearchServiceMock implements SearchService {

	@Override
	public <E extends Instance, S extends SearchArguments<E>> S getFilter(String filterName,
			Class<E> resultType, Context<String, Object> context) {

		return null;
	}

	@Override
	public <E extends Instance, S extends SearchArguments<E>> Map<String, S> getFilters(
			String placeHolder, Class<E> resultType, Context<String, Object> context) {

		return null;
	}

	@Override
	public <E extends Instance> List<String> getFilters(String placeHolder, Class<E> resultType) {

		return null;
	}

	@Override
	public <E extends Instance, S extends SearchArguments<E>> void search(Class<?> target,
			S arguments) {


	}

}
