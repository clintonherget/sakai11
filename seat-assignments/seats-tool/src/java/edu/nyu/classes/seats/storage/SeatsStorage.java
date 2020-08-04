package edu.nyu.classes.seats.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.*;
import org.sakaiproject.site.api.Group;

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
    }

    public void runDBMigrations() {
        BaseMigration.runMigrations();
    }

    private static class AuditEntry {
        private AuditEvents event;
        private String json;
        private String[] args;

        public AuditEntry(AuditEvents event, String json, String[] args) {
            this.event = event;
            this.json = json;
            this.args = args;
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

    public static void insertAuditEntry(DBConnection db, AuditEntry entry) {
        System.err.println(entry);
        System.err.println(entry.json);
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
        AuditEntry entry = new AuditEntry(AuditEvents.MEETING_DELETED,
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
        db.run("delete from seat_group_section where group_id = ?")
            .param(group.id)
            .executeUpdate();
        db.run("delete from seat_group where group_id = ?")
            .param(group.id)
            .executeUpdate();
    }

    public static SeatSection getSeatSection(DBConnection db, String siteId, String sectionId) {
        try {
            SeatSection section = new SeatSection(sectionId);

            try (DBResults rows = db.run("select sg.*, sec.id as section_id" +
                                         " from seat_group sg" +
                                         " inner join seat_group_section sec on sg.section_id = sec.id " +
                                         " where sec.sakai_roster_id = ? AND sg.site_id = ?")
                 .param(sectionId)
                 .param(siteId)
                 .executeQuery()) {
                for (ResultSet row : rows) {
                    section.addGroup(row.getString("id"), row.getString("name"), row.getString("description"));
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void bootstrapGroupsForSection(SeatSection section, int groupCount, SelectionType selection) {
        DB.transaction
            ("Bootstrap groups for a site and section",
             (DBConnection db) -> {
                for (SeatGroup group : section.listGroups()) {
                    for (Meeting meeting : group.listMeetings()) {
                        for (SeatAssignment seatAssignment : meeting.listSeatAssignments()) {
                            clearSeat(db, seatAssignment);
                        }

                        deleteMeeting(db, meeting);
                    }

                    deleteGroup(db, group);
                }

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

                return null;
            });
    }

}
