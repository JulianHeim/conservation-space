package com.sirma.cmf.web.rest;

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.javacrumbs.jsonunit.JsonAssert;

import org.testng.annotations.Test;

import com.sirma.cmf.CMFTest;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;

/**
 * Test for TooltipsRestService class.
 * 
 * @author svelikov
 */
@Test
public class TooltipsRestServiceTest extends CMFTest {

	private static final String DEFAULT_HEADER_FOR_INSTANCE = "default header for instance";
	private final TooltipsRestServise servise;
	private CaseInstance caseInstance;

	/**
	 * Instantiates a new tooltips rest service test.
	 */
	public TooltipsRestServiceTest() {
		servise = new TooltipsRestServise() {
			@Override
			public Instance fetchInstance(String instanceId, String instanceType) {
				if ((instanceId != null) && (instanceType != null)) {
					return caseInstance;
				}
				return null;
			}
		};

		caseInstance = createCaseInstance(Long.valueOf(1L));
		Map<String, Serializable> properties = new HashMap<>();
		caseInstance.setProperties(properties);
		properties.put(DefaultProperties.HEADER_DEFAULT, DEFAULT_HEADER_FOR_INSTANCE);
	}

	/**
	 * test for getTooltip method.
	 */
	public void getTooltipTest() {
		Response response = servise.getTooltip(null, null, null);
		assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());

		response = servise.getTooltip("instance1", "caseinstance", null);
		assertEquals(response.getStatus(), Status.OK.getStatusCode());
		JsonAssert.assertJsonEquals("{\"tooltip\":\"default header for instance\"}", response
				.getEntity().toString());
	}
}
