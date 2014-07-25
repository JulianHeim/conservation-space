package com.sirma.cmf.web.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sirma.itt.cmf.beans.LocalFileDescriptor;
import com.sirma.itt.cmf.beans.LocalProxyFileDescriptor;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.CmfConfigurationProperties;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.content.extract.MailExtractor;
import com.sirma.itt.cmf.event.document.FileUploadedEvent;
import com.sirma.itt.cmf.services.DocumentService;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.plugin.ExtensionPoint;

/**
 * The listener interface for receiving mailUpload events. Currently are processed mails for
 * splitting.
 * 
 * @see MailUploadEvent
 */
@ApplicationScoped
public class MailUploadListener {

	/** The logger. */
	@Inject
	private Logger logger;

	/** The default type to search for. */
	@Inject
	@Config(name = CmfConfigurationProperties.CODELIST_DOCUMENT_DEFAULT_ATTACHMENT_TYPE, defaultValue = "OT210027")
	private String defaultAttachmentType;

	/** The properties. */
	@Inject
	@ExtensionPoint(value = MailExtractor.TARGET_NAME)
	private Iterable<MailExtractor<?>> mailExtractors;

	/** The link service. */
	@Inject
	private LinkService linkService;

	// TODO may be as config
	/** The delete mail attachment on extract. */
	private boolean deleteMailAttachmentOnExtract = false;

	/** DocumentService instance. */
	@Inject
	protected DocumentService documentService;

	/**
	 * On mail uploaded.
	 * 
	 * @param event
	 *            the event
	 */
	public void onMailUploaded(@Observes FileUploadedEvent event) {
		DocumentInstance instance = event.getInstance();

		File tempDir = null;
		try {
			// get the mail store location
			Serializable descriptor = instance.getProperties().get(DocumentProperties.FILE_LOCATOR);
			if (descriptor instanceof LocalFileDescriptor) {
				LocalFileDescriptor fileDescriptor = (LocalFileDescriptor) descriptor;
				tempDir = new File(fileDescriptor.getId()).getParentFile();
			} else {
				logger.error("File location for eml could not be found");
				return;
			}

			splitMailAttachments(tempDir, (LocalFileDescriptor) descriptor, instance);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * Split mail attachments and add them to the current document section. Naming is handled by the
	 * 
	 * @param tempDir
	 *            the dir where mail is stored and where to store the attachments.
	 * @param descriptor
	 *            is the currently uploaded mail descriptor
	 * @param selectedDocumentInstance
	 *            the selected document instance that is uploaded
	 * @throws Exception
	 *             the exception on any error
	 */
	private void splitMailAttachments(File tempDir, DMSFileDescriptor descriptor,
			DocumentInstance selectedDocumentInstance) throws Exception {
		File[] listFiles = tempDir.listFiles();
		if ((listFiles == null) || (listFiles.length != 1)) {
			return;
		}
		if (!(selectedDocumentInstance.getOwningInstance() instanceof SectionInstance)) {
			logger.debug("Not implemented mail attachments to "
					+ (selectedDocumentInstance.getOwningInstance() != null ? selectedDocumentInstance
							.getOwningInstance().getClass().getSimpleName()
							: " non section parent") + ", yet");
			return;
		}
		if (documentService.canUploadDocumentInSection(
				(SectionInstance) selectedDocumentInstance.getOwningInstance(),
				defaultAttachmentType)) {
			for (MailExtractor<?> extractor : mailExtractors) {
				if (extractor.isApplicable(listFiles[0]) != null) {
					String prefix = descriptor.getId();
					if (descriptor instanceof LocalProxyFileDescriptor) {
						prefix = ((LocalProxyFileDescriptor) descriptor).getProxiedId();
					}
					@SuppressWarnings("unchecked")
					List<File> attachments = (List<File>) extractor.extractMail(listFiles[0],
							isDeleteMailAttachmentOnExtract(), tempDir, new File(prefix).getName(),
							MailExtractor.PART_ATTACHMENTS).get(MailExtractor.PART_ATTACHMENTS);
					for (File fileAttachment : attachments) {
						// fill the bean with data
						UploadedFile uploadedFile = new UploadedFile();
						uploadedFile.setLength(fileAttachment.length());
						uploadedFile.setName(fileAttachment.getName());
						// not known currently
						uploadedFile.setMimetype("");

						uploadedFile.setData(IOUtils
								.toByteArray(new FileInputStream(fileAttachment)));
						// create new instance
						DocumentInstance createdDocumentInstance = documentService
								.createDocumentInstance((SectionInstance) selectedDocumentInstance
										.getOwningInstance(), defaultAttachmentType);
						// prepare metadata for document
						UploadAction.updateDocumentModelOnUpload(uploadedFile,
								createdDocumentInstance.getProperties(), createdDocumentInstance,
								tempDir, logger);
						createdDocumentInstance.getProperties().put(
								DocumentProperties.VERSION_DESCRIPTION, "");
						createdDocumentInstance.getProperties().put(
								DocumentProperties.IS_MAJOR_VERSION, Boolean.TRUE);

						// link them
						linkService.link(selectedDocumentInstance, createdDocumentInstance,
								LinkConstants.LINK_MAIL_ATTACHMENT,
								LinkConstants.LINK_ATTACHMENT_MAIL,
								LinkConstants.DEFAULT_SYSTEM_PROPERTIES);
					}
				}

			}
		} else {
			logger.warn("Don't have permissions to upload file of type: " + defaultAttachmentType);
		}
	}

	/**
	 * Checks if is delete mail attachment on extract.
	 * 
	 * @return the deleteMailAttachmentOnExtract
	 */
	public boolean isDeleteMailAttachmentOnExtract() {
		return deleteMailAttachmentOnExtract;
	}

	/**
	 * Sets the delete mail attachment on extract.
	 * 
	 * @param deleteMailAttachmentOnExtract
	 *            the deleteMailAttachmentOnExtract to set
	 */
	public void setDeleteMailAttachmentOnExtract(boolean deleteMailAttachmentOnExtract) {
		this.deleteMailAttachmentOnExtract = deleteMailAttachmentOnExtract;
	}
}
