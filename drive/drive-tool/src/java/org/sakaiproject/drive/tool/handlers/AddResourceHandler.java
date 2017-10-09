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

package org.sakaiproject.drive.tool.handlers;

import org.sakaiproject.drive.tool.GoogleClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.component.cover.ServerConfigurationService;
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

/**
 * A handler to access Google Drive file data
 */
public class AddResourceHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            GoogleClient google = new GoogleClient();
            Drive drive = google.getDrive((String) context.get("googleUser"));

            String[] fileIds = request.getParameterValues("fileid[]");

            GoogleClient.LimitedBatchRequest batch = google.getBatch(drive);

            ContentHostingService chs = (ContentHostingService) ComponentManager.get("org.sakaiproject.content.api.ContentHostingService");
            String siteId = (String) context.get("siteID");
            String collectionId = "/group/" + siteId + "/";

            for (String id : fileIds) {
                batch.queue(drive.files().get(id).setFields("id, name, mimeType, description, webViewLink, iconLink, thumbnailLink"),
                        new GoogleFileImporter(google, id, chs, collectionId));
            }

            batch.execute();

            redirectTo = "/";
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


    private class GoogleFileImporter extends JsonBatchCallback<File> {
        private GoogleClient google;
        private String fileId;
        private ContentHostingService chs;
        private String collectionId;

        public GoogleFileImporter(GoogleClient google, String fileId, ContentHostingService chs, String collectionId) {
            this.google = google;
            this.fileId = fileId;
            this.chs = chs;
            this.collectionId = collectionId;
        }

        public void onSuccess(File googleFile, HttpHeaders responseHeaders) {
            ResourceProperties properties = new BaseResourcePropertiesEdit();
            properties.addProperty(ResourceProperties.PROP_DISPLAY_NAME, googleFile.getName());
            properties.addProperty("google-id", fileId);
            properties.addProperty("google-view-link", googleFile.getWebViewLink());
            properties.addProperty("google-icon-link", googleFile.getIconLink());

            try {
                chs.addResource(UUID.randomUUID().toString(), collectionId, 10, "x-nyu-google/item", new byte[0], properties, Collections.<String>emptyList(), 1);
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

}
