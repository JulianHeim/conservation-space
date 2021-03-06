package com.sirma.itt.emf.label;

import java.util.Map;

import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.domain.model.Identity;

/**
 * Definition of a single label. The definition has unique identifier used to get the label
 * definition and mapping of properties. The properties keys are the language codes and the value is
 * the label for that language.
 * 
 * @author BBonev
 */
public interface LabelDefinition extends Entity<Long>, Identity {

	/**
	 * Gets the labels mapping. The keys are the supported label languages.
	 * 
	 * @return the labels mapping.
	 */
	Map<String, String> getLabels();
}
