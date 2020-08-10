package edu.nyu.classes.seats.models;

public class SeatAssignment {
    public String id;
    public String netid;
    public String seat;
    public Meeting meeting;
    public long editableUntil;

    public SeatAssignment(String id, String netid, String seat, long editableUntil, Meeting meeting) {
        this.id = id;
        this.netid = netid;
        this.seat = seat;
        this.meeting = meeting;
        this.editableUntil = editableUntil;
    }
}

