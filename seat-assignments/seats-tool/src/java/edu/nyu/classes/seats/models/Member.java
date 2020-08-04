package edu.nyu.classes.seats.models;

public class Member {
    public enum Modality {
        ONLINE,
        IN_PERSON,
        UNSURE,
    }

    public String netid;
    public Modality modality;

    public Member(String netid) {
        this.netid = netid;
        this.modality = Modality.values()[(int)Math.floor(Math.random() * 3)];
    }

    public int hashCode() {
        return netid.hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof Member)) {
            return false;
        }

        return ((Member) other).netid.equals(netid);
    }
}
