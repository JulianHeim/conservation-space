package com.sirma.itt.cmf.services.observers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.event.TwoPhaseEvent;
import com.sirma.itt.emf.event.instance.AfterInstancePersistEvent;
import com.sirma.itt.emf.event.instance.InstanceAttachedEvent;
import com.sirma.itt.emf.event.instance.InstanceDetachedEvent;
import com.sirma.itt.emf.instance.InstanceUtil;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkService;

/**
 * Observer that populates links for the parent-child relations between section parent and section
 * children. This is complimentary class of the
 * {@link com.sirma.itt.emf.instance.observer.AutolinkObserver}.
 * 
 * @author BBonev
 */
@ApplicationScoped
public class CmfAutolinkObserver {

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
		if (parent instanceof SectionInstance) {
			// attach context if any. We had a requirement to link the child to the first context in the
			// tree explicitly
			attachToContext(instance, parent);
		}
	}

	/**
	 * Creates parent to child link that is marked as created by system
	 * 
	 * @param from
	 *            the parent
	 * @param to
	 *            the instance
	 */
	private void createParentToChildLink(Instance from, Instance to) {
		// first check if we already has the relation not to create it again
		if (!linkService.isLinked(from.toReference(), to.toReference(),
				LinkConstants.PARENT_TO_CHILD)) {
			linkService.link(from, to, LinkConstants.PARENT_TO_CHILD,
					LinkConstants.CHILD_TO_PARENT, LinkConstants.DEFAULT_SYSTEM_PROPERTIES);
		}
	}

	/**
	 * Attach the given new child to the first parent context different from the given direct parent
	 * if any
	 * 
	 * @param childToAttach
	 *            the child to attach
	 * @param directParent
	 *            the direct parent
	 */
	private void attachToContext(Instance childToAttach, Instance directParent) {
		Instance context = InstanceUtil.getParentContext(childToAttach, true);
		if ((context != null) && (context != directParent)) {
			createParentToChildLink(context, childToAttach);
		}
	}

	/**
	 * When instance is attached to other then both should be linked together.
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstanceAttachedEvent(@Observes InstanceAttachedEvent<Instance> event) {
		if (event.getInstance() instanceof SectionInstance) {
			if (!linkService.isLinked(event.getInstance().toReference(), event.getChild()
					.toReference(), LinkConstants.PARENT_TO_CHILD)) {
				// attach to the context instance
				attachToContext(event.getChild(), event.getInstance());
			}
		}
	}

	/**
	 * When instance is detached from another instance. The observer handles he special case for
	 * section content
	 * 
	 * @param event
	 *            the event
	 */
	public void onInstanceDetachedEvent(@Observes InstanceDetachedEvent<Instance> event) {
		if (event.getInstance() instanceof SectionInstance) {
			Instance instance = InstanceUtil.getParentContext(event.getInstance());
			linkService.unlink(instance.toReference(), event.getChild().toReference(),
					LinkConstants.PARENT_TO_CHILD, LinkConstants.CHILD_TO_PARENT);
		}
	}

}
