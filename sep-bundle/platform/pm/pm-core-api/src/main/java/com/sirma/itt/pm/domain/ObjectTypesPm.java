package com.sirma.itt.pm.domain;

import com.sirma.itt.cmf.domain.ObjectTypesCmf;

/**
 * Instance types qualifiers to identify the allowed.
 * {@link com.sirma.itt.emf.instance.dao.InstanceDao} implementations.
 * 
 * @author BBonev
 */
public interface ObjectTypesPm extends ObjectTypesCmf {

	/** The project qualifier. */
	String PROJECT = "project";
}
