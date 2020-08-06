package edu.nyu.classes.seats.api;

import java.util.List;

public interface SeatsService {
    public void markSitesForSync(String ...siteId);
    public void markSectionsForSync(List<String> sectionEids);
}
