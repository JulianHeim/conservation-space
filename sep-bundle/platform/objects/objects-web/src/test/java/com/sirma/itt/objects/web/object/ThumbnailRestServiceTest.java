package com.sirma.itt.objects.web.object;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.Response;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.security.model.EmfUser;
import com.sirma.itt.objects.ObjectsTest;
import com.sirma.itt.objects.domain.model.ObjectInstance;

/**
 * Test for ThumbnailRestService.
 * 
 * @author svelikov
 */
@Test
public class ThumbnailRestServiceTest extends ObjectsTest {

	private static final String OBJECT_INSTANCE_ID = "objectInstanceId";

	private static final String MISSING_OBJECT_ID = "missingObjectId";

	protected static final Object CASE_INSTANCE = "caseinstance";

	protected static final Object OBJECT_INSTANCE = "objectinstance";

	protected static final Object SECTION_INSTANCE_ID = "emf:482c15b7-845f-4597-a9fb-a6b451a72578";

	private final ThumbnailRestService controller;

	private final LinkService linkService;

	private TypeConverter typeConverter;

	private ObjectInstance objectInstance;

	private boolean linkExists;

	/**
	 * Instantiates a new object rest service test.
	 */
	public ThumbnailRestServiceTest() {
		controller = new ThumbnailRestService() {

			@Override
			@SuppressWarnings("unchecked")
			public <T extends Instance> T loadInstanceInternal(Class<T> type, Serializable id) {
				if (OBJECT_INSTANCE_ID.equals(id)) {
					return (T) objectInstance;
				}
				return null;
			}

			@Override
			protected LinkReference linkExists(InstanceReference instance, String linkType) {
				if (linkExists) {
					return new LinkReference();
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

		linkService = Mockito.mock(LinkService.class);
		typeConverter = Mockito.mock(TypeConverter.class);

		ReflectionUtils.setField(controller, "linkService", linkService);
		ReflectionUtils.setField(controller, "typeConverter", typeConverter);
	}

	/**
	 * Adds the thumnail test.
	 */
	public void addThumnailTest() {
		Response response = controller.addThumnail(null);
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

		response = controller.addThumnail("");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
		// TODO: extend test
	}

	/**
	 * Checks if has thumbanil method will return properly if requested object has a primary image
	 * attached.
	 */
	public void hasThumbnailTest() {
		Response response = controller.getThumbnail(null);
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

		response = controller.getThumbnail("");
		assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

		Mockito.when(typeConverter.convert(InstanceReference.class, ObjectInstance.class.getName()))
				.thenReturn(null);
		response = controller.getThumbnail(MISSING_OBJECT_ID);
		assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

		// if requested object already has a primary image
		List<LinkReference> linkReferences = new ArrayList<LinkReference>(1);
		linkReferences.add(new LinkReference());
		linkExists = true;
		Mockito.when(
				linkService.getLinks(Mockito.any(InstanceReference.class), Mockito.anyString()))
				.thenReturn(linkReferences);
		Mockito.when(typeConverter.convert(InstanceReference.class, ObjectInstance.class.getName()))
				.thenReturn(objectInstance.toReference());
		response = controller.getThumbnail(OBJECT_INSTANCE_ID);
		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

		// if requested object has no primary image
		linkReferences.clear();
		linkExists = false;
		Mockito.when(linkService.getLinks(any(InstanceReference.class), anyString())).thenReturn(
				linkReferences);
		response = controller.getThumbnail(OBJECT_INSTANCE_ID);
		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
	}
}
