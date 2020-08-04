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
                                    String sponsorRosterId = SeatsStorage.getSponsorSectionId(rosterId);

                                    if (rosterId.equals(sponsorRosterId)) {
                                        SeatsStorage.ensureRosterEntry(db, site.getId(), sponsorRosterId, Optional.of(null));
                                    } else {
                                        SeatsStorage.ensureRosterEntry(db, site.getId(), sponsorRosterId, Optional.of(rosterId));
                                    }
                                }

                                for (SeatSection section : SeatsStorage.siteSeatSections(db, siteId)) {
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
