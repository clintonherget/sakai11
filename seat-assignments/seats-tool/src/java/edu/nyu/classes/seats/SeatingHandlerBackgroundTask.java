package edu.nyu.classes.seats;

import edu.nyu.classes.seats.api.SeatsService;
import edu.nyu.classes.seats.models.*;
import edu.nyu.classes.seats.storage.*;
import edu.nyu.classes.seats.storage.db.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.*;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sakaiproject.email.cover.EmailService;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.email.api.EmailAddress.RecipientType;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.email.api.EmailAddress;
import org.sakaiproject.email.api.EmailMessage;

public class SeatingHandlerBackgroundTask extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(SeatingHandlerBackgroundTask.class);

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

    private List<ToProcess> findSitesToProcess(final long lastTime) {
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

        while (this.running.get()) {
            if ((System.currentTimeMillis() / 1000) % 60 == 0) {
                lastMtimeCheck = runMTimeChecks(lastMtimeCheck);
            }

            findProcessedSince = runRound(findProcessedSince);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private long runMTimeChecks(long lastCheck) {
        long now = System.currentTimeMillis();

        SeatsService service = (SeatsService) ComponentManager.get("edu.nyu.classes.seats.SeatsService");

        DB.transaction
                ("Mark any site or realm changed in the last 60 seconds for sync",
                        (DBConnection db) -> {
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

    public long runRound(long findProcessedSince) {
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

    Set<String> ROLES_TO_CC = new HashSet<>(Arrays.asList(new String[] { "Instructor", "Teaching Assistant", "Course Site Admin" }));

    private List<org.sakaiproject.user.api.User> usersToCC(Site site) {
        Set<org.sakaiproject.authz.api.Member> members = site.getMembers();
        List<String> netIds = members.stream()
            .filter((m) -> ROLES_TO_CC.contains(m.getRole().getId()))
            .map((m) -> m.getUserEid())
            .collect(Collectors.toList());

        return UserDirectoryService.getUsersByEids(netIds);
    }

    private void notifyUser(String studentNetId, SeatGroup group, Site site) throws Exception {
        List<org.sakaiproject.user.api.User> studentUser = UserDirectoryService.getUsersByEids(Arrays.asList(new String[] { studentNetId }));

        if (studentUser.size() == 0) {
            return;
        }

        EmailMessage msg = new EmailMessage();
        // Overriden by the email service anyway...
        msg.setFrom(new EmailAddress("no-reply-nyuclasses@nyu.edu", "NYU Classes"));
        msg.setSubject(String.format("You've been added to a cohort for %s",
                                     site.getTitle()));

        String body = String.format("<p>Dear %s,</p>" +
                                    "<p>You've been added to %s for %s. Please contact your instructor for information on when you will be meeting in-person for your course.</p>" +
                                    "<p>Note: you will be required to record your seating assignment for the duration of the semester in the Seating Assignments tool in NYU Classes. " +
                                    "For more information, see the <a href=\"%s\">Seating Assignments knowledgebase article</a>.</p>" +
                                    "<p>Best regards,<br>" +
                                    "The NYU Classes Team</p>",

                                    studentUser.get(0).getDisplayName(),
                                    group.name,
                                    site.getTitle(),
                                    "http://www.nyu.edu/servicelink/KB0018304"
                                    );

        msg.setBody(FormattedText.escapeHtmlFormattedText(body));
        msg.setContentType("text/html");
        msg.setCharacterSet("utf-8");
        msg.addHeader("Content-Transfer-Encoding", "quoted-printable");

        msg.setRecipients(RecipientType.TO, studentUser.stream().map((u) -> new EmailAddress(u.getEmail())).collect(Collectors.toList()));
        msg.setRecipients(RecipientType.CC,
                          usersToCC(site)
                          .stream()
                          .filter((u) -> u.getEmail() != null)
                          .map((u) -> new EmailAddress(u.getEmail()))
                          .collect(Collectors.toList()));

        EmailService.getInstance().send(msg);
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
                                SeatsStorage.SyncResult syncResult = SeatsStorage.syncGroupsToSection(db, section);

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
