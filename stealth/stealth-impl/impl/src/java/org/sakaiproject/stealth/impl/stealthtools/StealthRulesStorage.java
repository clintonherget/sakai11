
package org.sakaiproject.stealth.impl.stealthtools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.sakaiproject.stealth.api.ToolService;
import org.sakaiproject.stealth.api.StealthTool;
import org.sakaiproject.stealth.api.StealthRules;
import org.sakaiproject.stealth.impl.common.DB;
import org.sakaiproject.stealth.impl.common.DBAction;
import org.sakaiproject.stealth.impl.common.DBConnection;
import org.sakaiproject.stealth.impl.common.DBResults;

public class StealthRulesStorage implements ToolService{

    private final String stealthByUserTable;
    private final String stealthBySiteTable;
    private final String StealthToolTable;

    public StealthRulesStorage() {
        stealthByUserTable = ("stealth_byuser").toLowerCase(Locale.ROOT); //Insert table name here
        stealthBySiteTable = ("stealth_bysite").toLowerCase(Locale.ROOT); //Insert table name here
        StealthToolTable   = ("stealth_tools").toLowerCase(Locale.ROOT); //Insert table name here
    }

    public List<StealthRules> searchByNetId(final String netId) {
        return DB.transaction
                ("Search tool permissions by NetId",
                        new DBAction<List<StealthRules>>() {
                            @Override
                            public List<StealthRules> call(DBConnection db) throws SQLException {
                                List<StealthRules> tools = new ArrayList<StealthRules>();
                                String query ="SELECT X.netid,X.coursetitle,X.siteid,Y.toolname from ";
                                query +="(SELECT A.netid as netid, A.toolid as toolid, B.siteid as siteid, B.coursetitle as coursetitle from ";
                                query +="(SELECT netid,toolid from stealth_byuser where netid like 'sl5268%') as A, ";
                                query +="(select  ss.site_id as siteid, ss.title as coursetitle,suim.EID as netid ";
                                query +="from ";
                                query +=" sakai_site ss ";
                                query +=" inner join sakai_realm sr on CONCAT('/site/', ss.site_id) = sr.realm_id ";
                                query +=" inner join sakai_realm_rl_gr srrg on srrg.realm_key = sr.realm_key ";
                                query +=" inner join sakai_realm_role srr on srrg.role_key = srr.role_key ";
                                query +=" inner join sakai_user_id_map suim on srrg.user_id = suim.user_id ";
                                query +=" where ";
                                
                                query +=" (srr.role_key = 9 or srr.role_key = 10 or srr.role_key = 21 or srr.role_key = 5) ";
                                query +=" ) as B  ";
                                query +=" where A.netid=B.netid) as X, ";
                                query +=" (SELECT distinct TOOLID as toolid, toolname as toolname from stealth_tools) as Y ";
                                query +=" where X.toolid=Y.toolid;";

                                System.out.print(query);
                                try (DBResults results = db.run(query).executeQuery()) {
                                    for (ResultSet result : results) {
                                        tools.add(new StealthRules(result.getString("netid"),
                                                result.getString("coursetitle"),
                                                result.getString("siteid"),
                                                result.getString("toolname")));
                                    }
                                    return tools;
                                }
                            }
                        }
                );
    }

   public List<StealthRules> searchBySiteId(final String siteId) {
        return DB.transaction
                ("Search tool permissions by SiteId",
                        new DBAction<List<StealthRules>>() {
                            @Override
                            public List<StealthRules> call(DBConnection db) throws SQLException {
                                List<StealthRules> tools = new ArrayList<StealthRules>();
                                String query = "SELECT X.netid as netid,X.coursetitle as coursetitle,X.siteid as siteid,Y.toolname as toolname from ";
                                query += "(SELECT B.netid, A.siteid, A.toolid, B.coursetitle from ";
                                query += "(SELECT site_id as siteid ,toolid as toolid from " + stealthBySiteTable + " where siteid like '" + siteId + "%') as A, ";
                                query += "(SELECT S.SITE_ID as siteid ,S.TITLE as coursetitle,U.EID as netid from sakai_site S, sakai_user_id_map U where S.CREATEDBY=U.USER_ID) as B ";
                                query += "where A.siteid=B.siteid) as X, ";
                                query += "(SELECT distinct TOOLID as toolid, toolname as toolname from "+ StealthToolTable +") as Y ";
                                query += "where X.toolid=Y.toolid";
                                System.out.print(query);
                                try (DBResults results = db.run(query).executeQuery()) {
                                    for (ResultSet result : results) {
                                        tools.add(new StealthRules(result.getString("netid"),
                                            result.getString("coursetitle"),
                                            result.getString("siteid"),
                                            result.getString("toolname")));
                                    }
                                    return tools;
                                }
                            }
                        }
                );
    }

    public List<StealthTool> getAllStealthTools() {
        return DB.transaction
                ("Search tools present in the pilot tool table",
                        new DBAction<List<StealthTool>>() {
                            @Override
                            public List<StealthTool> call(DBConnection db) throws SQLException {
                                List<StealthTool> tools = new ArrayList<StealthTool>();
                                try (DBResults results = db.run("SELECT * from " + StealthToolTable)
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        tools.add(new StealthTool(
                                                result.getString("toolid"),
                                                result.getString("toolname")));
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
                                db.run("DELETE FROM " + StealthToolTable + " where toolid = ?")
                                        .param(toolId)
                                        .executeUpdate();
                                db.commit();
                                return null;

                            }
                        }
                );
    }

}
