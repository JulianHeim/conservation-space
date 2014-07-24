/**
 *
 */
package com.sirma.itt.cmf.test;

import com.sirma.itt.cmf.test.mock.MockupProvider;
import com.sirma.itt.cmf.test.mock.PmMockupProvider;

/**
 * Base remote client test for alfresco. All common methods should be placed here
 *
 * @author borislav banchev
 */
public class PmBaseAlfrescoTest extends BaseAlfrescoTest {


	@Override
	protected MockupProvider createMockupProvider() {
		return new PmMockupProvider(httpClient);
	}

	protected PmMockupProvider getMockupProvider() {
		return (PmMockupProvider) mockupProvider;
	}
}
