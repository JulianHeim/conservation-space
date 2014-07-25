package com.sirma.itt.objects.web.object;

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;

import javax.ws.rs.core.Response;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sirma.cmf.web.instance.AttachInstanceAction;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.SectionInstance;
import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.instance.dao.InstanceService;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.EmfUser;
import com.sirma.itt.objects.ObjectsTest;
import com.sirma.itt.objects.domain.model.ObjectInstance;
import com.sirma.itt.objects.services.ObjectService;

/**
 * The Class ObjectRestServiceTest.
 * 
 * @author svelikov
 */
@Test
public class ObjectRestServiceTest extends ObjectsTest {

	private static final String DOCUMENT_INSTANCE_ID = "documentInstanceId";
	private static final String OBJECT_INSTANCE_ID = "objectInstanceId";
	protected static final Object CASE_INSTANCE = "caseinstance";
	protected static final Object SECTION_INSTANCE = "sectioninstance";
	protected static final Object OBJECT_INSTANCE = "objectinstance";
	protected static final Object SECTION_INSTANCE_ID = "emf:482c15b7-845f-4597-a9fb-a6b451a72578";

	private final ObjectRestService controller;

	private InstanceService instanceService;

	private TypeConverter typeConverter;

	private ObjectService objectService;

	private ObjectInstance objectInstance;

	private DocumentInstance createDocumentInstance;

	private CaseInstance caseInstance;

	private SectionInstance sectionInstance;

	private boolean fetchNullInstance;

	private boolean fetchNullCase;

	private boolean fetchNullObject;

	private boolean fetchNullSection;

	private AttachInstanceAction attachInstanceAction;

	/**
	 * Instantiates a new object rest service test.
	 */
	public ObjectRestServiceTest() {
		controller = new ObjectRestService() {

			@Override
			@SuppressWarnings("unchecked")
			public <T extends Instance> T loadInstanceInternal(Class<T> type, Serializable id) {
				if (OBJECT_INSTANCE_ID.equals(id)) {
					return (T) objectInstance;
				} else if (DOCUMENT_INSTANCE_ID.equals(id)) {
					return (T) createDocumentInstance;
				} else if (SECTION_INSTANCE_ID.equals(id)) {
					return (T) sectionInstance;
				}
				return null;
			}

			@Override
			public Instance fetchInstance(String instanceId, String instanceType) {
				if (fetchNullInstance) {
					return null;
				}
				if (CASE_INSTANCE.equals(instanceType) && !fetchNullCase) {
					return caseInstance;
				} else if (SECTION_INSTANCE.equals(instanceType) && !fetchNullSection) {
					return sectionInstance;
				} else if (OBJECT_INSTANCE.equals(instanceType) && !fetchNullObject) {
					return objectInstance;
				}
				return null;
			}

			@Override
			public Resource getCurrentUser() {
				EmfUser user = new EmfUser("admin");
				user.setId("emf:" + user.getIdentifier());
				return user;
			}

		};

		objectInstance = createObjectInstance(Long.valueOf(1));
		objectInstance.setProperties(new HashMap<String, Serializable>());
		createDocumentInstance = createDocumentInstance(Long.valueOf(1));
		caseInstance = createCaseInstance(Long.valueOf(1));
		sectionInstance = createSectionInstance(Long.valueOf(1));

		instanceService = Mockito.mock(InstanceService.class);
		typeConverter = Mockito.mock(TypeConverter.class);
		objectService = Mockito.mock(ObjectService.class);
		attachInstanceAction = Mockito.mock(AttachInstanceAction.class);

		ReflectionUtils.setField(controller, "log", SLF4J_LOG);
		ReflectionUtils.setField(controller, "attachInstanceAction", attachInstanceAction);
		ReflectionUtils.setField(controller, "instanceService", instanceService);
		ReflectionUtils.setField(controller, "typeConverter", typeConverter);
		ReflectionUtils.setField(controller, "objectService", objectService);
	}

	/**
	 * Inits the test by reseting some fields.
	 */
	@BeforeMethod
	public void initTest() {
		fetchNullInstance = false;
		fetchNullCase = false;
		fetchNullSection = false;
		fetchNullObject = false;
	}

	/**
	 * Attach objects test.
	 */
	// FIXME: test
	// public void attachObjectsTest() {
	// // if data is not provided we should get error response
	// Response expectedResponse = Response.status(Response.Status.BAD_REQUEST).entity("").build();
	// Mockito.when(
	// attachInstanceAction.attachObjects(null, controller, null,
	// ObjectActionTypeConstants.ATTACH_OBJECT)).thenReturn(expectedResponse);
	// Response response = controller.attachObjects(null);
	// assertNotNull(response);
	// assertEquals(response.getStatus(), expectedResponse.getStatus());
	// }

	/**
	 * Move object same case test.
	 */
	@SuppressWarnings("boxing")
	public void moveObjectSameCaseTest() {
		// if all required arguments are missing
		Response response = controller.moveObjectSameCase(null, null, null);
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// if source and destination id are missing
		response = controller.moveObjectSameCase("objectid", null, null);
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// if destination id is missing
		response = controller.moveObjectSameCase("objectid", "sourceid", null);
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// if source id is missing
		response = controller.moveObjectSameCase("objectid", null, "destid");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

		// if all required arguments are missing
		response = controller.moveObjectSameCase("", "", "");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// if source and destination id are missing
		response = controller.moveObjectSameCase("objectid", "", "");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// if destination id is missing
		response = controller.moveObjectSameCase("objectid", "sourceid", "");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// if source id is missing
		response = controller.moveObjectSameCase("objectid", "", "destid");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

		// if object instance can't be loaded
		fetchNullObject = true;
		response = controller.moveObjectSameCase("objectid", "sourceid", "destid");
		assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		// if source section instance can't be loaded
		initTest();
		fetchNullSection = true;
		response = controller.moveObjectSameCase("objectid", "sourceid", "destid");
		assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		// if destination section instance can't be loaded
		// TODO: refactor to test this branch

		// if service respond that object can't be moved
		initTest();
		Mockito.when(
				objectService.move(Mockito.any(ObjectInstance.class),
						Mockito.any(SectionInstance.class), Mockito.any(SectionInstance.class)))
				.thenReturn(false);
		response = controller.moveObjectSameCase("objectid", "sourceid", "destid");
		assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

		// if everything is ok
		Mockito.when(
				objectService.move(Mockito.any(ObjectInstance.class),
						Mockito.any(SectionInstance.class), Mockito.any(SectionInstance.class)))
				.thenReturn(true);
		response = controller.moveObjectSameCase("objectid", "sourceid", "destid");
		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
	}

}
