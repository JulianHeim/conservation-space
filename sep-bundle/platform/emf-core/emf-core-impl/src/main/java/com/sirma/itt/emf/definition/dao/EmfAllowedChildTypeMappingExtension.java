package com.sirma.itt.emf.definition.dao;

import java.util.Map;

import com.sirma.itt.emf.definition.model.GenericDefinition;
import com.sirma.itt.emf.domain.ObjectTypes;
import com.sirma.itt.emf.domain.model.DefinitionModel;
import com.sirma.itt.emf.forum.model.CommentInstance;
import com.sirma.itt.emf.forum.model.ImageAnnotation;
import com.sirma.itt.emf.forum.model.TopicInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.link.LinkInstance;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.resources.model.EmfResource;
import com.sirma.itt.emf.security.model.EmfGroup;
import com.sirma.itt.emf.security.model.EmfUser;
import com.sirma.itt.emf.util.CollectionUtils;

/**
 * Extension point to register the EMF class types to the allowed children types.
 * 
 * @author BBonev
 */
@Extension(target = AllowedChildTypeMappingExtension.TARGET_NAME, order = 5)
public class EmfAllowedChildTypeMappingExtension implements AllowedChildTypeMappingExtension {

	/** The Constant definitionMapping. */
	private static final Map<String, Class<? extends DefinitionModel>> definitionMapping;
	/** The Constant instanceMapping. */
	private static final Map<String, Class<? extends Instance>> instanceMapping;
	/** The Constant typeMapping. */
	private static final Map<String, String> typeMapping;

	static {
		definitionMapping = CollectionUtils.createHashMap(10);
		definitionMapping.put(ObjectTypes.COMMENT, GenericDefinition.class);
		definitionMapping.put(ObjectTypes.LINK, GenericDefinition.class);
		definitionMapping.put(ObjectTypes.TOPIC, GenericDefinition.class);
		definitionMapping.put(ObjectTypes.USER, GenericDefinition.class);
		definitionMapping.put(ObjectTypes.GROUP, GenericDefinition.class);
		definitionMapping.put(ObjectTypes.RESOURCE, GenericDefinition.class);
		definitionMapping.put(ObjectTypes.IMAGE_ANNOTATION, GenericDefinition.class);

		instanceMapping = CollectionUtils.createHashMap(10);
		instanceMapping.put(ObjectTypes.COMMENT, CommentInstance.class);
		instanceMapping.put(ObjectTypes.LINK, LinkInstance.class);
		instanceMapping.put(ObjectTypes.TOPIC, TopicInstance.class);
		instanceMapping.put(ObjectTypes.USER, EmfUser.class);
		instanceMapping.put(ObjectTypes.GROUP, EmfGroup.class);
		instanceMapping.put(ObjectTypes.RESOURCE, EmfResource.class);
		instanceMapping.put(ObjectTypes.IMAGE_ANNOTATION, ImageAnnotation.class);

		typeMapping = CollectionUtils.createHashMap(10);
		typeMapping.put(ObjectTypes.COMMENT, "commentInstance");
		typeMapping.put(ObjectTypes.LINK, "linkInstance");
		typeMapping.put(ObjectTypes.TOPIC, "topicInstance");
		typeMapping.put(ObjectTypes.USER, "user");
		typeMapping.put(ObjectTypes.GROUP, "group");
		typeMapping.put(ObjectTypes.RESOURCE, "resource");
		typeMapping.put(ObjectTypes.IMAGE_ANNOTATION, "imageAnnotation");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Class<? extends DefinitionModel>> getDefinitionMapping() {
		return definitionMapping;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Class<? extends Instance>> getInstanceMapping() {
		return instanceMapping;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getTypeMapping() {
		return typeMapping;
	}


}
