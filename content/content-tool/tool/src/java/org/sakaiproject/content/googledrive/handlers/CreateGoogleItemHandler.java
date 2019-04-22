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

import edu.nyu.classes.groupersync.api.AddressFormatter;
import edu.nyu.classes.groupersync.api.GroupInfo;
import edu.nyu.classes.groupersync.api.GrouperSyncService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.GroupAwareEntity.AccessMode;
import org.sakaiproject.content.api.ResourceType;
import org.sakaiproject.content.googledrive.GoogleClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.exception.PermissionException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.util.BaseResourcePropertiesEdit;

import com.google.api.client.http.HttpHeaders;

import java.net.URL;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;


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

            String[] fileIds = request.getParameterValues("googleitemid[]");
            String[] sakaiGroupIds = request.getParameterValues("sakaiGroupId[]"); // FIXME look up google group ids again
            String collectionId = request.getParameter("collectionId");
            String notify = request.getParameter("notify");
            String role = request.getParameter("role");

            if ("commenter".equals(role)) {
                throw new RuntimeException("Can't currently handle 'commenter' permissions.");
            }

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

            // Set permissions on the Google side for the selected files.  We'll
            // do this first because there's no point continuing with the import
            // if the permissions aren't there.
            //
            // NOTE: We can't set "commenter" permission here because that's not
            // a part of the Drive API.  Do we want to special case handling for
            // Google Docs (presumably hitting the Docs-specific API), or just
            // drop this as a requirement?

            Map<String, List<String>> fileIdtoPermissionIdMap = new HashMap<>();

            for (String fileId : fileIds) {
                for (String group : googleGroupIds) {
                    Permission permission = new Permission().setRole(role).setType("group").setEmailAddress(group);
                    batch.queue(drive.permissions().create(fileId, permission),
                                new PermissionHandler(google, fileId, fileIdtoPermissionIdMap));
                }
            }

            for (String fileId : fileIds) {
                batch.queue(drive.files().get(fileId).setFields("id, name, mimeType, description, webViewLink, iconLink, thumbnailLink"),
                            new GoogleFileImporter(google, fileIdtoPermissionIdMap.get(fileId),
                                                   fileId, chs, collectionId, notificationSetting,
                                                   sakaiGroups, googleGroupIds, role));
            }

            batch.execute();

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

    private class GoogleFileImporter extends JsonBatchCallback<File> {
        private GoogleClient google;
        private List<String> permissionIds;
        private String fileId;
        private ContentHostingService chs;
        private String collectionId;
        private int notificationSetting;
        private List<AuthzGroup> sakaiGroups;
        private List<String> googleGroupIds;
        private String role;

        public GoogleFileImporter(GoogleClient google,
                                  List<String> permissionIds,
                                  String fileId,
                                  ContentHostingService chs,
                                  String collectionId,
                                  int notificationSetting,
                                  List<AuthzGroup> sakaiGroups,
                                  List<String> googleGroupIds,
                                  String role) {
            this.google = google;
            this.permissionIds = permissionIds;
            this.fileId = fileId;
            this.chs = chs;
            this.collectionId = collectionId;
            this.notificationSetting = notificationSetting;
            this.sakaiGroups = sakaiGroups;
            this.googleGroupIds = googleGroupIds;
            this.role = role;
        }

        public void onSuccess(File googleFile, HttpHeaders responseHeaders) {
            ResourceProperties properties = new BaseResourcePropertiesEdit();
            properties.addProperty(ResourceProperties.PROP_DISPLAY_NAME, googleFile.getName());
            properties.addProperty("google-id", fileId);
            properties.addProperty("google-view-link", googleFile.getWebViewLink());
            properties.addProperty("google-icon-link", googleFile.getIconLink());
            properties.addProperty("google-mime-type", googleFile.getMimeType());
            properties.addProperty("google-group-role", role);

            for (String googleGroupId : googleGroupIds) {
                properties.addPropertyToList("google-group-id", googleGroupId);
            }

            try {
                ContentResource resource = chs.addResource(UUID.randomUUID().toString(),
                                                           collectionId,
                                                           10,
                                                           "x-nyu-google/item",
                                                           googleFile.getWebViewLink().getBytes(),
                                                           properties,
                                                           Collections.<String>emptyList(),
                                                           notificationSetting);

                ContentResourceEdit resourceEdit = chs.editResource(resource.getId());
                resourceEdit.setResourceType(ResourceType.TYPE_GOOGLE_DRIVE_ITEM);
                if (sakaiGroups.isEmpty()) {
                    resourceEdit.setHidden();
                } else {
                    // sakaiGroups will contain a mixture of Site and Group
                    // objects.  We only need to explicitly handle the Group
                    // ones.
                    List<Group> groups = sakaiGroups
                        .stream()
                        .filter(obj -> obj instanceof Group)
                        .map(obj -> (Group)obj)
                        .collect(Collectors.toList());

                    if (groups.isEmpty()) {
                        if (AccessMode.GROUPED.equals(resourceEdit.getAccess())) {
                            // Clear other groups if there were some previously.
                            resourceEdit.clearGroupAccess();
                        }
                    } else {
                        resourceEdit.setGroupAccess(groups);
                    }
                }
                chs.commitResource(resourceEdit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
            if (e.getCode() == 403) {
                google.rateLimitHit();
            }

            throw new RuntimeException("Failed during Google lookup for file: " + fileId + " " + e);
        }
    }

    private class PermissionHandler extends JsonBatchCallback<Permission> {
        private GoogleClient google;
        private String fileId;
        private Map<String, List<String>> permissionMap;

        public PermissionHandler(GoogleClient google, String fileId, Map<String, List<String>> permissionMap) {
            this.google = google;
            this.fileId = fileId;
            this.permissionMap = permissionMap;
        }

        public void onSuccess(Permission permission, HttpHeaders responseHeaders) {
            if (!permissionMap.containsKey(this.fileId)) {
                permissionMap.put(this.fileId, new ArrayList<>(1));
            }

            permissionMap.get(this.fileId).add(permission.getId());
        }

        public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
            if (e.getCode() == 403) {
                google.rateLimitHit();
            }

            throw new RuntimeException("Failed to set permission on file: " + this.fileId + " " + e);
        }
    }

}
