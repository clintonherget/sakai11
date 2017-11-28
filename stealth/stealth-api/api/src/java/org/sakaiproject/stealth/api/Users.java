package org.sakaiproject.stealth.api;

import java.util.List;

public interface Users {

    public List<User> getNetIdList(String searchPattern);
}