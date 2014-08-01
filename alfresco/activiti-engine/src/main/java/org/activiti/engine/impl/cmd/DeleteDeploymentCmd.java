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

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

// TODO: Auto-generated Javadoc
/**
 * The Class DeleteDeploymentCmd.
 *
 * @author Joram Barrez
 */
public class DeleteDeploymentCmd implements Command<Void>, Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The deployment id. */
  protected String deploymentId;
  
  /** The cascade. */
  protected boolean cascade;

  /**
   * Instantiates a new delete deployment cmd.
   *
   * @param deploymentId the deployment id
   * @param cascade the cascade
   */
  public DeleteDeploymentCmd(String deploymentId, boolean cascade) {
    this.deploymentId = deploymentId;
    this.cascade = cascade;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  public Void execute(CommandContext commandContext) {
    if(deploymentId == null) {
      throw new ActivitiException("deploymentId is null");
    }

    commandContext
      .getDeploymentManager()
      .deleteDeployment(deploymentId, cascade);
    
    return null;
  }
}