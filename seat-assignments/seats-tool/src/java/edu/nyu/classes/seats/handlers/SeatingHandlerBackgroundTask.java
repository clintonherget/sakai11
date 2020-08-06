package edu.nyu.classes.seats.handlers;

import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.*;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;

import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.*;

import edu.nyu.classes.seats.api.SeatsService;

import org.sakaiproject.component.cover.ComponentManager;

// SeatsService service = (SeatsService) ComponentManager.get("edu.nyu.classes.seats.SeatsService");

// DB.transaction
//     ("@DEBUG TESTING",
//      (DBConnection db) -> {
//         java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(64);
//
//         for (String siteId : db.run("select site_id from sakai_site").executeQuery().getStringColumn("site_id")) {
//             pool.execute(() -> { service.markSitesForSync(siteId); });
//         }
//
//         pool.shutdown();
//
//         return null;
//     });
// Get the sections (possibly combined) - getMembers()
//   - group by combined rosters (crosslisted and explicitly combined)
//   - gives us list of SeatSections
//   - match each SeatSection to existing groups by any of their contained section ids
//   - create a new group for any SeatSection that didn't match

// Create groups...
//   (creates a meeting)
//   (assigns the initial student set)




// TODO: make this a background thread/quartz job
public class SeatingHandlerBackgroundTask extends Thread {

    private static long WINDOW_MS = 5000;
    private Map<String, Long> recentProcessed = new LinkedHashMap<>();

    private AtomicBoolean running = new AtomicBoolean(false);

    private class ToProcess {
        public String siteId;
        public long lastSyncRequestTime;

        public ToProcess(String siteId, long lastSyncRequestTime) {
            this.siteId = siteId;
            this.lastSyncRequestTime = lastSyncRequestTime;
        }
    }

    private List<ToProcess> findSitesToProcess() {
        // TODO: Stuff this might look at:
        //
        // site createdon (site created)
        // roster added - site realm update
        // person added to roster - ???
        // new section cross-listed/combined in SIS - ???
        // return Arrays.asList(new String[] { "fdb7a928-f38a-4154-ae46-43b44ad7b5dd", "e682f774-e900-4e00-b555-c9974e06c556", "fe0b92a0-34ec-4bc7-be24-1518f9ad9a81" });

        final List<ToProcess> result = new ArrayList<>();

        DB.transaction
            ("Find sites to process",
             (DBConnection db) -> {
                List<String> entries = new ArrayList(recentProcessed.keySet());

                for (String e : entries) {
                    if (recentProcessed.size() >= 1024) {
                        recentProcessed.remove(e);
                    }
                }

                db.run("SELECT q.site_id, q.last_sync_requested_time " +
                       " FROM seat_sync_queue q" +
                       " INNER JOIN sakai_site_tool sst ON sst.site_id = q.site_id AND sst.registration = 'nyu.seat-assignments'" +
                       " WHERE q.last_sync_requested_time > q.last_sync_time")
                    .executeQuery()
                    .each((row) -> {
                            String siteId = row.getString("site_id");
                            Long lastSyncRequestedTime = row.getLong("last_sync_requested_time");

                            if (recentProcessed.containsKey(siteId) &&
                                lastSyncRequestedTime != null &&
                                recentProcessed.get(siteId).equals(lastSyncRequestedTime)) {
                                // Already handled this one
                            } else {
                                result.add(new ToProcess(siteId, lastSyncRequestedTime));
                            }
                        });

                return null;
            });

        return result;
    }

    private void markAsProcessed(ToProcess entry, long timestamp) {
        DB.transaction
            ("Find sites to process",
             (DBConnection db) -> {
                db.run("update seat_sync_queue set last_sync_time = ? where site_id = ?")
                    .param(timestamp)
                    .param(entry.siteId)
                    .executeUpdate();

                recentProcessed.put(entry.siteId, entry.lastSyncRequestTime);

                db.commit();
                return null;
            });
    }


    public SeatingHandlerBackgroundTask startThread() {
        this.running = new AtomicBoolean(true);
        this.setDaemon(true);
        this.start();

        return this;
    }

    public void shutdown() {
        this.running.set(false);

        try {
            this.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void run() {
        while (this.running.get()) {
            runRound();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void runRound() {
        long now = System.currentTimeMillis() - WINDOW_MS;
        List<ToProcess> sites = findSitesToProcess();

        for (ToProcess entry : sites) {
            if (processSite(entry.siteId)) {
                markAsProcessed(entry, now);
            }
        }
    }

    private boolean processSite(String siteId) {
        try {
            if (!SeatsStorage.trylockSiteForUpdate(siteId)) {
                // Currently locked.  Skip processing and try again later.
                return false;
            }

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
                                SeatsStorage.ensureRosterEntry(db, site.getId(), sponsorRosterId, Optional.empty());
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

                return true;
            } catch (IdUnusedException e) {
                System.err.println("SeatJob: site not found: " + siteId);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            SeatsStorage.unlockSiteForUpdate(siteId);
        }
    }
}
