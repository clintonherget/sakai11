package edu.nyu.classes.seats.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.*;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.coursemanagement.api.CourseManagementService;

import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.migrations.BaseMigration;

import org.json.simple.JSONObject;

public class SeatsStorage {
    public enum SelectionType {
        RANDOM,
    }

    public enum AuditEvents {
        SEAT_CLEARED,
        MEETING_DELETED,
        GROUP_DELETED,
        GROUP_CREATED,
        MEETING_CREATED,
        MEMBER_ADDED,
    }

    public void runDBMigrations() {
        BaseMigration.runMigrations();
    }

    private static class AuditEntry {
        private AuditEvents event;
        private String json;
        private String[] args;
        private long timestamp;

        public AuditEntry(AuditEvents event, String json, String[] args) {
            this.event = event;
            this.json = json;
            this.args = args;
            this.timestamp = System.currentTimeMillis();
        }

        public String toString() {
            return String.format("%d %s: %s", timestamp, event, json);
        }
    }

    @SuppressWarnings("unchecked")
    private static String json(String ...args) {
        JSONObject obj = new JSONObject();

        for (int i = 0; i < args.length; i += 2) {
            obj.put(args[i], args[i + 1]);
        }

        return obj.toString();
    }

    public static void clearSeat(DBConnection db, SeatAssignment seat) throws SQLException {
        AuditEntry entry = new AuditEntry(AuditEvents.SEAT_CLEARED,
                                          json("seat", seat.seat,
                                               "meeting", seat.meeting.id,
                                               "meeting_name", seat.meeting.name,
                                               "meeting_location", seat.meeting.location_code,
                                               "group_name", seat.meeting.group.name,
                                               "group_id", seat.meeting.group.id,
                                               "section_id", seat.meeting.group.section.id,
                                               "user", seat.netid),
                                          new String[] {
                                              // FIXME: index useful stuff
                                          }
                                          );

        insertAuditEntry(db, entry);
        db.run("delete from seat_meeting_assignment where id = ?")
            .param(seat.id)
            .executeUpdate();
    }

    public static void insertAuditEntry(DBConnection db, AuditEntry entry) throws SQLException {
        db.run("insert into seat_audit (id, timestamp_ms, event_code, json) values (?, ?, ?, ?)")
            .param(db.uuid())
            .param(entry.timestamp)
            .param(entry.event.toString())
            .param(entry.json)
            .executeUpdate();
    }

    public static void deleteMeeting(DBConnection db, Meeting meeting) throws SQLException {
        AuditEntry entry = new AuditEntry(AuditEvents.MEETING_DELETED,
                                          json("meeting", meeting.id,
                                               "meeting_name", meeting.name,
                                               "meeting_location", meeting.location_code,
                                               "group_name", meeting.group.name,
                                               "group_id", meeting.group.id,
                                               "section_id", meeting.group.section.id),
                                          new String[] {
                                              // FIXME: index useful stuff
                                          }
                                          );

        insertAuditEntry(db, entry);
        db.run("delete from seat_meeting where id = ?")
            .param(meeting.id)
            .executeUpdate();
    }

    public static void deleteGroup(DBConnection db, SeatGroup group) throws SQLException {
        AuditEntry entry = new AuditEntry(AuditEvents.GROUP_DELETED,
                                          json("group_name", group.name,
                                               "group_id", group.id,
                                               "section_id", group.section.id),
                                          new String[] {
                                              // FIXME: index useful stuff
                                          }
                                          );

        insertAuditEntry(db, entry);
        db.run("delete from seat_group_members where group_id = ?")
            .param(group.id)
            .executeUpdate();
        db.run("delete from seat_group where id = ?")
            .param(group.id)
            .executeUpdate();
    }

    public static SeatSection getSeatSection(DBConnection db, String sectionId) throws SQLException {
        SeatSection section = new SeatSection(sectionId);

        try (DBResults rows = db.run("select sg.*, sec.id as section_id" +
                                     " from seat_group sg" +
                                     " inner join seat_group_section sec on sg.section_id = sec.id " +
                                     " where sec.id = ?")
             .param(sectionId)
             .executeQuery()) {
            for (ResultSet row : rows) {
                section.addGroup(row.getString("id"), row.getString("name"), row.getString("description"));
            }
        }

        try (DBResults rows = db.run("select sm.group_id, sm.id as meeting_id" +
                " from seat_meeting sm" +
                " where sm.group_id in (" + DB.placeholders(section.groupIds()) + ")")
                .stringParams(section.groupIds())
                .executeQuery()) {
            for (ResultSet row : rows) {
                section.fetchGroup(row.getString("group_id"))
                        .get()
                        .getOrCreateMeeting(row.getString("meeting_id"));
            }
        }

        try (DBResults rows = db.run("select sm.group_id, sm.id as meeting_id, assign.id as assign_id, assign.netid, assign.seat" +
                                     " from seat_meeting sm" +
                                     " inner join seat_meeting_assignment assign on assign.meeting_id = sm.id" +
                                     " where sm.group_id in (" + DB.placeholders(section.groupIds()) + ")")
             .stringParams(section.groupIds())
             .executeQuery()) {
            for (ResultSet row : rows) {
                section.fetchGroup(row.getString("group_id"))
                    .get()
                    .getOrCreateMeeting(row.getString("meeting_id"))
                    .addSeatAssignment(row.getString("assign_id"), row.getString("netid"), row.getString("seat"));
            }
        }

        return section;
    }

    private static String createGroup(DBConnection db, SeatSection section, String groupTitle, List<String> members) throws SQLException {
        String groupId = db.uuid();

        AuditEntry entry = new AuditEntry(AuditEvents.GROUP_CREATED,
                json("group_name", groupTitle,
                        "group_id", groupId,
                        "section_id", section.id),
                new String[] {
                        // FIXME: index useful stuff
                }
        );

        insertAuditEntry(db, entry);

        db.run("insert into seat_group (id, name, section_id) values (?, ?, ?)")
            .param(groupId)
            .param(groupTitle)
            .param(section.id)
            .executeUpdate();

        String meetingId = db.uuid();
        // FIXME pull location from somewhere
        String location = "FIXME";

        entry = new AuditEntry(AuditEvents.MEETING_CREATED,
                json("meeting", meetingId,
                "location", location,
                "group_name", groupTitle,
                        "group_id", groupId,
                        "section_id", section.id),
                new String[] {
                        // FIXME: index useful stuff
                }
        );

        insertAuditEntry(db, entry);

        db.run("insert into seat_meeting (id, group_id, location) values (?, ?, ?)")
                .param(meetingId)
                .param(groupId)
                .param(location)
                .executeUpdate();

        for (String netid : members) {
            entry = new AuditEntry(AuditEvents.MEMBER_ADDED,
                    json("netid", netid,
                            "group_name", groupTitle,
                            "group_id", groupId,
                            "section_id", section.id),
                    new String[] {
                            // FIXME: index useful stuff
                    }
            );

            insertAuditEntry(db, entry);

            db.run("insert into seat_group_members (netid, group_id) values (?, ?)")
                    .param(netid)
                    .param(groupId)
                    .executeUpdate();
        }

        return groupId;
    }

    public static List<String> getMembersForSection(DBConnection db, SeatSection section) throws SQLException {
        List<String> rosterIds = db.run("select sakai_roster_id from seat_group_section_rosters where section_id = ?")
                                    .param(section.id)
                                    .executeQuery()
                                    .getStringColumn("sakai_roster_id");

        Set<String> result = new HashSet<>(); 

        CourseManagementService cms = (CourseManagementService) ComponentManager.get("org.sakaiproject.coursemanagement.api.CourseManagementService");
        for (String rosterId : rosterIds) {
            for (Membership membership : cms.getSectionMemberships(rosterId)) {
                result.add(membership.getUserId());
            }
        }

        return new ArrayList<>(result);
    }

    private static List<List<String>> splitMembersForGroup(List<String> members, int groupCount, SelectionType selection) {
        // FIXME implement weighted selection
        // group by modality
        // shuffle groups
        // generate groups
        Collections.shuffle(members);
        
        List<Integer> groupSizes = new ArrayList<>();
        for (int i=0; i<groupCount; i++) {
            if (i < members.size() % groupCount) {
                groupSizes.add((members.size() / groupCount) + 1);
            } else {
                groupSizes.add(members.size() / groupCount);
            }
        }

        List<List<String>> result = new ArrayList<>();
        for (int groupSize : groupSizes) {
            result.add(members.subList(0, groupSize));
            members = members.subList(groupSize, members.size());
        }

        return result;
    }

    public static void bootstrapGroupsForSection(DBConnection db, SeatSection section, int groupCount, SelectionType selection) throws SQLException {
        for (SeatGroup group : section.listGroups()) {
            for (Meeting meeting : group.listMeetings()) {
                for (SeatAssignment seatAssignment : meeting.listSeatAssignments()) {
                    clearSeat(db, seatAssignment);
                }
    
                deleteMeeting(db, meeting);
            }
    
            deleteGroup(db, group);
        }
    
        List<String> sectionMembers = getMembersForSection(db, section);
    
        List<List<String>> membersPerGroup = splitMembersForGroup(sectionMembers, groupCount, selection);
    
        for (int i=0; i<groupCount; i++) {
            createGroup(db, section, String.format("Group %d", i + 1), membersPerGroup.get(i));
        }
    
        // Create groups
        //  - Create a meeting
        //  - get members for all rosters
        //  - insert seat_group_members for each member
    
        // List<String> groupsToDelete = db.run(
        //        "SELECT sg.id as group_id " +
        //        " from seat_group_selection sel " +
        //        " INNER JOIN seat_group sg ON sel.group_id = sg.id " +
        //        " AND sg.site_id = ? AND sel.sakai_roster_id = ?")
        //     .param(siteId)
        //     .param(section.getProviderGroupId())
        //     .executeQuery()
        //     .getStringColumn("group_id");
    
    
        // try (DBResults results = db.run(
        //                                 "SELECT sg.id as group_id " +
        //                                 " from seat_group_selection sel " +
        //                                 " INNER JOIN seat_group sg ON sel.group_id = sg.id " +
        //                                 " AND sg.site_id = ? AND sel.sakai_roster_id = ?")
        //      .param(siteId)
        //      .param(section.getProviderGroupId())
        //      .executeQuery()) {
        //     for (ResultSet result : results) {
        //         groupsToDelete.add(result.getString("group_id"));
        //     }
        // }
        // 
        // try (DBResults results = db.run(
        //                                 "SELECT group_id, netid " +
        //                                 " from seat_group_members members " +
        //                                 " where group_id in (" + DB.placeholders(groupsToDelete) + ")")
        //      .stringParams(groupsToDelete)
        //      .executeQuery()) {
        // }
    }

}
