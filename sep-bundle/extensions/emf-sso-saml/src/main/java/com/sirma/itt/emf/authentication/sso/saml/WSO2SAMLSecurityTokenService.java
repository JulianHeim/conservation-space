/**
 * Copyright (c) 2013 11.07.2013 , Sirma ITT. /* /**
 */
package com.sirma.itt.emf.authentication.sso.saml;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.security.SecurityTokenService;

/**
 * Called when a security token is needed. Connects to the WSO2 Identity Server
 * using HTTP client (simulating web browser) to request a SAML token. WARNING
 * this implementation is tightly coupled to the WSO2 Identity Server and cannot
 * be reused for other servers.
 * 
 * @author Adrian Mitev
 */
@ApplicationScoped
public class WSO2SAMLSecurityTokenService implements SecurityTokenService {
	/** The logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(WSO2SAMLSecurityTokenService.class);

	@Inject
	private SAMLMessageProcessor messageProcessor;

	@Inject
	@Config(name = SSOConfiguration.ISSUER_ID)
	private String issuerId;

	/** The default idp address. */
	@Inject
	@Config(name = SSOConfiguration.SECURITY_SSO_IDP_URL)
	private String idpUrl;

	@Inject
	private EmfContext emfContext;

	@Override
	public String requestToken(String username, String password) {
		// get context path
		String contextPath = emfContext.getServletContext().getContextPath();
		if (StringUtils.isNullOrEmpty(contextPath)) {
			LOGGER.warn("Was unable to extract context path, using default which is : \\emf");
			contextPath = "/emf";
		}
		final String consumerURL = "http://" + issuerId.replace("_", ":") + contextPath + "/ServiceLogin";
		String buildAuthenticationRequest = messageProcessor
				.buildAuthenticationRequest(issuerId, consumerURL);
		LOGGER.debug("Issuer id is : " + issuerId + ", " + " ConsumerURL is: " + consumerURL + ", idp URL is: "
				+ idpUrl + ", provided username is: " + username  + ", provided pass is: " + password);
		HttpClient client = new HttpClient();
		try {
			final String getUrl = idpUrl + "?SAMLRequest=" + buildAuthenticationRequest
					+ "&RelayState=null";
			LOGGER.debug("Sending get request to: {}", getUrl);
			// get credential submit document
			GetMethod get = new GetMethod(getUrl);
			client.executeMethod(get);
			String assertionString = getValueOfElement(get.getResponseBodyAsString(), "assertionString");

			// post credentials and fetch the saml assertion response
			PostMethod post = new PostMethod(idpUrl);
			post.addParameter("username", username);
			post.addParameter("password", password);
			post.addParameter("assertnConsumerURL", consumerURL);
			post.addParameter("issuer", issuerId);
			post.addParameter("assertionString", assertionString);
			post.addParameter("id", "0");
			post.addParameter("subject", "null");
			post.addParameter("relyingPartySessionId", "null");
			post.addParameter("RelayState", "null");

			client.executeMethod(post);

			String samlResponse = getValueOfElement(post.getResponseBodyAsString(), "SAMLResponse");
			return StringEscapeUtils.unescapeXml(samlResponse);
		} catch (HttpException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Extracts the value from a html input tag by its name attribute.
	 * 
	 * @param html
	 *            html content.
	 * @param name
	 *            value of the name of the input tag.
	 * @return the fetched value or null if the element is not found.
	 */
	private String getValueOfElement(String html, String name) {
		// find the name attribute matching the provided name parameter
		int indexOfElement = html.indexOf("name=\"" + name);
		if (indexOfElement != -1) {
			// find the value attribute for the element with the provided name
			int indexOfValue = html.indexOf("value=", indexOfElement) + 7;
			// get the content of the value attribute
			return html.substring(indexOfValue, html.indexOf("\"", indexOfValue));
		}
		return null;
	}

	@Override
	public boolean validateToken(String token) {
		return messageProcessor.validateToken(token);
	}

}
