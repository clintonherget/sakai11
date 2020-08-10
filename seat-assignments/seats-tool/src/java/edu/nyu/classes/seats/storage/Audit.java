package edu.nyu.classes.seats.storage;

import edu.nyu.classes.seats.storage.db.*;
import java.sql.SQLException;

public class Audit {
    public enum AuditEvents {
        SEAT_CLEARED,
        SEAT_ASSIGNED,
        SEAT_REASSIGNED,
        MEETING_DELETED,
        GROUP_DELETED,
        GROUP_CREATED,
        MEETING_CREATED,
        MEMBER_ADDED,
        MEMBER_DELETED,
    }

    public static void insert(DBConnection db, AuditEvents event, String json, String[] args) throws SQLException {
        long timestamp = System.currentTimeMillis();

        db.run("insert into seat_audit (id, timestamp_ms, event_code, json) values (?, ?, ?, ?)")
            .param(db.uuid())
            .param(timestamp)
            .param(event.toString())
            .param(json)
            .executeUpdate();
    }
}
