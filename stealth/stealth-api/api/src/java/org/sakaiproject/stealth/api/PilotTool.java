package org.sakaiproject.stealth.api;

public class PilotTool implements Comparable<PilotTool> {
    private final String toolId;

    public PilotTool(String toolId) {
        this.toolId  = toolId;
    }

    public String getToolId() {
        return this.toolId;
    }

    @Override
    public String toString() {
        return getToolId();
    }

    @Override
    public int compareTo(PilotTool other) {
        return this.toolId.compareTo(other.toolId);
    }
}
