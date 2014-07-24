package com.sirma.itt.objects.alfresco4.services;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sirma.itt.cmf.alfresco4.AlfrescoCommunicationConstants;
import com.sirma.itt.cmf.alfresco4.AlfrescoErroreReader;
import com.sirma.itt.cmf.alfresco4.services.convert.Converter;
import com.sirma.itt.cmf.alfresco4.services.convert.DMSTypeConverter;
import com.sirma.itt.cmf.constants.CaseProperties;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.domain.model.TopLevelDefinition;
import com.sirma.itt.emf.remote.DMSClientException;
import com.sirma.itt.emf.remote.RESTClient;
import com.sirma.itt.objects.alfresco4.ObjectsServiceURIRegistry;
import com.sirma.itt.objects.alfresco4.services.converter.ObjectConverterContants;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.services.adapters.CMFObjectInstanceAdapterService;

/**
 * The Class ObjectInstanceAlfresco4Service.
 *
 * @author BBonev
 */
public class ObjectInstanceAlfresco4Service implements CMFObjectInstanceAdapterService,
		AlfrescoCommunicationConstants {
	/** The simple date format. */
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss-SSS");
	/** The index of created cases application scoped. */
	private static AtomicLong index = new AtomicLong(0);

	/** The rest client. */
	@Inject
	private RESTClient restClient;

	/** The dictionary service. */
	@Inject
	private DictionaryService dictionaryService;
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -3918037079181596698L;
	/** the logger. */
	private static final Logger LOGGER = Logger.getLogger(ObjectInstanceAlfresco4Service.class);

	/** The Constant DEBUG_ENABLED. */
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	/** The case convertor. */
	@Inject
	@Converter(name = ObjectConverterContants.OBJECT)
	private DMSTypeConverter objectConveter;

	/** The doc convertor. */
	@Inject
	@Converter(name = ObjectConverterContants.DOCUMENT)
	private DMSTypeConverter docConvertor;

	/** The system language. */
	@Inject
	@Config(name = EmfConfigurationProperties.SYSTEM_LANGUAGE, defaultValue = "bg")
	private String systemLanguage;

	/**
	 * Creates the case instance.
	 *
	 * @param objectInstance
	 *            the case instance
	 * @return the string
	 * @throws DMSException
	 *             if case is not saved
	 */
	@Override
	public String createInstance(ObjectInstance objectInstance) throws DMSException {
		TopLevelDefinition definition = (TopLevelDefinition) dictionaryService
				.getInstanceDefinition(objectInstance);
		if (definition == null) {
			throw new DMSException("Object has no valid definition attached!");
		}
		JSONObject request = new JSONObject();
		try {
			request.put(KEY_DEFINITION_ID, definition.getDmsId());
			fixtureCaseNumber(objectInstance);
			Map<String, Serializable> convertProperties = objectConveter.convertCMFtoDMSProperties(
					objectInstance.getProperties(), objectInstance,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
			convertProperties.put("cmf:revision", objectInstance.getRevision());
			if (StringUtils.isNotNullOrEmpty(objectInstance.getContainer())) {
				request.put(KEY_SITE_ID, objectInstance.getContainer());
			}
			request.put(KEY_PROPERTIES, new JSONObject(convertProperties));

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ObjectsServiceURIRegistry.CREATE_OBJECT_SERVICE,
					createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("CREATE For object " + objectInstance.getId() + " result: "
						+ restResult);
			}
			// convert the result and get the id
			if (restResult != null) {
				JSONObject parent = new JSONObject(restResult).getJSONObject(KEY_ROOT_DATA)
						.getJSONObject(KEY_PARENT);
				if (parent != null) {
					return parent.getString(KEY_NODEREF);
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (RuntimeException e) {
			throw new DMSException(e);
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Object " + objectInstance.getId() + " is not saved!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deleteInstance(ObjectInstance caseInstance) throws DMSException {
		return deleteInstance(caseInstance, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deleteInstance(ObjectInstance objectInstance, boolean force) throws DMSException {
		if (objectInstance.getDmsId() == null) {
			throw new DMSException("Invalid object is provided: " + objectInstance.getId());
		}
		JSONObject request = new JSONObject();
		try {
			request.put(KEY_NODEID, objectInstance.getDmsId());
			request.put(KEY_FORCED_OPERATION, Boolean.valueOf(force));

			if (!force) { // add properties for not forced delete
				Map<String, Serializable> convertProperties = objectConveter
						.convertCMFtoDMSProperties(objectInstance.getProperties(), objectInstance,
								DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
				request.put(KEY_PROPERTIES, new JSONObject(convertProperties));
			}

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ObjectsServiceURIRegistry.DELETE_OBJECT_SERVICE,
					createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("DELETE For objectInstance" + objectInstance.getId() + " result: "
						+ restResult);
			}
			// convert the result and get the id
			if (restResult != null) {
				JSONArray nodes = new JSONObject(restResult).getJSONObject(KEY_ROOT_DATA)
						.getJSONArray(KEY_DATA_ITEMS);
				if (nodes.length() == 1) {
					return nodes.getJSONObject(0).getString(KEY_NODEREF);
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Case " + objectInstance.getId() + " is not deleted!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String updateInstance(ObjectInstance caseInstance) throws DMSException {
		if (caseInstance.getDmsId() == null) {
			throw new DMSException("Invalid object is provided: " + caseInstance.getId());
		}
		JSONObject request = new JSONObject();
		try {
			request.put(KEY_NODEID, caseInstance.getDmsId());
			// add the properties
			Map<String, Serializable> convertProperties = objectConveter.convertCMFtoDMSProperties(
					caseInstance.getProperties(), caseInstance,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
			request.put(KEY_PROPERTIES, new JSONObject(convertProperties));

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ObjectsServiceURIRegistry.UPDATE_OBJECT_SERVICE,
					createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("UPDATE For objectInstance" + caseInstance.getId() + " result: "
						+ restResult);
			}
			// convert the result and get the id
			if (restResult != null) {
				JSONArray nodes = new JSONObject(restResult).getJSONObject(KEY_ROOT_DATA)
						.getJSONArray(KEY_DATA_ITEMS);
				if (nodes.length() == 1) {
					return nodes.getJSONObject(0).getString(KEY_NODEREF);
				}
			}
		} catch (DMSClientException e) {
			throw new DMSException(e.getMessage());
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Object " + caseInstance.getId() + " is not updated!");
	}

	/**
	 * TODO remove it <br>
	 * Fixture case number.
	 *
	 * @param caseInstance
	 *            the case instance
	 */
	private void fixtureCaseNumber(ObjectInstance caseInstance) {
		// TODO mock up
		// added atomic index for high concurrent requests to limit the
		// duplicate identifiers
		// REVIEW: this should be replaces by SequenceGeneratorService and the call should be moved
		// to CaseInstanceService
		if (caseInstance.getContentManagementId() == null) {
			caseInstance.setContentManagementId(simpleDateFormat.format(new Date()) + "-"
					+ index.incrementAndGet());
		}
		if (caseInstance.getProperties().get(CaseProperties.UNIQUE_IDENTIFIER) == null) {
			caseInstance.getProperties().put(CaseProperties.UNIQUE_IDENTIFIER,
					caseInstance.getContentManagementId());
		}
	}
}
