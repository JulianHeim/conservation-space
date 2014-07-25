package com.sirma.cmf.web.upload;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.cmf.web.util.LabelConstants;
import com.sirma.itt.cmf.beans.LocalProxyFileDescriptor;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionRef;
import com.sirma.itt.cmf.beans.definitions.DocumentDefinitionTemplate;
import com.sirma.itt.cmf.beans.definitions.impl.DocumentDefinitionRefProxy;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.CmfConfigurationProperties;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.constants.allowed_action.ActionTypeConstants;
import com.sirma.itt.cmf.content.UploadPostProcessor;
import com.sirma.itt.cmf.event.document.FileUploadedEvent;
import com.sirma.itt.cmf.services.DocumentService;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.annotation.Proxy;
import com.sirma.itt.emf.codelist.CodelistService;
import com.sirma.itt.emf.codelist.model.CodeValue;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.configuration.RuntimeConfiguration;
import com.sirma.itt.emf.configuration.RuntimeConfigurationProperties;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.domain.model.Identity;
import com.sirma.itt.emf.domain.model.Purposable;
import com.sirma.itt.emf.event.EventService;
import com.sirma.itt.emf.exceptions.TypeConversionException;
import com.sirma.itt.emf.instance.InstanceUtil;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.instance.model.InitializedInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.io.TempFileProvider;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.plugin.ExtensionPoint;
import com.sirma.itt.emf.rest.RestServiceConstants;
import com.sirma.itt.emf.security.AuthenticationService;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.state.operation.Operation;
import com.sirma.itt.emf.util.CollectionUtils;
import com.sirma.itt.emf.util.FileUtil;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.emf.util.PropertyModelComparator;

/**
 * Handles file uploads.
 * <p>
 * REVIEW: What about changing the scope to Application? The methods does not require scope and are
 * designed to handle parallel requests.
 * 
 * @author Adrian Mitev
 */
@Path("/upload")
public class UploadRestService {

	/** The Constant ITEMS_COUNT. */
	private static final String ITEMS_COUNT = "itemsCount";

	/** The Constant TYPE. */
	private static final String TYPE = "type";

	/** The Constant FILE. */
	private static final String FILE = "file";

	/** The Constant PATH. */
	private static final String PATH = "path";

	/** The Constant LOGGER. */
	private static final Logger LOGGER = Logger.getLogger(UploadRestService.class);

	/** Index used to create base upload folder for the upload session. */
	private static AtomicLong index = new AtomicLong(0);

	/** The upload operation. */
	private static Operation UPLOAD_OPERATION = new Operation(ActionTypeConstants.UPLOAD);

	/** temp file provider. */
	@Inject
	private TempFileProvider tempFileProvider;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;

	/** The codelist service. */
	@Inject
	private CodelistService codelistService;

	/** The document service. */
	@Inject
	private DocumentService documentService;

	/** The documet type codelist. */
	@Inject
	@Config(name = CmfConfigurationProperties.CODELIST_DOCUMENT_TITLE, defaultValue = "210")
	private Integer documetTypeCodelist;

	/** The default language. */
	@Inject
	@Config(name = EmfConfigurationProperties.SYSTEM_LANGUAGE, defaultValue = "bg")
	private String defaultLanguage;

	/** The max allowed file size. */
	@Inject
	@Config(name = EmfConfigurationProperties.FILE_UPLOAD_MAXSIZE, defaultValue = "10000000")
	private Integer maxSize;

	/** The dictionary service. */
	@Inject
	private DictionaryService dictionaryService;

	/** The link service. */
	@Inject
	private LinkService linkService;

	/** The label provider. */
	@Inject
	private LabelProvider labelProvider;

	/** The authentication service. */
	@Inject
	protected javax.enterprise.inject.Instance<AuthenticationService> authenticationService;

	/** The event service. */
	@Inject
	private EventService eventService;

	/** The instance service. */
	@Inject
	@Proxy
	private InstanceService<Instance, DefinitionModel> instanceService;

	@Inject
	@ExtensionPoint(value = UploadPostProcessor.TARGET_NAME)
	private Iterable<UploadPostProcessor> postProcessors;

	/**
	 * Retrieve document types allowed to be upload in specified section.
	 * 
	 * @param instanceType
	 *            Document type - objectinstance or documentinstance.
	 * @param sectionId
	 *            Section id.
	 * @return JSON object as string containing an array of types .
	 */
	@GET
	@Path("/{instanceType}/{instanceId}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String retrieveAllowedTypes(@PathParam("instanceType") String instanceType,
			@PathParam("instanceId") String sectionId) {

		// load the section if needed
		Instance sectionInstance = loadInstanceInternal(instanceType, sectionId);

		Map<String, CodeValue> codeValues = codelistService.getCodeValues(documetTypeCodelist);

		// collect all code values for available document definitions
		Map<String, CodeValue> filteredCodevalues = new HashMap<String, CodeValue>();
		// the number of upload times for each document
		Map<String, Pair<? extends Identity, Integer>> allowedDocuments;

		// if section is present that is part of a case as parent we should check the possible
		// documents for the section
		if (isSectionInCase(sectionInstance)
				&& (((Purposable) sectionInstance).getPurpose() == null)
				&& SectionInstance.class.getSimpleName().equalsIgnoreCase(instanceType)) {
			// get allowed documents for selected section
			Map<String, Pair<DocumentDefinitionRef, Integer>> documents = documentService
					.getAllowedDocuments((SectionInstance) sectionInstance);

			allowedDocuments = CollectionUtils.createLinkedHashMap(documents.size());
			// filter only default documents
			for (Entry<String, Pair<DocumentDefinitionRef, Integer>> entry : documents.entrySet()) {
				if (entry.getValue().getFirst().getPurpose() == null) {
					allowedDocuments.put(entry.getKey(), entry.getValue());
				}
			}

			// filter the documents by codelist
			CollectionUtils.copyValuesIfExist(codeValues, filteredCodevalues,
					allowedDocuments.keySet());
		} else {
			// otherwise we should get all possible documents and return them
			List<DocumentDefinitionTemplate> allDocuments = dictionaryService
					.getAllDefinitions(DocumentDefinitionTemplate.class);

			allowedDocuments = CollectionUtils.createLinkedHashMap(allDocuments.size());

			// all documents from this list could be uploaded unlimited number of times to we do
			// that for all
			Integer count = Integer.valueOf(-1);
			for (DocumentDefinitionTemplate template : allDocuments) {

				// only return documents that a part of the codelist - same as above the 'else'
				if (CollectionUtils.copyValueIfExist(codeValues, filteredCodevalues,
						template.getIdentifier())) {
					allowedDocuments.put(template.getIdentifier(), new Pair<Identity, Integer>(
							template, count));
				}
			}
		}

		JSONArray result = new JSONArray();
		String language = getLanguage();
		String unlimited = labelProvider
				.getValue(LabelConstants.DOCUMENT_UPLOAD_NUMBER_OF_COPIES_UNLIMITED);

		List<CodeValue> list = new ArrayList<CodeValue>(filteredCodevalues.values());
		// sort the values by the user locale
		String locale = getLanguage();
		Collections.sort(list, new PropertyModelComparator(true, locale));
		// copy sorted list to the map
		Map<String, CodeValue> map = CollectionUtils.createLinkedHashMap(list.size());
		for (CodeValue codeValue : list) {
			map.put(codeValue.getValue(), codeValue);
		}
		filteredCodevalues = map;

		// build result for the allowed documents
		for (Map.Entry<String, CodeValue> entry : filteredCodevalues.entrySet()) {
			Pair<? extends Identity, Integer> pair = allowedDocuments.get(entry.getKey());

			String numberOfCopies = Integer.toString(pair.getSecond());

			// unlimited number of documents can be uploaded
			if ("-1".equals(numberOfCopies)) {
				numberOfCopies = unlimited;
			}

			JSONObject value = new JSONObject();
			JsonUtil.addToJson(value, "value", entry.getKey());

			StringBuilder builder = new StringBuilder(100);
			builder.append(entry.getValue().getProperties().get(language)).append(" (")
					.append(numberOfCopies).append(")");

			JsonUtil.addToJson(value, "label", builder.toString());
			result.put(value);
		}

		return result.toString();
	}

	/**
	 * Retrieve document mandatory fields depending on definition.
	 * 
	 * @param type
	 *            Document type.
	 * @return JSON object as string containing an array of mandatory fields.
	 */
	@GET
	@Path("/{type}")
	@Produces(RestServiceConstants.APPLICATION_JSON_UTF_ENCODED)
	public String retrieveMandatoryFields(@PathParam("type") String type) {

		JSONArray result = new JSONArray();
		DefinitionModel documentDefinition = dictionaryService.getDefinition(
				DocumentDefinitionTemplate.class, type);
		List<PropertyDefinition> properties = documentDefinition.getFields();

		for (PropertyDefinition propertie : properties) {
			if (propertie.isMandatory() && !"type".equals(propertie.getIdentifier())) { // &&
																						// propertie.getDisplayType()
																						// = ==
																						// DisplayType.EDITABLE
				// && !"title".equals(propertie.getIdentifier()) &&
				// !"type".equals(propertie.getIdentifier())
				JSONObject value = new JSONObject();
				try {
					value.put("fieldName", propertie.getIdentifier());
					value.put("codelistNumber", propertie.getCodelist());
					value.put("fieldType", propertie.getCodelist());
				} catch (JSONException e) {
					LOGGER.error("JSON exception", e);
				}

				result.put(value);
			}
		}

		// return result.toString();
		return null;
	}

	/**
	 * Gets the current language for user or system defined.
	 * 
	 * @return the language
	 */
	private String getLanguage() {
		try {
			User user = authenticationService.get().getCurrentUser();
			if (user != null) {
				return user.getUserInfo().getLanguage();
			}
		} catch (ContextNotActiveException e) {
			LOGGER.info("", e);
			User loggedUser = SecurityContextManager.getFullAuthentication();
			if (loggedUser != null) {
				return loggedUser.getUserInfo().getLanguage();
			}
		}
		return defaultLanguage;
	}

	/**
	 * Load instance internal.
	 * 
	 * @param type
	 *            the type
	 * @param id
	 *            the id
	 * @return the t
	 */
	private Instance loadInstanceInternal(String type, String id) {
		InstanceReference reference;
		try {
			reference = typeConverter.convert(InstanceReference.class, type);
		} catch (TypeConversionException e) {
			LOGGER.error("", e);
			return null;
		}
		reference.setIdentifier(id);
		InitializedInstance instance = typeConverter.convert(InitializedInstance.class, reference);
		return instance.getInstance();
	}

	/**
	 * Consumes a file upload POST request. Call doUpload and return Id's of newly created
	 * (uploaded) documents.
	 * 
	 * @param request
	 *            http request containing posted data.
	 * @return the id's of uploaded files
	 */
	@POST
	@Path("/upload")
	@Consumes("multipart/form-data")
	public String upload(@Context HttpServletRequest request) {
		List<DocumentInstance> instances = doUpload(request);
		JSONArray result = new JSONArray();

		for (DocumentInstance instance : instances) {
			JSONObject value = new JSONObject();
			JsonUtil.addToJson(value, "dbId", instance.getId());
			JsonUtil.addToJson(value, "type", "documentinstance");
			JsonUtil.addToJson(value, "default_header",
					instance.getProperties().get("default_header"));
			JsonUtil.addToJson(value, "compact_header",
					instance.getProperties().get("compact_header"));
			result.put(value);
		}
		return result.toString();
	}

	/**
	 * Consumes a file upload POST request, creates a document within the specified context (parent)
	 * and attach it to a parent.
	 * 
	 * @param request
	 *            http request containing posted data.
	 * @return the list
	 */
	private List<DocumentInstance> doUpload(HttpServletRequest request) {
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);

		List<FileItem> items = null;
		try {
			items = upload.parseRequest(request);

		} catch (FileUploadException e) {
			LOGGER.error("Failed to parse upload request due to: ", e);
			return Collections.emptyList();
		}

		Map<String, Serializable> args = parseRequestItems(items);
		Instance targetInstance = getTargetInstance(args);

		if (targetInstance == null) {
			LOGGER.warn("Failed to load target instance for the upload of a document! The request is: "
					+ args);
			return Collections.emptyList();
		}
		File uploadFolder = tempFileProvider.createTempDir("Upload-" + System.currentTimeMillis()
				+ '-' + index.getAndIncrement());

		// build document request for parallel upload to DMS
		List<DocumentInstance> documentsToUpload = buildInstancesForUpload(args, targetInstance,
				uploadFolder);
		if (documentsToUpload.isEmpty()) {
			LOGGER.warn("NO documents to upload after processing the upload request!");
			return Collections.emptyList();
		}

		try {
			DocumentInstance[] instances = documentsToUpload
					.toArray(new DocumentInstance[documentsToUpload.size()]);

			// upload documents to DMS in parallel
			if (documentService.upload(targetInstance, true, instances)) {
				// on success attach all documents to the parent instance depending on the type
				// for documents there is a special requirement to add the uploaded sub documents to
				// the document's section
				Instance uploadTargetInstance = targetInstance;
				if (targetInstance instanceof DocumentInstance) {
					SectionInstance sectionInstance = InstanceUtil.getParent(SectionInstance.class,
							targetInstance);
					if (sectionInstance != null) {
						uploadTargetInstance = sectionInstance;
					} else {
						LOGGER.warn("Trying to upload documents to other document that is not part of a section. The documents will be uploaded to the original document!");
					}
				}

				// instanceService.attach(uploadTargetInstance, UPLOAD_OPERATION, instances);

				updateParentInstanceForUploadedDocuments(targetInstance);

				// CMF-5568 !!
				if ("objectinstance".equals(args.get("instance"))
						|| "documentinstance".equals(args.get("instance"))) {
					for (Instance inst : instances) {
						List<LinkReference> links = linkService.getLinks(inst.toReference());

						for (LinkReference link : links) {
							if ("caseinstance".equals(link.getTo().getReferenceType().getName())) {
								linkService.removeLinkById(link.getId());
							}
						}
					}
				}
			}
			return documentsToUpload;
		} finally {
			tempFileProvider.deleteFile(uploadFolder);
		}
	}

	/**
	 * Update parent instance for uploaded documents.
	 * 
	 * @param targetInstance
	 *            the target instance
	 */
	private void updateParentInstanceForUploadedDocuments(Instance targetInstance) {
		try {
			// no need to update the instance tree when only need to touch it
			RuntimeConfiguration.setConfiguration(
					RuntimeConfigurationProperties.DO_NOT_SAVE_CHILDREN, Boolean.TRUE);
			if (isSectionInCase(targetInstance) || (targetInstance instanceof DocumentInstance)) {
				CaseInstance caseInstance = InstanceUtil.getParent(CaseInstance.class,
						targetInstance);
				// if document instance is part of an object then we will not have a case
				if (caseInstance != null) {
					instanceService.save(caseInstance, UPLOAD_OPERATION);
				}
			}
		} finally {
			RuntimeConfiguration
					.clearConfiguration(RuntimeConfigurationProperties.DO_NOT_SAVE_CHILDREN);
		}
	}

	/**
	 * Builds the instances for upload.
	 * 
	 * @param args
	 *            the args
	 * @param targetInstance
	 *            the target instance
	 * @param uploadFolder
	 *            the upload folder
	 * @return the list
	 */
	private List<DocumentInstance> buildInstancesForUpload(Map<String, Serializable> args,
			Instance targetInstance, File uploadFolder) {
		String documentType;
		String documentTitle = null;
		String documentDescription = null;

		String[] fileNames = ((String) args.get("fileName")).split(",");
		String[] types = ((String) args.get("type")).split(",");
		String[] descriptions = ((String) args.get("description")).split(",");

		int itemsCount = Integer.parseInt(args.get(ITEMS_COUNT).toString());
		List<DocumentInstance> documentsToUpload = new ArrayList<>(itemsCount);

		for (int i = 0; i < types.length; i++) {

			try {
				if (descriptions[i] != null) {
					documentDescription = convertEncoding(descriptions[i]);
				}

				if (fileNames[i] != null) {
					documentTitle = convertEncoding(fileNames[i]);
				}
			} catch (UnsupportedEncodingException e1) {
				LOGGER.error("", e1);
			}

			documentType = types[i];
			if (documentType == null) {
				LOGGER.warn("Missing document type on document upload! Skipping document..");
				continue;
			}

			try {
				DocumentInstance uploadedDocument = null;
				if (isSectionInCase(targetInstance)) {
					// we have selected a section that is part of a case on root level so we could
					// create the target document as a part of the case/section
					SectionInstance sectionInstance = (SectionInstance) targetInstance;
					if (documentService.canUploadDocumentInSection(sectionInstance, documentType)) {
						uploadedDocument = documentService.createDocumentInstance(sectionInstance,
								documentType);
					}
					// if not allowed we could try to upload it as any random document
				}

				if (uploadedDocument == null) {
					DocumentDefinitionTemplate template = dictionaryService.getDefinition(
							DocumentDefinitionTemplate.class, documentType);
					if (template == null) {
						// we can't upload a document for a type that is not recognized by the
						// system
						LOGGER.warn("Can't upload a document for a type '" + documentType
								+ "' because is not recognized by the system");
						continue;
					}

					DocumentInstance createInstance = documentService.createInstance(
							new DocumentDefinitionRefProxy(template), targetInstance);
					if (createInstance == null) {
						// something is wrong and we couldn't create the instance
						LOGGER.warn("Failed to create document instance for document with type '"
								+ documentType + "'");
						continue;
					}
					uploadedDocument = createInstance;
					// due to the fact that the file is not part of a case but a separate/standalone
					// it should be marked as such
					uploadedDocument.setStandalone(true);
				}

				uploadedDocument.getProperties().put(DocumentProperties.DESCRIPTION,
						documentDescription);
				uploadedDocument.getProperties().put(DocumentProperties.TYPE, documentType);
				uploadedDocument.getProperties().put(DocumentProperties.TITLE, documentTitle);

				// FIXME for upload new version
				// uploadedDocument.getProperties().put(DocumentProperties.VERSION_DESCRIPTION,
				// null);
				// uploadedDocument.getProperties().put(DocumentProperties.IS_MAJOR_VERSION, false);

				// check max allowed file size server side and skip file if it's bigger
				if (((FileItem) args.get(FILE + i)).getSize() < maxSize) {
					if (!updateDocumentModelOnUpload((FileItem) args.get(FILE + i),
							uploadedDocument, uploadFolder)) {
						LOGGER.warn("Failed to download the file to the server cache!");
						continue;
					}
					// remove any old thumbnail if document version is updated
					// will wait for the new thumbnail to be generated
					uploadedDocument.getProperties().remove(DocumentProperties.THUMBNAIL_IMAGE);

					for (UploadPostProcessor nextProcessor : postProcessors) {
						documentsToUpload.addAll(nextProcessor.proccess(uploadedDocument));
					}
					// fire event that file is to be uploaded
					eventService.fire(new FileUploadedEvent(uploadedDocument, targetInstance));
				}
			} catch (Exception e) {
				LOGGER.error("Failed to upload document due to", e);
			}
		}
		return documentsToUpload;
	}

	/**
	 * Convert encoding from ISO-8859-1 to UTF-8. The method will remove also any extra white spaces
	 * from the beginning ot the end.
	 * 
	 * @param descriptions
	 *            the descriptions
	 * @return the string
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	private String convertEncoding(String descriptions) throws UnsupportedEncodingException {
		String documentDescription;
		byte[] strToBytes = descriptions.getBytes("ISO-8859-1");
		documentDescription = new String(strToBytes, "UTF-8").trim();
		return documentDescription;
	}

	/**
	 * Gets the target instance.
	 * 
	 * @param args
	 *            the args
	 * @return the target instance
	 */
	@SuppressWarnings("unchecked")
	private Instance getTargetInstance(Map<String, Serializable> args) {
		Serializable id = args.get("id");
		Serializable type = args.get("instance");
		if ((type != null) && (id != null)) {
			return loadInstanceInternal(type.toString(), id.toString());
		}
		Serializable serializable = args.get(PATH);
		if (serializable instanceof List) {
			List<Pair<String, String>> path = (List<Pair<String, String>>) serializable;
			if (!path.isEmpty()) {
				Pair<String, String> lastElement = path.get(path.size() - 1);
				return loadInstanceInternal(lastElement.getFirst(), lastElement.getSecond());
			}
		}
		return null;
	}

	/**
	 * Parses the request items.
	 * 
	 * @param items
	 *            the items
	 * @return the map
	 */
	private Map<String, Serializable> parseRequestItems(List<FileItem> items) {
		Map<String, Serializable> map = CollectionUtils.createLinkedHashMap(items.size());
		int itemIndex = 0;
		for (FileItem item : items) {
			if (item.isFormField()) {
				if ("docPath".equals(item.getFieldName())
						&& StringUtils.isNotNullOrEmpty(item.getString())) {
					try {
						JSONObject jsonObj = new JSONObject(item.getString());
						JSONArray jsonArray = JsonUtil.getJsonArray(jsonObj, PATH);
						ArrayList<Pair<String, String>> path = new ArrayList<>(jsonArray.length());
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject object = jsonArray.getJSONObject(i);
							String instanceId = JsonUtil.getStringValue(object, "id");
							String instanceType = JsonUtil.getStringValue(object, TYPE);
							path.add(new Pair<String, String>(instanceType, instanceId));
						}
						map.put(PATH, path);
					} catch (JSONException e) {
						LOGGER.warn("Failed to parse json for docPath: " + item.getString(), e);
					}
				} else {
					if (StringUtils.isNotNullOrEmpty(item.getString())) {
						map.put(item.getFieldName(), item.getString());
					}
				}
			} else {
				map.put(FILE + itemIndex, item);
				itemIndex++;
			}
		}
		map.put(ITEMS_COUNT, Integer.valueOf(itemIndex));
		return map;
	}

	/**
	 * Checks if is section in case.
	 * 
	 * @param targetInstance
	 *            the target instance
	 * @return true, if is section in case
	 */
	private boolean isSectionInCase(Instance targetInstance) {
		return (targetInstance instanceof SectionInstance)
				&& (InstanceUtil.getDirectParent(targetInstance, true) instanceof CaseInstance);
	}

	/**
	 * Method to update the document properties before upload.
	 * 
	 * @param item
	 *            is the bean holding uploaded data
	 * @param documentInstance
	 *            is the currently uploaded instance
	 * @param parentFolder
	 *            is the dir where to store instance
	 * @return <code>true</code> on success
	 * @throws IOException
	 *             if any error occurs
	 */
	private boolean updateDocumentModelOnUpload(FileItem item, DocumentInstance documentInstance,
			File parentFolder) throws IOException {

		// check if created
		if (parentFolder == null) {
			LOGGER.error("\n==================================\n\tDocument will not be uploaded because: No temporary directory"
					+ "\n==================================");
			return false;
		}

		long lenght = item.getSize();
		String name = item.getName();
		String mimetype = item.getContentType();
		byte[] data = item.get();

		String finalFileName = new File(name).getName();
		int lastSeparator = finalFileName.lastIndexOf("\\");
		if (lastSeparator > 0) {
			finalFileName = finalFileName.substring(lastSeparator + 1, finalFileName.length());
		}
		File localFile = null;
		if (finalFileName.getBytes().length > 255) {
			String extension = FileUtil.splitNameAndExtension(finalFileName).getSecond();
			if (StringUtils.isNullOrEmpty(extension)) {
				extension = ".tmp";
			}
			localFile = new File(parentFolder, System.nanoTime() + "." + extension);
		} else {
			localFile = new File(parentFolder, finalFileName);
		}
		BufferedOutputStream output = null;
		try {
			// copy the file now
			output = new BufferedOutputStream(new FileOutputStream(localFile));
			IOUtils.copyLarge(new ByteArrayInputStream(data), output);
			IOUtils.closeQuietly(output);

			Map<String, Serializable> properties = documentInstance.getProperties();

			properties.put(DocumentProperties.FILE_LOCATOR, new LocalProxyFileDescriptor(
					finalFileName, localFile.getAbsolutePath()));

			// No empty title is allowed - CMF-6612
			if (org.apache.commons.lang.StringUtils.isBlank((String) properties
					.get(DocumentProperties.TITLE))) {
				properties.put(DocumentProperties.TITLE, finalFileName);
			}

			properties.put(DocumentProperties.MIMETYPE, mimetype);
			properties.put(DocumentProperties.FILE_SIZE, humanReadableByteCount(lenght));

			return true;
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	/**
	 * Converts bytes into human readable format.
	 * 
	 * @param bytes
	 *            bytes to convert.
	 * @return human readable string.
	 */
	public String humanReadableByteCount(long bytes) {
		int unit = 1024;
		if (bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "kMGTPE".charAt(exp - 1) + "";
		return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
	}

}