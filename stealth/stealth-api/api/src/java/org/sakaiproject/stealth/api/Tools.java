package org.sakaiproject.stealth.api;

import java.util.List;

public interface Tools {

    public List<ToolsByUser> searchByNetId(String netId);
    public List<ToolsBySite> searchBySiteId(String siteId);
    public void removePermissionBySite(String siteId);
    public void removePermissionByUser(String netId, int term);
    public List<PilotTool> getAllPilotTools();
    public void removeToolsFromPilotTable(String toolId);
}