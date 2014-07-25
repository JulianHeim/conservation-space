package com.sirma.cmf.web.help;

import static org.testng.Assert.assertEquals;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.sirma.cmf.CMFTest;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.mail.notification.MailNotificationContext;
import com.sirma.itt.emf.mail.notification.MailNotificationService;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.EmfUser;
import com.sirma.itt.emf.security.model.UserInfo;

/**
 * Tests for HelpRequestRestService.
 * 
 * @author svelikov
 */
@Test
public class HelpRequestRestServiceTest extends CMFTest {

	private final HelpRequestRestService restService;
	private MailNotificationService mailNotificationService;

	/**
	 * Instantiates a new help request rest service test.
	 */
	public HelpRequestRestServiceTest() {
		restService = new HelpRequestRestService() {
			@Override
			public Resource getCurrentUser() {
				EmfUser user = new EmfUser("admin");
				user.setUserInfo(new UserInfo("Admin", "Adminov"));
				return user;
			}
		};

		mailNotificationService = Mockito.mock(MailNotificationService.class);
		ReflectionUtils.setField(restService, "mailNotificationService", mailNotificationService);
	}

	/**
	 * Send help request test.
	 */
	public void sendHelpRequestTest() {
		Response response = restService.sendHelpRequest(null);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		//
		String requestString = "{\"subject\":\"some question\"}";
		response = restService.sendHelpRequest(requestString);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		//
		requestString = "{\"subject\":\"some question\",\"type\":\"Question\"}";
		response = restService.sendHelpRequest(requestString);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		//
		requestString = "{\"subject\":\"some question\",\"type\":\"Question\",\"description\":\"<p>???<br data-mce-bogus=\'1\'></p>\"}";
		response = restService.sendHelpRequest(requestString);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		Mockito.verify(mailNotificationService, Mockito.atLeastOnce()).sendEmail(
				Mockito.any(MailNotificationContext.class));
	}
}
