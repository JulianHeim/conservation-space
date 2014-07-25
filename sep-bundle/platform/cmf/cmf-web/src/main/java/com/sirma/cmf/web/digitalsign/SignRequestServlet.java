package com.sirma.cmf.web.digitalsign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sirma.itt.cmf.beans.LocalFileDescriptor;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.cmf.services.CaseService;
import com.sirma.itt.cmf.services.DocumentService;
import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.io.TempFileProvider;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.security.AuthenticationService;
import com.sirma.itt.emf.security.Secure;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.security.model.UserInfo;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.util.EqualsHelper;

/**
 * The SignRequestServlet is proxy to download/upload sign resources to dms.
 */
public class SignRequestServlet extends HttpServlet {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = Logger.getLogger(SignRequestServlet.class);
	/** The Constant debug. */
	private static final boolean debug = LOGGER.isDebugEnabled();

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The temp file provider. */
	@Inject
	private TempFileProvider tempFileProvider;

	/** The content service. */
	@Inject
	private DocumentService documentService;

	/** The case service. */
	@Inject
	private CaseService caseService;

	@Inject
	private LabelProvider labelProvider;

	@Inject
	private AuthenticationService authenticationService;

	@Inject
	private EventService eventService;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		processGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		processPost(req, resp);
	}

	/**
	 * Process post request, that is supposed to be upload operation. In header
	 * should be provided the 'documentid' that represents the dmsid for
	 * document. The multipart request is parsed and the field name is used for
	 * the new filename.
	 * 
	 * @param req
	 *            the req to process
	 * @param resp
	 *            the response to generate.
	 */
	public void processPost(HttpServletRequest req, HttpServletResponse resp) {
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (isMultipart) {
			String parameter = req.getHeader("documentid");
			if ((parameter == null) || parameter.trim().isEmpty()) {
				throw new RuntimeException("Invalid document or no document was provided!");
			}
			if (debug) {
				LOGGER.debug("Trying to load file for signing: " + parameter);
			}
			CaseInstance caseInstance = caseService.getPrimaryCaseForDocument(parameter);
			if (caseInstance == null) {
				throw new RuntimeException("Document is not recognized in the system!");
			}

			DocumentInstance loadInstance = null;
			for (SectionInstance sectionInstance : caseInstance.getSections()) {
				for (Instance documentInstance : sectionInstance.getContent()) {
					if ((documentInstance instanceof DocumentInstance)
							&& EqualsHelper.nullSafeEquals(((DocumentInstance) documentInstance).getDmsId(),
									parameter)) {
						loadInstance = (DocumentInstance) documentInstance;
						break;
					}
				}
				if (loadInstance != null) {
					break;
				}
			}
			// this should not happen on this step
			if (loadInstance == null) {
				throw new RuntimeException("Document is not recognized in the system!");
			}
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
			FileOutputStream fos = null;
			try {
				// iterate over data
				List<?> items = upload.parseRequest(req);
				for (Object el : items) {
					if (!(el instanceof FileItem)) {
						LOGGER.warn("Not a fileitem " + el);
					}
					FileItem item = (FileItem) el;
					if (item.isFormField()) {
						// we haven't form fields set in the request
					} else {
						// should as the filename
						String fieldName = item.getFieldName();
						// generate the temporary name
						File signedFile = tempFileProvider.createTempFile(System.currentTimeMillis() + "",
								tempFileProvider.getExtension(fieldName));
						try {
							// copy content
							fos = new FileOutputStream(signedFile);
							IOUtils.copy(item.getInputStream(), fos);
							IOUtils.closeQuietly(fos);
							// set the new version data
							loadInstance.getProperties().put(DocumentProperties.IS_MAJOR_VERSION,
									Boolean.FALSE);
							loadInstance.getProperties().put(DocumentProperties.VERSION_DESCRIPTION,
									labelProvider.getValue("document.sign.auto.description"));
							loadInstance.getProperties().put(DocumentProperties.FILE_LOCATOR,
									new LocalFileDescriptor(signedFile));
							// mark as signed using this prop
							loadInstance.getProperties().put(DocumentProperties.DOCUMENT_SIGNED_DATE,
									new Date());
							// indicate that we a signing
							loadInstance.getProperties()
									.put(DocumentProperties.DOCUMENT_SIGNED, Boolean.TRUE);
							// get current user in order to execute the action
							// with it.
							User currentUser = authenticationService.getCurrentUser();
							if (currentUser == null) {
								throw new RuntimeException(
										"Could not extract the current user from AuthenticationService.");
							}
							UserInfo userInfo = currentUser.getUserInfo();
							if (userInfo != null) {
								String displayName = userInfo.getDisplayName();
								loadInstance.getProperties().put(DocumentProperties.DOCUMENT_SIGNED_BY,
										displayName);
							}
							// save the document
							final DocumentInstance instance = loadInstance;
							SecurityContextManager.callAs(currentUser, new Callable<DocumentInstance>() {
								@Override
								public DocumentInstance call() throws Exception {
									return documentService.save(instance, new Operation(
											ActionTypeConstants.SIGN));
								}
							});
							RuntimeConfiguration.setConfiguration(
									RuntimeConfigurationProperties.DO_NOT_SAVE_CHILDREN, Boolean.TRUE);
							caseService.save(caseInstance, new Operation(ActionTypeConstants.EDIT_DETAILS));
							// fire event that the document was signed successfully
							AfterDigitalSignEvent afterDigitalSignEvent = new AfterDigitalSignEvent();
							afterDigitalSignEvent.setCaseInstance(caseInstance);
							afterDigitalSignEvent.setDocumentInstance(instance);
							eventService.fire(afterDigitalSignEvent);
						} finally {
							IOUtils.closeQuietly(fos);
							tempFileProvider.deleteFile(signedFile);
							RuntimeConfiguration
									.clearConfiguration(RuntimeConfigurationProperties.DO_NOT_SAVE_CHILDREN);
						}
					}
				}
			} catch (Exception e) {
				LOGGER.error("", e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Process the request.
	 * 
	 * @param req
	 *            HttpServletRequest.
	 * @param resp
	 *            HttpServletResponse.
	 */
	@Secure
	public void processGet(HttpServletRequest req, HttpServletResponse resp) {
		DocumentInstance doc = new DocumentInstance();
		String parameter = req.getHeader("documentid");
		if ((parameter == null) || parameter.trim().isEmpty()) {
			throw new RuntimeException("Invalid document is provided!");
		}
		doc.setDmsId(parameter);
		doc.setProperties(new HashMap<String, Serializable>(1));
		File content = documentService.getContent(doc);
		InputStream input = null;
		try {
			input = new FileInputStream(content);
			OutputStream writer = resp.getOutputStream();
			resp.setCharacterEncoding("UTF-8");
			resp.reset();
			IOUtils.copyLarge(input, writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			LOGGER.error("", e);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
	}

}
