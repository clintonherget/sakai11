package org.sakaiproject.content.entityproviders;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.content.tool.GoogleClient;
import org.sakaiproject.content.tool.RequestParams;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.*;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.UserDirectoryService;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class GoogleDriveEntityProvider extends AbstractEntityProvider implements EntityProvider, AutoRegisterEntityProvider, ActionsExecutable, Outputable, Describeable, RequestAware {

	public final static String ENTITY_PREFIX = "google-drive";

	public static final String DRIVE_MODE_RECENT = "recent";
	public static final String DRIVE_MODE_MY_DRIVE = "home";
	public static final String DRIVE_MODE_STARRED = "starred";

	public static final String AUTH_MODE_SEND_TO_GOOGLE = "send_to_google";
	public static final String AUTH_MODE_HANDLE = "handle";
	public static final String AUTH_MODE_RESET = "reset";

	private static final String GOOGLE_DOMAIN = "gqa.nyu.edu";

	@Override
	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	@Override
	public String[] getHandledOutputFormats() {
		return new String[] {Formats.JSON};
	}

	@EntityCustomAction(action = "drive-data", viewKey = EntityView.VIEW_LIST)
	public List<GoogleItem> getGoogleDriveItems(EntityView view, Map<String, Object> params) {
		HttpServletRequest request = requestGetter.getRequest();
		HttpServletResponse response = requestGetter.getResponse();

		try {
			GoogleClient google = new GoogleClient();

			RequestParams p = new RequestParams(request);
			String mode = p.getString("mode", DRIVE_MODE_RECENT);

			String user = getCurrentGoogleUser();

			FileList fileList = null;

			if (DRIVE_MODE_RECENT.equals(mode)) {
				fileList = getRecentFiles(google, user, p);
			} else if (DRIVE_MODE_MY_DRIVE.equals(mode)) {
				fileList = getChildrenForContext(google, user, p);
			} else if (DRIVE_MODE_STARRED.equals(mode)) {
				fileList = getChildrenForContext(google, user, p, true);
			} else {
				throw new RuntimeException("DriveHandler mode not supported: " + mode);
			}

			List<GoogleItem> items = new ArrayList<>();

			for (File entry : fileList.getFiles()) {
				items.add(new GoogleItem(entry.getId(),
					entry.getName(),
					entry.getIconLink(),
					entry.getThumbnailLink(),
					entry.getWebViewLink(),
					entry.getMimeType()));
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(response.getOutputStream(), new GoogleItemPage(items, fileList.getNextPageToken()));

			return items;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

//	@EntityCustomAction(action = "auth", viewKey = EntityView.VIEW_LIST)
//	public void auth(EntityView view, Map<String, Object> params) {
//		HttpServletRequest request = requestGetter.getRequest();
//		HttpServletResponse response = requestGetter.getResponse();
//
//		RequestParams p = new RequestParams(request);
//		String mode = p.getString("mode", null);
//
//		try {
//			GoogleClient google = new GoogleClient();
//
//			if (AUTH_MODE_HANDLE.equals(mode)) {
//				handleOAuth(request, response, context);
//			} else if (AUTH_MODE_SEND_TO_GOOGLE.equals(mode)) {
//				sendToGoogle(request, response, context);
//			} else if (AUTH_MODE_RESET.equals(mode)) {
//				String googleUser = getCurrentGoogleUser();
//
//				if (googleUser != null) {
//					google.deleteCredential(googleUser);
//				}
//
//				// WHAT TO DO?
//			}
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	private FileList getRecentFiles(GoogleClient google, String user, RequestParams p) {
		try {
			Drive drive = google.getDrive(user);

			String query = p.getString("q", null);
			String pageToken = p.getString("pageToken", null);

			Drive.Files files = drive.files();
			Drive.Files.List list = files.list();

			list.setFields("nextPageToken, files(id, name, mimeType, description, webViewLink, iconLink, thumbnailLink)");

			String queryString = "mimeType != 'application/vnd.google-apps.folder'";

			if (query == null) {
				// API restriction: We can only sort if we don't have a search query
				list.setOrderBy("viewedByMeTime desc");
			} else {
				queryString += " AND fullText contains '" + query.replace("'", "\\'") + "'";
			}

			list.setQ(queryString);
			list.setPageSize(50);

			if (pageToken != null) {
				list.setPageToken(pageToken);
			}

			return list.execute();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private FileList getChildrenForContext(GoogleClient google, String user, RequestParams p) {
		return getChildrenForContext(google, user, p, false);
	}

	private FileList getChildrenForContext(GoogleClient google, String user, RequestParams p, boolean starred) {
		try {
			Drive drive = google.getDrive(user);

			String folderId = p.getString("folderId", "root");
			String pageToken = p.getString("pageToken", null);
			String query = p.getString("q", null);

			Drive.Files files = drive.files();
			Drive.Files.List list = files.list();

			list.setFields("nextPageToken, files(id, name, mimeType, description, webViewLink, iconLink, thumbnailLink)");

			String queryString = "'"+folderId+"' in parents";

			if (starred && folderId.equals("root")) {
				queryString = "starred";
			}

			if (query == null) {
				list.setOrderBy("folder,name");
			} else {
				queryString += " AND fullText contains '" + query.replace("'", "\\'") + "'";
			}

			if (pageToken != null) {
				list.setPageToken(pageToken);
			}

			list.setQ(queryString);
			list.setPageSize(50);

			return list.execute();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private class GoogleItemPage {
		public String nextPageToken;
		public List<GoogleItem> files;

		public GoogleItemPage(List<GoogleItem> files, String nextPageToken) {
			this.files = files;
			this.nextPageToken = nextPageToken;
		}
	}

	private class GoogleItem {
		public String id;
		public String name;
		public String iconLink;
		public String thumbnailLink;
		public String viewLink;
		public String mimeType;

		public GoogleItem(String id, String name, String iconLink, String thumbnailLink, String viewLink, String mimeType) {
			this.id = id;
			this.name = name;
			this.iconLink = iconLink;
			this.thumbnailLink = thumbnailLink;
			this.viewLink = viewLink;
			this.mimeType = mimeType;
		}

		public String getId() { return id; }
		public String getName() { return name; }
		public String getIconLink() { return iconLink; }
		public String getThumbnailLink() { return thumbnailLink; }
		public String getViewLink() { return viewLink; }
		public boolean isFolder() { return mimeType.equals("application/vnd.google-apps.folder"); }
	}

	private String getCurrentGoogleUser() {
		return org.sakaiproject.user.cover.UserDirectoryService.getCurrentUser().getEid() + "@" + GOOGLE_DOMAIN;
	}

	private RequestGetter requestGetter;

	public void setRequestGetter(RequestGetter requestGetter) {
		this.requestGetter = requestGetter;
	}

	@Setter
	private SiteService siteService;

	@Setter
	private ToolManager toolManager;

	@Setter
	private SecurityService securityService;

	@Setter
	private UserDirectoryService userDirectoryService;
}
