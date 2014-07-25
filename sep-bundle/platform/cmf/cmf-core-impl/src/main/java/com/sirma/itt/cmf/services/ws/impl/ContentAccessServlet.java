package com.sirma.itt.cmf.services.ws.impl;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.domain.ObjectTypesCmf;
import com.sirma.itt.cmf.services.DocumentService;
import com.sirma.itt.emf.converter.TypeConverterUtil;
import com.sirma.itt.emf.instance.dao.InstanceDao;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.security.AuthenticationService;

/**
 * The provides access to content of documents, by proxing access to dms.
 */
@WebServlet(urlPatterns = { "/content/*" }, name = "Content Access Proxy")
public class ContentAccessServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(ContentAccessServlet.class);

	private static final boolean debug = LOGGER.isDebugEnabled();

	private static final long serialVersionUID = 1L;

	@Inject
	private DocumentService documentService;

	@Inject
	@InstanceType(type = ObjectTypesCmf.DOCUMENT)
	private InstanceDao<Instance> documentInstanceDao;

	@Inject
	private AuthenticationService authenticationService;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		processGet(req, resp);
	}

	/**
	 * Process the request of getting the content of file. Content is write in the response stream
	 * of servlet. The id of doc is its db id and it is provided in the last url segment
	 * <code> .../content/10</code>.
	 * 
	 * @param req
	 *            HttpServletRequest.
	 * @param resp
	 *            HttpServletResponse.
	 * @throws ServletException
	 *             the servlet exception
	 */
	public void processGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		String requestURI = req.getRequestURI();
		int idTokenStart = requestURI.lastIndexOf("/");
		if (idTokenStart == -1) {
			throw new ServletException("Invalid request uri");
		}
		DocumentInstance documentInstance = null;
		try {
			String parameter = requestURI.substring(idTokenStart + 1);
			if (debug) {
				LOGGER.debug("Accessing document with id:" + parameter + " by user "
						+ authenticationService.getCurrentUserId());
			}
			try {
				Serializable id = TypeConverterUtil.getConverter().convert(
						documentInstanceDao.getPrimaryIdType(), parameter);
				documentInstance = documentService.loadByDbId(id);
			} catch (Exception e) {
				LOGGER.warn("Passed invalid document identifier: " + parameter
						+ ". Failed to convert with: " + e.getMessage());
				return;
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		if (documentInstance == null) {
			throw new ServletException("Invalid document is provided or not found!");
		}

		try (BufferedOutputStream out =  new BufferedOutputStream(resp.getOutputStream())) {
			resp.setCharacterEncoding("UTF-8");
			resp.reset();
			resp.setContentType((String) documentInstance.getProperties().get(
					DocumentProperties.MIMETYPE));
			documentService.getContent(documentInstance, out);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
