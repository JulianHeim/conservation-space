package com.sirma.itt.emf.instance.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.event.TwoPhaseEvent;
import com.sirma.itt.emf.event.instance.AfterInstanceDeleteEvent;
import com.sirma.itt.emf.event.instance.AfterInstancePersistEvent;
import com.sirma.itt.emf.event.instance.InstanceAttachedEvent;
import com.sirma.itt.emf.event.instance.InstanceDetachedEvent;
import com.sirma.itt.emf.instance.InstanceUtil;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkService;

/**
 * Observer that populates links for the parent-child relations.
 * 
 * @author BBonev
 */
@ApplicationScoped
public class AutolinkObserver {

	/** The link service. */
	@Inject
	private LinkService linkService;

	/**
	 * Listens for after instance persist events to link the parent to child object links
	 * 
	 * @param event
	 *            the event
	 */
	public void onAfterInstanceCreated(
			@Observes AfterInstancePersistEvent<Instance, TwoPhaseEvent> event) {
		if (RuntimeConfiguration
				.isConfigurationSet(RuntimeConfigurationProperties.DISABLE_AUTOMATIC_LINKS)) {
			return;
		}
		Instance instance = event.getInstance();

		Instance parent = InstanceUtil.getDirectParent(instance, true);
		if (parent != null) {
			createParentToChildLink(parent, instance);
		}
	}

	/**
	 * Creates parent to child link that is marked as created by system
	 * 
	 * @param parent
	 *            the parent
	 * @param child
	 *            the instance
	 */
	private void createParentToChildLink(Instance parent, Instance child) {
		// first check if we already has the relation not to create it again
		if (!linkService.isLinked(parent.toReference(), child.toReference(),
				LinkConstants.PARENT_TO_CHILD)) {
			linkService.link(parent, child, LinkConstants.PARENT_TO_CHILD,
					LinkConstants.CHILD_TO_PARENT, LinkConstants.DEFAULT_SYSTEM_PROPERTIES);
		}
	}

	/**
	 * When instance is attached to other then both should be linked together.
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstanceAttachedEvent(@Observes InstanceAttachedEvent<Instance> event) {
		if (!linkService.isLinked(event.getInstance().toReference(),
				event.getChild().toReference(), LinkConstants.PARENT_TO_CHILD)) {

			createParentToChildLink(event.getInstance(), event.getChild());
		}
		if (!linkService.isLinkedSimple(event.getChild().toReference(), event.getInstance()
				.toReference(), LinkConstants.PART_OF_URI)) {
			linkService.linkSimple(event.getChild().toReference(), event.getInstance()
					.toReference(), LinkConstants.PART_OF_URI);
		}
	}

	/**
	 * When instance is detached from other then both should not be linked together.
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstanceDetachedEvent(@Observes InstanceDetachedEvent<Instance> event) {
		linkService.unlink(event.getInstance().toReference(), event.getChild().toReference(),
				LinkConstants.PARENT_TO_CHILD, LinkConstants.CHILD_TO_PARENT);

		linkService.unlinkSimple(event.getChild().toReference(), event.getInstance().toReference(),
				LinkConstants.PART_OF_URI);
	}

	/**
	 * Method that removes all links for instance upon instance deletion.
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstanceDeleted(@Observes AfterInstanceDeleteEvent<Instance, ?> event) {
		linkService.removeLinksFor(event.getInstance().toReference());
	}

}
