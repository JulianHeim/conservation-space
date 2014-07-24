package com.sirma.itt.emf.authentication.sso.saml;

import java.io.IOException;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.SessionIndex;
import org.opensaml.xml.util.Base64;

import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.SystemConfiguration;
import com.sirma.itt.emf.resources.ResourceService;
import com.sirma.itt.emf.resources.ResourceType;
import com.sirma.itt.emf.security.AuthenticationService;
import com.sirma.itt.emf.security.event.UserLogoutEvent;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.util.CDI;

/**
 * Handles Single Logout.
 */
@WebServlet(name = "SAML2ServiceLogout", urlPatterns = SAMLServiceLogout.SERVICE_LOGOUT, loadOnStartup = 1)
public class SAMLServiceLogout extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -4749929835873786286L;

	/** The Constant SERVICE_LOGOUT. */
	static final String SERVICE_LOGOUT = "/ServiceLogout";

	/** The Constant SAML_RESPONSE. */
	private static final String SAML_RESPONSE = "SAMLResponse";
	/** PILCROW SIGN. */
	private static final String DELIMITER = "\u00B6";
	/** The log. */
	private static final Logger LOGGER = Logger.getLogger(SAMLServiceLogout.class);

	/** The message processor. */
	@Inject
	private SAMLMessageProcessor messageProcessor;

	/** The logout event. */
	@Inject
	private Event<UserLogoutEvent> logoutEvent;

	/** The default idp address. */
	@Inject
	@Config(name = SSOConfiguration.SECURITY_SSO_IDP_URL)
	private String defaultIdPUrl;

	/** The system configuration. */
	@Inject
	private SystemConfiguration systemConfiguration;

	/** The logout manager. */
	@Inject
	private SessionManager sessionManager;

	/** The bean manager. */
	@Inject
	private BeanManager beanManager;

	@Inject
	private ResourceService resourceService;

	/** The use multi browser mode. */
	private boolean useMultiBrowserMode = true;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init() {
		String logoutContextPath = getServletContext().getContextPath() + SERVICE_LOGOUT;
		getServletContext().setAttribute("logoutContextPath", logoutContextPath);
	}

	/**
	 * Called by the browser when a logout should be performed. Sends a single logout request to the
	 * IdP.
	 *
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @throws ServletException
	 *             the servlet exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		User currentUser = null;
		try {
			AuthenticationService authenticationService = CDI.instantiateDefaultBean(
					AuthenticationService.class, beanManager);
			currentUser = authenticationService.getCurrentUser();
			LOGGER.debug("Sending SAML logout request: "
					+ (currentUser == null ? " null" : currentUser.getIdentifier()));
			String id = getProcessingId(currentUser, request);
			if (id == null) {
				invalidateSession(request.getSession());
				response.sendRedirect(request.getContextPath());
				return;
			}

			if (sessionManager.isProcessing(id)) {
				invalidateSession(request.getSession());
				response.sendRedirect(request.getContextPath());
				return;
			}
			sessionManager.beginLogout(id);
		} catch (Exception e) {
			LOGGER.warn("Logout error: " + e.getLocalizedMessage());
			invalidateSession(request.getSession());
			response.sendRedirect(request.getContextPath());
			return;
		}
		StringBuilder requestURI = buildLogoutMessage(currentUser.getIdentifier(), request);
		LOGGER.debug("Firing UserLogoutEvent for " + currentUser);
		logoutEvent.fire(new UserLogoutEvent(currentUser));
		invalidateSession(request.getSession());
		response.sendRedirect(requestURI.toString());
	}

	/**
	 * Build a logout message for idp saml2 processor using the arguments.
	 *
	 * @param userId
	 *            is user that would be logged out
	 * @param request
	 *            is the http request
	 * @return the builder url for the request.
	 */
	private StringBuilder buildLogoutMessage(String userId, HttpServletRequest request) {
		Object sessionIndex = request.getSession().getAttribute("SessionIndex");
		String issuerId = request.getServerName() + "_" + request.getServerPort();
		String encodedRequestMessage = messageProcessor.buildLogoutRequest(userId, issuerId,
				sessionIndex);

		String idpUrlKey = SSOConfiguration.SECURITY_SSO_IDP_URL + "." + request.getLocalAddr();
		String idpUrl = systemConfiguration.getConfiguration(idpUrlKey);

		StringBuilder requestURI = new StringBuilder();
		requestURI.append(idpUrl);
		requestURI.append("?SAMLRequest=");
		requestURI.append(encodedRequestMessage);
		requestURI.append("&RelayState=");
		requestURI.append(request.getContextPath());
		return requestURI;
	}

	/**
	 * Gets a unique identifier based on the request and current user.
	 *
	 * @param currentUser
	 *            is the current user if null - null is returned
	 * @param request
	 *            is the request for servlet
	 * @return the id for the user or/and browser
	 */
	private String getProcessingId(User currentUser, HttpServletRequest request) {
		if (currentUser == null) {
			return null;
		}
		if (!useMultiBrowserMode) {
			return currentUser.getIdentifier();
		}
		String header = request.getHeader("User-Agent");
		return header + DELIMITER + currentUser.getIdentifier();

	}

	/**
	 * Called by the IdP when the logout request is processed. Sends a logout event and invalidates
	 * the session.
	 *
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @throws ServletException
	 *             the servlet exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO here is the right place to invalidate session, but since redirect to idp might be
		// blocked(FF) the session is invalidated at first place
		String responseMessage = request.getParameter(SAML_RESPONSE);
		if (responseMessage != null) {
			LOGGER.debug("Logout SAML response received!");
			response.sendRedirect(request.getContextPath());
		} else {
			// request.getSession().invalidate();
			String encodedRequestMessage = request.getParameter("SAMLRequest");
			LOGGER.debug("IDP SAML request has been received! " + encodedRequestMessage);
			if (encodedRequestMessage != null) {
				LogoutRequest processSAMLRequest = messageProcessor.processSAMLRequest(new String(
						Base64.decode(encodedRequestMessage)));
				if (processSAMLRequest != null && processSAMLRequest.getSessionIndexes() != null) {
					for (SessionIndex nextSession : processSAMLRequest.getSessionIndexes()) {
						String sessionIndex = nextSession.getSessionIndex();
						HttpSession session = sessionManager.getSession(sessionIndex);
						if (session != null) {
							LOGGER.debug("Automatic end of session " + sessionIndex);
							if (invalidateSession(session)) {
								sessionManager.unregisterSession(sessionIndex);
							}
						}
					}
					String userId = processSAMLRequest.getNameID().getValue();
					User user = resourceService.getResource(userId, ResourceType.USER);
					// fire event for backend logout
					LOGGER.debug("Firing UserLogoutEvent for " + user);
					logoutEvent.fire(new UserLogoutEvent(user));
				}

			} else {
				LOGGER.error("Valid logout SAML response has not been received!");
			}
		}
	}

	/**
	 * Invalidates session of the request. Handle exceptions
	 *
	 * @param session
	 *            the session to invalidate
	 * @return true on successful invalidation
	 */
	private boolean invalidateSession(HttpSession session) {
		try {
			if (session != null) {
				session.invalidate();
				return true;
			}
		} catch (Exception e) {
			LOGGER.warn("Error during session closing!", e);
		}
		return false;
	}
}
