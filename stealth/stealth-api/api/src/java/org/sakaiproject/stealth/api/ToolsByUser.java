package org.sakaiproject.stealth.api;
import lombok.Getter;

/**
 * A data object representing a Tools permission by NetId.
 */
public class ToolsByUser implements Comparable<ToolsByUser> {
    private final String netid;
    @Getter
    private final int term;
    @Getter
    private final long index;
    @Getter
    private final String toolid;

    public ToolsByUser(String netid, int term, long index, String toolid) {
        this.netid  = netid;
        this.term   = term;
        this.index  = index;
        this.toolid = toolid;
    }

    public String getNetId() {
        return this.netid;
    }

    @Override
    public int compareTo(ToolsByUser other) {
        return this.toolid.compareTo(other.toolid);
    }
}
