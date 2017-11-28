package org.sakaiproject.stealth.api;

import lombok.Getter;

/**
 * A data object representing a banner.
 */
public class User implements Comparable<User> {
    private final String netId;

    public User(String netId) {
        this.netId  = netId;
    }

    public String getNetId() {
        return this.netId;
    }

    @Override
    public int compareTo(User other) {
        return this.netId.compareTo(other.netId);
    }
}
