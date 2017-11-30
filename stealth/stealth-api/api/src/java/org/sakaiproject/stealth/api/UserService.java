package org.sakaiproject.stealth.api;

import java.util.List;

public interface UserService {

    public List<User> getNetIdList(String searchPattern);
}