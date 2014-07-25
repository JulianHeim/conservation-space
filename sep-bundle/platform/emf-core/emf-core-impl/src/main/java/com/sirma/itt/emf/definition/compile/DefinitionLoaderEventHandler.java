package com.sirma.itt.emf.definition.compile;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.definition.event.AllDefinitionsLoaded;
import com.sirma.itt.emf.definition.event.LoadAllDefinitions;
import com.sirma.itt.emf.definition.event.LoadTemplateDefinitions;
import com.sirma.itt.emf.definition.event.LoadTopLevelDefinitions;
import com.sirma.itt.emf.definition.event.TemplateDefinitionsLoaded;
import com.sirma.itt.emf.definition.event.TopLevelDefinitionsLoaded;
import com.sirma.itt.emf.definition.load.DefinitionLoader;
import com.sirma.itt.emf.domain.MessageType;
import com.sirma.itt.emf.domain.VerificationMessage;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.emf.util.ValidationLoggingUtil;

/**
 * Asynchronous event handler for definition loading.
 *
 * @author BBonev
 */
@Stateless
@Secure(runAsSystem = true)
public class DefinitionLoaderEventHandler {
	/** The logger. */
	@Inject
	private Logger logger;

	/** The definition loader. */
	@Inject
	private DefinitionLoader definitionLoader;

	/** The event service. */
	@Inject
	private EventService eventService;

	/** The disable compiler. */
	@Inject
	@Config(name = EmfConfigurationProperties.DISABLE_DEFINITION_COMPILER, defaultValue = "false")
	private Instance<Boolean> disableCompiler;

	/**
	 * Initialize.
	 */
	@PostConstruct
	public void initialize() {
		if (disableCompiler.get()) {
			logger.warn("WARNING: Definition compiler has been disabled. Until enabled no definitions will be updated!");
		}
	}

	/**
	 * Load definitions asynchronously based on the given event.
	 *
	 * @param event
	 *            the event
	 */
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void loadAllDefinitions(@Observes LoadAllDefinitions event) {
		if (disableCompiler.get()) {
			// if the definition compilation is disabled but was called with forced event then we
			// should perform it.
			if (!event.isForced()) {
				// compilation is disabled
				logger.info("Called definition compilation on disabled compiler. Nothing is updated.");
				return;
			}
		}
		TimeTracker tracker = new TimeTracker().begin();
		logger.info("Received event: " + event);
		List<VerificationMessage> errors = new LinkedList<VerificationMessage>();
		List<VerificationMessage> messages = definitionLoader.loadTemplateDefinitions();
		ValidationLoggingUtil.copyMessages(messages, errors, MessageType.ERROR, MessageType.WARNING);
		messages = definitionLoader.loadDefinitions();
		ValidationLoggingUtil.copyMessages(messages, errors, MessageType.ERROR, MessageType.WARNING);
		if (!errors.isEmpty()) {
			String string = ValidationLoggingUtil.printMessages(errors);
			logger.error("\n=======================================================================\nFound problems executing event "
					+ event
					+ string
					+ "\n=======================================================================\n");
		}
		logger.info("Completed definition loading in " + tracker.stopInSeconds() + " s for event: "
				+ event);
		eventService.fire(new AllDefinitionsLoaded());
	}

	/**
	 * Load definitions asynchronously based on the given event.
	 *
	 * @param event
	 *            the event
	 */
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void loadDefinitions(@Observes LoadTopLevelDefinitions event) {
		if (disableCompiler.get()) {
			// if the definition compilation is disabled but was called with forced event then we
			// should perform it.
			if (!event.isForced()) {
				// compilation is disabled
				logger.info("Called definition compilation on disabled compiler. Nothing is updated.");
				return;
			}
		}

		TimeTracker tracker = new TimeTracker().begin();
		logger.info("Received event: " + event);
		List<VerificationMessage> errors = new LinkedList<VerificationMessage>();
		List<VerificationMessage> messages = definitionLoader.loadDefinitions();
		ValidationLoggingUtil
				.copyMessages(messages, errors, MessageType.ERROR, MessageType.WARNING);
		if (!errors.isEmpty()) {
			String string = ValidationLoggingUtil.printMessages(errors);
			logger.error("\n=======================================================================\nFound problems executing event "
					+ event
					+ string
					+ "\n=======================================================================\n");
		}
		logger.info("Completed definition loading in " + tracker.stopInSeconds() + " s for event: "
				+ event);
		eventService.fire(new TopLevelDefinitionsLoaded());
	}

	/**
	 * Load definitions asynchronously based on the given event.
	 *
	 * @param event
	 *            the event
	 */
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void loadTemplateDefinitions(@Observes LoadTemplateDefinitions event) {
		if (disableCompiler.get()) {
			// if the definition compilation is disabled but was called with forced event then we
			// should perform it.
			if (!event.isForced()) {
				// compilation is disabled
				logger.info("Called definition compilation on disabled compiler. Nothing is updated.");
				return;
			}
		}
		TimeTracker tracker = new TimeTracker().begin();
		logger.info("Received event: " + event);
		List<VerificationMessage> errors = new LinkedList<VerificationMessage>();
		List<VerificationMessage> messages = definitionLoader.loadTemplateDefinitions();
		ValidationLoggingUtil
				.copyMessages(messages, errors, MessageType.ERROR, MessageType.WARNING);
		if (!errors.isEmpty()) {
			String string = ValidationLoggingUtil.printMessages(errors);
			logger.error("\n=======================================================================\nFound problems executing event "
					+ event
					+ string
					+ "\n=======================================================================\n");
		}
		logger.info("Completed definition loading in " + tracker.stopInSeconds() + " s for event: "
				+ event);
		eventService.fire(new TemplateDefinitionsLoaded());
	}
}
