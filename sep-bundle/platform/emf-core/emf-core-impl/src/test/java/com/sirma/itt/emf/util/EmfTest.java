package com.sirma.itt.emf.util;

import org.testng.annotations.BeforeClass;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.converter.TypeConverterImpl;
import com.sirma.itt.emf.converter.TypeConverterUtil;
import com.sirma.itt.emf.converter.extensions.DefaultTypeConverter;
import com.sirma.itt.emf.converter.extensions.InstanceRefereneToClassConverterProvider;
import com.sirma.itt.emf.db.DefaultDbIdGenerator;
import com.sirma.itt.emf.db.SequenceEntityGenerator;
import com.sirma.itt.emf.resources.ResourceProperties;
import com.sirma.itt.emf.security.context.SecurityContextManager;
import com.sirma.itt.emf.security.model.EmfUser;
import com.sirma.itt.emf.security.model.UserInfo;

/**
 * The Class EmfTest.
 * 
 * @author BBonev
 */
public abstract class EmfTest {

	/**
	 * Sets a current user to be admin before class execution.<br>
	 * Initialize the sequence generator.
	 */
	@BeforeClass
	public void setCurrentUser() {
		EmfUser user = new EmfUser("admin");
		user.setUserInfo(new UserInfo("Admin", "Adminov"));
		user.getUserInfo().put(ResourceProperties.LANGUAGE, "bg");
		SecurityContextManager.authenticateFullyAs(user);

		// initialize the sequence generator
		SequenceEntityGenerator generator = new SequenceEntityGenerator();
		ReflectionUtils.setField(generator, "idGenerator", new DefaultDbIdGenerator());
		generator.initialize();
	}

	/**
	 * Creates the type converter initialized with some default converter.s
	 * 
	 * @return the type converter
	 */
	public TypeConverter createTypeConverter() {
		TypeConverterImpl converter = new TypeConverterImpl();
		new DefaultTypeConverter().register(converter);
		new InstanceRefereneToClassConverterProvider().register(converter);

		// initialize type converter util instance using the created converter
		TypeConverterUtil util = new TypeConverterUtil();
		ReflectionUtils
				.setField(util, "converter", new InstanceProxyMock<TypeConverter>(converter));
		util.initialize();

		return converter;
	}

}
