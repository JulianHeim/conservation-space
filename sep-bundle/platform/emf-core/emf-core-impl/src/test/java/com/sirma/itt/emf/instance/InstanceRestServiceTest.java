package com.sirma.itt.emf.instance;

import static org.testng.Assert.assertEquals;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.converter.TypeConverter;

/**
 * Test for InstanceRestService.
 * 
 * @author svelikov
 */
@Test
public class InstanceRestServiceTest {

	private final InstanceRestService service;
	private final TypeConverter typeConverter;

	/**
	 * Instantiates a new instance rest service test.
	 */
	public InstanceRestServiceTest() {
		service = new InstanceRestService() {
			//
		};

		typeConverter = Mockito.mock(TypeConverter.class);

		ReflectionUtils.setField(service, "typeConverter", typeConverter);
	}

	/**
	 * Detach test.
	 */
	public void detachTest() {
		Response response = service.detach(null);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		String request = "{targetId:'1',targetType:'sectioninstance',linked:[{instanceId:'1',instanceType:'documentinstance'}]}";

		// response = service.detach(request);
	}
}
