package org.sakaiproject.tool.assessment.util;

import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.tool.assessment.data.dao.grading.ItemGradingData;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AnswerIfc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class NYUSamigoLogger {
    static public void logGetAnswerScore(ItemGradingData data, AnswerIfc answer, Map publishedAnswerHash) {
        if (!"true".equals(HotReloadConfigurationService.getString("nyu.samigo.loggger.getanswerscore", "false"))) {
            return;
        }

        try {
            // get back what we think is the answer.isCorrect
            SqlService sqlService = (SqlService) org.sakaiproject.component.cover.ComponentManager.get("org.sakaiproject.db.api.SqlService"); 
            Connection db = sqlService.borrowConnection();

            Boolean isCorrectFromDB = null;

            try {
                PreparedStatement ps = db.prepareStatement("select iscorrect " +
                        "from SAM_PUBLISHEDANSWER_T " +
                        "where answerid = ?");
                ps.setLong(1, answer.getId());

                ResultSet rs = ps.executeQuery();
                try {
                    if (rs.next()) {
                        isCorrectFromDB = Boolean.valueOf(rs.getInt(1) == 1); 
                    }
                } finally {
                    rs.close();
                }
            } finally {
                sqlService.returnConnection(db);
            }

            if (isCorrectFromDB == null) {
                // No answer entry
            } else {
                if (answer.getIsCorrect().booleanValue() == isCorrectFromDB.booleanValue()) {
                    // OK!
                } else {
                    // Not OK
                    System.err.println("DO THE LOGGING");
                }
            }
        } catch (Exception e) {
        }
    }
}
