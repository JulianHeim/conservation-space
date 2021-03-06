package com.sirma.itt.cmf.services.mock;

import javax.ejb.Asynchronous;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Specializes;

import org.apache.log4j.Logger;

import com.sirma.itt.cmf.services.impl.MailNotificationServiceImpl;
import com.sirma.itt.emf.exceptions.EmfRuntimeException;
import com.sirma.itt.emf.mail.notification.MailNotificationContext;
import com.sirma.itt.emf.mail.notification.SyncMailNotificationTemplatesEvent;
import com.sirma.itt.emf.security.context.SecurityContextManager;

/**
 * Mock service for mails notifications
 * 
 * @author bbanchev
 */
@Singleton
@Startup
@DependsOn(value = { SecurityContextManager.SERVICE_NAME })
@Specializes
public class MailNotificationServiceMock extends MailNotificationServiceImpl {
	private static final Logger logger = Logger.getLogger(MailNotificationServiceMock.class);

	/**
	 * Send email to the user specified by the delegate's data.
	 * 
	 * @param delegate
	 *            the delegate
	 * @throws EmfRuntimeException
	 *             on unexpected error during build of model - wrong users or model
	 */
	public void sendEmail(MailNotificationContext delegate) throws EmfRuntimeException {
		logger.debug("Sending mail: " + delegate);
	}

	/**
	 * Synchronize templates.
	 * 
	 * @param event
	 *            the event
	 */
	@Asynchronous
	public void synchronizeTemplates(@Observes SyncMailNotificationTemplatesEvent event) {
	}

}
