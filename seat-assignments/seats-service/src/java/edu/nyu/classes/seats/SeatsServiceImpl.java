package edu.nyu.classes.seats;

import edu.nyu.classes.seats.api.SeatsService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
}
