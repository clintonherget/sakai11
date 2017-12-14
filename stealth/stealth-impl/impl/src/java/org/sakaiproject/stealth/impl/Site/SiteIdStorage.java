
package org.sakaiproject.stealth.impl.site;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.sakaiproject.stealth.api.Site;
import org.sakaiproject.stealth.api.SiteService;
import org.sakaiproject.stealth.impl.common.DB;
import org.sakaiproject.stealth.impl.common.DBAction;
import org.sakaiproject.stealth.impl.common.DBConnection;
import org.sakaiproject.stealth.impl.common.DBResults;

public class SiteIdStorage implements SiteService{

    private final String SiteIdTable;

    public SiteIdStorage() {
        SiteIdTable = ("sakai_site").toLowerCase(Locale.ROOT);
    }

    public List<Site> getSiteIdList(final String searchPattern) {
        return DB.transaction
                ("Search siteIds starting with some search Pattern",
                        new DBAction<List<Site>>() {
                            @Override
                            public List<Site> call(DBConnection db) throws SQLException {
                                List<Site> siteIds = new ArrayList<Site>();
                                try (DBResults results = db.run("SELECT * from " + SiteIdTable + " where site_id like '" + searchPattern + "%'")
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        siteIds.add(new Site(result.getString("site_id")));
                                    }
                                    return siteIds;
                                }
                            }
                        }
                );
    }
}
