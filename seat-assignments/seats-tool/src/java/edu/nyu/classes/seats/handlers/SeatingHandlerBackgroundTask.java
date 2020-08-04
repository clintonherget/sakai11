package edu.nyu.classes.seats.handlers;

import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;

import java.sql.*;
import java.util.*;
import java.util.stream.*;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;

import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.*;

// TODO: make this a background thread/quartz job
public class SeatingHandlerBackgroundTask {

    private List<String> findSitesToProcess() {
        // TODO: Stuff this might look at:
        //
        // site createdon (site created)
        // roster added - site realm update
        // person added to roster - ???
        // new section cross-listed/combined in SIS - ???
        return Arrays.asList(new String[] { "fdb7a928-f38a-4154-ae46-43b44ad7b5dd", "e682f774-e900-4e00-b555-c9974e06c556" });
    }


    public void run() {
        List<String> siteIds = findSitesToProcess();

        // Get the sections (possibly combined) - getMembers()
        //   - group by combined rosters (crosslisted and explicitly combined)
        //   - gives us list of SeatSections
        //   - match each SeatSection to existing groups by any of their contained section ids
        //   - create a new group for any SeatSection that didn't match

        // Create groups...
        //   (creates a meeting)
        //   (assigns the initial student set)

        for (String siteId : siteIds) {
            processSite(siteId);
        }

        System.err.println("Hi!");
    }


    private static String getSponsorSectionId(String rosterId) {
        // FIXME: resolve the sponsor course roster if this isn't it.
        return rosterId;
    }


    private void ensureRosterEntry(DBConnection db, String siteId, String primaryRosterId, String secondaryRosterId) throws SQLException {
        List<String> existingId = db.run("select id from seat_group_section where primary_roster_id = ? AND site_id = ?")
            .param(primaryRosterId)
            .param(siteId)
            .executeQuery()
            .getStringColumn("id");

        String sectionId = existingId.size() > 0 ? existingId.get(0) : null;

        if (sectionId == null) {
            sectionId = UUID.randomUUID().toString();

            db.run("insert into seat_group_section (id, primary_roster_id, site_id) values (?, ?, ?)")
                .param(sectionId)
                .param(primaryRosterId)
                .param(siteId)
                .executeUpdate();

            db.run("insert into seat_group_section_rosters (section_id, sakai_roster_id, role) values (?, ?, ?)")
                .param(sectionId)
                .param(primaryRosterId)
                .param("primary")
                .executeUpdate();
        }

        if (secondaryRosterId != null) {
            int count = db.run("select count(1) from seat_group_section_rosters where section_id = ? AND sakai_roster_id = ?")
                .param(sectionId)
                .param(secondaryRosterId)
                .executeQuery()
                .getCount();

            if (count == 0) {
                db.run("insert into seat_group_section_rosters (section_id, sakai_roster_id, role) values (?, ?, ?)")
                    .param(sectionId)
                    .param(secondaryRosterId)
                    .param("secondary")
                    .executeUpdate();
            }
        }
    }

    private List<SeatSection> siteSeatSections(DBConnection db, String siteId) throws SQLException {
        return db.run("select id from seat_group_section where site_id = ?")
            .param(siteId)
            .executeQuery()
            .stream()
            .map((row) -> {
                    try {
                        return SeatsStorage.getSeatSection(db, row.getString("id"));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
            .collect(Collectors.toList());
    }

    private void processSite(String siteId) {
        try {
            Site site = SiteService.getSite(siteId);

            DB.transaction
                    ("Bootstrap groups for a site and section",
                            (DBConnection db) -> {
                                // Sync the rosters
                                for (Group section : site.getGroups()) {
                                    if (section.getProviderGroupId() == null) {
                                        continue;
                                    }

                                    String rosterId = section.getProviderGroupId();
                                    String sponsorRosterId = getSponsorSectionId(rosterId);

                                    if (rosterId.equals(sponsorRosterId)) {
                                        ensureRosterEntry(db, site.getId(), sponsorRosterId, null);
                                    } else {
                                        ensureRosterEntry(db, site.getId(), sponsorRosterId, rosterId);
                                    }
                                }

                                for (SeatSection section : siteSeatSections(db, siteId)) {
                                    SeatsStorage.bootstrapGroupsForSection(db, section, 1, SeatsStorage.SelectionType.RANDOM);
                                }

                                db.commit();

                                return null;
                            });
        } catch (IdUnusedException e) {
            System.err.println("SeatJob: site not found: " + siteId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
