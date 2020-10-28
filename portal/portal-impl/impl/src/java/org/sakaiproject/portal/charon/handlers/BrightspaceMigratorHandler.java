package org.sakaiproject.portal.charon.handlers;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.cover.UserDirectoryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BrightspaceMigratorHandler extends BasePortalHandler {

    private static final String URL_FRAGMENT = "brightspace-migrator";
    private static final String SESSION_KEY_IS_INSTRUCTOR = "NYU_IS_INSTRUCTOR";
    private SqlService sqlService;
    private static Log M_log = LogFactory.getLog(BrightspaceMigratorHandler.class);

    public BrightspaceMigratorHandler()
    {
        setUrlFragment(BrightspaceMigratorHandler.URL_FRAGMENT);
        if(sqlService == null) {
            sqlService = (SqlService) org.sakaiproject.component.cover.ComponentManager.get("org.sakaiproject.db.api.SqlService");
        }
    }

    @Override
    public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
                     Session session) throws PortalHandlerException
    {
        if ((parts.length >= 2) && (parts[1].equals(BrightspaceMigratorHandler.URL_FRAGMENT)))
        {
            try
            {
                if (!isInstructor()) {
                    return NEXT;
                }

                JSONObject obj = new JSONObject();

                JSONArray sitesJSON = new JSONArray();
                Set<String> terms = new HashSet<>();

                String termFilter = StringUtils.trimToNull(req.getParameter("term"));
                String queryFilter = StringUtils.trimToNull(req.getParameter("q"));

                for (SiteToArchive site : instructorSites().values()) {
                    terms.add(site.term);

                    if (termFilter != null && !termFilter.equals(site.term)) {
                        continue;
                    }

                    if (queryFilter != null && !site.title.toLowerCase().contains(queryFilter.toLowerCase())) {
                        continue;
                    }

                    JSONObject siteJSON = new JSONObject();
                    siteJSON.put("site_id", site.siteId);
                    siteJSON.put("title", site.title);
                    siteJSON.put("term", site.term);

                    boolean queued = false;
                    for (SiteArchiveRequest request : site.requests) {
                        siteJSON.put("queued_by", request.queuedBy);
                        siteJSON.put("queued_at", request.queuedAt);
                        siteJSON.put("archived_at", request.archivedAt);
                        siteJSON.put("uploaded_at", request.uploadedAt);
                        siteJSON.put("completed_at", request.completedAt);
                        siteJSON.put("status", request.getStatus());
                        siteJSON.put("brightspace_org_unit_id", request.brightspaceOrgUnitId);
                        queued = true;
                    }
                    siteJSON.put("queued", queued);
                    sitesJSON.add(siteJSON);
                }

                obj.put("sites", sitesJSON);

                JSONArray termsJSON = new JSONArray();
                for (String term : terms) {
                    termsJSON.add(term);
                }
                obj.put("terms", termsJSON);

                res.getWriter().write(obj.toString());

                return END;
            }
            catch (Exception ex)
            {
                throw new PortalHandlerException(ex);
            }
        }
        else
        {
            return NEXT;
        }
    }

    @Override
    public int doPost(String[] parts, HttpServletRequest req,
                      HttpServletResponse res, Session session)
            throws PortalHandlerException {
        if ((parts.length >= 2) && (parts[1].equals(BrightspaceMigratorHandler.URL_FRAGMENT)))
        {
            try {
                if (!isInstructor()) {
                    return NEXT;
                }

                String siteId = StringUtils.trimToNull(req.getParameter("site_id"));
                if (instructorSites().containsKey(siteId)) {
                    queueSiteForArchive(siteId);
                }
                res.getWriter().write("{'success':true}");

                return END;
            } catch (Exception e) {
                throw new PortalHandlerException(e);
            }
        }

        return NEXT;
    }

    public boolean isInstructor() {
        if (!"true".equals(HotReloadConfigurationService.getString("brightspace.selfservice.enabled", "true"))) {
            // Self-service is disabled!
            return false;
        }

        Session session = SessionManager.getCurrentSession();
        if (session.getAttribute(SESSION_KEY_IS_INSTRUCTOR) != null) {
            return (boolean)session.getAttribute(SESSION_KEY_IS_INSTRUCTOR);
        }

        boolean result = false;

        String netid = UserDirectoryService.getCurrentUser().getEid();

        try {
            Connection db = sqlService.borrowConnection();

            try {
                PreparedStatement ps = db.prepareStatement("select count(1) " +
                        " from CM_MEMBERSHIP_T mem" +
                        " where mem.user_id = ?" +
                        " and mem.role = ?" +
                        " and rownum < 2");
                ps.setString(1, netid);
                ps.setString(2, "I");

                ResultSet rs = ps.executeQuery();
                try {
                    if (rs.next()) {
                        result = rs.getInt(1) > 0;
                    }
                } finally {
                    rs.close();
                }
            } finally {
                sqlService.returnConnection(db);
            }
        } catch (SQLException e) {
            M_log.warn(this + ".isInstructor: " + e);
        }

        session.setAttribute(SESSION_KEY_IS_INSTRUCTOR, result);

        return result;
    }

    private Map<String, SiteToArchive> instructorSites() {
        String netid = UserDirectoryService.getCurrentUser().getEid();
        Map<String, SiteToArchive> results = new HashMap<>();

        try {
            Connection db = sqlService.borrowConnection();

            try {
                PreparedStatement ps = db.prepareStatement("select ss.site_id, ss.title, to_char(ssp.value) as term, saq.*" +
                        " from SAKAI_SITE ss" +
                        " inner join SAKAI_SITE_USER ssu on ssu.site_id = ss.site_id" +
                        " inner join SAKAI_USER_ID_MAP umap on umap.user_id = ssu.user_id" +
                        " inner join SAKAI_SITE_PROPERTY ssp on ssp.site_id = ss.site_id and ssp.name = 'term_eid'" +
                        " inner join NYU_T_ACAD_SESSION sess on sess.cle_eid = to_char(ssp.value)" +
                        " left join NYU_T_SITE_ARCHIVES_QUEUE saq on saq.site_id = ss.site_id" +
                        " where umap.eid = ?" +
                        " and ssu.permission = -1" +
                        " and sess.strm >= 1194" + //Spring 2019
                        " order by ss.createdon desc, saq.queued_at asc");
                ps.setString(1, netid);

                ResultSet rs = ps.executeQuery();
                try {
                    while (rs.next()) {
                        String siteId = rs.getString("site_id");

                        if (!results.containsKey(siteId)) {
                            SiteToArchive site = new SiteToArchive();
                            site.siteId = siteId;
                            site.title = rs.getString("title");
                            site.term = rs.getString("term");
                            site.requests = new ArrayList<>();
                            results.put(siteId, site);
                        }

                        if (rs.getString("queued_by") != null) {
                            SiteArchiveRequest request = new SiteArchiveRequest();
                            request.queuedAt = rs.getLong("queued_at");
                            request.queuedBy = rs.getString("queued_by");
                            request.archivedAt = rs.getLong("archived_at");
                            request.uploadedAt = rs.getLong("uploaded_at");
                            request.completedAt = rs.getLong("completed_at");
                            request.brightspaceOrgUnitId = rs.getLong("brightspace_org_unit_id");
                            results.get(siteId).requests.add(request);
                        }
                    }
                } finally {
                    rs.close();
                }
            } finally {
                sqlService.returnConnection(db);
            }
        } catch (SQLException e) {
            M_log.error(this + ".instructorSites: " + e);
        }

        return results;
    }

    private void queueSiteForArchive(String siteId) {
        String netid = UserDirectoryService.getCurrentUser().getEid();

        try {
            Connection db = sqlService.borrowConnection();

            try {
                PreparedStatement ps = db.prepareStatement("insert into NYU_T_SITE_ARCHIVES_QUEUE (site_id, queued_at, queued_by)" +
                                                           " values (?, ?, ?)");
                ps.setString(1, siteId);
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, netid);

                ps.executeUpdate();

                db.commit();
            } finally {
                sqlService.returnConnection(db);
            }
        } catch (SQLException e) {
            M_log.error(this + ".queueSiteForArchive: " + e);
        }
    }

    private class SiteToArchive {
        public String siteId;
        public String title;
        public String term;
        public List<SiteArchiveRequest> requests;
    }

    private class SiteArchiveRequest {
        public String queuedBy;
        public long queuedAt;
        public long archivedAt;
        public long uploadedAt;
        public long completedAt;
        public long brightspaceOrgUnitId;

        public String getStatus() {
            if (this.completedAt > 0) {
                return "COMPLETED";
            } else if (this.uploadedAt > 0) {
                return "UPLOADED";
            } else if (this.archivedAt > 0) {
                return "ARCHIVED";
            } else if (this.queuedAt > 0) {
                return "QUEUED";
            } else {
                return "READY_FOR_ARCHIVE";
            }
        }
    }
}


//    CREATE TABLE "NYU_T_SITE_ARCHIVES_QUEUE"
//        ("SITE_ID" VARCHAR2(100) NOT NULL,
//        "QUEUED_BY" VARCHAR2(100),
//        "QUEUED_AT" NUMBER(38,0),
//        "ARCHIVED_AT" NUMBER(38,0),
//        "UPLOADED_AT" NUMBER(38,0),
//        "COMPLETED_AT" NUMBER(38,0),
//        "BRIGHTSPACE_ORG_UNIT_ID" NUMBER(38,0),
//        CONSTRAINT "NYU_T_SITE_ARCHIVES_QUEUE_PK" PRIMARY KEY ("SITE_ID"))

