package org.sakaiproject.stealth.api;

import lombok.Getter;

/**
 * A data object representing a Tool Permission by Site.
 */
public class ToolsBySite implements Comparable<ToolsBySite> {
    private final String siteid;
    @Getter
    private final long index;
    @Getter
    private final String toolid;

    public ToolsBySite(String siteid, long index, String toolid) {
        this.siteid  = siteid;
        this.index  = index;
        this.toolid = toolid;
    }

    public String getSiteId() {
        return this.siteid;
    }

    @Override
    public int compareTo(ToolsBySite other) {
        return this.toolid.compareTo(other.toolid);
    }
}
