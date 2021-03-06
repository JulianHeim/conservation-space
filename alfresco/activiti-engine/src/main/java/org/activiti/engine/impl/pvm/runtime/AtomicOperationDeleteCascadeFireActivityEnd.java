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

package org.activiti.engine.impl.pvm.runtime;

import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;


// TODO: Auto-generated Javadoc
/**
 * The Class AtomicOperationDeleteCascadeFireActivityEnd.
 *
 * @author Tom Baeyens
 */
public class AtomicOperationDeleteCascadeFireActivityEnd extends AbstractEventAtomicOperation {

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.pvm.runtime.AbstractEventAtomicOperation#getScope(org.activiti.engine.impl.pvm.runtime.InterpretableExecution)
   */
  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();

    if (activity!=null) {
      return activity;
    } else {
      InterpretableExecution parent = (InterpretableExecution) execution.getParent();
      if (parent != null) {
        return getScope((InterpretableExecution) execution.getParent());
      }
      return execution.getProcessDefinition();
    }
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.pvm.runtime.AbstractEventAtomicOperation#getEventName()
   */
  @Override
  protected String getEventName() {
    return org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END;
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.pvm.runtime.AbstractEventAtomicOperation#eventNotificationsCompleted(org.activiti.engine.impl.pvm.runtime.InterpretableExecution)
   */
  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    if ( (execution.isScope())
            && (activity!=null)
            && (!activity.isScope())
          )  {
      execution.setActivity(activity.getParentActivity());
      execution.performOperation(AtomicOperation.DELETE_CASCADE_FIRE_ACTIVITY_END);
      
    } else {
      if (execution.isScope()) {
        execution.destroy();
      }
 
      execution.remove();

      if (!execution.isDeleteRoot()) {
        InterpretableExecution parent = (InterpretableExecution) execution.getParent();
        if (parent!=null) {
          parent.performOperation(AtomicOperation.DELETE_CASCADE);
        }
      }
    }
  }
}
