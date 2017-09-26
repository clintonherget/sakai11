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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.tool.cover.SessionManager;

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

import java.util.stream.Collectors;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.net.URL;

/**
 * A handler for the index page in the PA System administration tool.
 */
public class IndexHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            context.put("subpage", "index");

//            GoogleClient google = new GoogleClient();
//
//            Drive drive = google.getDrive((String) context.get("googleUser"));
//
//            Drive.Files files = drive.files();
//            Drive.Files.List list = files.list();
//
//            list.setFields("nextPageToken, files(id, name, mimeType, description, webViewLink, iconLink, thumbnailLink)");
//            list.setOrderBy("name");
//
//            list.setQ("mimeType != 'application/vnd.google-apps.folder'");
//
//            FileList fileList = list.execute();
//
//            List<GoogleItem> filenames = new ArrayList<>();
//
//            for (File e : fileList.getFiles()) {
//                filenames.add(new GoogleItem(e.getName(),
//                        e.getIconLink(),
//                        e.getThumbnailLink(),
//                        e.getWebViewLink()));
//            }
//
//            context.put("filenames", filenames);
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


    private class GoogleItem {
        public String name;
        public String iconLink;
        public String thumbnailLink;
        public String viewLink;

        public GoogleItem(String name, String iconLink, String thumbnailLink, String viewLink) {
            this.name = name;
            this.iconLink = iconLink;
            this.thumbnailLink = thumbnailLink;
            this.viewLink = viewLink;
        }

        public String getName() { return name; }
        public String getIconLink() { return iconLink; }
        public String getThumbnailLink() { return thumbnailLink; }
        public String getViewLink() { return viewLink; }
    }
}
