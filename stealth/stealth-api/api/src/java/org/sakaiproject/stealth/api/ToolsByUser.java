package org.sakaiproject.stealth.api;
import lombok.Getter;

/**
 * A data object representing a Tools permission by NetId.
 */
public class ToolsByUser implements Comparable<ToolsByUser> {
    private final String netid;
    private final String term;
    private final long index;
    private final String toolid;

    public ToolsByUser(String netid, String term, long index, String toolid) {
        this.netid  = netid;
        this.term   = term;
        this.index  = index;
        this.toolid = toolid;
    }

    public String getNetId() {
        return this.netid;
    }

    public String getTerm() {
        return this.term;
    }

    public String getToolId() {
        return this.toolid;
    }

    @Override
    public int compareTo(ToolsByUser other) {
        return this.toolid.compareTo(other.toolid);
    }
}
