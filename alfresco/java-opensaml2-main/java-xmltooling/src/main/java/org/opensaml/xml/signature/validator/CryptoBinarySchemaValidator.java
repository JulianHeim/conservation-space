/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package org.opensaml.xml.signature.validator;

import org.opensaml.xml.schema.validator.XSBase64BinarySchemaValidator;
import org.opensaml.xml.signature.CryptoBinary;

/**
 * Checks {@link org.opensaml.xml.signature.CryptoBinary} for Schema compliance. 
 */
public class CryptoBinarySchemaValidator extends XSBase64BinarySchemaValidator<CryptoBinary> {
    
    /**
     * Constructor.
     *
     */
    public CryptoBinarySchemaValidator() {
        // Don't allow empty content
        super(false);
    }

}
