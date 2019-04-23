package org.sakaiproject.content.googledrive.google;

import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sakaiproject.content.googledrive.GoogleClient;


public class Permissions {

    private GoogleClient google;
    private Drive drive;

    public Permissions(GoogleClient google, Drive drive) {
        this.google = google;
        this.drive = drive;
    }

    // Give everyone in `googleGroupIds` `role` permission to every file in `fileIds`.
    //
    // Returns a map from FileID to list of permission IDs that were created.
    public Map<String, List<String>> applyPermissions(List<String> fileIds,
                                                      String role,
                                                      List<String> googleGroupIds)
        throws Exception {
        Map<String, List<String>> fileIdtoPermissionIdMap = new HashMap<>();

        GoogleClient.LimitedBatchRequest batch = google.getBatch(drive);

        for (String fileId : fileIds) {
            for (String group : googleGroupIds) {
                Permission permission = new Permission().setRole(role).setType("group").setEmailAddress(group);
                batch.queue(drive.permissions().create(fileId, permission),
                            new PermissionHandler(google, fileId, fileIdtoPermissionIdMap));
            }
        }

        batch.execute();

        return fileIdtoPermissionIdMap;
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
