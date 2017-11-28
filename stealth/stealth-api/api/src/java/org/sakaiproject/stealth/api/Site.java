package org.sakaiproject.stealth.api;

import lombok.Getter;

/**
 * A data object representing a banner.
 */
public class Site implements Comparable<Site> {
    private final String siteId;

    public Site(String siteId) {
        this.siteId  = siteId;
    }

    public String getSiteId() {
        return this.siteId;
    }

    @Override
    public int compareTo(Site other) {
        return this.siteId.compareTo(other.siteId);
    }
}