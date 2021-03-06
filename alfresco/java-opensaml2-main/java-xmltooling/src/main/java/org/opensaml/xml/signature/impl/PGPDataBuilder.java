/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.xml.signature.impl;

import org.opensaml.xml.AbstractXMLObjectBuilder;
import org.opensaml.xml.signature.PGPData;
import org.opensaml.xml.signature.XMLSignatureBuilder;
import org.opensaml.xml.util.XMLConstants;

/**
 * Builder of {@link org.opensaml.xml.signature.PGPData}
 */
public class PGPDataBuilder extends AbstractXMLObjectBuilder<PGPData> implements XMLSignatureBuilder<PGPData> {

    /**
     * Constructor
     *
     */
    public PGPDataBuilder() {
    }

    /** {@inheritDoc} */
    public PGPData buildObject(String namespaceURI, String localName, String namespacePrefix) {
        return new PGPDataImpl(namespaceURI, localName, namespacePrefix);
    }

    /** {@inheritDoc} */
    public PGPData buildObject() {
        return buildObject(XMLConstants.XMLSIG_NS, PGPData.DEFAULT_ELEMENT_LOCAL_NAME, XMLConstants.XMLSIG_PREFIX);
    }

}
