package com.sirma.itt.cmf.content.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.io.TempFileProvider;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.util.FileUtil;

/**
 * The Class RFC822Parser.
 */
@Extension(target = MailExtractor.TARGET_NAME, order = 0)
public class RFC822Parser implements MailExtractor<MimeMessage> {

	@Inject
	private Logger logger;

	/** The temp file provider. */
	@Inject
	private TempFileProvider tempFileProvider;

	@Override
	public Map<String, Object> extractMail(File mail, boolean deletedata, File tempDir,
			String prefix, String... metadata) {
		Map<String, Object> model = new HashMap<>();
		if (metadata == null) {
			return model;
		}
		for (String part : metadata) {
			if (PART_ATTACHMENTS.equals(part)) {
				List<File> attachmentList = new ArrayList<>();
				FileOutputStream os = null;
				try {
					Pair<MimeMessage, String> message = openMessage(mail);
					File tempStoreDir = tempDir;
					if ((tempStoreDir == null) || !tempStoreDir.canWrite()) {
						tempStoreDir = tempFileProvider.createTempDir(UUID.randomUUID().toString());
						tempStoreDir.mkdirs();
					}
					MimeMessage mimeMessage = message.getFirst();
					Pair<String, String> nameAndExtension = FileUtil.splitNameAndExtension(prefix);
					// extract the attachments and delete them optionally
					extractAttachments(mimeMessage, null, nameAndExtension.getFirst() + "_",
							tempStoreDir, deletedata, attachmentList);
					model.put(PART_ATTACHMENTS, attachmentList);
					mimeMessage.saveChanges();
					os = new FileOutputStream(mail);
					mimeMessage.writeTo(os);

				} catch (Exception e) {
					logger.error("Error during exraction of mail!", e);
				} finally {
					IOUtils.closeQuietly(os);
				}
			} else {
				throw new RuntimeException("Unimplemented operation!");
			}
		}

		return model;

	}

	@Override
	public Pair<MimeMessage, String> openMessage(File mail) throws Exception {
		return openMessage(new FileInputStream(mail));
	}

	@Override
	public Pair<MimeMessage, String> openMessage(InputStream stream) throws Exception {
		Properties props = System.getProperties();
		Session mailSession = Session.getDefaultInstance(props, null);

		MimeMessage message = new MimeMessage(mailSession, stream);

		return new Pair<>(message, message.getEncoding());
	}

	@Override
	public MailExtractor<MimeMessage> isApplicable(File file) {
		return file.canRead() && file.getName().endsWith(".eml") ? this : null;
	}

	/**
	 * Extract message and fills attachment list. Optionally could delete the attachments.
	 * 
	 * @param currentPart
	 *            the part currently processed
	 * @param parentPart
	 *            the parent part of current
	 * @param prefix
	 *            the prefix for attachment file name
	 * @param tempStoreDir
	 *            the temp store dir to use to store attachments in
	 * @param deletePart
	 *            the delete part. whether to delete attachment after extract
	 * @param attachmentList
	 *            the attachment list. Filled during iterate
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws MessagingException
	 *             the messaging exception
	 */
	private void extractAttachments(Part currentPart, Part parentPart, String prefix,
			File tempStoreDir, boolean deletePart, List<File> attachmentList) throws IOException,
			MessagingException {

		if (currentPart.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) currentPart.getContent();
			int count = mp.getCount();
			List<BodyPart> parts = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				BodyPart bodyPart = mp.getBodyPart(i);
				extractAttachments(bodyPart, currentPart, prefix, tempStoreDir, deletePart,
						attachmentList);
				parts.add(bodyPart);
			}
			for (BodyPart bodyPart : parts) {
				if (((bodyPart.getDisposition() != null) && bodyPart.getDisposition().contains(
						Part.ATTACHMENT))) {
					if (deletePart) {
						mp.removeBodyPart(bodyPart);
					}
				}
			}

		} else if (currentPart.isMimeType("message/rfc822")) {
			extractAttachments((Part) currentPart.getContent(), currentPart, prefix, tempStoreDir,
					deletePart, attachmentList);
		} else {
			String disposition = currentPart.getDisposition();
			if (((disposition != null) && disposition.contains(Part.INLINE))) {
				logger.error("INLINE ATTACHMENT CURRENTLY NOT SUPPORTED");
			}
			if (((disposition != null) && disposition.contains(Part.ATTACHMENT))) {
				InputStream stream = currentPart.getInputStream();
				String filename = currentPart.getFileName();
				if (filename.startsWith("=?")) {
					filename = MimeUtility.decodeText(filename);
				}
				File tempFile = new File(tempStoreDir, prefix + filename);
				OutputStream writer = new FileOutputStream(tempFile);
				IOUtils.copy(stream, writer);
				IOUtils.closeQuietly(writer);
				attachmentList.add(tempFile);

			}
		}
	}
}
