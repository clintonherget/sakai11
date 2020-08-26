package edu.nyu.classes.seats;

import edu.nyu.classes.seats.api.SeatsService;
import edu.nyu.classes.seats.Emails;
import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.*;
import edu.nyu.classes.seats.storage.db.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.*;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sakaiproject.user.cover.UserDirectoryService;

public class SeatingHandlerBackgroundTask extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(SeatingHandlerBackgroundTask.class);

    private static long WINDOW_MS = 5000;
    private Map<String, Long> recentProcessed = new LinkedHashMap<>();

    private AtomicBoolean running = new AtomicBoolean(false);

    private static AtomicLong dbTimingThresholdMs = new AtomicLong(-1);


    public static void setDBTimingThresholdMs(long ms) {
        dbTimingThresholdMs.set(ms);
    }

    public long dbTimingThresholdMs() {
        return dbTimingThresholdMs.get();
    }

    private class ToProcess {
        public String siteId;
        public long lastSyncRequestTime;

        public ToProcess(String siteId, long lastSyncRequestTime) {
            this.siteId = siteId;
            this.lastSyncRequestTime = lastSyncRequestTime;
        }
    }

    private List<ToProcess> findSitesToProcess(final long lastTime) {
        final List<ToProcess> result = new ArrayList<>();

        DB.transaction
            ("Find sites to process",
             (DBConnection db) -> {
                db.setTimingEnabled(dbTimingThresholdMs());
                List<String> entries = new ArrayList<>(recentProcessed.keySet());

                for (String e : entries) {
                    if (recentProcessed.size() >= 1024) {
                        recentProcessed.remove(e);
                    }
                }

                db.run("SELECT q.site_id, q.last_sync_requested_time " +
                       " FROM seat_sync_queue q" +
                       " INNER JOIN sakai_site_tool sst ON sst.site_id = q.site_id AND sst.registration = 'nyu.seat-assignments'" +
                       " WHERE q.last_sync_requested_time > ? AND q.last_sync_requested_time > q.last_sync_time")
                    .param(lastTime)
                    .executeQuery()
                    .each((row) -> {
                            String siteId = row.getString("site_id");
                            Long lastSyncRequestedTime = row.getLong("last_sync_requested_time");

                            if (recentProcessed.containsKey(siteId) &&
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
            ("Mark site as processed",
             (DBConnection db) -> {
                db.setTimingEnabled(dbTimingThresholdMs());
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
        long lastMtimeCheck = System.currentTimeMillis();
        long findProcessedSince = 0;

        long loopCount = 0;

        // Syncing to Sakai groups needs a logged in user.
        SakaiGroupSync.login();

        while (this.running.get()) {
            try {
                if (loopCount % 60 == 0) {
                    lastMtimeCheck = runMTimeChecks(lastMtimeCheck);
                }

                findProcessedSince = handleSeatGroupUpdates(findProcessedSince);

                // THINKME: What frequency should we run this at?
                // if (loopCount % 30 == 0) {
                handleSakaiGroupSync();
                // }

            } catch (Exception e) {
                LOG.error("SeatingHandlerBackgroundTask main loop hit top level: " + e);
                e.printStackTrace();
            }

            try {
                loopCount += 1;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.error("Interrupted sleep: " + e);
            }
        }
    }

    private static class SakaiGroupSyncRequest {
        public enum Action {
            SYNC_SEAT_GROUP,
            DELETE_SAKAI_GROUP,
        }

        public final String id;
        public final Action action;
        public final String arg1;

        public SakaiGroupSyncRequest(String id, Action action, String arg1) {
            this.id = id;
            this.action = action;
            this.arg1 = arg1;
        }
    }


    // Bring the Sakai Groups we're managing into line with our Seat Groups
    private void handleSakaiGroupSync() {
        DB.transaction
            ("Synchronize Seat Groups with their Sakai Groups",
             (DBConnection db) -> {
                try {

                    // Process a decent-size chunk, but with an upper limit.  We'll go around again soon anyway
                    List<SakaiGroupSyncRequest> requests =
                        db.run("select * from (select * from seat_sakai_group_sync_queue order by id) where rownum <= 5000")
                        .executeQuery()
                        .map((row) -> new SakaiGroupSyncRequest(row.getString("id"),
                                                                SakaiGroupSyncRequest.Action.valueOf(row.getString("action")),
                                                                row.getString("arg1")));

                    String lastProcessedId = null;
                    Set<String> alreadyProcessedArgs = new HashSet<>();
                    try {
                        for (SakaiGroupSyncRequest request : requests) {

                            // Right now it doesn't make sense to sync a seat group or delete a sakai group
                            // more than once, so skip over the dupes here.
                            if (alreadyProcessedArgs.contains(request.arg1)) {
                                continue;
                            }

                            if (request.action == SakaiGroupSyncRequest.Action.SYNC_SEAT_GROUP) {
                                long startTime = System.currentTimeMillis();
                                SakaiGroupSync.syncSeatGroup(db, request.arg1);

                                LOG.info(String.format("Synced seat group to sakai group in %dms", System.currentTimeMillis() - startTime));

                                alreadyProcessedArgs.add(request.arg1);
                            } else if (request.action == SakaiGroupSyncRequest.Action.DELETE_SAKAI_GROUP) {
                                SakaiGroupSync.deleteSakaiGroup(db, request.arg1);
                                alreadyProcessedArgs.add(request.arg1);
                            } else {
                                LOG.error("Unknown action: " + request.action);
                            }

                            lastProcessedId = request.id;
                        }
                    } finally {
                        if (lastProcessedId != null) {
                            // Mark requests as completed by deleting them.
                            for (SakaiGroupSyncRequest request : requests) {
                                if (request.id.compareTo(lastProcessedId) > 0) {
                                    break;
                                }

                                db.run("delete from seat_sakai_group_sync_queue where id = ?")
                                    .param(request.id)
                                    .executeUpdate();
                            }

                            db.commit();
                        }
                    }

                } catch (Exception e) {
                    LOG.error("Things have gone badly during handleSakaiGroupSync: " + e);
                    e.printStackTrace();
                }

                return null;
            });
    }


    private long runMTimeChecks(long lastCheck) {
        long now = System.currentTimeMillis();

        SeatsService service = (SeatsService) ComponentManager.get("edu.nyu.classes.seats.SeatsService");

        DB.transaction
                ("Mark any site or realm changed in the last 60 seconds for sync",
                 (DBConnection db) -> {
                    db.setTimingEnabled(dbTimingThresholdMs());
                    List<String> updatedSiteIds = db.run("select site_id from sakai_site where modifiedon >= ?")
                        .param(new Date(lastCheck - 60 * 1000), new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC")))
                        .executeQuery()
                        .getStringColumn("site_id");

                    service.markSitesForSync(updatedSiteIds.toArray(new String[0]));

                    List<String> updatedRealmSiteIds = db.run("select ss.site_id from sakai_site ss" +
                                                              " inner join sakai_realm sr on sr.realm_id = concat('/site/', ss.site_id)" +
                                                              " where sr.modifiedon >= ?")
                        .param(new Date(lastCheck - 60 * 1000), new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC")))
                        .executeQuery()
                        .getStringColumn("site_id");

                    service.markSitesForSync(updatedRealmSiteIds.toArray(new String[0]));

                    return null;
                }
                );

        return now;
    }

    public long handleSeatGroupUpdates(long findProcessedSince) {
        long now = System.currentTimeMillis() - WINDOW_MS;
        List<ToProcess> sites = findSitesToProcess(findProcessedSince);

        for (ToProcess entry : sites) {
            long startTime = System.currentTimeMillis();

            if (processSite(entry.siteId)) {
                markAsProcessed(entry, now);
                LOG.info(String.format("Processed site %s in %d ms", entry.siteId, (System.currentTimeMillis() - startTime)));
            }
        }

        return now;
    }

    private void notifyUser(String studentNetId, SeatGroup group, Site site) throws Exception {
        List<org.sakaiproject.user.api.User> studentUser = UserDirectoryService.getUsersByEids(Arrays.asList(new String[] { studentNetId }));

        if (studentUser.size() == 0) {
            return;
        }

        Emails.sendUserAddedEmail(studentUser.get(0), group, site);
    }

    private boolean processSite(String siteId) {
        try {
            if (!Locks.trylockSiteForUpdate(siteId)) {
                // Currently locked.  Skip processing and try again later.
                LOG.info(String.format("Site %s already locked for update.  Skipping...", siteId));

                return false;
            }

            try {
                Site site = SiteService.getSite(siteId);

                DB.transaction
                    ("Bootstrap groups for a site and section",
                     (DBConnection db) -> {
                        try {
                            db.setTimingEnabled(dbTimingThresholdMs());
                            // Sync the rosters
                            for (Group section : site.getGroups()) {
                                if (section.getProviderGroupId() == null) {
                                    continue;
                                }

                                String rosterId = section.getProviderGroupId();
                                String sponsorStemName = SeatsStorage.getSponsorSectionId(db, rosterId);

                                if (Utils.rosterToStemName(rosterId).equals(sponsorStemName)) {
                                    SeatsStorage.ensureRosterEntry(db, site.getId(), sponsorStemName, Optional.empty());
                                } else {
                                    SeatsStorage.ensureRosterEntry(db, site.getId(), sponsorStemName, Optional.of(rosterId));
                                }
                            }

                            for (SeatSection section : SeatsStorage.siteSeatSections(db, siteId)) {
                                if (section.provisioned) {
                                    SeatsStorage.SyncResult syncResult = SeatsStorage.syncGroupsToSection(db, section, site);

                                    if (section.listGroups().size() > 1) {
                                        for (Map.Entry<String, List<Member>> entry : syncResult.adds.entrySet()) {
                                            String groupId = entry.getKey();

                                            for (Member m : entry.getValue()) {
                                                if (m.isInstructor()) {
                                                    // No email sent to instructors
                                                    continue;
                                                }

                                                Optional<SeatGroup> group = section.fetchGroup(groupId);
                                                if (group.isPresent()) {
                                                    try {
                                                        notifyUser(m.netid, group.get(), site);
                                                    } catch (Exception e) {
                                                        LOG.error(String.format("Failure while notifying user '%s' in group '%s' for site '%s': %s",
                                                                                m.netid,
                                                                                group.get().id,
                                                                                site.getId(),
                                                                                e));
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    SeatsStorage.bootstrapGroupsForSection(db, section, 1, SeatsStorage.SelectionType.RANDOM);
                                }
                            }

                            db.commit();

                            return null;
                        } catch (Exception e) {
                            db.rollback();
                            throw e;
                        }
                    });

                return true;
            } catch (IdUnusedException e) {
                LOG.info("SeatJob: site not found: " + siteId);
                return true;
            } catch (Exception e) {
                LOG.error(String.format("Error while processing site '%s': ", siteId) + e);
                e.printStackTrace();
                return true;
            }
        } finally {
            Locks.unlockSiteForUpdate(siteId);
        }
    }
}
