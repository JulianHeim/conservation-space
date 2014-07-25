package com.sirma.itt.pm.util.serialization;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.emf.serialization.kryo.KryoInitializer;
import com.sirma.itt.pm.domain.definitions.impl.ProjectDefinitionImpl;

/**
 * The initialization extension to register project definition classes to Kryo library.
 * 
 * @author BBonev
 */
@Extension(target = KryoInitializer.TARGET_NAME, order = 200)
public class PmKryoInitializer implements KryoInitializer {

	/** The Constant CLASS_REGISTER. */
	private static final List<Pair<Class<?>, Integer>> CLASS_REGISTER = Collections
			.unmodifiableList(Arrays.asList(new Pair<Class<?>, Integer>(
					ProjectDefinitionImpl.class, 201)));

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Pair<Class<?>, Integer>> getClassesToRegister() {
		return CLASS_REGISTER;
	}

}
