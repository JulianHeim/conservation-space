package com.sirma.itt.objects.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.instance.model.CommonInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.properties.DefaultProperties;
import com.sirma.itt.emf.search.NamedQueries;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.search.SearchService;
import com.sirma.itt.objects.web.menu.ObjectLibraryMenuItem;
import com.sirma.itt.objects.web.menu.SemanticObjectLibrariesMenuProvider;

/**
 * Tests for SemanticObjectLibrariesMenuProvider class.
 * 
 * @author svelikov
 */
@Test
public class SemanticObjectLibrariesMenuProviderTest {

	private static final String CHD_PAINTINGS = "chd:Paintings";
	private static final String PAINTINGS = "Paintings";
	private static final String CHD_BOOKS = "chd:Books";
	private static final String BOOKS = "Books";

	private final SemanticObjectLibrariesMenuProvider menuProvider;

	private final SearchService searchService;

	/**
	 * Instantiates a new semantic object libraries menu provider test.
	 */
	public SemanticObjectLibrariesMenuProviderTest() {
		menuProvider = new SemanticObjectLibrariesMenuProvider();

		searchService = Mockito.mock(SearchService.class);

		ReflectionUtils.setField(menuProvider, "searchService", searchService);
	}

	/**
	 * Test for getObjectLibraryMenuItems.
	 */
	public void getObjectLibraryMenuItemsTest() {
		SearchArguments<Instance> argumentsWrapper = new SearchArguments<>();
		Map<String, Serializable> arguments = new HashMap<String, Serializable>();
		argumentsWrapper.setArguments(arguments);
		List<Instance> result = new ArrayList<>();
		argumentsWrapper.setResult(result);
		Mockito.when(
				searchService.getFilter(NamedQueries.QUERY_CLASSES_PART_OF_OBJECT_LIBRARY,
						Instance.class, null)).thenReturn(argumentsWrapper);
		//
		menuProvider.init();
		List<ObjectLibraryMenuItem> menuItems = menuProvider.getObjectLibraryMenuItems();
		Assert.assertNotNull(menuItems);
		Assert.assertTrue(menuItems.isEmpty());

		//
		result.add(buildSemanticInstanceClass(BOOKS, CHD_BOOKS));
		result.add(buildSemanticInstanceClass(PAINTINGS, CHD_PAINTINGS));
		menuProvider.init();
		menuItems = menuProvider.getObjectLibraryMenuItems();
		Assert.assertNotNull(menuItems);
		Assert.assertTrue(menuItems.size() == 2);
		ObjectLibraryMenuItem menuItem1 = menuItems.get(0);
		Assert.assertEquals(menuItem1.getLabel(), BOOKS);
		Assert.assertEquals(menuItem1.getObjectType(), CHD_BOOKS);
		ObjectLibraryMenuItem menuItem2 = menuItems.get(1);
		Assert.assertEquals(menuItem2.getLabel(), PAINTINGS);
		Assert.assertEquals(menuItem2.getObjectType(), CHD_PAINTINGS);
	}

	/**
	 * Builds the semantic instance class.
	 * 
	 * @param title
	 *            the title
	 * @param id
	 *            the id
	 * @return the instance
	 */
	private Instance buildSemanticInstanceClass(String title, Serializable id) {
		Instance semanticClass = new CommonInstance();
		semanticClass.setId(id);
		Map<String, Serializable> properties = new HashMap<>();
		semanticClass.setProperties(properties);
		properties.put(DefaultProperties.TITLE, title);
		return semanticClass;
	}

}
