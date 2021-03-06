/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.cmd;

import java.io.Serializable;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;


// TODO: Auto-generated Javadoc
/**
 * The Class GetUserInfoCmd.
 *
 * @author Tom Baeyens
 */
public class GetUserInfoCmd implements Command<String>, Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The user id. */
  protected String userId;
  
  /** The key. */
  protected String key;
  
  /**
   * Instantiates a new gets the user info cmd.
   *
   * @param userId the user id
   * @param key the key
   */
  public GetUserInfoCmd(String userId, String key) {
    this.userId = userId;
    this.key = key;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  public String execute(CommandContext commandContext) {
    IdentityInfoEntity identityInfo = commandContext
      .getIdentityInfoManager()
      .findUserInfoByUserIdAndKey(userId, key);

    return (identityInfo!=null ? identityInfo.getValue() : null);
  }
}
