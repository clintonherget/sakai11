package edu.nyu.classes.seats.handlers;

import java.util.*;
import java.text.DateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.time.api.Time;

import java.util.stream.Collectors;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.net.URL;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentTypeImageService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.content.api.GroupAwareEntity.AccessMode;


import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;

import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.*;
import edu.nyu.classes.seats.storage.db.DBConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SectionHandler implements Handler {

    protected String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
            try {
                String siteId = (String)context.get("siteId");
                DBConnection db = (DBConnection)context.get("db");

                RequestParams p = new RequestParams(request);
                String sectionId = p.getString("sectionId", null);

                if (sectionId == null) {
                    throw new RuntimeException("Need argument: sectionId");
                }

                Optional<SeatSection> seatSection = SeatsStorage.getSeatSection(db, sectionId, siteId);

                if (!seatSection.isPresent()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                JSONObject sectionJSON = new JSONObject();
                sectionJSON.put("id", seatSection.get().id);
                sectionJSON.put("name", seatSection.get().name);
                sectionJSON.put("shortName", seatSection.get().shortName);
                sectionJSON.put("provisioned", seatSection.get().provisioned);
                sectionJSON.put("split", seatSection.get().hasSplit);



                Site site = SiteService.getSite(siteId);
                ResourceProperties props = site.getProperties();
                String instructionMode = null;
                String propInstructionModeOverrides = props.getProperty("OverrideToBlended");
                if (propInstructionModeOverrides != null) {
                    if (Arrays.asList(propInstructionModeOverrides.split(" *, *")).contains(seatSection.get().primaryStemName)) {
                        instructionMode = "OB";
                    };
                }

                String propMaxGroupsString = props.getProperty("SeatingAssignmentsMaxGroups");

                sectionJSON.put("maxGroups", propMaxGroupsString == null ? 4 : Integer.valueOf(propMaxGroupsString));
                sectionJSON.put("instructionMode", instructionMode == null ? SeatsStorage.getSectionInstructionMode(db, seatSection.get().primaryStemName) : instructionMode);

                JSONArray sectionGroups = new JSONArray();
                sectionJSON.put("groups", sectionGroups);

                Map<String, String> memberNames = SeatsStorage.getMemberNames(seatSection.get());

                for (SeatGroup group : seatSection.get().listGroups()) {
                    JSONObject groupJSON = new JSONObject();
                    sectionGroups.add(groupJSON);

                    groupJSON.put("id", group.id);
                    groupJSON.put("name", group.name);
                    groupJSON.put("description", group.description);

                    groupJSON.put("isGroupEmpty", group.listMembers().isEmpty());

                    JSONArray meetingsJSON = new JSONArray();
                    groupJSON.put("meetings", meetingsJSON);

                    for (Meeting meeting : group.listMeetings()) {
                        JSONObject meetingJSON = new JSONObject();
                        meetingsJSON.add(meetingJSON);

                        meetingJSON.put("id", meeting.id);
                        meetingJSON.put("name", meeting.name);

                        JSONArray seatAssignments = new JSONArray();
                        meetingJSON.put("seatAssignments", seatAssignments);

                        Map<String, JSONObject> seatAssignmentSet = new HashMap<>();

                        for (Member member : group.listMembers()) {
                            JSONObject assignmentJSON = new JSONObject();
                            assignmentJSON.put("netid", member.netid);
                            assignmentJSON.put("official", member.official);
                            assignmentJSON.put("seat", null);
                            assignmentJSON.put("displayName", memberNames.getOrDefault(member.netid, member.netid));
                            assignmentJSON.put("studentLocation", member.studentLocation.toString());
                            seatAssignmentSet.put(member.netid, assignmentJSON);
                        }

                        for (SeatAssignment seatAssignment : meeting.listSeatAssignments()) {
                            JSONObject assignmentJSON = seatAssignmentSet.get(seatAssignment.netid);
                            assignmentJSON.put("id", seatAssignment.id);
                            assignmentJSON.put("seat", seatAssignment.seat);
                        }

                        seatAssignments.addAll(seatAssignmentSet.values());
                    }
                }

                try {
                    response.getWriter().write(sectionJSON.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean hasTemplate() {
        return false;
    }

    @Override
    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }

    public Errors getErrors() {
        return null;
    }

    public boolean hasRedirect() {
        return false;
    }

    public String getRedirect() {
        return "";
    }

    @Override
    public boolean isSiteUpdRequired() {
        return false;
    }
}


