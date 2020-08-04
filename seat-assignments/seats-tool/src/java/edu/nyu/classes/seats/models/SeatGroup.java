package edu.nyu.classes.seats.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SeatGroup {
    public String id;
    public String name;
    public String description;
    public SeatSection section;

    public Map<String, Meeting> meetings = new HashMap<>();

    public Collection<Meeting> listMeetings() {
        return meetings.values();
    }

    public Meeting getOrCreateMeeting(String meetingId) {
        if (!this.meetings.containsKey(meetingId)) {
            this.meetings.put(meetingId, new Meeting(meetingId, this));
        }

        return this.meetings.get(meetingId);
    }

    public SeatGroup(String id, String name, String description, SeatSection section) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.section = section;
    }
}
