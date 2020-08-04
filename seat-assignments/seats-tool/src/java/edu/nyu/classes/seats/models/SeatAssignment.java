package edu.nyu.classes.seats.models;

public class SeatAssignment {
    public String id;
    public String netid;
    public String seat;
    public Meeting meeting;

    public SeatAssignment(String id, String netid, String seat, Meeting meeting) {
        this.id = id;
        this.netid = netid;
        this.seat = seat;
        this.meeting = meeting;
    }
}

