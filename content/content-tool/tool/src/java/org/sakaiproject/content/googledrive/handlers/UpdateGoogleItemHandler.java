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

import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import edu.nyu.classes.groupersync.api.AddressFormatter;
import edu.nyu.classes.groupersync.api.GroupInfo;
import edu.nyu.classes.groupersync.api.GrouperSyncService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ResourceType;
import org.sakaiproject.content.googledrive.GoogleClient;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.util.BaseResourcePropertiesEdit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class UpdateGoogleItemHandler implements Handler {

    private String redirectTo = null;
    private GrouperSyncService grouper = null;

    public UpdateGoogleItemHandler() {
        grouper = (GrouperSyncService) ComponentManager.get("edu.nyu.classes.groupersync.api.GrouperSyncService");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            GoogleClient google = new GoogleClient();
            Drive drive = google.getDrive((String) context.get("googleUser"));

            String resourceId = request.getParameter("resourceId");
            String[] sakaiGroupIds = request.getParameterValues("sakaiGroupId[]");
            String notify = request.getParameter("notify");
            String role = request.getParameter("role");

            ContentHostingService chs = (ContentHostingService) ComponentManager.get("org.sakaiproject.content.api.ContentHostingService");
            String siteId = (String) context.get("siteId");

            int notificationSetting = NotificationService.NOTI_NONE;
            if ("r".equals(notify)) {
                notificationSetting = NotificationService.NOTI_REQUIRED;
            } else if ("o".equals(notify)) {
                notificationSetting = NotificationService.NOTI_OPTIONAL;
            }

            Site site = SiteService.getSite(siteId);
            List<Group> sakaiGroups = new ArrayList<Group>();
            List<String> googleGroupIds = new ArrayList<String>();
            if (sakaiGroupIds != null) {
                for (String groupId : sakaiGroupIds) {
                    GroupInfo googleGroupInfo = grouper.getGroupInfo(groupId);
                    if (googleGroupInfo != null && googleGroupInfo.isReadyForUse()) {
                        sakaiGroups.add(site.getGroup(groupId));
                        googleGroupIds.add(AddressFormatter.format(googleGroupInfo.getGrouperId()));
                    }
                }
            }

            ContentResourceEdit resource = null;
            try {
                resource = chs.editResource(resourceId);
                ResourcePropertiesEdit properties = resource.getPropertiesEdit();

                // Use these to update permissions on the google side
                List<String> previousGoogleGroupIds = properties.getPropertyList("google-group-id");

                // replace role
                properties.removeProperty("google-group-role");
                properties.addProperty("google-group-role", role);

                // replace google groups
                properties.removeProperty("google-group-id");
                for (String googleGroupId : googleGroupIds) {
                    properties.addPropertyToList("google-group-id", googleGroupId);
                }

                resource.clearRoleAccess();
                if (sakaiGroups.isEmpty()) {
                    resource.setHidden();
                } else {
                    resource.setAvailability(false, null, null);
                    resource.setGroupAccess(sakaiGroups);
                }

                // commit changes
                chs.commitResource(resource, notificationSetting);

            } catch (Exception e) {
                // force rollback and removal of lock
                if (resource != null) {
                    chs.cancelResource(resource);
                }

                throw new RuntimeException(e);
            }

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

}
