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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.util.ClockUtil;


// TODO: Auto-generated Javadoc
/**
 * The Class AcquireJobsCmd.
 *
 * @author Nick Burch
 * @author Daniel Meyer
 */
public class AcquireJobsCmd implements Command<AcquiredJobs> {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  /** The job executor. */
  private final JobExecutor jobExecutor;

  /**
   * Instantiates a new acquire jobs cmd.
   *
   * @param jobExecutor the job executor
   */
  public AcquireJobsCmd(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
  }
  
  /* (non-Javadoc)
   * @see org.activiti.engine.impl.interceptor.Command#execute(org.activiti.engine.impl.interceptor.CommandContext)
   */
  public AcquiredJobs execute(CommandContext commandContext) {
    
    String lockOwner = jobExecutor.getLockOwner();
    int lockTimeInMillis = jobExecutor.getLockTimeInMillis();
    int maxJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();    
    int jobsInThisAcquisition = 0;
    
    AcquiredJobs acquiredJobs = new AcquiredJobs();
    List<JobEntity> jobs = commandContext
      .getJobManager()
      .findNextJobsToExecute(new Page(0, maxJobsPerAcquisition));
        
    for (JobEntity job: jobs) {
      List<String> jobIds = new ArrayList<String>();

      if (job != null && !acquiredJobs.contains(job.getId())) {     
        if (job.isExclusive() && job.getProcessInstanceId() != null) {
          // acquire all exclusive jobs in the same process instance
          // (includes the current job)
          List<JobEntity> exclusiveJobs = commandContext.getJobManager()
            .findExclusiveJobsToExecute(job.getProcessInstanceId());
          for (JobEntity exclusiveJob : exclusiveJobs) {   
            if(exclusiveJob != null) {
              lockJob(exclusiveJob, lockOwner, lockTimeInMillis);
              jobIds.add(exclusiveJob.getId());
            }
          }
        } else {
          lockJob(job, lockOwner, lockTimeInMillis);
          jobIds.add(job.getId());        
        }
        
      } 

      acquiredJobs.addJobIdBatch(jobIds);
      
      jobsInThisAcquisition += jobIds.size();
      if(jobsInThisAcquisition >= maxJobsPerAcquisition) {
        break;
      }      
    }
    
    return acquiredJobs;
  }

  /**
   * Lock job.
   *
   * @param job the job
   * @param lockOwner the lock owner
   * @param lockTimeInMillis the lock time in millis
   */
  protected void lockJob(JobEntity job, String lockOwner, int lockTimeInMillis) {    
    job.setLockOwner(lockOwner);
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(ClockUtil.getCurrentTime());
    gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
    job.setLockExpirationTime(gregorianCalendar.getTime());    
  }
}
