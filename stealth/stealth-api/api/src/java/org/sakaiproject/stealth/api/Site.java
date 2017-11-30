package org.sakaiproject.stealth.api;

public class Site implements Comparable<Site> {
    private final String siteId;

    public Site(String siteId) {
        this.siteId  = siteId;
    }

    public String getSiteId() {
        return this.siteId;
    }

    @Override
    public String toString() {
        return getSiteId();
    }

    @Override
    public int compareTo(Site other) {
        return this.siteId.compareTo(other.siteId);
    }
}
