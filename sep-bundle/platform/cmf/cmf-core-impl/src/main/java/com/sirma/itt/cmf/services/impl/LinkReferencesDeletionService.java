package com.sirma.itt.cmf.services.impl;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.emf.annotation.Proxy;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.link.LinkInstance;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.scheduler.SchedulerActionAdapter;
import com.sirma.itt.emf.scheduler.SchedulerContext;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.util.CollectionUtils;

/**
 * The LinkReferencesDeletionService is schedule wrapper, that deletes a link instances provided as
 * argument in the context.
 */
@Named("LinkReferencesDeletionService")
public class LinkReferencesDeletionService extends SchedulerActionAdapter {
	public static final String BEAN_ID = "LinkReferencesDeletionService";

	/** Key which holds Collection of LinkInstance. */
	public static final String DELEATABLE = "deleatable";

	/** The OPERATION as string value */
	public static final String OPERATION = "operation";

	/** Is delete a permanent? */
	public static final String PERMANENT = "permanent";

	/** Provide some custom message. */
	public static final String CUSTOM_ERROR = "custom_error";

	/** The instance service. */
	@Proxy
	@Inject
	private InstanceService<com.sirma.itt.emf.instance.model.Instance, DefinitionModel> instanceService;

	/** The logger. */
	@Inject
	private Logger logger;
	@Inject
	private LinkService linkService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(SchedulerContext context) throws Exception {
		List<LinkReference> failed = new LinkedList<LinkReference>();
		@SuppressWarnings("unchecked")
		List<LinkReference> links = (List<LinkReference>) context.get(DELEATABLE);
		logger.debug("Executing scheduled deletion of " + links.size() + " links!");
		Map<Serializable, LinkReference> mapping = generateMappingForReferences(links);
		List<LinkInstance> convertToLinkInstance = linkService.convertToLinkInstance(links, false);
		Operation operation = new Operation(context.getIfSameType(OPERATION, String.class));
		Boolean permanent = context.getIfSameType(PERMANENT, Boolean.class, Boolean.FALSE);
		for (LinkInstance linkInstance : convertToLinkInstance) {
			com.sirma.itt.emf.instance.model.Instance child = null;
			try {
				child = linkInstance.getTo();
				if (child != null) {
					if (ActionTypeConstants.DELETE.equals(operation.getOperation())) {
						instanceService.delete(child, operation, permanent);
					} else if (ActionTypeConstants.STOP.equals(operation.getOperation())) {
						instanceService.cancel(child);
					}
				} else {
					logger.error("Instance is null for association: " + linkInstance
							+ "! Deletion would be skipped for this item!");
				}
			} catch (Exception e) {
				e.printStackTrace();
				linkService.getLinkReference(linkInstance.getId());
				failed.add(mapping.get(linkInstance.getId()));
			}
		}
		if (failed.size() > 0) {
			// update context
			context.put(DELEATABLE, (Serializable) failed);
			if (context.containsKey(CUSTOM_ERROR)) {
				throw new EmfRuntimeException(context.get(CUSTOM_ERROR).toString());
			}
			throw new EmfRuntimeException(
					"There are problems during deletion of items! Action is rescheduled!");
		}

	}

	/**
	 * Generate mapping internal between the reference and its id for faster retrieval.
	 *
	 * @param links
	 *            are the list of {@link LinkReference}
	 * @return the mapping by id
	 */
	private Map<Serializable, LinkReference> generateMappingForReferences(List<LinkReference> links) {
		Map<Serializable, LinkReference> mapping = CollectionUtils
				.createLinkedHashMap(links.size());
		for (LinkReference linkReference : links) {
			mapping.put(linkReference.getId(), linkReference);
		}
		return mapping;
	}
}
