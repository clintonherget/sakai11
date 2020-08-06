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
    public static int EDIT_WINDOW_MS = 5 * 60 * 1000;

    public enum SelectionType {
        RANDOM,
        WEIGHTED,
    }

    public enum AuditEvents {
        SEAT_CLEARED,
        SEAT_ASSIGNED,
        SEAT_REASSIGNED,
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

    public static void syncGroupsToSection(DBConnection db, SeatSection section) throws SQLException {
        List<Member> sectionMembers = getMembersForSection(db, section);
        Set<String> seatGroupMembers = new HashSet<>();
        Map<String, Integer> groupCounts = new HashMap<>();

        for (SeatGroup group : section.listGroups()) {
            groupCounts.put(group.id, group.listMeetings().size());
            seatGroupMembers.addAll(group.listMembers().stream().map(m -> m.netid).collect(Collectors.toList()));
        }

        for (Member member : sectionMembers) {
            if (seatGroupMembers.contains(member.netid)) {
                continue;
            }

            String groupId = groupCounts.keySet().stream().min((o1, o2) -> groupCounts.get(o1) - groupCounts.get(o2)).get();
            addMemberToGroup(db, member, groupId, section.id);
            groupCounts.put(groupId, groupCounts.get(groupId) + 1);
        }
    }; 

    public static void addMemberToGroup(DBConnection db, Member member, String groupId, String sectionId) throws SQLException {
        db.run("insert into seat_group_members (netid, group_id, official) values (?, ?, ?)")
                .param(member.netid)
                .param(groupId)
                .param(member.official ? 1 : 0)
                .executeUpdate();

        AuditEntry entry = new AuditEntry(AuditEvents.MEMBER_ADDED,
                json("netid", member.netid,
                        "modality", member.modality.toString(),
                        "group_id", groupId,
                        "section_id", sectionId),
                new String[] {
                        // FIXME: index useful stuff
                }
        );

        insertAuditEntry(db, entry);
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

        int rowsUpdated = db.run("delete from seat_meeting_assignment where netid = ? and meeting_id = ?")
            .param(seat.netid)
            .param(seat.meeting.id)
            .executeUpdate();

        if (rowsUpdated > 0) {
            insertAuditEntry(db, entry);
        }
    }

    public static boolean setSeat(DBConnection db, SeatAssignment seat, String lastSeat, boolean shouldSetEditWindow) throws SQLException {
        db.run("select id" +
                " from seat_meeting_assignment" +
                " where meeting_id = ? and netid = ?")
                .param(seat.meeting.id)
                .param(seat.netid)
                .executeQuery()
                .each(row -> {
                    seat.id = row.getString("id");
                });

        long editWindow = shouldSetEditWindow ? System.currentTimeMillis() + EDIT_WINDOW_MS : 0;

        if (seat.id == null) {
            try {
                db.run("insert into seat_meeting_assignment (id, meeting_id, editable_until, netid, seat) values (?, ?, ?, ?, ?)")
                        .param(db.uuid())
                        .param(seat.meeting.id)
                        .param(editWindow)
                        .param(seat.netid)
                        .param(seat.seat)
                        .executeUpdate();
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) {
                    return false;
                } else {
                    throw e;
                }
            }
        } else {
            try {
                int updated = db.run("update seat_meeting_assignment set editable_until = ?, seat = ? where id = ? and seat = ?")
                        .param(editWindow)
                        .param(seat.seat)
                        .param(seat.id)
                        .param(lastSeat)
                        .executeUpdate();

                if (updated == 0) {
                    return false;
                }
            } catch (SQLException e) {
                if (e.getSQLState().startsWith("23")) {
                    return false;
                } else {
                    throw e;
                }
            }
        }

        AuditEvents eventType = (seat.id == null) ? AuditEvents.SEAT_ASSIGNED : AuditEvents.SEAT_REASSIGNED;
        AuditEntry entry = new AuditEntry(eventType,
                json("seat", seat.seat,
                        "lastSeat", lastSeat,
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

        return true;
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

    public static SeatSection getSeatSection(DBConnection db, String sectionId, String siteId) throws SQLException {
        List<SeatSection> sections = db.run("select * from seat_group_section where id = ?")
                .param(sectionId)
                .executeQuery()
                .map(row -> {
                    return new SeatSection(sectionId, siteId, row.getInt("provisioned") == 1, row.getInt("has_split") == 1);
                });

        if (sections.isEmpty()) {
            // FIXME 404?
            throw new RuntimeException("Section does not exist");
        }

        SeatSection section = sections.get(0);

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

        db.run("select sg.*, mem.netid, mem.official" +
               " from seat_group sg" +
               " inner join seat_group_members mem on sg.id = mem.group_id " +
               " where sg.id in (" + db.placeholders(section.groupIds()) + ")")
            .stringParams(section.groupIds())
            .executeQuery()
            .each(row -> {
                    section.fetchGroup(row.getString("id"))
                        .get()
                        .addMember(row.getString("netid"),
                                   row.getInt("official") == 1);
                });

        db.run("select sm.group_id, sm.id as meeting_id" +
               " from seat_meeting sm" +
               " where sm.group_id in (" + db.placeholders(section.groupIds()) + ")")
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
               " where sm.group_id in (" + db.placeholders(section.groupIds()) + ")")
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
            addMemberToGroup(db, member, groupId, section.id);
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
                result.add(new Member(membership.getUserId(), true));
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

        markSectionAsProvisioned(db, section);
    }

    public static void markSectionAsProvisioned(DBConnection db, SeatSection section) throws SQLException {
        db.run("update seat_group_section set provisioned = 1 where id = ?")
            .param(section.id)
            .executeUpdate();
    }



    public static void markSectionAsSplit(DBConnection db, SeatSection section) throws SQLException {
        db.run("update seat_group_section set has_split = 1 where id = ?")
                .param(section.id)
                .executeUpdate();
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

            db.run("insert into seat_group_section (id, primary_roster_id, site_id, provisioned, has_split) values (?, ?, ?, ?, ?)")
                .param(sectionId.get())
                .param(primaryRosterId)
                .param(siteId)
                .param(0)
                .param(0)
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
        return db.run("select * from seat_group_section where site_id = ?")
            .param(siteId)
            .executeQuery()
            .map(row -> SeatsStorage.getSeatSection(db, row.getString("id"), siteId));
    }

    // If you've held a lock this long, something is wrong and we'll forcibly unlock
    // you.
    private static int MAX_LOCK_AGE_MS = 5000;

    public static boolean trylockSiteForUpdate(String siteId) {
        return DB.transaction("Get a lock for site: " + siteId,
                              (db) -> {
                                  long now = System.currentTimeMillis();
                                  db.run("delete from seat_sync_locks where site_id = ? AND lock_time < ?")
                                      .param(siteId)
                                      .param(now - MAX_LOCK_AGE_MS)
                                      .executeUpdate();

                                  try {
                                      db.run("insert into seat_sync_locks (site_id, lock_time) values (?, ?)")
                                          .param(siteId)
                                          .param(now)
                                          .executeUpdate();
                                  } catch (SQLException e) {
                                      if (e.getSQLState().startsWith("23")) {
                                          db.rollback();

                                          return false;
                                      } else {
                                          throw e;
                                      }
                                  }

                                  db.commit();
                                  return true;
                              });
    }

    public static void lockSiteForUpdate(String siteId) {
        // This should terminate when the lock succeeds...
        for (int i = 0; i < 10; i++) {
            if (trylockSiteForUpdate(siteId)) {
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void unlockSiteForUpdate(String siteId) {
        DB.transaction("Unlock site: " + siteId,
                       (db) -> {
                           db.run("delete from seat_sync_locks where site_id = ?").param(siteId).executeUpdate();
                           db.commit();

                           return null;
                       });
    }

}
