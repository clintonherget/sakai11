package edu.nyu.classes.seats.storage.migrations;

import edu.nyu.classes.seats.storage.*;

public class V1__Init extends BaseMigration {

    final static String TABLE_DEFS =
        "create table seat_group_section (                                                                   " +
        "    id varchar2(255) primary key,                                                                   " +
        "    primary_roster_id varchar2(255),                                                                " +
        "    site_id varchar2(255),                                                                          " +
        "    constraint uniq_section_id_siteid unique (primary_roster_id, site_id)                           " +
        ");                                                                                                  " +

        "create table seat_group_section_rosters (                                                           " +
        "    sakai_roster_id varchar2(255)            ,                                                      " +
        "    role varchar2(255),                                                                             " +
        "    section_id varchar2(255),                                                                       " +
        "    constraint fk_seat_group_section_id foreign key (section_id) references seat_group_section (id) " +
        ");                                                                                                  " +

        "create table seat_group (                                                                           " +
        "    id varchar2(255) primary key,                                                                   " +
        "    name varchar2(255) not null,                                                                    " +
        "    description varchar2(255),                                                                      " +
        "    section_id varchar2(255),                                                                       " +
        "    constraint fk_seat_section_id foreign key (section_id) references seat_group_section (id)       " +
        ");                                                                                                  " +

        "create table seat_group_members (                                                                   " +
        "    group_id varchar2(255),                                                                         " +
        "    netid varchar2(255),                                                                            " +
        "    official number(1) not null,                                                                    " +
        "    primary key (group_id, netid),                                                                  " +
        "    constraint fk_seat_group_id foreign key (group_id) references seat_group (id)                   " +
        ");                                                                                                  " +

        "create table seat_meeting (                                                                         " +
        "    id varchar2(255) primary key,                                                                   " +
        "    group_id varchar2(255) not null,                                                                " +
        "    location varchar2(255) not null,                                                                " +
        "    constraint fk_seat_meeting_group_id foreign key (group_id) references seat_group (id)           " +
        ");                                                                                                  " +

        "create table seat_meeting_assignment (                                                              " +
        "    id varchar2(255) primary key,                                                                   " +
        "    meeting_id varchar2(255) not null,                                                              " +
        "    editable_until number not null,                                                                 " +
        "    netid varchar2(255),                                                                            " +
        "    seat varchar2(32),                                                                              " +
        "    constraint uniq_seat_assignment_netid unique (meeting_id, netid),                               " +
        "    constraint uniq_seat_assignment_seat unique (meeting_id, seat),                                 " +
        "    constraint fk_seat_meeting_id foreign key (meeting_id) references seat_meeting (id)             " +
        ");                                                                                                  " +

        "create table seat_sync_queue (                                                                      " +
        "    site_id varchar2(255) primary key,                                                              " +
        "    last_sync_requested_time number default 0,                                                      " +
        "    last_sync_time number default 0                                                                " +
        ");                                                                                                  " +

        "create table seat_audit (                                                                           " +
        "    id varchar2(255) primary key,                                                                   " +
        "    timestamp_ms number not null,                                                                   " +
        "    event_code varchar2(32) not null,                                                               " +
        "    json clob not null,                                                                             " +
        "    arg0 varchar2(255),                                                                             " +
        "    arg1 varchar2(255),                                                                             " +
        "    arg2 varchar2(255),                                                                             " +
        "    arg3 varchar2(255),                                                                             " +
        "    arg4 varchar2(255),                                                                             " +
        "    arg5 varchar2(255),                                                                             " +
        "    arg6 varchar2(255),                                                                             " +
        "    arg7 varchar2(255),                                                                             " +
        "    arg8 varchar2(255),                                                                             " +
        "    arg9 varchar2(255)                                                                              " +
        ");                                                                                                  ";


    public void migrate(DBConnection connection) throws Exception {
        for (String ddl : TABLE_DEFS.split(";")) {
            if (ddl.trim().isEmpty()) {
                continue;
            }

            connection.run(ddl.trim()).executeUpdate();
        }
    }
}
