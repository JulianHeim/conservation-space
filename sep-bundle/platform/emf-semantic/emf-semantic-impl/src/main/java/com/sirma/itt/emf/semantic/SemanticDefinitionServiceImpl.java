package com.sirma.itt.emf.semantic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.definition.SemanticDefinitionService;
import com.sirma.itt.emf.definition.event.LoadSemanticDefinitions;
import com.sirma.itt.emf.instance.model.ClassInstance;
import com.sirma.itt.emf.instance.model.CommonInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.PropertyInstance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.emf.time.TimeTracker;
import com.sirma.itt.semantic.NamespaceRegistryService;
import com.sirma.itt.semantic.search.SemanticQueries;

/**
 * Implementation of SemanticDefinitionsService interface
 *
 * @author kirq4e
 */
@ApplicationScoped
@Singleton
public class SemanticDefinitionServiceImpl implements SemanticDefinitionService {

	/** The logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SemanticDefinitionService.class);

	@Inject
	private SearchService searchService;

	@Inject
	private NamespaceRegistryService namespaceRegistryService;

	private Map<String, ClassInstance> classesCache;
	private Map<String, PropertyInstance> propertiesCache;
	private Map<String, PropertyInstance> relationsCache;

	/**
	 * Initializes
	 */
	@PostConstruct
	public void init() {
		TimeTracker timeTracker = new TimeTracker().begin();
		classesCache = new HashMap<>(250);
		propertiesCache = new HashMap<>(250);
		relationsCache = new HashMap<>(250);

		// fetch classes
		SearchArguments<CommonInstance> filter = searchService.getFilter(
				SemanticQueries.QUERY_CLASSES_TYPES_FOR_SEARCH.getName(), CommonInstance.class,
				null);
		filter.getArguments().put("includeInferred", Boolean.FALSE);
		filter.setPageSize(0);
		searchService.search(CommonInstance.class, filter);

		List<CommonInstance> result = filter.getResult();
		// iterate over classes
		for (CommonInstance instance : result) {
			ClassInstance classInstance = new ClassInstance();
			classInstance.setId(instance.getId());
			classInstance.setProperties(Collections.unmodifiableMap(instance.getProperties()));
			classesCache.put(classInstance.getId().toString(), classInstance);
		}

		for (String key : classesCache.keySet()) {
			ClassInstance classInstance = classesCache.get(key);
			Serializable superClassId = classInstance.getProperties().get("superClass");
			if (superClassId != null) {
				ClassInstance superClass = classesCache.get(superClassId);
				if (superClass != null) {
					classInstance.setOwningInstance(superClass);
					superClass.getSubClasses().put(classInstance.getId().toString(), classInstance);
				}
			}
		}

		// fetch properties
		filter = searchService.getFilter(SemanticQueries.QUERY_DATA_PROPERTIES.getName(),
				CommonInstance.class, null);
		filter.getArguments().put("includeInferred", Boolean.FALSE);
		filter.setPageSize(0);
		searchService.search(CommonInstance.class, filter);
		result = filter.getResult();

		for (Instance instance : result) {
			Map<String, Serializable> properties = instance.getProperties();

			PropertyInstance propertyInstance = new PropertyInstance();
			propertyInstance.setId(instance.getId());
			propertyInstance.setProperties(Collections.unmodifiableMap(properties));
			String domainClass = properties.get("domainClass").toString();
			propertyInstance.setDomainClass(domainClass);
			Serializable rangeClass = properties.get("rangeClass");
			if (rangeClass != null) {
				propertyInstance.setRangeClass(rangeClass.toString());
			}

			ClassInstance domainClassInstance = classesCache.get(domainClass);
			if (domainClassInstance != null) {
				domainClassInstance.getFields().put(propertyInstance.getId().toString(),
						propertyInstance);
			}

			propertiesCache.put(propertyInstance.getId().toString(), propertyInstance);
		}

		// fetch relations
		filter = searchService.getFilter(SemanticQueries.QUERY_RELATION_PROPERTIES.getName(),
				CommonInstance.class, null);
		filter.getArguments().put("includeInferred", Boolean.FALSE);
		filter.setPageSize(0);
		searchService.search(CommonInstance.class, filter);
		result = filter.getResult();

		for (Instance instance : result) {
			Map<String, Serializable> properties = instance.getProperties();

			PropertyInstance relationInstance = new PropertyInstance();
			relationInstance.setId(instance.getId());
			relationInstance.setProperties(Collections.unmodifiableMap(properties));

			Serializable domainClass = properties.get("domainClass");
			if (domainClass != null) {
				relationInstance.setDomainClass(domainClass.toString());
			}
			Serializable rangeClass = properties.get("rangeClass");
			if (rangeClass != null) {
				relationInstance.setRangeClass(rangeClass.toString());
			}

			ClassInstance domainClassInstance = classesCache.get(domainClass);
			if (domainClassInstance != null) {
				domainClassInstance.getRelations().put(relationInstance.getId().toString(),
						relationInstance);
			}

			relationsCache.put(relationInstance.getId().toString(), relationInstance);
		}

		LOGGER.debug("SemanticDefinitionService load took {} s!", timeTracker.stopInSeconds());
	}

	@Override
	public List<ClassInstance> getClasses() {
		return new ArrayList<ClassInstance>(classesCache.values());
	}

	@Override
	public List<String> getHierarchy(String classType) {
		List<String> result = new ArrayList<>();

		ClassInstance classInstance = classesCache.get(classType);
		result.add(classInstance.getId().toString());
		classInstance = (ClassInstance) classInstance.getOwningInstance();

		while (classInstance != null) {
			result.add(0, classInstance.getId().toString());
			classInstance = (ClassInstance) classInstance.getOwningInstance();
		}

		return result;
	}

	@Override
	public List<PropertyInstance> getProperties() {
		return new ArrayList<PropertyInstance>(propertiesCache.values());
	}

	@Override
	public List<PropertyInstance> getProperties(String classType) {
		ClassInstance classInstance = classesCache.get(classType);
		List<PropertyInstance> result = new ArrayList<>();
		while (classInstance != null) {
			result.addAll(classInstance.getFields().values());
			classInstance = (ClassInstance) classInstance.getOwningInstance();
		}
		return result;
	}

	@Override
	public List<PropertyInstance> getRelations() {
		return new ArrayList<PropertyInstance>(relationsCache.values());
	}

	@Override
	public PropertyInstance getRelation(String relationUri) {
		// TODO : Remove conversion to short URI and define short URI in types.xml
		// and avoid using namespaceRegistryService.getShortUri(fromClass) on the following line
		return relationsCache.get(namespaceRegistryService.getShortUri(relationUri));
	}

	@Override
	public List<PropertyInstance> getRelations(String fromClass, String toClass) {
		List<PropertyInstance> result = new ArrayList<>();
		if (StringUtils.isNotNullOrEmpty(fromClass)) {
			// TODO : Remove conversion to short URI and define short URI in types.xml
			// and avoid using namespaceRegistryService.getShortUri(fromClass) on the following line
			ClassInstance classInstance = classesCache.get(namespaceRegistryService
					.getShortUri(fromClass));
			while (classInstance != null) {
				result.addAll(classInstance.getRelations().values());
				classInstance = (ClassInstance) classInstance.getOwningInstance();
			}
		} else {
			result.addAll(relationsCache.values());
		}

		if (StringUtils.isNotNullOrEmpty(toClass)) {
			Iterator<PropertyInstance> iterator = result.iterator();
			while (iterator.hasNext()) {
				PropertyInstance propertyInstance = iterator.next();
				if (!toClass.equals(propertyInstance.getRangeClass())) {
					iterator.remove();
				}
			}
		}
		return result;
	}

	@Override
	public Map<String, PropertyInstance> getRelationsMap() {
		return Collections.unmodifiableMap(relationsCache);
	}

	@Override
	public List<ClassInstance> getSearchableClasses() {
		List<ClassInstance> searchableClasses = new ArrayList<>();
		for (ClassInstance classInstance : classesCache.values()) {
			if (Boolean.TRUE == classInstance.getProperties().get("searchable")) {
				searchableClasses.add(classInstance);
			}
		}
		return searchableClasses;
	}

	@Override
	public ClassInstance getClassInstance(String identifier) {
		return classesCache.get(identifier);
	}

	@Override
	public List<PropertyInstance> getOwnProperties(String classType) {
		ClassInstance classInstance = classesCache.get(classType);
		return new LinkedList<>(classInstance.getFields().values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void observeReloadDefinitionEvent(@Observes LoadSemanticDefinitions event) {
		init();
	}

}
