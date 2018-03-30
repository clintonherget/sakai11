// Convert the fields that were previously 1:1 into their typed input equivalents.

package org.sakaiproject.profile2.conversion;

import org.sakaiproject.db.cover.SqlService;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileTypedListConverter {
    private static final Logger log = LoggerFactory.getLogger(ProfileTypedListConverter.class);

    String[] UPDATES_MYSQL = new String[] {
        "insert into PROFILE_TYPED_VALUES_T (USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select agent_uuid, 'phoneNumbers', null, 'Home', HOME_PHONE from SAKAI_PERSON_T where HOME_PHONE is not null",
        "insert into PROFILE_TYPED_VALUES_T (USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select agent_uuid, 'phoneNumbers', null, 'Work', TELEPHONE_NUMBER from SAKAI_PERSON_T where TELEPHONE_NUMBER is not null",
        "insert into PROFILE_TYPED_VALUES_T (USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select agent_uuid, 'phoneNumbers', null, 'Mobile', MOBILE from SAKAI_PERSON_T where MOBILE is not null",
        "insert into PROFILE_TYPED_VALUES_T (USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select agent_uuid, 'phoneNumbers', 'Fax', 'Other', FAX_NUMBER from SAKAI_PERSON_T where FAX_NUMBER is not null",
    };

    String[] UPDATES_ORACLE = new String[] {
        "insert into PROFILE_TYPED_VALUES_T (ID, USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select PROFILE_TYPED_VALUES_S.nextval, agent_uuid, 'phoneNumbers', null, 'Home', HOME_PHONE from SAKAI_PERSON_T where HOME_PHONE is not null",
        "insert into PROFILE_TYPED_VALUES_T (ID, USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select PROFILE_TYPED_VALUES_S.nextval, agent_uuid, 'phoneNumbers', null, 'Work', TELEPHONE_NUMBER from SAKAI_PERSON_T where TELEPHONE_NUMBER is not null",
        "insert into PROFILE_TYPED_VALUES_T (ID, USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select PROFILE_TYPED_VALUES_S.nextval, agent_uuid, 'phoneNumbers', null, 'Mobile', MOBILE from SAKAI_PERSON_T where MOBILE is not null",
        "insert into PROFILE_TYPED_VALUES_T (ID, USER_UUID, VALUE_GROUP, TYPE, TYPE_QUALIFIER, VALUE) select PROFILE_TYPED_VALUES_S.nextval, agent_uuid, 'phoneNumbers', 'Fax', 'Other', FAX_NUMBER from SAKAI_PERSON_T where FAX_NUMBER is not null",
    };


    public void runConversion() {
        log.info("Running conversion to map profile properties to typed lists");

        try {
            Connection db = SqlService.borrowConnection();
            boolean oldAutoCommit = db.getAutoCommit();
            db.setAutoCommit(false);

            String[] updates = SqlService.getVendor().equals("oracle") ? UPDATES_ORACLE : UPDATES_MYSQL;

            try {
                for (String sql : updates) {
                    PreparedStatement ps = db.prepareStatement(sql);
                    ps.executeUpdate();
                    ps.close();
                }

                PreparedStatement ps = db.prepareStatement("update SAKAI_PERSON_T set HOME_PHONE = null, TELEPHONE_NUMBER = null, MOBILE = null, FAX_NUMBER = null");
                ps.executeUpdate();
                ps.close();

                db.commit();
            } finally {
                db.setAutoCommit(oldAutoCommit);
                SqlService.returnConnection(db);
            }
        } catch (SQLException e) {
            log.error("Failure during typed list conversion: {}", e);
            throw new RuntimeException(e);
        }
    }
}
