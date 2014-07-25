/**
 * Copyright (c) 2013 22.07.2013 , Sirma ITT. /* /**
 */
package com.sirma.itt.idoc.web.document;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for {@link IntelligentDocumentRestService}.
 * 
 * @author Adrian Mitev
 */
@Test
public class IntelligentDocumentRestSerivceTest {

	private static IntelligentDocumentRestService idocService;

	/**
	 * Init CUT.
	 */
	@BeforeClass
	public void init() {
		idocService = new IntelligentDocumentRestService();
	}

}
