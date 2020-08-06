// TODO:
// Lock sites during sync
// Flag on SeatSection to mark split as done
// mtime checks for realms/sites to periodically refresh
// test sync + ui in tight loop


package edu.nyu.classes.seats;

import edu.nyu.classes.seats.api.SeatsService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.sakaiproject.db.cover.SqlService;


public class SeatsServiceImpl implements SeatsService {
    public void init() {
    }
    public void destroy() {
    }

    // FIXME: use a sequence instead?
    private void insertRequests(Connection db, String[] siteIds) throws SQLException {
        long now = System.currentTimeMillis();
        boolean synced[] = new boolean[siteIds.length];

        try (PreparedStatement ps = db.prepareStatement("update seat_sync_queue set last_sync_requested_time = ? where site_id = ?")) {
            for (int i = 0; i < siteIds.length; i++) {
                String siteId = siteIds[i];

                ps.clearParameters();
                ps.setLong(1, now);
                ps.setString(2, siteId);

                if (ps.executeUpdate() == 1) {
                    synced[i] = true;
                }
            }
        }

        try (PreparedStatement ps = db.prepareStatement("insert into seat_sync_queue (site_id, last_sync_requested_time) values (?, ?)")) {
            for (int i = 0; i < siteIds.length; i++) {
                String siteId = siteIds[i];

                if (synced[i]) {
                    continue;
                }

                try {
                    ps.clearParameters();
                    ps.setString(1, siteId);
                    ps.setLong(2, now);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    if (e.getSQLState().startsWith("23")) {
                        // Someone got in with this site while our back was turned.  Good enough.
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    public void markSitesForSync(String ...siteIds) {
        for (String siteId : siteIds) {
            // "MARKING SITE FOR SYNC", siteId
            System.err.println("\n*** @DEBUG " + System.currentTimeMillis() + "[SeatsServiceImpl.java:80 DefensiveEagle]: " + "\n    'MARKING SITE FOR SYNC' => " + ("MARKING SITE FOR SYNC") + "\n    siteId => " + (siteId) + "\n");
        }

        Connection db = null;
        try {
            db = SqlService.borrowConnection();
            boolean autoCommit = db.getAutoCommit();
            db.setAutoCommit(false);

            insertRequests(db, siteIds);

            db.commit();
            db.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            System.err.println(String.format("%s Failure during site sync update: %s", new Date(), e));
        } finally {
            if (db != null) {
                SqlService.returnConnection(db);
            }
        }
    }

    public void markSectionsForSync(List<String> sectionEids) {
        if (sectionEids.isEmpty()) {
            return;
        }

        Connection db = null;
        try {
            db = SqlService.borrowConnection();
            boolean autoCommit = db.getAutoCommit();
            db.setAutoCommit(false);

            int batchSize = 200;

            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < batchSize; i++) {
                if (placeholders.length() > 0) {
                    placeholders.append(",");
                }
                placeholders.append("?");
            }

            List<String> siteIds = new ArrayList<>();

            try (PreparedStatement siteLookup = db.prepareStatement
                 (
                  "select distinct(ss.site_id) " +
                  " from sakai_realm_provider srp " +
                  " inner join sakai_realm sr on sr.realm_key = srp.realm_key " +
                  " inner join sakai_site ss on sr.realm_id = concat('/site/', ss.site_id)" +
                  " where srp.provider_id in (" + placeholders.toString() + ")"
                  )) {
                for (int i = 0; i < sectionEids.size();) {
                    siteLookup.clearParameters();
                    int upper = Math.min(i + batchSize, sectionEids.size());

                    List<String> chunk = sectionEids.subList(i, upper);

                    for (int p = 0; p < batchSize; p++) {
                        if (p < chunk.size()) {
                            siteLookup.setString(p + 1, chunk.get(p));
                        } else {
                            // Fill with first value to pad out to batchSize
                            siteLookup.setString(p + 1, chunk.get(0));
                        }
                    }

                    try (ResultSet rs = siteLookup.executeQuery()) {
                        while (rs.next()) {
                            siteIds.add(rs.getString("site_id"));
                        }
                    }

                    i = upper;
                }
            }

            insertRequests(db, siteIds.toArray(new String[0]));

            db.commit();
            db.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            System.err.println(String.format("%s Failure during site sync update: %s", new Date(), e));
        } finally {
            if (db != null) {
                SqlService.returnConnection(db);
            }
        }
    }
}
