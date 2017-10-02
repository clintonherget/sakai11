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

package org.sakaiproject.drive.tool;

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
import com.google.api.client.auth.oauth2.StoredCredential;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;


import java.util.Collections;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File;

import java.net.URL;

/**
 * A handler for the index page in the PA System administration tool.
 */
public class GoogleClient {
    private static final String APPLICATION = "Sakai Drive";

    private HttpTransport httpTransport = null;
    private JacksonFactory jsonFactory = null;
    private GoogleClientSecrets clientSecrets = null;

    public GoogleClient() {
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            jsonFactory = JacksonFactory.getDefaultInstance();
            clientSecrets = GoogleClientSecrets.load(jsonFactory,
                    new InputStreamReader(new FileInputStream(ServerConfigurationService.getSakaiHomePath() + "/client_secrets.json")));

;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GoogleAuthorizationCodeFlow getAuthFlow() throws Exception {
        File dataStoreLocation = new File(ServerConfigurationService.getSakaiHomePath() + "/googly-data-store");
        FileDataStoreFactory store = new FileDataStoreFactory(dataStoreLocation);

        // set up authorization code flow
        return new GoogleAuthorizationCodeFlow.Builder(
                                                       httpTransport,
                                                       jsonFactory,
                                                       clientSecrets,
                                                       Collections.singleton(DriveScopes.DRIVE))
                .setDataStoreFactory(store)
                .setApprovalPrompt("force")
                .setAccessType("offline")
                .build();
    }


    public Credential getCredential(String user) {
        try {
            GoogleAuthorizationCodeFlow flow = getAuthFlow();

            Credential storedCredential = flow.loadCredential(user);

            if (storedCredential == null) {
                return null;
            }

            // Take our credential and wrap it in a GoogleCredential.  As far as
            // I can tell, all this gives us is the ability to update our stored
            // credentials as they get refreshed (using the
            // DataStoreCredentialRefreshListener).
            Credential credential = new GoogleCredential.Builder()
                    .setTransport(flow.getTransport())
                    .setJsonFactory(flow.getJsonFactory())
                    .setClientSecrets(clientSecrets)
                    .addRefreshListener(new CredentialRefreshListener() {
                        public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) {
                            System.err.println("OAuth token refresh error: " + tokenErrorResponse);
                        }

                        public void onTokenResponse(Credential credential, TokenResponse tokenResponse) {
                            System.err.println("OAuth token was refreshed");
                        }
                    })
                    .addRefreshListener(new DataStoreCredentialRefreshListener(user, flow.getCredentialDataStore()))
                    .build();

            credential.setAccessToken(storedCredential.getAccessToken());
            credential.setRefreshToken(storedCredential.getRefreshToken());

            return credential;
        } catch (Exception e) {
            // FIXME: Log this
            return null;
        }
    }

    public boolean deleteCredential(String user) throws Exception {
        GoogleAuthorizationCodeFlow flow = getAuthFlow();

        DataStore<StoredCredential> credentialStore = flow.getCredentialDataStore();

        return (credentialStore.delete(user) != null);
    }

    public Drive getDrive(String googleUser) throws Exception {
        return new Drive.Builder(httpTransport, jsonFactory, getCredential(googleUser))
                .setApplicationName(APPLICATION)
                .build();
    }

}
