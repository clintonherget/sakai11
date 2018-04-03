package org.sakaiproject.portal.entityprovider;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.RuntimeConstants;

import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;

import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.profile2.logic.*;
import org.sakaiproject.profile2.model.BasicPerson;
import org.sakaiproject.profile2.model.ProfileImage;
import org.sakaiproject.profile2.model.UserProfile;
import org.sakaiproject.profile2.types.PrivacyType;
import org.sakaiproject.profile2.util.ProfileConstants;

import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

import org.sakaiproject.portal.beans.PortalNotifications;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * An entity provider to serve Portal information
 * 
 */
@Slf4j
public class PortalEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Outputable, ActionsExecutable, Describeable {

	public final static String PREFIX = "portal";
	public final static String TOOL_ID = "sakai.portal";

	@Setter
	private ProfileConnectionsLogic profileConnectionsLogic;

	@Setter
	private ProfileLogic profileLogic;

	@Setter
	private ProfileLinkLogic profileLinkLogic;

	@Setter
	private ProfilePrivacyLogic privacyLogic;

	@Setter
	private ServerConfigurationService serverConfigurationService;

	@Setter
	private SessionManager sessionManager;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private SiteService siteService;

	@Setter
	private CourseManagementService cms;

	private Template formattedProfileTemplate = null;
	private Template profileDrawerTemplate = null;

	public void init() {

		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("resource.loader", "class");
		ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		// No logging. (If you want to log, you need to set an approrpriate directory in an approrpriate
		// velocity.properties file, or the log will be created in the directory in which tomcat is started, or
		// throw an error if it can't create/write in that directory.)
		ve.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
		try {
			ve.init();
			formattedProfileTemplate = ve.getTemplate("org/sakaiproject/portal/entityprovider/nyu-profile-popup.vm");
			profileDrawerTemplate = ve.getTemplate("org/sakaiproject/portal/entityprovider/nyu-profile-drawer.vm");
		} catch (Exception e) {
			log.error("Failed to load profile-popup.vm", e);
		}
	}

	public String getEntityPrefix() {
		return PREFIX;
	}

	public String getAssociatedToolId() {
		return TOOL_ID;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.TXT ,Formats.JSON, Formats.HTML};
	}

	// So far all we do is errors to solve SAK-29531
	// But this could be extended...
	@EntityCustomAction(action = "notify", viewKey = "")
	public PortalNotifications handleNotify(EntityView view) {
		Session s = sessionManager.getCurrentSession();
		if ( s == null ) {
			throw new IllegalArgumentException("Session not found");
		}
		List<String> retval = new ArrayList<String> ();
                String userWarning = (String) s.getAttribute("userWarning");
                if (StringUtils.isNotEmpty(userWarning)) {
			retval.add(userWarning);
		}
		PortalNotifications noti = new PortalNotifications ();
		noti.setError(retval);
		s.removeAttribute("userWarning");
		return noti;
	}

	@EntityCustomAction(action="formatted",viewKey=EntityView.VIEW_SHOW)
	public ActionReturn getFormattedProfile(EntityReference ref, Map<String, Object> params) {

		String currentUserId = developerHelperService.getCurrentUserId();

		ResourceLoader rl = new ResourceLoader(currentUserId, "profile-popup");

		UserProfile userProfile = (UserProfile) profileLogic.getUserProfile(ref.getId());

		String connectionUserId = userProfile.getUserUuid();

		VelocityContext context = new VelocityContext();
		context.put("displayName", userProfile.getDisplayName());
		context.put("profileUrl", profileLinkLogic.getInternalDirectUrlToUserProfile(connectionUserId));

		String email = userProfile.getEmail();
		if (StringUtils.isEmpty(email)) email = "";
		context.put("email", email);

		context.put("currentUserId", currentUserId);
		context.put("connectionUserId", connectionUserId);
		context.put("requestMadeLabel", rl.getString("connection.requested"));
		context.put("cancelLabel", rl.getString("connection.cancel"));
		context.put("incomingRequestLabel", rl.getString("connection.incoming.request"));
		context.put("removeConnectionLabel", rl.getString("connection.remove"));
		context.put("acceptLabel", rl.getString("connection.accept"));
		context.put("rejectLabel", rl.getString("connection.reject"));
		context.put("addConnectionLabel", rl.getString("connection.add"));
		context.put("you", currentUserId.equals(connectionUserId));

		// NYU things!
		BasicPerson person = profileLogic.getBasicPerson(connectionUserId);
		context.put("pictureUrl", "/direct/profile/" + connectionUserId + "/image/thumb");
		String eid;
		try {
			eid = userDirectoryService.getUserEid(connectionUserId);
			context.put("eid", eid);
		} catch (UserNotDefinedException e) {
			// FIXME do something else?
			throw new RuntimeException("User is not real");
		}
		String siteId = (String)params.get("siteId");
		addSectionAndRoleToContext(context, siteId, connectionUserId);

		StringWriter writer = new StringWriter();

		try {
			formattedProfileTemplate.merge(context, writer);
			return new ActionReturn(Formats.UTF_8, Formats.HTML_MIME_TYPE, writer.toString());
		} catch (IOException ioe) {
			throw new EntityException("Failed to format profile.", ref.getReference());
		}
	}

	@EntityCustomAction(action="drawer",viewKey=EntityView.VIEW_SHOW)
	public ActionReturn getProfileDrawer(EntityReference ref, Map<String, Object> params) {

		String siteId = (String)params.get("siteId");

		String currentUserId = developerHelperService.getCurrentUserId();

		ResourceLoader rl = new ResourceLoader(currentUserId, "profile-popup");

		UserProfile userProfile = (UserProfile) profileLogic.getUserProfile(ref.getId());

		String connectionUserId = userProfile.getUserUuid();

		VelocityContext context = new VelocityContext();
		context.put("currentUserId", currentUserId);
		context.put("connectionUserId", connectionUserId);
		context.put("you", currentUserId.equals(connectionUserId));

		context.put("displayName", userProfile.getDisplayName());
		context.put("profileUrl", profileLinkLogic.getInternalDirectUrlToUserProfile(connectionUserId));
		context.put("pictureUrl", "/direct/profile/" + connectionUserId + "/image");

		try {
			context.put("eid", userDirectoryService.getUserEid(connectionUserId));
		} catch (UserNotDefinedException e) {
			context.put("eid", "");
		}

		if (privacyLogic.isActionAllowed(connectionUserId, currentUserId, PrivacyType.PRIVACY_OPTION_BASICINFO)) {
			boolean showAge = privacyLogic.isBirthYearVisible(connectionUserId);
			addBasicInfoToContext(context, userProfile, showAge);
		}
		if (privacyLogic.isActionAllowed(connectionUserId, currentUserId, PrivacyType.PRIVACY_OPTION_STAFFINFO)) {
			addStaffInfoToContext(context, userProfile);
		}
		if (privacyLogic.isActionAllowed(connectionUserId, currentUserId, PrivacyType.PRIVACY_OPTION_STUDENTINFO)) {
			addStudentInfoToContext(context, userProfile);
		}
		if (privacyLogic.isActionAllowed(connectionUserId, currentUserId, PrivacyType.PRIVACY_OPTION_PERSONALINFO)) {
			addBiographyToContext(context, userProfile);
		}
		if (privacyLogic.isActionAllowed(connectionUserId, currentUserId, PrivacyType.PRIVACY_OPTION_CONTACTINFO)) {
			addContactInfoToContext(context, userProfile);
		}
		addSectionAndRoleToContext(context, siteId, connectionUserId);
		addConnectionDataToContext(context, currentUserId, connectionUserId);

		StringWriter writer = new StringWriter();

		try {
			profileDrawerTemplate.merge(context, writer);
			return new ActionReturn(Formats.UTF_8, Formats.HTML_MIME_TYPE, writer.toString());
		} catch (IOException ioe) {
			throw new EntityException("Failed to format profile.", ref.getReference());
		}
	}

	private void addSectionAndRoleToContext(VelocityContext context, String siteId, String connectionUserId) {
		String sections = "";
		String role = "";

		if (!StringUtils.isBlank(siteId)) {
			try {
				Site site = siteService.getSite(siteId);
				Member member = site.getMember(connectionUserId);
				if (member != null) {
					Role memberRole = member.getRole();
					if (memberRole != null) {
						role = memberRole.getId();
					}
					List<String> groups = new ArrayList<>();
					Collection<Group> siteGroups = site.getGroups();
					for (Group group : siteGroups) {
						if (group.getMember(connectionUserId) != null) {
							groups.add(group.getTitle());
						}
					}
					sections = groups.stream().collect(Collectors.joining(", "));
				}
			} catch (IdUnusedException e) {
			}
		}

		context.put("siteId", siteId);
		context.put("sections", sections);
		context.put("role", role);
	}

	private void addConnectionDataToContext(VelocityContext context, String currentUserId, String connectionUserId) {
		boolean connectionsEnabled = serverConfigurationService.getBoolean("profile2.connections.enabled",
			ProfileConstants.SAKAI_PROP_PROFILE2_CONNECTIONS_ENABLED);

		if (connectionsEnabled && !currentUserId.equals(connectionUserId)) {

			int connectionStatus = profileConnectionsLogic.getConnectionStatus(currentUserId, connectionUserId);

			if (connectionStatus == ProfileConstants.CONNECTION_CONFIRMED) {
				context.put("connected", true);
			} else if (connectionStatus == ProfileConstants.CONNECTION_REQUESTED) {
				context.put("requested", true);
			} else if (connectionStatus == ProfileConstants.CONNECTION_INCOMING) {
				context.put("incoming", true);
			} else {
				context.put("unconnected", true);
			}
		}
	}

	private void addBasicInfoToContext(VelocityContext context, UserProfile profile, boolean showAge) {
		context.put("showBasicInfo", true);
		if (StringUtils.isBlank(profile.getNickname())) {
			context.put("preferredName", profile.getDisplayName());
		} else {
			context.put("preferredName", profile.getNickname());
		}

		if (showAge) {
			Date dob = profile.getDateOfBirth();
			if (dob != null) {
				Calendar birthday = new GregorianCalendar();
				birthday.setTime(dob);
				Calendar today = new GregorianCalendar();
				today.setTime(new Date());
				int age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR);

				context.put("age", String.valueOf(age));
			}
		}
	}

	private void addStaffInfoToContext(VelocityContext context, UserProfile profile) {
		context.put("showStaffInfo", true);
		context.put("position", profile.getPosition());
		context.put("department", profile.getDepartment());
	}

	private void addStudentInfoToContext(VelocityContext context, UserProfile profile) {
		context.put("showStudentInfo", true);
		context.put("school", profile.getSchool());
		context.put("degree", profile.getCourse());
		context.put("subjects", profile.getSubjects());
	}

	private void addBiographyToContext(VelocityContext context, UserProfile profile) {
		context.put("showBiography", true);
		context.put("personalSummary", profile.getPersonalSummary());
		context.put("staffProfile", profile.getStaffProfile());
	}

	private void addContactInfoToContext(VelocityContext context, UserProfile profile) {
		context.put("showContactInfo", true);
		if (!StringUtils.isBlank(profile.getEmail())) {
			context.put("email", profile.getEmail());
		}
		if (!profile.getPhoneNumbers().isEmpty()) {
			context.put("phoneNumbers", profile.getPhoneNumbers());
		}
	}
}
