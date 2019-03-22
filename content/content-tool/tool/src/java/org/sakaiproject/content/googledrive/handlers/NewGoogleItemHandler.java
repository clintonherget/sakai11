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
import com.google.api.services.drive.model.File;
import org.sakaiproject.content.googledrive.GoogleClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NewGoogleItemHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            GoogleClient google = new GoogleClient();
            Drive drive = google.getDrive((String) context.get("googleUser"));

            String[] fileIds = request.getParameterValues("googleitemid[]");

            if (fileIds == null || fileIds.length == 0) {
                // FIXME show listing again with message?
                throw new RuntimeException("fileid required");
            }

            Drive.Files.Get getRequest = drive.files().get(fileIds[0]); // only handling one at a time?
            File googleFile = getRequest.setFields("id, name, mimeType, description, webViewLink, iconLink, thumbnailLink").execute();

            context.put("collectionId", request.getParameter("collectionId"));
            context.put("googleFileId", googleFile.getId());
            context.put("googleFileName", googleFile.getName());
            context.put("googleFileIconLink", googleFile.getIconLink());
            context.put("subpage", "new_google_item");
            context.put("layout", false);
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
