/*
 * Based on JUEL 2.2.1 code, 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.activiti.engine.impl.juel;

// TODO: Auto-generated Javadoc
/**
 * The Class AstLiteral.
 */
public abstract class AstLiteral extends AstRightValue {
	
	/* (non-Javadoc)
	 * @see org.activiti.engine.impl.juel.Node#getCardinality()
	 */
	public final int getCardinality() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.activiti.engine.impl.juel.Node#getChild(int)
	 */
	public final AstNode getChild(int i) {
		return null;
	}
}