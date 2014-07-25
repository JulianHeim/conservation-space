/*
 *
 */
package com.sirma.itt.emf.codelist;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.emf.codelist.event.LoadCodelists;

/**
 * Forcefully initialize codelists on server startup.
 *
 * @author BBonev
 */
@Stateless
public class CodelistBootstrap {
	@Inject
	private CodelistService codelistService;

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void bootstrapCodelists() {
		codelistService.getCodeValues(0);
	}

	/**
	 * FIXME initCodelists event {@inheritDoc}
	 */
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initCodelists(@Observes LoadCodelists events) {
		bootstrapCodelists();
	}
}
