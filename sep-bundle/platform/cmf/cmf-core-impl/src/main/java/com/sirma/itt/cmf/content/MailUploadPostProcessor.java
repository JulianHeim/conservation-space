package com.sirma.itt.cmf.content;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.sirma.itt.cmf.beans.LocalFileDescriptor;
import com.sirma.itt.cmf.beans.LocalProxyFileDescriptor;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.CmfConfigurationProperties;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.content.extract.MailExtractor;
import com.sirma.itt.cmf.services.DocumentService;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.link.LinkConstants;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.util.FileUtil;

/**
 * Extraction of mails as post process of uploading eml/msg files.
 * 
 * @author bbanchev
 */
@Extension(target = UploadPostProcessor.TARGET_NAME, order = 1)
@ApplicationScoped
public class MailUploadPostProcessor implements UploadPostProcessor {

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

	/** The delete mail attachment on extract. */
	private boolean deleteMailAttachmentOnExtract = false;

	/** DocumentService instance. */
	@Inject
	protected DocumentService documentService;

	/** The link service. */
	@Inject
	private LinkService linkService;

	/**
	 * On mail uploaded extract the content and return it as list of created documents<br>
	 * {@inheritDoc}
	 */
	@Override
	public List<DocumentInstance> proccess(final DocumentInstance instance) throws Exception {

		File tempDir = null;
		// get the mail store location
		Serializable descriptor = instance.getProperties().get(DocumentProperties.FILE_LOCATOR);
		if (descriptor instanceof LocalFileDescriptor) {
			LocalFileDescriptor fileDescriptor = (LocalFileDescriptor) descriptor;
			tempDir = new File(fileDescriptor.getId()).getParentFile();
		} else {
			logger.error("File location for eml could not be found");
			return Collections.singletonList(instance);
		}

		return splitMailAttachments(tempDir, (LocalFileDescriptor) descriptor, instance);
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
	 * @return the list of created instances during split of mail
	 * @throws Exception
	 *             the exception on any error
	 */
	private List<DocumentInstance> splitMailAttachments(File tempDir, DMSFileDescriptor descriptor,
			DocumentInstance selectedDocumentInstance) throws Exception {
		File[] listFiles = tempDir.listFiles();
		if ((listFiles == null) || (listFiles.length != 1)) {
			return Collections.singletonList(selectedDocumentInstance);
		}
		if (!(selectedDocumentInstance.getOwningInstance() instanceof SectionInstance)) {
			logger.debug("Not implemented mail attachments to "
					+ (selectedDocumentInstance.getOwningInstance() != null ? selectedDocumentInstance
							.getOwningInstance().getClass().getSimpleName()
							: " non section parent") + ", yet");
			return Collections.singletonList(selectedDocumentInstance);
		}
		for (MailExtractor<?> extractor : mailExtractors) {
			if (extractor.isApplicable(listFiles[0]) != null) {
				if (documentService.canUploadDocumentInSection(
						(SectionInstance) selectedDocumentInstance.getOwningInstance(),
						defaultAttachmentType)) {

					String prefix = descriptor.getId();
					if (descriptor instanceof LocalProxyFileDescriptor) {
						prefix = ((LocalProxyFileDescriptor) descriptor).getProxiedId();
					}
					@SuppressWarnings("unchecked")
					List<File> attachments = (List<File>) extractor.extractMail(listFiles[0],
							isDeleteMailAttachmentOnExtract(), tempDir, new File(prefix).getName(),
							MailExtractor.PART_ATTACHMENTS).get(MailExtractor.PART_ATTACHMENTS);
					List<DocumentInstance> createdDocuments = new ArrayList<>(
							attachments.size() + 1);
					createdDocuments.add(selectedDocumentInstance);
					for (File fileAttachment : attachments) {
						// create new instance
						DocumentInstance createdDocumentInstance = documentService
								.createDocumentInstance((SectionInstance) selectedDocumentInstance
										.getOwningInstance(), defaultAttachmentType);
						// prepare metadata for document
						updateDocumentModelOnUpload(fileAttachment, createdDocumentInstance);
						createdDocumentInstance.getProperties().put(
								DocumentProperties.VERSION_DESCRIPTION, "");
						createdDocumentInstance.getProperties().put(
								DocumentProperties.IS_MAJOR_VERSION, Boolean.TRUE);

						// link them
						linkService.link(selectedDocumentInstance, createdDocumentInstance,
								LinkConstants.LINK_MAIL_ATTACHMENT,
								LinkConstants.LINK_ATTACHMENT_MAIL,
								LinkConstants.DEFAULT_SYSTEM_PROPERTIES);
						createdDocuments.add(createdDocumentInstance);
					}
					return createdDocuments;
				} else {
					logger.warn("Don't have permissions to upload file of type: "
							+ defaultAttachmentType);
				}
			}

		}

		return Collections.singletonList(selectedDocumentInstance);
	}

	/**
	 * Method to update the document properties before upload.
	 * 
	 * @param fileAttachment
	 *            is the file that is extracted
	 * @param documentInstance
	 *            is the currently uploaded instance
	 * @return true on success
	 * @throws IOException
	 *             if any error occurs
	 */
	private boolean updateDocumentModelOnUpload(File fileAttachment,
			DocumentInstance documentInstance) throws IOException {

		long lenght = fileAttachment.length();
		String name = fileAttachment.getName();
		String mimetype = "";

		String finalFileName = new File(name).getName();
		int lastSeparator = finalFileName.lastIndexOf("\\");
		if (lastSeparator > 0) {
			finalFileName = finalFileName.substring(lastSeparator + 1, finalFileName.length());
		}
		File localFile = null;
		if (finalFileName.getBytes().length > 255) {
			String extension = FileUtil.splitNameAndExtension(finalFileName).getSecond();
			if (StringUtils.isNullOrEmpty(extension)) {
				extension = "tmp";
			}
			localFile = new File(System.nanoTime() + "." + extension);
		} else {
			localFile = new File(finalFileName);
		}

		Map<String, Serializable> properties = documentInstance.getProperties();

		properties
				.put(DocumentProperties.FILE_LOCATOR,
						new LocalProxyFileDescriptor(localFile.getName(), fileAttachment
								.getAbsolutePath()));

		// No empty title is allowed - CMF-6612
		if (org.apache.commons.lang.StringUtils.isBlank((String) properties
				.get(DocumentProperties.TITLE))) {
			properties.put(DocumentProperties.TITLE, finalFileName);
		}

		properties.put(DocumentProperties.MIMETYPE, mimetype);
		properties.put(DocumentProperties.FILE_SIZE, humanReadableByteCount(lenght));

		return true;
	}

	/**
	 * Converts bytes into human readable format.
	 * 
	 * @param bytes
	 *            bytes to convert.
	 * @return human readable string.
	 */
	public static String humanReadableByteCount(long bytes) {
		int unit = 1000;
		if (bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "kMGTPE".charAt(exp - 1) + "";
		return String.format("%.1f%sB", Double.valueOf(bytes / Math.pow(unit, exp)), pre);
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
