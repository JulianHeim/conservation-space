/**
 *
 */
package com.sirma.itt.cmf.alfresco4.services;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.cmf.alfresco4.AlfrescoCommunicationConstants;
import com.sirma.itt.cmf.alfresco4.AlfrescoErroreReader;
import com.sirma.itt.cmf.alfresco4.ServiceURIRegistry;
import com.sirma.itt.cmf.alfresco4.descriptor.AlfrescoFileAndPropertiesDescriptor;
import com.sirma.itt.cmf.alfresco4.descriptor.AlfrescoFileDescriptor;
import com.sirma.itt.cmf.alfresco4.remote.AlfrescoUploader;
import com.sirma.itt.cmf.alfresco4.services.convert.Converter;
import com.sirma.itt.cmf.alfresco4.services.convert.ConverterConstants;
import com.sirma.itt.cmf.alfresco4.services.convert.DMSTypeConverter;
import com.sirma.itt.cmf.beans.ContentPreviewDescriptor;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.VersionInfo;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.services.adapter.CMFDocumentAdapterService;
import com.sirma.itt.cmf.services.adapter.ThumbnailGenerationMode;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.adapter.DMSFileAndPropertiesDescriptor;
import com.sirma.itt.emf.adapter.DMSFileDescriptor;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.instance.model.DMSInstance;
import com.sirma.itt.emf.remote.DMSClientException;
import com.sirma.itt.emf.remote.RESTClient;
import com.sirma.itt.emf.util.StringEncoder;

/**
 * The class adapter for documents in dms<->cmf communication.
 *
 * @author Borislav Banchev
 */
public class DocumentAlfresco4Service implements CMFDocumentAdapterService,
		AlfrescoCommunicationConstants {

	private static final String PREVIEW_URL = ServiceURIRegistry.CMF_TO_DMS_PROXY_SERVICE
			+ ServiceURIRegistry.CONTENT_ACCESS_URI;
	/** the logger. */
	private static final Logger LOGGER = Logger.getLogger(DocumentAlfresco4Service.class);
	/** The debug enabled. */
	private boolean debugEnabled;
	/** The rest client. */
	@Inject
	private RESTClient restClient;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;
	/** The alfresco uploader. */
	@Inject
	private AlfrescoUploader alfrescoUploader;

	/** The doc convertor. */
	@Inject
	@Converter(name = ConverterConstants.DOCUMENT)
	private DMSTypeConverter docConvertor;

	/**
	 * default contstructor.
	 */
	public DocumentAlfresco4Service() {
		debugEnabled = LOGGER.isDebugEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AlfrescoFileAndPropertiesDescriptor performDocumentOperation(DocumentInstance document,
			DocumentOperation operation) throws DMSException {
		return performDocumentOperation(document, null, operation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DMSFileAndPropertiesDescriptor checkOut(DocumentInstance document, String userId)
			throws DMSException {
		JSONObject object = performOperation(document, userId, DocumentOperation.CHECKOUT);

		String serializable;
		Map<String, Serializable> map;
		try {
			map = docConvertor.convertDMSToCMFProperties(object, document.getRevision(), document,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);

			serializable = object.getString(KEY_NODEREF);
			String container = null;
			if (object.has(KEY_SITE_ID)) {
				container = object.getString(KEY_SITE_ID);
			}

			map.put(DocumentProperties.WORKING_COPY_LOCATION, new AlfrescoFileDescriptor(
					serializable, container, restClient));
			map.put(DocumentProperties.LOCKED_BY, userId);
		} catch (JSONException e) {
			throw new DMSException("Failed to convert properties from the responce", e);
		}

		return new AlfrescoFileAndPropertiesDescriptor(document.getDmsId(), map, restClient);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AlfrescoFileAndPropertiesDescriptor performDocumentOperation(DocumentInstance document,
			String userId, DocumentOperation operation) throws DMSException {
		// perform the operation as specified
		JSONObject object = performOperation(document, userId, operation);
		Map<String, Serializable> dmsToCMFProperties = null;
		try {
			// convert the new data
			dmsToCMFProperties = docConvertor.convertDMSToCMFProperties(object,
					document.getRevision(), document,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
		} catch (JSONException e) {
			throw new DMSException("Failed to convert properties from the responce", e);
		}

		return new AlfrescoFileAndPropertiesDescriptor(document.getDmsId(), dmsToCMFProperties,
				restClient);
	}

	/**
	 * Performs the given operation.
	 *
	 * @param document
	 *            the document
	 * @param userId
	 *            the user id
	 * @param operation
	 *            the operation
	 * @return the jSON object
	 * @throws DMSException
	 *             the dMS exception
	 */
	private JSONObject performOperation(DocumentInstance document, String userId,
			DocumentOperation operation) throws DMSException {
		if ((document == null) || (document.getDmsId() == null)) {
			throw new DMSException("Invalid document is provided");
		}
		if (operation == null) {
			throw new DMSException("Invalid operation for document!");
		}
		String url = null;
		JSONObject request = new JSONObject();
		try {
			Serializable attachmentId = document.getDmsId();
			// fill needed data for each operation
			if (operation == DocumentOperation.CHECKIN) {
				// TODO: add the actual file for check in
				Serializable serializable = document.getProperties().get(
						DocumentProperties.FILE_LOCATOR);
				if (serializable instanceof DMSFileDescriptor) {

				} else {
					throw new DMSException("Invalid or missing file to CHECK IN");
				}
				// as uploading new version
				Serializable temp = document.getProperties().get(
						DocumentProperties.IS_MAJOR_VERSION);
				Boolean isMajor = temp == null ? Boolean.FALSE : Boolean.valueOf(temp.toString());
				temp = document.getProperties().get(DocumentProperties.VERSION_DESCRIPTION);
				String description = temp == null ? "Checked in file" : temp.toString();
				request.put(KEY_DESCRIPTION, description);
				request.put(KEY_MAJOR_VERSION, isMajor);
				url = ServiceURIRegistry.CMF_DOCUMENT_CHECKIN;
				// add the properties
				request.put(KEY_PROPERTIES, docConvertor.convertCMFtoDMSProperties(
						document.getProperties(), document,
						DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL));
			} else if (operation == DocumentOperation.CHECKOUT) {
				url = ServiceURIRegistry.CMF_DOCUMENT_CHECKOUT;
			} else if (operation == DocumentOperation.LOCK) {
				// TODO - may be some other user?
				url = ServiceURIRegistry.CMF_DOCUMENT_LOCK_NODE;
			} else if (operation == DocumentOperation.UNLOCK) {
				url = ServiceURIRegistry.CMF_DOCUMENT_UNLOCK_NODE;
			} else if (operation == DocumentOperation.CANCEL_CHECKOUT) {
				Serializable workingCopyId = document.getProperties().get(
						DocumentProperties.WORKING_COPY_LOCATION);
				if (workingCopyId != null) {
					attachmentId = workingCopyId;
				}
				url = ServiceURIRegistry.CMF_DOCUMENT_CHECKOUT_CANCEL;
			}
			if (userId != null) {

				request.put(KEY_LOCK_OWNER, userId);
			}

			request.put(KEY_ATTACHMENT_ID, attachmentId);
			HttpMethod createdMethod = restClient.createMethod(new PostMethod(),
					request.toString(), true);
			String response = restClient.request(url, createdMethod);
			if (response != null) {
				JSONObject result = new JSONObject(response);
				if (result.has(KEY_ROOT_DATA)) {
					JSONArray nodes = result.getJSONObject(KEY_ROOT_DATA).getJSONArray(
							KEY_DATA_ITEMS);
					if (nodes.length() == 1) {
						// return nodes
						JSONObject jsonObject = nodes.getJSONObject(0);
						return jsonObject;

					}
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException("Failure during execution: " + AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Attachment " + document.getDmsId()
				+ " is not updated during request!");
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFDocumentAdapterService#uploadNewVersion
	 * (com.sirma.itt.cmf.beans.model.DocumentInstance,
	 * com.sirma.itt.cmf.services.adapter.DMSFileDescriptor)
	 */
	@Override
	public DMSFileAndPropertiesDescriptor uploadNewVersion(DocumentInstance document,
			DMSFileAndPropertiesDescriptor descriptor) throws DMSException {
		if ((document == null) || (document.getDmsId() == null)) {
			throw new DMSException("Invalid document is provided");
		}
		// get the file
		String uploadFile = null;
		String dmsId = document.getDmsId();
		// if we are performing checkIn operation
		Serializable workingCopyId = document.getProperties().get(
				DocumentProperties.WORKING_COPY_LOCATION);
		if (workingCopyId != null) {
			dmsId = (String) workingCopyId;
		}

		Map<String, Serializable> propertiesProp = descriptor.getProperties();
		Map<String, Serializable> copyOfData = null;
		if (propertiesProp != null) {
			copyOfData = new HashMap<String, Serializable>(propertiesProp);
		} else {
			copyOfData = new HashMap<>();
		}
		// remove the name as it comes from the upload descriptor
		copyOfData.remove(DocumentProperties.NAME);
		copyOfData = docConvertor.convertCMFtoDMSProperties(copyOfData, document,
				DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
		// remove name as it is automatically maintained
		Set<String> aspectsProp = new TreeSet<String>();
		Serializable temp = document.getProperties().get(DocumentProperties.IS_MAJOR_VERSION);
		Boolean isMajor = temp == null ? Boolean.FALSE : Boolean.valueOf(temp.toString());
		temp = document.getProperties().get(DocumentProperties.VERSION_DESCRIPTION);
		String description = temp == null ? "" : temp.toString();
		if (debugEnabled) {
			LOGGER.debug("New version for " + document.getDmsId() + " isMajor " + isMajor
					+ " properties: " + document.getProperties());
		}
		try {
			ThumbnailGenerationMode thumbnailMode = (ThumbnailGenerationMode) document
					.getProperties().get(DocumentProperties.DOCUMENT_THUMB_MODE);

			uploadFile = alfrescoUploader.updateFile(
					ServiceURIRegistry.CMF_ATTACH_TO_INSTANCE_SERVICE, descriptor, dmsId,
					TYPE_CM_CONTENT, copyOfData, aspectsProp, isMajor, description,
					thumbnailMode != null ? thumbnailMode.toString() : null);

			// upload the file now
			if (uploadFile != null) {
				JSONObject fileData = new JSONObject(uploadFile);
				Map<String, Serializable> dmsToCMFProperties = docConvertor
						.convertDMSToCMFProperties(fileData, document.getRevision(), document,
								DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
				return new AlfrescoFileAndPropertiesDescriptor(fileData.getString(KEY_NODEREF),
						null, dmsToCMFProperties, restClient);
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException("Failed to upload document: " + AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("File is not uploaded!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DMSFileAndPropertiesDescriptor uploadContent(DocumentInstance documentInstance,
			DMSFileAndPropertiesDescriptor descriptor, Set<String> aspectsToInclude)
			throws DMSException {
		if (documentInstance.getDmsId() != null) {
			// REVIEW: why we throw an exception here when we could just call the update method
			throw new DMSException("File is already known in dms! Use update instead!");
		} // get the file

		if (descriptor.getContainerId() == null) {
			// REVIEW: why we throw an exception here when we could just call the update method
			throw new DMSException("The container for uploaded file is not known!");
		}
		String uploadFile = null;
		try {
			String dmsId = descriptor.getContainerId();
			Map<String, Serializable> propertiesProp = null;
			Map<String, Serializable> metadataToInclude = descriptor.getProperties();
			if (metadataToInclude == null) {
				propertiesProp = new HashMap<String, Serializable>(1);
			} else {
				Map<String, Serializable> copyOfData = new HashMap<String, Serializable>(
						metadataToInclude);
				copyOfData.remove(DocumentProperties.NAME);
				propertiesProp = docConvertor.convertCMFtoDMSProperties(copyOfData,
						documentInstance, DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
				// remove name as it is automatically maintained
			}

			Set<String> aspectsProp = new HashSet<String>();
			if (aspectsToInclude != null) {
				for (String nextAspect : aspectsToInclude) {

					Pair<String, Serializable> dmsAspect = docConvertor.convertCMFtoDMSProperty(
							nextAspect, "", DMSTypeConverter.PROPERTIES_MAPPING);
					if (dmsAspect == null) {
						throw new DMSException("Invalid aspect provided (" + nextAspect + ")");
					}
					aspectsProp.add(dmsAspect.getFirst());
				}
			}
			ThumbnailGenerationMode thumbnailMode = (ThumbnailGenerationMode) documentInstance
					.getProperties().get(DocumentProperties.DOCUMENT_THUMB_MODE);
			uploadFile = alfrescoUploader.uploadFile(
					ServiceURIRegistry.CMF_ATTACH_TO_INSTANCE_SERVICE, descriptor, null, null,
					dmsId, TYPE_CM_CONTENT, propertiesProp, aspectsProp,
					thumbnailMode != null ? thumbnailMode.toString() : null);

			// upload the file now
			if (uploadFile != null) {
				JSONObject fileData = new JSONObject(uploadFile);
				Map<String, Serializable> dmsToCMFProperties = docConvertor
						.convertDMStoCMFPropertiesByValue(fileData, documentInstance.getRevision(),
								documentInstance, DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
				return new AlfrescoFileAndPropertiesDescriptor(fileData.getString(KEY_NODEREF),
						null, dmsToCMFProperties, restClient);
			}
		} catch (DMSException e) {
			throw e;
		} catch (Exception e) {
			throw new DMSException("Error during file upload: " + AlfrescoErroreReader.parse(e), e);
		}
		throw new DMSException("File is not uploaded!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deleteAttachment(DocumentInstance documentInstance) throws DMSException {
		if (documentInstance == null) {
			throw new DMSException("Invalid document instance is provided");
		}
		Serializable attachmentId = documentInstance.getDmsId();
		Serializable caseId = documentInstance.getProperties().get(DocumentProperties.CASE_DMS_ID);
		JSONObject request = new JSONObject();
		try {

			request.put(KEY_ATTACHMENT_ID, attachmentId);
			request.put(KEY_NODEID, caseId);
			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);
			String restResult = restClient.request(
					ServiceURIRegistry.CMF_DETTACH_FROM_INSTANCE_SERVICE, createMethod);
			if (restResult != null) {
				JSONObject result = new JSONObject(restResult);
				if (result.has("deleted")) {
					if (attachmentId.equals(result.getString("deleted"))) {
						return attachmentId.toString();
					}
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException("Failure during execution: " + AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Attachment " + documentInstance.getIdentifier()
				+ " is not deleted!");
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFDocumentAdapterService#
	 * getDocumentPreview(com.sirma.itt.cmf.beans.model.DocumentInstance)
	 */
	@Override
	public DMSFileDescriptor getDocumentPreview(DocumentInstance document, String targetMimetype)
			throws DMSException {
		if ((document == null) || (document.getDmsId() == null)) {
			throw new DMSException("Invalid document is provided: "
					+ (document == null ? "null document instance"
							: (document.getDmsId() == null ? "no DMS id" : "unknown")));
		}
		try {
			HttpMethod createdMethod = restClient.createMethod(new GetMethod(), "", true);
			String idPreview = document.getDmsId().replace(":/", "");
			String uri = MessageFormat.format(ServiceURIRegistry.CONTENT_TRANSFORM_SERVICE,
					idPreview, targetMimetype);
			String response = restClient.request(uri, createdMethod);
			if ((response != null) && (createdMethod.getStatusCode() == 200)) {
				JSONObject result = new JSONObject(response);
				if (result.has(KEY_NODEID)) {
					String idParts = result.getString(KEY_NODEID).replace(":/", "");
					if (debugEnabled) {
						LOGGER.debug("Generating uri content for " + result.getString(KEY_NODEID));
					}
					return new ContentPreviewDescriptor(MessageFormat.format(PREVIEW_URL, idParts),
							targetMimetype);
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException("Requested document: " + document.getIdentifier()
					+ " is not transformed: " + AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Requested document: " + document.getIdentifier()
				+ " is not transformed");
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFDocumentAdapterService#
	 * performDocumentInfoOperation (com.sirma.itt.cmf.beans.model.DocumentInstance, java.util.Set)
	 */
	@Override
	public Map<DocumentInfoOperation, Serializable> performDocumentInfoOperation(
			DocumentInstance document, Set<DocumentInfoOperation> operations) throws DMSException {
		if ((document == null) || (document.getDmsId() == null)) {
			throw new DMSException("Invalid document is provided: "
					+ (document == null ? "null document instance"
							: (document.getDmsId() == null ? "no DMS id" : "unknown")));
		}
		if (operations == null) {
			throw new DMSException("Invalid operations are provided for "
					+ document.getIdentifier());
		}
		try {
			Map<DocumentInfoOperation, Serializable> resultData = new HashMap<DocumentInfoOperation, Serializable>();
			for (DocumentInfoOperation documentInfoOperation : operations) {
				switch (documentInfoOperation) {
					case DOCUMENT_VERSIONS: {
						HttpMethod createdMethod = restClient.createMethod(new GetMethod(), "",
								true);
						String response = restClient.request(
								ServiceURIRegistry.CMF_DOCUMENT_VERSION_BY_NODE
										+ document.getDmsId(), createdMethod);
						if (response != null) {
							ArrayList<VersionInfo> versions = new ArrayList<VersionInfo>();
							JSONArray result = new JSONArray(response);
							for (int i = 0; i < result.length(); i++) {
								JSONObject versionInfo = result.getJSONObject(i);
								VersionInfo version = new VersionInfo(document,
										versionInfo.getString(KEY_LABEL),
										versionInfo.getString(KEY_NAME), typeConverter.convert(
												Date.class,
												versionInfo.getString(KEY_CREATED_DATE_ISO)),
										versionInfo.getJSONObject(KEY_CREATOR).getString(
												KEY_USER_NAME),
										versionInfo.getString(KEY_DESCRIPTION));
								versions.add(version);
							}
							resultData.put(documentInfoOperation, versions);
						}
						break;
					}
					case DOCUMENT_INFO:
						break;
					case DOCUMENT_WORKFLOWS:
						break;
					default:
						break;
				}
			}
			return resultData;
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException("Requested document: " + document.getIdentifier()
					+ " is not processed: " + AlfrescoErroreReader.parse(e));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFDocumentAdapterService#
	 * getDocumentVersion(com.sirma.itt.cmf.beans.model.DocumentInstance, java.lang.String)
	 */
	@Override
	public DocumentInstance getDocumentVersion(DocumentInstance documentInstance, String version)
			throws DMSException {
		Map<String, Serializable> dmsToCMFProperties = versionOperation(documentInstance, version,
				ServiceURIRegistry.CMF_DOCUMENT_HISTORIC_VERSION);
		DocumentInstance clone = documentInstance.clone();
		// the location to set
		clone.setDmsId((String) dmsToCMFProperties.get(DocumentProperties.ATTACHMENT_LOCATION));
		clone.getProperties().putAll(dmsToCMFProperties);
		return clone;
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFDocumentAdapterService#
	 * getDocumentVersion(com.sirma.itt.cmf.beans.model.DocumentInstance, java.lang.String)
	 */
	@Override
	public DocumentInstance revertVersion(DocumentInstance documentInstance, String version)
			throws DMSException {
		Map<String, Serializable> dmsToCMFProperties = versionOperation(documentInstance, version,
				ServiceURIRegistry.CMF_DOCUMENT_REVERT_VERSION);
		dmsToCMFProperties.remove(KEY_NODEREF);
		documentInstance.getProperties().putAll(dmsToCMFProperties);
		return documentInstance;
	}

	/**
	 * Version operation.
	 *
	 * @param documentInstance
	 *            the document instance
	 * @param version
	 *            the version
	 * @param service
	 *            the service
	 * @return the map
	 * @throws DMSException
	 *             the dMS exception
	 */
	private Map<String, Serializable> versionOperation(DocumentInstance documentInstance,
			String version, String service) throws DMSException {
		if ((documentInstance == null) || (documentInstance.getDmsId() == null)) {
			throw new DMSException("Invalid document is provided: "
					+ (documentInstance == null ? "null document instance"
							: (documentInstance.getDmsId() == null ? "no DMS id" : "unknown")));
		}
		Serializable attachmentId = documentInstance.getDmsId();
		JSONObject request = new JSONObject();
		try {

			request.put(KEY_ATTACHMENT_ID, attachmentId);
			request.put(KEY_VERSION, version);
			// update the revert data if revert is pressed
			if (ServiceURIRegistry.CMF_DOCUMENT_REVERT_VERSION.equals(service)) {
				Serializable temp = documentInstance.getProperties().get(
						DocumentProperties.IS_MAJOR_VERSION);
				Boolean isMajor = temp == null ? Boolean.FALSE : Boolean.valueOf(temp.toString());
				temp = documentInstance.getProperties().get(DocumentProperties.VERSION_DESCRIPTION);
				String description = temp == null ? "" : temp.toString();
				request.put(KEY_MAJOR_VERSION, isMajor);
				request.put(KEY_DESCRIPTION, description);
			}
			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);
			String restResult = restClient.request(service, createMethod);
			if (restResult != null) {
				JSONObject result = new JSONObject(restResult);
				if (result.has(KEY_ROOT_DATA)) {
					JSONArray nodes = result.getJSONObject(KEY_ROOT_DATA).getJSONArray(
							KEY_DATA_ITEMS);
					if (nodes.length() == 1) {
						// return metadata
						Map<String, Serializable> dmsToCMFProperties = docConvertor
								.convertDMSToCMFProperties(
										nodes.getJSONObject(0).getJSONObject(KEY_PROPERTIES),
										DMSTypeConverter.DOCUMENT_LEVEL);
						// dmsToCMFProperties.put(KEY_NODEREF,
						// jsonObject.getString(KEY_NODEREF));
						return dmsToCMFProperties;
					}
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException("Failure during execution: " + AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Attachment " + documentInstance.getIdentifier()
				+ " is not deleted!");
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFCaseInstanceAdapterService#moveDocument
	 * (com.sirma.itt.cmf.beans.model.DocumentInstance,
	 * com.sirma.itt.cmf.beans.model.SectionInstance)
	 */
	@Override
	public AlfrescoFileAndPropertiesDescriptor moveDocument(DocumentInstance documentInstance,
			DMSInstance target) throws DMSException {
		validateExistingDMSInstance(target);
		JSONObject request = new JSONObject();
		try {
			request.put(KEY_ATTACHMENT_ID, documentInstance.getDmsId());
			request.put(KEY_DESTINATION, target.getDmsId());

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ServiceURIRegistry.CMF_DOCUMENT_MOVE,
					createMethod);
			if (debugEnabled) {
				LOGGER.debug("Move for documentInstance" + documentInstance.getId() + " result: "
						+ restResult);
			}
			// convert the result and get the id
			if (restResult != null) {
				JSONArray nodes = new JSONObject(restResult).getJSONObject(KEY_ROOT_DATA)
						.getJSONArray(KEY_DATA_ITEMS);
				if (nodes.length() == 1) {
					Map<String, Serializable> convertDMSToCMFProperties = docConvertor
							.convertDMSToCMFProperties(
									nodes.getJSONObject(0).getJSONObject(KEY_PROPERTIES),
									DMSTypeConverter.DOCUMENT_LEVEL);
					return new AlfrescoFileAndPropertiesDescriptor(documentInstance.getDmsId(),
							convertDMSToCMFProperties, restClient);
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Document " + documentInstance.getId() + " is not moved!");
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFCaseInstanceAdapterService#copyDocument
	 * (com.sirma.itt.cmf.beans.model.DocumentInstance,
	 * com.sirma.itt.cmf.beans.model.SectionInstance, java.lang.String)
	 */
	@Override
	public AlfrescoFileAndPropertiesDescriptor copyDocument(DocumentInstance documentInstance,
			DMSInstance target, String newDocumentName) throws DMSException {
		validateExistingDMSInstance(documentInstance);
		validateExistingDMSInstance(target);
		// Instance targetInstance = (Instance) target;
		// if ((targetInstance == null)
		// || (targetInstance.getProperties().get(DocumentProperties.SECTION_LOCATION) == null)) {
		// throw new DMSException("Invalid target section is provided");
		// }
		JSONObject request = new JSONObject();
		try {
			request.put(KEY_ATTACHMENT_ID, documentInstance.getDmsId());
			request.put(KEY_DESTINATION, target.getDmsId());
			request.put(KEY_NAME, newDocumentName);
			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ServiceURIRegistry.CMF_DOCUMENT_COPY,
					createMethod);
			if (debugEnabled) {
				LOGGER.debug("Copy for documentInstance" + documentInstance.getId() + " result: "
						+ restResult);
			}
			// convert the result and get the id
			if (restResult != null) {
				JSONArray nodes = new JSONObject(restResult).getJSONObject(KEY_ROOT_DATA)
						.getJSONArray(KEY_DATA_ITEMS);
				if (nodes.length() == 1) {
					Map<String, Serializable> convertDMSToCMFProperties = docConvertor
							.convertDMSToCMFProperties(
									nodes.getJSONObject(0).getJSONObject(KEY_PROPERTIES),
									DMSTypeConverter.DOCUMENT_LEVEL);
					return new AlfrescoFileAndPropertiesDescriptor(documentInstance.getDmsId(),
							convertDMSToCMFProperties, restClient);
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Document " + documentInstance.getId() + " is not copied!");
	}

	/**
	 * Internal method to validate dmsinstance.
	 *
	 * @param instance
	 *            is the instance to check
	 * @return true if this is valid dms instance
	 * @throws DMSException
	 *             if this is not a valid dms instance
	 */
	private boolean validateExistingDMSInstance(final DMSInstance instance) throws DMSException {
		if (instance == null || instance.getDmsId() == null) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Invalid ");
			exceptionMsg.append(instance != null ? instance.getClass().getSimpleName()
					: " instance");
			exceptionMsg.append(" is provided: ");
			exceptionMsg.append((instance == null ? "null"
					: (instance.getDmsId() == null ? "no DMS id" : "unknown")));
			throw new DMSException(exceptionMsg.toString());
		}
		return true;
	}

	@Override
	public String getDocumentDirectAccessURI(DocumentInstance instance) throws DMSException {
		if (instance == null || instance.getDmsId() == null) {
			return null;
		}
		try {
			String uri = null;
			String filename = (String) instance.getProperties().get(DocumentProperties.NAME);
			String filenameEncoded = null;
			if (StringUtils.isNotBlank(filename)) {
				try {
					filenameEncoded = StringEncoder.encode(filename, "UTF-8");
				} catch (Exception e) {
					LOGGER.error("Filename could not be encoded!", e);
				}
			}
			if (filenameEncoded == null) {
				// TODO find extension by mimetype
				filenameEncoded = "file";
			}
			uri = MessageFormat.format(ServiceURIRegistry.CMF_DOCUMENT_ACCESS_URI, instance
					.getDmsId().replace(":/", ""), filenameEncoded, "true");
			return uri;
		} catch (Exception e) {
			throw new DMSException(e);
		}
	}
}
