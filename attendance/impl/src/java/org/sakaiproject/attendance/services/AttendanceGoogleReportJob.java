/**********************************************************************************
 *
 * Copyright (c) 2018 The Sakai Foundation
 *
 * Original developers:
 *
 *   New York University
 *   Payten Giles
 *   Mark Triggs
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.attendance.services;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.UsageSession;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

import java.util.Date;

@Slf4j
@DisallowConcurrentExecution
public class AttendanceGoogleReportJob implements Job {
            private   String serverName   = "N/A";
    final   private   String jobName      = "AttendanceGoogleReportJob";
    final   private   String jobError     = "attend.googlereport.job.error"; // FIXME translation for this?

    public AttendanceGoogleReportJob() {
        super();
    }

    public void init() {
        log.info(jobName + " init.");
        serverName = ServerConfigurationService.getServerName();
    }

    public void destroy() {
        log.info(jobName +"  D E S T R O Y E D.");
    }

    public void execute(JobExecutionContext context) {
        log.debug(jobName + "execute()");
        loginToSakai("admin", jobName, jobError);

        String jobName = context.getJobDetail().getKey().getName();
        String triggerName = context.getTrigger().getKey().getName();
        Date requestedFire = context.getScheduledFireTime();
        Date actualFire = context.getFireTime();

        StringBuffer whoAmI = new StringBuffer(jobName + " $");
        whoAmI.append(" Job: ");
        whoAmI.append(jobName);
        whoAmI.append(" Trigger: ");
        whoAmI.append(triggerName);

        if (requestedFire != null) {
            whoAmI.append(" Fire scheduled: ");
            whoAmI.append(requestedFire.toString());
        }

        if (actualFire != null) {
            whoAmI.append(" Fire actual: ");
            whoAmI.append(actualFire.toString());
        }

        log.info("Start Job: " + whoAmI.toString());

        new AttendanceGoogleReportExport().export();

        logoutFromSakai();

    }

    public void loginToSakai(String whoAs, String jobName, String jobError) {
        log.debug(jobName + " loginToSakai()");

        UsageSession session = usageSessionService.startSession(whoAs, serverName, jobName);
        if (session == null) {
            eventTrackingService.post(eventTrackingService.newEvent(jobError, whoAs + " unable to log into " + serverName, true));
            return;
        }

        Session sakaiSession = sessionManager.getCurrentSession();
        sakaiSession.setUserId(whoAs);
        sakaiSession.setUserEid(whoAs);

        authzGroupService.refreshUser(whoAs);
    }

    public void logoutFromSakai() {
        log.debug(jobName + " Logging out of Sakai on " + serverName);
        usageSessionService.logout(); // safe to logout? what if other jobs are running?
    }

    static public String safeEventLength(final String target)
    {
        return (target.length() > 255 ? target.substring(0, 255) : target);
    }

    @Setter
    private AttendanceStatCalc attendanceStatCalc;

    @Setter
    private EventTrackingService eventTrackingService;

    @Setter
    private UsageSessionService usageSessionService;

    @Setter
    private AuthzGroupService authzGroupService;

    @Setter
    private SessionManager sessionManager;
}
