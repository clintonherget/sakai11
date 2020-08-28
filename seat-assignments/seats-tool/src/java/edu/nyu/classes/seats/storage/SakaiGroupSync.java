package edu.nyu.classes.seats.storage;

import edu.nyu.classes.seats.models.Member;
import edu.nyu.classes.seats.storage.db.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SakaiGroupSync {
    private static final Logger LOG = LoggerFactory.getLogger(SakaiGroupSync.class);
    private static final String IS_SEAT_GROUP_PROPERTY = "isSeatGroup";

    private static AtomicLong sequence = new AtomicLong(0);

    private static String buildId(DBConnection db) {
        return String.format("%013d_%016d_%s", System.currentTimeMillis(), sequence.incrementAndGet(), db.uuid());
    }

    public static void markGroupForSync(DBConnection db, String seatGroupId) throws SQLException {
        db.run("insert into seat_sakai_group_sync_queue (id, action, arg1) VALUES (?, ?, ?)")
            .param(buildId(db))
            .param("SYNC_SEAT_GROUP")
            .param(seatGroupId)
            .executeUpdate();
    }

    public static void markGroupForDelete(DBConnection db, String sakaiGroupId) throws SQLException {
        db.run("insert into seat_sakai_group_sync_queue (id, action, arg1) VALUES (?, ?, ?)")
            .param(buildId(db))
            .param("DELETE_SAKAI_GROUP")
            .param(sakaiGroupId)
            .executeUpdate();
    }


    public static void syncSeatGroup(DBConnection db, String seatGroupId) throws Exception {
        // Load our seat group members (keyed on sakai user_id)
        Map<String, Member> seatGroupMembers = new HashMap<>();

        db.run("select mem.netid, map.user_id, mem.role, mem.official" +
               " from seat_group sg" +
               " inner join seat_group_members mem on sg.id = mem.group_id" +
               " inner join sakai_user_id_map map on map.eid = mem.netid" +
               " where sg.id = ?")
            .param(seatGroupId)
            .executeQuery()
            .each((row) -> {
                    seatGroupMembers.put(row.getString("user_id"),
                                         new Member(row.getString("netid"),
                                                    row.getInt("official") == 1,
                                                    Member.Role.valueOf(row.getString("role")),
                                                    // unused here
                                                    Member.StudentLocation.IN_PERSON));
                });

        db.run("select sec.site_id, sg.sakai_group_id, sg.name, sg.description" +
               " from seat_group sg" +
               " inner join seat_group_section sec on sec.id = sg.section_id" +
               " where sg.id = ?")
            .param(seatGroupId)
            .executeQuery()
            .each((row) -> {
                    // Actually only expecting one row here...
                    String siteId = row.getString("site_id");
                    try {
                        Site site = SiteService.getSite(siteId);
                        Optional<Group> group = Optional.ofNullable(row.getString("sakai_group_id"))
                            .map((groupId) -> site.getGroup(groupId));

                        String groupDescription = row.getString("description");
                        if (groupDescription == null) {
                            groupDescription = "";
                        }

                        if (!group.isPresent()) {
                            // Either sg.sakai_group_id was null, or the groupId is bogus.  This might
                            // happen if a section is moved between sites, or if an instructor manually
                            // deleted our group.  Either way, you're getting a new group.
                            group = createSakaiGroup(db, site, seatGroupId,
                                                     String.format("Cohort: %s", row.getString("name")),
                                                     groupDescription);
                        }

                        if (!group.isPresent())  {
                            // We tried and failed to create the group.  Bailing out.
                            return;
                        }

                        group.get().setDescription(groupDescription);
                        applyMemberUpdates(group.get(), seatGroupMembers);
                        SiteService.save(site);
                    } catch (IdUnusedException e) {
                        LOG.error("site not found: " + siteId);
                    } catch (PermissionException e) {
                        LOG.error("Permission denied updating site: " + siteId);
                    }
                });
    }

    private static void applyMemberUpdates(Group sakaiGroup, Map<String, Member> seatGroupMembers) {
        // Temporarily unlock the group for our own nefarious purposes
        String lock = sakaiGroup.getProperties().getProperty(Group.GROUP_PROP_LOCKED_BY);
        sakaiGroup.unlockGroup();

        sakaiGroup.removeMembers();
        for (String userId : seatGroupMembers.keySet()) {
            Member m = seatGroupMembers.get(userId);
            sakaiGroup.insertMember(userId, m.sakaiRoleId(), true, false);
        }

        sakaiGroup.lockGroup(lock);
    }

    private static Optional<Group> createSakaiGroup(DBConnection db, Site site, String seatGroupId, String groupTitle, String groupDescription) {
        try {
            Group newGroup = site.addGroup();
            newGroup.getProperties().addProperty(IS_SEAT_GROUP_PROPERTY, "true");
            newGroup.getProperties().addProperty(Group.GROUP_PROP_WSETUP_CREATED, "true");
            newGroup.getProperties().addProperty(Group.GROUP_PROP_LOCKED_BY, "nyu.seat-assignments");

            newGroup.setTitle(groupTitle);
            newGroup.setDescription(groupDescription);

            if (db.run("update seat_group set sakai_group_id = ? where id = ?")
                .param(newGroup.getId())
                .param(seatGroupId)
                .executeUpdate() > 0) {

                return Optional.of(newGroup);
            }
        } catch (Exception e) {
            LOG.error(String.format("Failure while creating new group for site '%s', seat group: '%s': %s",
                                    site.getId(), seatGroupId, e));
            e.printStackTrace();
        }

        // Failcake
        return Optional.empty();
    }

    public static void deleteSakaiGroup(DBConnection db, String sakaiGroupId) {
        try {
            Optional<String> siteId = db.run("select site_id from sakai_site_group where group_id = ?")
                .param(sakaiGroupId)
                .executeQuery()
                .oneString();

            if (!siteId.isPresent()) {
                return;
            }

            Site site = SiteService.getSite(siteId.get());
            Group group = site.getGroup(sakaiGroupId);

            if (group == null) {
                LOG.error("No group found for ID: " + sakaiGroupId);
                return;
            }

            if ("true".equals(group.getProperties().getProperty(IS_SEAT_GROUP_PROPERTY))) {
                group.unlockGroup();
                site.removeGroup(group);
                SiteService.save(site);
            } else {
                LOG.error("Refusing to delete a non-seat-group group");
                return;
            }
        } catch (Exception e) {
            LOG.error(String.format("Failure ignored while trying to delete group '%s': %s",
                                    sakaiGroupId, e));
            e.printStackTrace();
        }
    }
}
