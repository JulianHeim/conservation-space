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
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.form.StartFormHandler;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;


// TODO: Auto-generated Javadoc
/**
 * The Class GetStartFormCmd.
 *
 * @author Tom Baeyens
 */
public class GetStartFormCmd implements Command<StartFormData>, Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The process definition id. */
  protected String processDefinitionId;

  /**
   * Instantiates a new gets the start form cmd.
   *
   * @param processDefinitionId the process definition id
   */
  public GetStartFormCmd(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  public StartFormData execute(CommandContext commandContext) {
    ProcessDefinitionEntity processDefinition = Context
      .getProcessEngineConfiguration()
      .getDeploymentCache()
      .findDeployedProcessDefinitionById(processDefinitionId);
    if (processDefinition == null) {
      throw new ActivitiException("No process definition found for id '" + processDefinitionId +"'");
    }
    
    StartFormHandler startFormHandler = processDefinition.getStartFormHandler();
    if (startFormHandler == null) {
      throw new ActivitiException("No startFormHandler defined in process '" + processDefinitionId +"'");
    }
    
    
    return startFormHandler.createStartFormData(processDefinition);
  }
}
