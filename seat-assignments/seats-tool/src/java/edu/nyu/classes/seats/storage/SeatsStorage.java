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
        WEIGHTED,
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

        db.run("select sg.*, sec.id as section_id" +
               " from seat_group sg" +
               " inner join seat_group_section sec on sg.section_id = sec.id " +
               " where sec.id = ?")
            .param(sectionId)
            .executeQuery()
            .each(row -> {
                    section.addGroup(row.getString("id"), row.getString("name"), row.getString("description"));
                });

        if (section.groupIds().isEmpty()) {
            return section;
        }

        db.run("select sm.group_id, sm.id as meeting_id" +
               " from seat_meeting sm" +
               " where sm.group_id in (" + DB.placeholders(section.groupIds()) + ")")
            .stringParams(section.groupIds())
            .executeQuery()
            .each(row -> {
                    section.fetchGroup(row.getString("group_id"))
                        .get()
                        .getOrCreateMeeting(row.getString("meeting_id"));
                });

        db.run("select sm.group_id, sm.id as meeting_id, assign.id as assign_id, assign.netid, assign.seat" +
               " from seat_meeting sm" +
               " inner join seat_meeting_assignment assign on assign.meeting_id = sm.id" +
               " where sm.group_id in (" + DB.placeholders(section.groupIds()) + ")")
            .stringParams(section.groupIds())
            .executeQuery()
            .each(row -> {
                    section.fetchGroup(row.getString("group_id"))
                        .get()
                        .getOrCreateMeeting(row.getString("meeting_id"))
                        .addSeatAssignment(row.getString("assign_id"), row.getString("netid"), row.getString("seat"));
                });

        return section;
    }

    private static String createGroup(DBConnection db, SeatSection section, String groupTitle, List<Member> members) throws SQLException {
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

        for (Member member : members) {
            entry = new AuditEntry(AuditEvents.MEMBER_ADDED,
                    json("netid", member.netid,
                            "modality", member.modality.toString(),
                            "group_name", groupTitle,
                            "group_id", groupId,
                            "section_id", section.id),
                    new String[] {
                            // FIXME: index useful stuff
                    }
            );

            insertAuditEntry(db, entry);

            db.run("insert into seat_group_members (netid, group_id) values (?, ?)")
                    .param(member.netid)
                    .param(groupId)
                    .executeUpdate();
        }

        return groupId;
    }

    public static List<Member> getMembersForSection(DBConnection db, SeatSection section) throws SQLException {
        List<String> rosterIds = db.run("select sakai_roster_id from seat_group_section_rosters where section_id = ?")
                                    .param(section.id)
                                    .executeQuery()
                                    .getStringColumn("sakai_roster_id");

        Set<Member> result = new HashSet<>();

        CourseManagementService cms = (CourseManagementService) ComponentManager.get("org.sakaiproject.coursemanagement.api.CourseManagementService");
        for (String rosterId : rosterIds) {
            for (Membership membership : cms.getSectionMemberships(rosterId)) {
                result.add(new Member(membership.getUserId()));
            }
        }

        return new ArrayList<>(result);
    }

    private static List<List<Member>> splitMembersForGroup(List<Member> members, int groupCount, SelectionType selection) {
        List<List<Member>> result = new ArrayList<>();

        Collections.shuffle(members);

        if (SelectionType.WEIGHTED.equals(selection)) {
            Collections.sort(members, new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    if (Member.Modality.ONLINE.equals(m1.modality)) {
                        return -1;
                    } else if (Member.Modality.ONLINE.equals(m2.modality)) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            List<Integer> groupSizes = new ArrayList<>();
            for (int i=0; i<groupCount; i++) {
                if (i < members.size() % groupCount) {
                    groupSizes.add((members.size() / groupCount) + 1);
                } else {
                    groupSizes.add(members.size() / groupCount);
                }
            }

            for (int groupSize : groupSizes) {
                result.add(members.subList(0, groupSize));
                members = members.subList(groupSize, members.size());
            }
        } else {
            // random
            for (int i=0; i<groupCount; i++) {
                result.add(new ArrayList<>());
            }

            Map<Member.Modality, List<Member>> groupedMembers = members.stream().collect(Collectors.groupingBy((m) -> m.modality));

            ArrayDeque<List<Member>> memberGroups = new ArrayDeque<>(groupedMembers.values());
            int currentGroup = 0;
            while(!memberGroups.isEmpty()) {
                List<Member> groupToProcess = memberGroups.removeFirst();
                for (Member member : groupToProcess) {
                    result.get(currentGroup).add(member);
                    currentGroup = (currentGroup + 1) % groupCount;
                }
            }
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

        List<Member> sectionMembers = getMembersForSection(db, section);

        List<List<Member>> membersPerGroup = splitMembersForGroup(sectionMembers, groupCount, selection);

        for (int i=0; i<groupCount; i++) {
            createGroup(db, section, String.format("Group %d", i + 1), membersPerGroup.get(i));
        }
    }

    public static String getSponsorSectionId(String rosterId) {
        // FIXME: resolve the sponsor course roster if this isn't it.
        return rosterId;
    }


    public static void ensureRosterEntry(DBConnection db, String siteId, String primaryRosterId, Optional<String> secondaryRosterId) throws SQLException {
        Optional<String> sectionId = db.run("select id from seat_group_section where primary_roster_id = ? AND site_id = ?")
            .param(primaryRosterId)
            .param(siteId)
            .executeQuery()
            .oneString();

        if (!sectionId.isPresent()) {
            sectionId = Optional.of(UUID.randomUUID().toString());

            db.run("insert into seat_group_section (id, primary_roster_id, site_id) values (?, ?, ?)")
                .param(sectionId.get())
                .param(primaryRosterId)
                .param(siteId)
                .executeUpdate();

            db.run("insert into seat_group_section_rosters (section_id, sakai_roster_id, role) values (?, ?, ?)")
                .param(sectionId.get())
                .param(primaryRosterId)
                .param("primary")
                .executeUpdate();
        }

        if (secondaryRosterId.isPresent()) {
            int count = db.run("select count(1) from seat_group_section_rosters where section_id = ? AND sakai_roster_id = ?")
                .param(sectionId.get())
                .param(secondaryRosterId.get())
                .executeQuery()
                .getCount();

            if (count == 0) {
                db.run("insert into seat_group_section_rosters (section_id, sakai_roster_id, role) values (?, ?, ?)")
                    .param(sectionId.get())
                    .param(secondaryRosterId.get())
                    .param("secondary")
                    .executeUpdate();
            }
        }
    }

    public static List<SeatSection> siteSeatSections(DBConnection db, String siteId) throws SQLException {
        return db.run("select id from seat_group_section where site_id = ?")
            .param(siteId)
            .executeQuery()
            .map(row -> SeatsStorage.getSeatSection(db, row.getString("id")));
    }

}
