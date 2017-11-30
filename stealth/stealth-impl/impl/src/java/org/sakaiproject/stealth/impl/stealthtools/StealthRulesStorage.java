
package org.sakaiproject.stealth.impl.stealthtools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.sakaiproject.stealth.api.ToolService;
import org.sakaiproject.stealth.api.PilotTool;
import org.sakaiproject.stealth.api.ToolsByUser;
import org.sakaiproject.stealth.api.ToolsBySite;
import org.sakaiproject.stealth.impl.common.DB;
import org.sakaiproject.stealth.impl.common.DBAction;
import org.sakaiproject.stealth.impl.common.DBConnection;
import org.sakaiproject.stealth.impl.common.DBResults;

public class StealthRulesStorage implements ToolService{

    private final String stealthByUserTable;
    private final String stealthBySiteTable;
    private final String PilotToolTable;

    public StealthRulesStorage() {
        stealthByUserTable = ("stealth_byuser").toLowerCase(Locale.ROOT); //Insert table name here
        stealthBySiteTable = ("stealth_bysite").toLowerCase(Locale.ROOT); //Insert table name here
        PilotToolTable     = ("stealth_tools").toLowerCase(Locale.ROOT); //Insert table name here
    }

    public List<ToolsByUser> searchByNetId(final String netId) {
        return DB.transaction
                ("Search tool permissions by NetId",
                        new DBAction<List<ToolsByUser>>() {
                            @Override
                            public List<ToolsByUser> call(DBConnection db) throws SQLException {
                                List<ToolsByUser> tools = new ArrayList<ToolsByUser>();
                                try (DBResults results = db.run("SELECT * from " + stealthByUserTable + " where netid like '" + netId + "%'")
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        tools.add(new ToolsByUser(result.getString("netid"),
                                                result.getString("term"),
                                                result.getLong("index"),
                                                result.getString("toolid")));
                                    }
                                    return tools;
                                }
                            }
                        }
                );
    }

   public List<ToolsBySite> searchBySiteId(final String siteId) {
        return DB.transaction
                ("Search tool permissions by SiteId",
                        new DBAction<List<ToolsBySite>>() {
                            @Override
                            public List<ToolsBySite> call(DBConnection db) throws SQLException {
                                List<ToolsBySite> tools = new ArrayList<ToolsBySite>();
                                try (DBResults results = db.run("SELECT * from " + stealthBySiteTable + " where siteid like '" + siteId + "%'")
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        tools.add(new ToolsBySite(result.getString("siteid"),
                                                result.getLong("index"),
                                                result.getString("toolid")));
                                    }
                                    return tools;
                                }
                            }
                        }
                );
    }

    public List<PilotTool> getAllPilotTools() {
        return DB.transaction
                ("Search tools present in the pilot tool table",
                        new DBAction<List<PilotTool>>() {
                            @Override
                            public List<PilotTool> call(DBConnection db) throws SQLException {
                                List<PilotTool> tools = new ArrayList<PilotTool>();
                                try (DBResults results = db.run("SELECT * from " + PilotToolTable)
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        tools.add(new PilotTool(
                                                result.getString("toolid")));
                                    }
                                    return tools;
                                }
                            }
                        }
                );
    }

    public void removePermissionBySite(final String siteId) {
        DB.transaction
                ("Remove tool permissions by SiteId",
                        new DBAction<Void>() {
                            @Override
                            public Void call(DBConnection db) throws SQLException {
                                db.run("DELETE FROM " + stealthBySiteTable + " where siteid = ?")
                                        .param(siteId)
                                        .executeUpdate();
                                db.commit();
                                return null;

                            }
                        }
                );
    }

    public void removePermissionByUser(final String netId, final int term) {
        DB.transaction
                ("Remove tool permissions by netId and term",
                        new DBAction<Void>() {
                            @Override
                            public Void call(DBConnection db) throws SQLException {
                                db.run("DELETE FROM " + stealthByUserTable + " where netid = ? AND term = ?")
                                        .param(netId)
                                        .param(term)
                                        .executeUpdate();
                                db.commit();
                                return null;

                            }
                        }
                );
    }

    public void removeToolsFromPilotTable(final String toolId) {
        DB.transaction
                ("Remove tools from pilot tool table",
                        new DBAction<Void>() {
                            @Override
                            public Void call(DBConnection db) throws SQLException {
                                db.run("DELETE FROM " + PilotToolTable + " where toolid = ?")
                                        .param(toolId)
                                        .executeUpdate();
                                db.commit();
                                return null;

                            }
                        }
                );
    }

}
