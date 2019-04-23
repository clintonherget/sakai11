/**********************************************************************************
 *
 * Copyright (c) 2015 The Sakai Foundation
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

package org.sakaiproject.content.googledrive.handlers;

import com.google.api.services.drive.Drive;
import edu.nyu.classes.groupersync.api.AddressFormatter;
import edu.nyu.classes.groupersync.api.GrouperSyncService;
import edu.nyu.classes.groupersync.api.GroupInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.googledrive.GoogleClient;
import org.sakaiproject.content.googledrive.google.FileImport;
import org.sakaiproject.content.googledrive.google.Permissions;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;


public class CreateGoogleItemHandler implements Handler {

    private String redirectTo = null;
    private GrouperSyncService grouper = null;

    public CreateGoogleItemHandler() {
        grouper = (GrouperSyncService) ComponentManager.get("edu.nyu.classes.groupersync.api.GrouperSyncService");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            GoogleClient google = new GoogleClient();
            Drive drive = google.getDrive((String) context.get("googleUser"));

            List<String> fileIds = Arrays.asList(request.getParameterValues("googleitemid[]"));
            String[] sakaiGroupIds = request.getParameterValues("sakaiGroupId[]"); // FIXME look up google group ids again
            String collectionId = request.getParameter("collectionId");
            String notify = request.getParameter("notify");
            String role = request.getParameter("role");

            GoogleClient.LimitedBatchRequest batch = google.getBatch(drive);

            ContentHostingService chs = (ContentHostingService) ComponentManager.get("org.sakaiproject.content.api.ContentHostingService");
            String siteId = (String) context.get("siteId");

            int notificationSetting = NotificationService.NOTI_NONE;
            if ("r".equals(notify)) {
                notificationSetting = NotificationService.NOTI_REQUIRED;
            } else if ("o".equals(notify)) {
                notificationSetting = NotificationService.NOTI_OPTIONAL;
            }

            Site site = SiteService.getSite(siteId);
            List<AuthzGroup> sakaiGroups = new ArrayList<>();
            List<String> googleGroupIds = new ArrayList<>();
            if (sakaiGroupIds != null) {
                for (String groupId : sakaiGroupIds) {
                    GroupInfo googleGroupInfo = grouper.getGroupInfo(groupId);
                    if (googleGroupInfo != null && googleGroupInfo.isReadyForUse()) {
                        if (groupId.equals(site.getId())) {
                            // Whole site group
                            sakaiGroups.add(site);
                        } else {
                            sakaiGroups.add(site.getGroup(groupId));
                        }
                        googleGroupIds.add(ensureCorrectDomain(AddressFormatter.format(googleGroupInfo.getGrouperId())));
                    }
                }
            }

            // FIXME: Spamming this for testing purposes
            googleGroupIds.clear();
            googleGroupIds.add("mst-resources-tool-test-group@gqa.nyu.edu");

            // Set permissions on the Google side for the selected files.  We'll
            // do this first because there's no point continuing with the import
            // if the permissions aren't there.
            Map<String, List<String>> fileIdToPermissionIdMap =
                new Permissions(google, drive).applyPermissions(fileIds, role, googleGroupIds);

            new FileImport(google, drive, chs)
                .importFiles(fileIds, fileIdToPermissionIdMap,
                             collectionId,
                             notificationSetting,
                             sakaiGroups, role);

            redirectTo = "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasRedirect() {
        return (redirectTo != null);
    }

    public String getRedirect() {
        return redirectTo;
    }

    public Errors getErrors() {
        return null;
    }

    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }

    private String ensureCorrectDomain(String email) {
        if (email.endsWith("@" + GoogleClient.GOOGLE_DOMAIN)) {
            return email;
        } else {
            return String.format("%s@%s", email.split("@")[0],
                                 GoogleClient.GOOGLE_DOMAIN);
        }

    }
}
