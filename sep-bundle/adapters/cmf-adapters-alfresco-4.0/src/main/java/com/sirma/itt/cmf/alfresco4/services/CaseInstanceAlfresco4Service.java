package com.sirma.itt.cmf.alfresco4.services;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sirma.itt.cmf.alfresco4.AlfrescoCommunicationConstants;
import com.sirma.itt.cmf.alfresco4.AlfrescoErroreReader;
import com.sirma.itt.cmf.alfresco4.ServiceURIRegistry;
import com.sirma.itt.cmf.alfresco4.services.convert.Converter;
import com.sirma.itt.cmf.alfresco4.services.convert.ConverterConstants;
import com.sirma.itt.cmf.alfresco4.services.convert.DMSTypeConverter;
import com.sirma.itt.cmf.beans.definitions.CaseDefinition;
import com.sirma.itt.cmf.beans.definitions.SectionDefinition;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.cmf.constants.CaseProperties;
import com.sirma.itt.cmf.constants.CaseProperties.CaseAutomaticProperties;
import com.sirma.itt.cmf.constants.SectionProperties;
import com.sirma.itt.cmf.services.adapter.CMFCaseInstanceAdapterService;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.adapter.DMSException;
import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.configuration.EmfConfigurationProperties;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.domain.model.Node;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.remote.DMSClientException;
import com.sirma.itt.emf.remote.RESTClient;
import com.sirma.itt.emf.resources.ResourceProperties;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.User;

/**
 * The Class CaseInstanceAlfresco4Service.
 */
public class CaseInstanceAlfresco4Service implements CMFCaseInstanceAdapterService,
		AlfrescoCommunicationConstants {

	private static final String KEY_DMS_DESCRIPTION = "cm:description";

	private static final String KEY_DMS_TITLE = "cm:title";

	private static final String KEY_DMS_NAME = "cm:name";

	private static final String REVISION = "cmf:revision";

	private static final long serialVersionUID = -3918037079181596698L;

	private static final Logger LOGGER = Logger.getLogger(CaseInstanceAlfresco4Service.class);

	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss-SSS");

	private static AtomicLong index = new AtomicLong(0);

	@Inject
	private RESTClient restClient;

	@Inject
	private DictionaryService dictionaryService;

	@Inject
	@Converter(name = ConverterConstants.CASE)
	private DMSTypeConverter caseConvertor;

	@Inject
	@Converter(name = ConverterConstants.DOCUMENT)
	private DMSTypeConverter docConvertor;

	@Inject
	@Converter(name = ConverterConstants.SECTION)
	private DMSTypeConverter sectionConvertor;

	@Inject
	@Converter(name = ConverterConstants.TASK)
	private DMSTypeConverter taskConvertor;

	@Inject
	@Config(name = EmfConfigurationProperties.SYSTEM_LANGUAGE, defaultValue = "bg")
	private String systemLanguage;

	@Inject
	private LabelProvider labelProvider;

	/**
	 * Creates the case instance.
	 * 
	 * @param caseInstance
	 *            the case instance
	 * @return the string
	 * @throws DMSException
	 *             if case is not saved
	 */
	@Override
	public String createCaseInstance(CaseInstance caseInstance) throws DMSException {
		CaseDefinition caseDefinition = (CaseDefinition) dictionaryService
				.getInstanceDefinition(caseInstance);
		if (caseDefinition == null) {
			throw new DMSException("Case has no valid definition attached!");
		}

		try {
			JSONObject request = new JSONObject();

			// map tenant to site id
			if (StringUtils.isNotNullOrEmpty(caseInstance.getContainer())) {
				request.put(KEY_SITE_ID, caseInstance.getContainer());
			} else {
				throw new DMSException("Tenant id (container) is not provided");
			}

			request.put(KEY_DEFINITION_ID, caseDefinition.getDmsId());
			fixtureCaseNumber(caseInstance);
			Map<String, Serializable> convertProperties = caseConvertor.convertCMFtoDMSProperties(
					caseInstance.getProperties(), caseInstance,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
			convertProperties.put(REVISION, caseInstance.getRevision());
			List<SectionInstance> sections = caseInstance.getSections();
			request.put(KEY_SECTIONS, addSectionsData(caseDefinition.getSectionDefinitions()));
			request.put(KEY_PROPERTIES, new JSONObject(convertProperties));

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ServiceURIRegistry.CMF_CREATE_CASE_SERVICE,
					createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("CREATE For caseInstance " + caseInstance.getId() + " result: "
						+ restResult);
			}
			// convert the result and get the id
			if (restResult != null) {
				JSONObject rootData = new JSONObject(restResult).getJSONObject(KEY_ROOT_DATA);
				JSONObject parent = rootData.getJSONObject(KEY_PARENT);
				JSONArray sectionsData = rootData.getJSONArray(KEY_DATA_ITEMS);

				for (int i = 0; i < sectionsData.length(); i++) {
					JSONObject currentSection = sectionsData.getJSONObject(i);
					String sectionName = currentSection.getString(KEY_DMS_NAME);
					for (SectionInstance sectionInstance : sections) {
						if (sectionInstance.getIdentifier().equals(sectionName)) {
							String dmsId = currentSection.getString(KEY_NODEREF);
							sectionInstance.getProperties().put(SectionProperties.DMS_ID, dmsId);
							sectionInstance.setDmsId(dmsId);
						}
					}
				}
				if ((parent != null) && (sectionsData.length() == sections.size())) {
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

		throw new DMSException("Case " + caseInstance.getId() + " is not saved!");
	}

	/**
	 * Add section data in request.
	 * 
	 * @param sectionsDefinitions
	 *            the sections definitions
	 * @return the jSON array
	 */
	private JSONArray addSectionsData(List<SectionDefinition> sectionsDefinitions) {
		JSONArray sections = new JSONArray();
		// TODO uncomment after sections are ready

		// do the mapping here as section is special
		// for (SectionDefinition definition : sectionsDefinitions) {
		// Map<String, Serializable> sectionProperties = new HashMap<String, Serializable>(3);
		// sectionProperties.put(DefaultProperties.NAME, definition.getIdentifier());
		// Node node = definition.getChild(DefaultProperties.TITLE);
		// // CMF-1620
		// Serializable title = definition.getIdentifier();
		// String language = getLanguage();
		// if (node instanceof PropertyDefinition) {
		// String labelId = ((PropertyDefinition) node).getLabelId();
		// title = labelProvider.getLabel(labelId, language);
		// }
		// sectionProperties.put(DefaultProperties.TITLE, title);
		//
		// Serializable sectionDescription = null;
		// node = definition.getChild(SectionProperties.DESCRIPTION);
		// if (node instanceof PropertyDefinition) {
		// String labelId = ((PropertyDefinition) node).getLabelId();
		// sectionDescription = labelProvider.getLabel(labelId, language);
		// }
		// if (sectionDescription != null) {
		// sectionProperties.put(DefaultProperties.DESCRIPTION, sectionDescription);
		// }
		// sections.put(new JSONObject(sectionConvertor.convertCMFtoDMSProperties(
		// sectionProperties, DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL)));
		// }
		// return sections;
		// do the mapping here as section is special
		for (SectionDefinition definition : sectionsDefinitions) {
			Map<String, Serializable> sectionProperties = new HashMap<>();
			sectionProperties.put(KEY_DMS_NAME, definition.getIdentifier());
			Node node = definition.getChild(DefaultProperties.TITLE);
			// CMF-1620
			Serializable title = definition.getIdentifier();
			String language = getLanguage();
			if (node instanceof PropertyDefinition) {
				String labelId = ((PropertyDefinition) node).getLabelId();
				title = labelProvider.getLabel(labelId, language);
			}
			sectionProperties.put(KEY_DMS_TITLE, title);

			Serializable sectionDescription = null;
			node = definition.getChild(SectionProperties.DESCRIPTION);
			if (node instanceof PropertyDefinition) {
				String labelId = ((PropertyDefinition) node).getLabelId();
				sectionDescription = labelProvider.getLabel(labelId, language);
			}
			if (sectionDescription != null) {
				sectionProperties.put(KEY_DMS_DESCRIPTION, sectionDescription);
			}
			sections.put(new JSONObject(sectionProperties));
		}
		return sections;
	}

	/**
	 * Gets the language.
	 * 
	 * @return the language
	 */
	private String getLanguage() {
		User user = SecurityContextManager.getFullAuthentication();
		if (user == null) {
			user = SecurityContextManager.getAdminUser();
		}
		String language = (String) user.getUserInfo().get(ResourceProperties.LANGUAGE);
		if (StringUtils.isNullOrEmpty(language)) {
			language = systemLanguage;
		}
		return language;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deleteCaseInstance(CaseInstance caseInstance) throws DMSException {
		return deleteCaseInstance(caseInstance, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public String deleteCaseInstance(CaseInstance caseInstance, boolean force) throws DMSException {
		if (caseInstance.getDmsId() == null) {
			throw new DMSException("Invalid case is provided: " + caseInstance.getId());
		}
	
		try {
			JSONObject request = new JSONObject();
			request.put(KEY_NODEID, caseInstance.getDmsId());
			request.put(KEY_FORCED_OPERATION, Boolean.valueOf(force));

			Map<String, Serializable> autoWorkflowProperties = (Map<String, Serializable>) caseInstance
					.getProperties().remove(CaseAutomaticProperties.AUTOMATIC_ACTIONS_SET);
			if (!force) { // add properties for not forced delete
				Map<String, Serializable> convertProperties = caseConvertor
						.convertCMFtoDMSProperties(caseInstance.getProperties(), caseInstance,
								DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
				request.put(KEY_PROPERTIES, new JSONObject(convertProperties));
			}

			fixActiveWorkflowData(request, autoWorkflowProperties);
			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ServiceURIRegistry.CMF_DELETE_CASE_SERVICE,
					createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("DELETE For caseInstance" + caseInstance.getId() + " result: "
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
		throw new DMSException("Case " + caseInstance.getId() + " is not deleted!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String updateCaseInstance(CaseInstance caseInstance) throws DMSException {
		if (caseInstance.getDmsId() == null) {
			throw new DMSException("Invalid case is provided: " + caseInstance.getId());
		}
		JSONObject request = new JSONObject();
		try {
			request.put(KEY_NODEID, caseInstance.getDmsId());
			// add the properties
			Map<String, Serializable> convertProperties = caseConvertor.convertCMFtoDMSProperties(
					caseInstance.getProperties(), caseInstance,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
			request.put(KEY_PROPERTIES, new JSONObject(convertProperties));

			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request("/cmf/instance/update", createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("UPDATE For caseInstance" + caseInstance.getId() + " result: "
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
			throw new DMSException(e.getMessage(), e);
		} catch (Exception e) {
			throw new DMSException(AlfrescoErroreReader.parse(e));
		}
		throw new DMSException("Case " + caseInstance.getId() + " is not updated!");
	}

	/**
	 * TODO remove it <br>
	 * Fixture case number.
	 * 
	 * @param caseInstance
	 *            the case instance
	 */
	private void fixtureCaseNumber(CaseInstance caseInstance) {
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

	/*
	 * (non-Javadoc)
	 * @see com.sirma.itt.cmf.services.adapter.CMFCaseInstanceAdapterService#
	 * closeCaseInstance(com.sirma.itt.cmf.beans.model.CaseInstance)
	 */
	@Override
	public String closeCaseInstance(CaseInstance caseInstance) throws DMSException {
		if (caseInstance.getDmsId() == null) {
			throw new DMSException("Invalid case is provided: " + caseInstance.getId());
		}

		JSONObject request = new JSONObject();
		try {
			request.put(KEY_NODEID, caseInstance.getDmsId());
			// add the properties
			@SuppressWarnings("unchecked")
			Map<String, Serializable> autoWorkflowProperties = (Map<String, Serializable>) caseInstance
					.getProperties().remove(CaseAutomaticProperties.AUTOMATIC_ACTIONS_SET);
			Map<String, Serializable> convertProperties = caseConvertor.convertCMFtoDMSProperties(
					caseInstance.getProperties(), caseInstance,
					DMSTypeConverter.EDITABLE_HIDDEN_MANDATORY_LEVEL);
			request.put(KEY_PROPERTIES, new JSONObject(convertProperties));
			fixActiveWorkflowData(request, autoWorkflowProperties);
			HttpMethod createMethod = restClient.createMethod(new PostMethod(), request.toString(),
					true);

			String restResult = restClient.request(ServiceURIRegistry.CMF_CLOSE_CASE_SERVICE,
					createMethod);
			if (DEBUG_ENABLED) {
				LOGGER.debug("Close for caseInstance" + caseInstance.getId() + " result: "
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
		throw new DMSException("Case " + caseInstance.getId() + " is not closed!");
	}

	/**
	 * Fix active workflow provided as part of the request. <br>
	 * REVIEW: could we implement something more pretty :) at least not to be here like this
	 * 
	 * @param request
	 *            the request to send
	 * @param autoWorkflowProperties
	 *            the properties provided to send
	 * @throws JSONException
	 *             on parse error.
	 */
	@SuppressWarnings("unchecked")
	private void fixActiveWorkflowData(JSONObject request,
			Map<String, Serializable> autoWorkflowProperties) throws JSONException {
		if ((autoWorkflowProperties != null)
				&& (autoWorkflowProperties
						.get(CaseProperties.CaseAutomaticProperties.ACTIVE_TASKS_PROPS_UPDATE) != null)) {
			Map<String, Serializable> convertCMFtoDMSProperties = taskConvertor
					.convertCMFtoDMSProperties((Map<String, Serializable>) autoWorkflowProperties
							.get(CaseProperties.CaseAutomaticProperties.ACTIVE_TASKS_PROPS_UPDATE),
							DMSTypeConverter.ALLOW_WITH_PREFIX);

			Map<String, Serializable> result = new HashMap<>(2);
			result.put(CaseAutomaticProperties.AUTOMATIC_CANCEL_ACTIVE_WF,
					autoWorkflowProperties.get(CaseAutomaticProperties.AUTOMATIC_CANCEL_ACTIVE_WF));
			result.put(CaseAutomaticProperties.ACTIVE_TASKS_PROPS_UPDATE,
					(Serializable) convertCMFtoDMSProperties);
			request.put(CaseAutomaticProperties.AUTOMATIC_ACTIONS_SET, result);
		}
	}

}
