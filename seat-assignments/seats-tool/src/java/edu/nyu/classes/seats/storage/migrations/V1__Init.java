package edu.nyu.classes.seats.storage.migrations;

import edu.nyu.classes.seats.storage.*;

public class V1__Init extends BaseMigration {

    final static String TABLE_DEFS =
        "create table seat_group (                                                                " +
        "    id varchar2(255) primary key,                                                        " +
        "    name varchar2(255) not null,                                                         " +
        "    sakai_roster_id varchar2(255) not null,                                              " +
        "    section_stem_name varchar2(255) not null,                                            " +
        "    description varchar2(255)                                                            " +
        ");                                                                                       " +

        "create table seat_group_members (                                                        " +
        "    group_id varchar2(255),                                                              " +
        "    netid varchar2(255),                                                                 " +
        "    primary key (group_id, netid),                                                       " +
        "    constraint fk_seat_group_id foreign key (group_id) references seat_group (id)        " +
        ");                                                                                       " +

        "create table seat_meeting (                                                              " +
        "    id varchar2(255) primary key,                                                        " +
        "    group_id varchar2(255) not null,                                                     " +
        "    location varchar2(255) not null,                                                     " +
        "    constraint fk_seat_meeting_group_id foreign key (group_id) references seat_group (id)" +
        ");                                                                                       " +

        "create table seat_meeting_assignment (                                                   " +
        "    id varchar2(255) primary key,                                                        " +
        "    meeting_id varchar2(255) not null,                                                   " +
        "    seat varchar2(32),                                                                   " +
        "    constraint fk_seat_meeting_id foreign key (meeting_id) references meeting (id)       " +
        ");                                                                                       " +

        "create table seat_audit (                                                                " +
        "    id varchar2(255) primary key,                                                        " +
        "    timestamp_ms number not null,                                                        " +
        "    event_code varchar2(32) not null,                                                    " +
        "    json clob not null,                                                                  " +
        "    arg0 varchar2(255),                                                                  " +
        "    arg1 varchar2(255),                                                                  " +
        "    arg2 varchar2(255),                                                                  " +
        "    arg3 varchar2(255),                                                                  " +
        "    arg4 varchar2(255),                                                                  " +
        "    arg5 varchar2(255),                                                                  " +
        "    arg6 varchar2(255),                                                                  " +
        "    arg7 varchar2(255),                                                                  " +
        "    arg8 varchar2(255),                                                                  " +
        "    arg9 varchar2(255)                                                                   " +
        ");                                                                                       ";


    public void migrate(DBConnection connection) throws Exception {
        for (String ddl : TABLE_DEFS.split(";")) {
            if (ddl.trim().isEmpty()) {
                continue;
            }

            connection.run(ddl.trim()).executeUpdate();
        }
    }
}
