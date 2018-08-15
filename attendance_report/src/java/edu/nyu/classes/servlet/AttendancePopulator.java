package edu.nyu.classes.servlet;

import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.attendance.logic.AttendanceLogic;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sakaiproject.db.cover.SqlService;
import java.util.stream.Collectors;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

import java.util.Collections;
import org.sakaiproject.attendance.model.AttendanceSite;
import org.sakaiproject.attendance.model.AttendanceRecord;
import org.sakaiproject.attendance.model.Status;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;

import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalField;
import java.time.format.DateTimeFormatter;

public class AttendancePopulator {

    private static final String ATTENDANCE_PREPOPULATED = "attendance_prepopulated";

    // The big idea:
    //
    //   * Find any site that is connected to a London roster (done)
    //
    //   * Check whether it has a site property to indicate that it's had its
    //     attendance populated.  Skip if it has.  Otherwise: (TODO)
    //
    //   * Generate the list of attendance events for the site, based on the
    //     meeting pattern of the roster(s). (done)
    //
    //   * If there are multiple rosters, assert that their meeting patterns all
    //     match.  If they don't, report an error and email it somewhere.  Set
    //     the site property as "failed" and give instructions on how to
    //     correct. (done)
    //
    //   * Create one attendance event per meeting, checking first whether the
    //     event already exists.  Skip if it does. (TODO)
    //
    //   * Set the site property to mark it as populated (TODO)
    //
    //   * If anything goes wrong along the way, we'll retry next run.  Set a
    //     failure count property on the site and email alert when it hits 10. (TODO)
    //

    private static final String LOCATION_CODE = "GLOBAL-0L";
    private static final int STRM = 1188;

    public void run() {
        StringBuilder failureReport = new StringBuilder();

        try{
            Connection conn = SqlService.borrowConnection();
            try {
                try (PreparedStatement ps =
                     conn.prepareStatement("select ss.site_id, cc.stem_name" +
                                           " from nyu_t_course_catalog cc" +
                                           " inner join sakai_realm_provider srp on srp.provider_id = replace(cc.stem_name, ':', '_')" +
                                           " inner join sakai_realm sr on sr.realm_key = srp.realm_key" +
                                           " inner join sakai_site ss on sr.realm_id = concat('/site/', ss.site_id)" +
                                           " left join sakai_site_property ssp on ssp.name = 'attendance_prepopulated' AND ssp.value is null" +
                                           " where cc.location = ? AND cc.strm >= ?")) {
                    ps.setString(1, LOCATION_CODE);
                    ps.setInt(2, STRM);

                    try (ResultSet rs = ps.executeQuery()) {
                        Map<String, List<String>> siteRosters = new HashMap<>();

                        while (rs.next()) {
                            String siteId = rs.getString("site_id");
                            String roster = rs.getString("stem_name");

                            if (!siteRosters.containsKey(siteId)) {
                                siteRosters.put(siteId, new ArrayList<String>());
                            }

                            siteRosters.get(siteId).add(roster);
                        }

                        for (String siteId : siteRosters.keySet()) {
                            try {
                                prepopulateAttendance(conn, siteId, siteRosters.get(siteId));
                            } catch (Exception e) {
                                failureReport.append(String.format("Error processing attendance for site %s: %s",
                                                                   siteId,
                                                                   e.toString()));
                                System.err.println(e.toString());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } finally {
                SqlService.returnConnection(conn);
            }
        } catch (SQLException e) {
            // FIXME: Make better
            failureReport.append("SQL Error: " + e.toString());
        }

        // TODO: If failureReport not empty, email it...
        if (failureReport.length() > 0) {
            System.err.println("BAD THINGS HAPPENED: " + failureReport.toString());
        }
    }

    private static final String[] MEETING_DAYS = new String[] { "MON", "TUES", "WED", "THURS", "FRI", "SAT", "SUN" };

    private class MeetingPattern {
        public String stemName;
        public LocalDate startDate;
        public LocalDate endDate;
        public String holidaySchedule;
        public List<String> days = new ArrayList<>();
    }

    private Set<LocalDate> holidaysForSchedule(Connection conn, String schedule) throws Exception {
        Set<LocalDate> result = new HashSet<>();

        try (PreparedStatement ps = conn.prepareStatement("select holiday" +
                                                          " from nyu_t_holiday_date" +
                                                          " where holiday_schedule = ?")) {
            ps.setString(1, schedule);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getDate("holiday").toLocalDate());
                }
            }
        }

        return result;
    }

    private static List<LocalDate> getDaysBetweenDates(LocalDate start, LocalDate end)
    {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate day = start;

        while (!day.isAfter(end)) {
            dates.add(day);
            day = day.plusDays(1);
        }

        return dates;
    }

    private class Meeting {
        public String title;
        public LocalDate date;

        public Meeting(String title, LocalDate date) {
            this.title = title;
            this.date = date;
        }

        public String toString() {
            return String.format("%s <%s>", title, date);
        }
    }

    private List<Meeting> meetingsForRoster(Connection conn, String rosterId) throws Exception {
        // Load meeting patterns
        List<MeetingPattern> meetingPatterns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("select ct.holiday_schedule, mp.*" +
                                                          " from nyu_t_class_tbl ct" +
                                                          " inner join nyu_t_class_mtg_pat mp on mp.stem_name = ct.stem_name" +
                                                          " where mp.stem_name = ?" +
                                                          " order by mp.start_dt")) {
            ps.setString(1, rosterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MeetingPattern meetingPattern = new MeetingPattern();

                    meetingPattern.stemName = rs.getString("stem_name");
                    meetingPattern.startDate = rs.getDate("start_dt").toLocalDate();
                    meetingPattern.endDate = rs.getDate("end_dt").toLocalDate();
                    meetingPattern.holidaySchedule = rs.getString("holiday_schedule");

                    for (String day : MEETING_DAYS) {
                        if ("Y".equals(rs.getString(day))) {
                            meetingPattern.days.add(day.substring(0, 3));
                        }
                    }

                    meetingPatterns.add(meetingPattern);
                }
            }
        }

        if (meetingPatterns.size() == 0) {
            throw new RuntimeException("No meeting patterns found for roster: " + rosterId);
        }

        // Get term start date
        LocalDate termStartDate;
        try (PreparedStatement ps = conn.prepareStatement("select s.term_begin_dt" +
                                                          " from nyu_t_course_catalog cc" +
                                                          " inner join nyu_t_acad_session s on cc.strm = s.strm AND s.acad_career = cc.acad_career" +
                                                          " where cc.stem_name = ?")) {
            ps.setString(1, rosterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    termStartDate = rs.getDate("term_begin_dt").toLocalDate();
                } else {
                    throw new RuntimeException("Failed to determine term start date for roster: " + rosterId);
                }
            }
        }

        Set<LocalDate> holidays = holidaysForSchedule(conn, meetingPatterns.get(0).holidaySchedule);

        List<Meeting> result = new ArrayList<>();

        DateTimeFormatter dayOfWeek = DateTimeFormatter.ofPattern("EEE");
        Map<Integer, Integer> weekCounts = new HashMap<>();

        TemporalField weekOfYear = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();

        for (MeetingPattern meeting : meetingPatterns) {
            for (LocalDate day : getDaysBetweenDates(meeting.startDate, meeting.endDate)) {
                if (!meeting.days.contains(dayOfWeek.format(day).toUpperCase(Locale.ROOT))) {
                    // If our meeting doesn't meet this day, skip.
                    continue;
                }

                if (holidays.contains(day)) {
                    continue;
                }

                int meetingWeek = day.get(weekOfYear) - termStartDate.get(weekOfYear) + 1;

                if (!weekCounts.containsKey(meetingWeek)) {
                    weekCounts.put(meetingWeek, 0);
                }

                weekCounts.put(meetingWeek, weekCounts.get(meetingWeek) + 1);

                Meeting m = new Meeting(String.format("Week %d Session %d",
                                                      meetingWeek, weekCounts.get(meetingWeek)),
                                        day);
                result.add(m);
            }
        }

        return result;
    }

    private void prepopulateAttendance(Connection conn, String siteId, List<String> rosters) throws Exception {
        List<List<Meeting>> rosterMeetings = new ArrayList<>();

        for (String rosterId : rosters) {
            rosterMeetings.add(meetingsForRoster(conn, rosterId));
        }

        if (rosterMeetings.stream().map(e -> e.size()).distinct().count() > 1) {
            throw new RuntimeException(String.format("Site %s has multiple rosters with differing meeting patterns (count mismatch)",
                                                     siteId));
        }

        for (int i = 0; i < rosterMeetings.get(0).size(); i++) {
            Set<String> titles = new HashSet<>();
            for (int r = 0; r < rosters.size(); r++) {
                titles.add(rosterMeetings.get(r).get(i).title);
            }

            if (titles.size() > 1) {
                throw new RuntimeException(String.format("Site %s has multiple rosters with differing meeting patterns (title mismatch)",
                                                         siteId));
            }
        }

        // FIXME: do something
        List<Meeting> siteMeetingDates = rosterMeetings.get(0);

        System.err.println("Rosters for site:");
        for (String roster : rosters) {
            System.err.println("  * " + roster);
        }

        System.err.println("\nMeeting dates:");
        for (Meeting meeting : siteMeetingDates) {
            System.err.println("  * " + meeting);
        }
    }
}
