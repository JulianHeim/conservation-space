package com.sirma.itt.cmf.services.observers;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.cmf.beans.model.TaskInstance;
import com.sirma.itt.cmf.constants.LinkConstantsCmf;
import com.sirma.itt.cmf.event.task.workflow.BeforeTaskPersistEvent;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;

/**
 * Observer that manages link transfer between workflow tasks. The observer copies the links from
 * previous tasks to the newly created task.
 *
 * @author BBonev
 */
@Stateless
public class TaskAutoLinkObserver {

	static final String partOf = LinkConstants.PART_OF_URI.substring(LinkConstants.PART_OF_URI.indexOf(":") + 1);
	static final String hasChild = LinkConstants.HAS_CHILD_URI.substring(LinkConstants.HAS_CHILD_URI.indexOf(":") + 1);
	static final String logWork = LinkConstantsCmf.LOGGED_WORK.substring(LinkConstantsCmf.LOGGED_WORK.indexOf(":") + 1);

	/** The link service. */
	@Inject
	private LinkService linkService;

	/**
	 * Observes for task create event and
	 *
	 * @param event
	 *            the event
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void onTaskCreated(@Observes BeforeTaskPersistEvent event) {

		TaskInstance taskInstance = event.getInstance();

		TaskInstance parentTask = taskInstance.getParentTask();
		if (parentTask == null) {
			return;
		}

		List<LinkReference> references = linkService.getLinks(parentTask.toReference());
		if (references.isEmpty()) {
			return;
		}

		for (LinkReference linkReference : references) {
			String linkType = linkReference.getIdentifier();
			// we don't want to create part of and has child relations
			if (linkType.endsWith(partOf) || linkType.endsWith(hasChild)
					|| linkType.endsWith(logWork)) {
				continue;
			}
			linkService.link(taskInstance.toReference(), linkReference.getTo(),
					linkReference.getIdentifier(), linkReference.getIdentifier(),
					linkReference.getProperties());
		}
	}

}
