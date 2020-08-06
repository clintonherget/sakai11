package edu.nyu.classes.seats.models;

import java.util.HashMap;
import java.util.Optional;
import java.util.Collection;
import java.util.Map;

public class SeatSection {
    private Map<String, SeatGroup> groups = new HashMap<>();

    public String id;
    public String siteId;

    public SeatSection(String id, String siteId) {
        this.id = id;
        this.siteId = siteId;
    }

    public Collection<String> groupIds() {
        return groups.keySet();
    }

    public void addGroup(String id, String name, String description) {
        groups.put(id, new SeatGroup(id, name, description, this));
    }

    public Optional<SeatGroup> fetchGroup(String id) {
        return Optional.of(groups.get(id));
    }

    public Collection<SeatGroup> listGroups() {
        return groups.values();
    }
}
