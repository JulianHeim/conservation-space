package com.sirma.itt.cmf.services.observers;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.cmf.event.LoadTemplates;
import com.sirma.itt.cmf.services.DocumentTemplateService;
import com.sirma.itt.emf.security.Secure;

/**
 * Event observer for asynchronous template loading/reloading.
 * 
 * @author BBonev
 */
@Stateless
@Secure(runAsSystem = true)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TemplateLoaderObserver {

	/** The template service. */
	@Inject
	private DocumentTemplateService templateService;

	/**
	 * Listens for {@link LoadTemplates} event to initiate a template loading.
	 * 
	 * @param event
	 *            the event
	 */
	@Asynchronous
	public void onApplicationLoaded(@Observes LoadTemplates event) {
		templateService.reload();
	}

}
