package edu.nyu.classes.seats;

import edu.nyu.classes.seats.api.SeatsService;
import edu.nyu.classes.seats.Emails;
import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.*;
import edu.nyu.classes.seats.storage.db.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sakaiproject.user.cover.UserDirectoryService;

public class SeatGroupUpdatesTask {
    private static final Logger LOG = LoggerFactory.getLogger(SeatGroupUpdatesTask.class);

    private static long WINDOW_MS = 5000;
    private static Map<String, Long> recentProcessed = new LinkedHashMap<>();
    private static AtomicLong dbTimingThresholdMs = new AtomicLong(-1);

    private static class ToProcess {
        public String siteId;
        public long lastSyncRequestTime;

        public ToProcess(String siteId, long lastSyncRequestTime) {
            this.siteId = siteId;
            this.lastSyncRequestTime = lastSyncRequestTime;
        }
    }

    private static List<ToProcess> findSitesToProcess(final long lastTime) {
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

    private static void markAsProcessed(ToProcess entry, long timestamp) {
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


    public static long runMTimeChecks(long lastCheck) {
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

    public static long handleSeatGroupUpdates(long findProcessedSince) {
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

    private static void notifyUser(String studentNetId, SeatGroup group, Site site) throws Exception {
        List<org.sakaiproject.user.api.User> studentUser = UserDirectoryService.getUsersByEids(Arrays.asList(new String[] { studentNetId }));

        if (studentUser.size() == 0) {
            return;
        }

        Emails.sendUserAddedEmail(studentUser.get(0), group, site);
    }

    private static boolean processSite(String siteId) {
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

    public static void setDBTimingThresholdMs(long ms) {
        dbTimingThresholdMs.set(ms);
    }

    private static long dbTimingThresholdMs() {
        return dbTimingThresholdMs.get();
    }
}
