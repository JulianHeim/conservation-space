package com.sirma.itt.cmf.content.extract;

import java.io.ByteArrayInputStream;
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
import java.util.UUID;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.datatypes.ByteChunk;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.StringChunk;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.io.TempFileProvider;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.util.FileUtil;

/**
 * Defines a Msg document content extractor. Mail is extracted based on the requested parts only.
 */
@Extension(target = MailExtractor.TARGET_NAME, order = 1)
public class MSGParser implements MailExtractor<MAPIMessage> {

	/** The temp file provider. */
	@Inject
	private TempFileProvider tempFileProvider;
	@Inject
	private Logger logger;

	@Override
	public Map<String, Object> extractMail(File mail, boolean deleteParts, File tempDir,
			String prefix, String... parts) {
		Map<String, Object> model = new HashMap<>();
		if (parts == null) {
			return model;
		}
		for (String string : parts) {
			if (PART_ATTACHMENTS.equals(string)) {
				List<File> attachmentList = new ArrayList<>();
				try {
					Pair<MAPIMessage, String> openedMessage = openMessage(mail);
					// prepare parent
					File tempStoreDir = tempDir;
					if ((tempStoreDir == null) || !tempStoreDir.canWrite()) {
						tempStoreDir = tempFileProvider.createTempDir(UUID.randomUUID().toString());
						tempStoreDir.mkdirs();
					}
					Pair<String, String> nameAndExtension = FileUtil.splitNameAndExtension(prefix);
					// extract and fill the list
					extractAttachments(openedMessage, nameAndExtension.getFirst() + "_",
							tempStoreDir, attachmentList);
					model.put(PART_ATTACHMENTS, attachmentList);
					if (deleteParts) {
						POIFSAttachmentCleaner.clean(mail);
					}
				} catch (Exception e) {
					logger.error("Error during exraction of mail!", e);
				}
			} else {
				throw new RuntimeException("Unimplemented operation!");
			}
		}

		return model;

	}

	@Override
	public Pair<MAPIMessage, String> openMessage(File file) throws Exception {
		return openMessage(new FileInputStream(file));
	}

	@Override
	public Pair<MAPIMessage, String> openMessage(InputStream stream) throws Exception {
		MAPIMessage message = new MAPIMessage(new NPOIFSFileSystem(stream));
		String encoding = null;
		message.setReturnNullOnMissingChunk(true);
		// // If the message contains strings that aren't stored
		// // as Unicode, try to sort out an encoding for them
		if (message.has7BitEncodingStrings()) {
			if (message.getHeaders() != null) {
				// There's normally something in the headers
				message.guess7BitEncoding();
				encoding = "utf-7";
			} else {
				// // Nothing in the header, try encoding detection
				// // on the message body
				StringChunk text = message.getMainChunks().textBodyChunk;
				if (text != null) {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(text.getRawValue());
					CharsetMatch match = detector.detect();
					if (match.getConfidence() > 35) {
						message.set7BitEncoding(match.getName());
						encoding = match.getName();
					}
				}
			}
		} else {
			encoding = "utf-8";
		}
		return new Pair<>(message, encoding);
	}

	@Override
	public MailExtractor<MAPIMessage> isApplicable(File file) {
		return file.canRead() && file.getName().endsWith(".msg") ? this : null;
	}

	/**
	 * Extract multipart and fills attachment list.
	 * 
	 * @param openedMessage
	 *            the message to get attachments for
	 * @param prefix
	 *            the prefix for attachment (optional)
	 * @param tempStoreDir
	 *            the temp store dir to use
	 * @param attachmentList
	 *            is list with attachments to fill
	 * @throws MessagingException
	 *             the messaging exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void extractAttachments(Pair<MAPIMessage, String> openedMessage, String prefix,
			File tempStoreDir, List<File> attachmentList) throws MessagingException, IOException {
		// just in case
		if (prefix == null) {
			prefix = "";
		}
		MAPIMessage mapiMessage = openedMessage.getFirst();
		for (AttachmentChunks attachment : mapiMessage.getAttachmentFiles()) {
			// attachment.getEmbeddedAttachmentObject()
			String filename = null;
			if (attachment.attachLongFileName != null) {
				filename = attachment.attachLongFileName.getValue();
			} else if (attachment.attachFileName != null) {
				filename = attachment.attachFileName.getValue();
			}

			if ((filename != null) && (filename.length() > 0)) {
				Chunk[] chunks = attachment.getChunks();
				byte[] data = null;
				// String mimetype = null;
				for (Chunk chunk : chunks) {
					if ((MAPIProperty.ATTACH_DATA.id == chunk.getChunkId())
							&& (chunk instanceof ByteChunk)) {
						ByteChunk chunkByte = (ByteChunk) chunk;
						data = chunkByte.getValue();
						break;
					}

				}
				if ((filename != null) && (data != null)) {
					File tempFile = new File(tempStoreDir, prefix + filename);
					OutputStream writer = new FileOutputStream(tempFile);
					IOUtils.copy(new ByteArrayInputStream(data), writer);
					IOUtils.closeQuietly(writer);
					attachmentList.add(tempFile);
				} else {
					// TODO
				}
			}

		}
	}

	/**
	 * Attachment cleaner for msg files. Working with poi fs
	 * 
	 * @author bbanchev
	 */
	private static class POIFSAttachmentCleaner {

		/**
		 * Clean the message from attachments
		 * 
		 * @param msg
		 *            the msg
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public static void clean(File msg) throws IOException {
			parse(new POIFSFileSystem(new FileInputStream(msg)).getRoot());

		}

		/**
		 * Parses the.
		 * 
		 * @param node
		 *            the node
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		private static void parse(DirectoryNode node) throws IOException {
			// FIXME currently not working
		}
	}
}
