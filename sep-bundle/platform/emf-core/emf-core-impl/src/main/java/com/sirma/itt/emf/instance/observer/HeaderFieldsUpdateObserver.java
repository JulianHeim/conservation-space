package com.sirma.itt.emf.instance.observer;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.definition.DictionaryService;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.evaluation.ExpressionContext;
import com.sirma.itt.emf.evaluation.ExpressionContextProperties;
import com.sirma.itt.emf.evaluation.ExpressionsManager;
import com.sirma.itt.emf.event.TwoPhaseEvent;
import com.sirma.itt.emf.event.instance.AfterInstancePersistEvent;
import com.sirma.itt.emf.event.instance.InstanceChangeEvent;
import com.sirma.itt.emf.instance.dao.InstanceDao;
import com.sirma.itt.emf.instance.dao.ServiceRegister;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.properties.PropertiesService;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.User;
import com.sirma.itt.emf.util.CollectionUtils;

/**
 * Observer that populates the header fields if any
 *
 * @author BBonev
 */
@ApplicationScoped
public class HeaderFieldsUpdateObserver {

	@Inject
	private ExpressionsManager expressionsManager;

	@Inject
	private DictionaryService dictionaryService;

	@Inject
	private LabelProvider labelProvider;

	@Inject
	private PropertiesService propertiesService;

	@Inject
	private ServiceRegister serviceRegister;

	/** The header properties. */
	private static final Set<String> headerProperties = DefaultProperties.DEFAULT_HEADERS;

	/**
	 * The method is called on every change of an instance.
	 *
	 * @param event
	 *            the event
	 */
	public void onInstanceChange(@Observes InstanceChangeEvent<Instance> event) {
		Instance instance = event.getInstance();
		evalInternal(instance, false);
	}

	/**
	 * The method is called on first instance save of an instance.
	 *
	 * @param event
	 *            the event
	 */
	public void onInstancePersist(@Observes AfterInstancePersistEvent<Instance, TwoPhaseEvent> event) {
		// Instance instance = event.getInstance();
		// evalInternal(instance, true);
	}

	/**
	 * This method is called when information about an HeaderFieldsUpdate which was previously
	 * requested using an asynchronous interface becomes available.
	 *
	 * @param instance
	 *            the instance
	 * @param saveProperties
	 *            the save properties
	 */
	private void evalInternal(Instance instance, boolean saveProperties) {
		ExpressionContext context = new ExpressionContext();
		context.put(ExpressionContextProperties.CURRENT_INSTANCE, instance);
		context.put(ExpressionContextProperties.LANGUAGE, getUserLanguage());

		// collect header properties in the map
		Map<String, Serializable> properties = CollectionUtils.createLinkedHashMap(headerProperties.size());
		for (String headerKey : headerProperties) {
			String defaultLabel = evaluateLabel(instance, context, headerKey);
			if (defaultLabel != null) {
				properties.put(headerKey, defaultLabel);
			}
		}
		// add the headers to the instance
		instance.getProperties().putAll(properties);
		if (saveProperties) {
			InstanceDao<Instance> dao = serviceRegister.getInstanceDao(instance);
			if (!propertiesService.isModelSupported(instance) && (dao != null)) {
				dao.saveProperties(instance, false);
			} else {
				// if needed add only the new properties the DB
				propertiesService.saveProperties(instance, instance.getRevision(), instance,
						properties, true);
			}
		}
	}

	/**
	 * Evaluates the label for the given property
	 *
	 * @param instance
	 *            the instance
	 * @param context
	 *            the context
	 * @param property
	 *            the property
	 * @return the label or <code>null</code> if not defined
	 */
	private String evaluateLabel(Instance instance, ExpressionContext context, String property) {
		PropertyDefinition definition = dictionaryService.getProperty(property,
				instance.getRevision(), instance);
		if (definition != null) {
			String labelId = definition.getLabelId();
			if (StringUtils.isNullOrEmpty(labelId)) {
				return null;
			}
			context.put(ExpressionContextProperties.TARGET_FIELD, (Serializable) definition);
			String label = labelProvider.getLabel(labelId, getUserLanguage());
			String value = expressionsManager.evaluateRule(label, String.class, context, instance);
			return value != null ? value : label;
		}
		return null;
	}

	/**
	 * Fetches the user language if present if not tries to fetch the system language.
	 *
	 * @return the user language
	 */
	private String getUserLanguage() {
		User authentication = SecurityContextManager.getFullAuthentication();
		String language = null;
		if (authentication != null) {
			language = authentication.getUserInfo().getLanguage();
		}
		if (language == null) {
			language = ( SecurityContextManager.getAdminUser()).getUserInfo().getLanguage();
		}
		return language;
	}
}
