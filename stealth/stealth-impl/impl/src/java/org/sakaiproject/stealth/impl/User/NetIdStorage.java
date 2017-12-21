
package org.sakaiproject.stealth.impl.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.sakaiproject.stealth.api.User;
import org.sakaiproject.stealth.api.UserService;
import org.sakaiproject.stealth.impl.common.DB;
import org.sakaiproject.stealth.impl.common.DBAction;
import org.sakaiproject.stealth.impl.common.DBConnection;
import org.sakaiproject.stealth.impl.common.DBResults;

public class NetIdStorage implements UserService {

    private final String NetIdTable;

    public NetIdStorage() {
        NetIdTable = ("sakai_user_id_map").toLowerCase(Locale.ROOT);
    }

    public List<User> getNetIdList(final String searchPattern) {
        return DB.transaction
                ("Search NetIds starting with some search Pattern",
                        new DBAction<List<User>>() {
                            @Override
                            public List<User> call(DBConnection db) throws SQLException {
                                List<User> netids = new ArrayList<User>();
                                try (DBResults results = db.run("SELECT * from " + NetIdTable + " where eid like '" + searchPattern + "%'")
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        netids.add(new User(result.getString("eid")));
                                    }
                                    return netids;
                                }
                            }
                        }
                );
    }
}
