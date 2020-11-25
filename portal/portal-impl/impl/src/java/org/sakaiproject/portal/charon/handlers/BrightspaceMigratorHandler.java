package org.sakaiproject.portal.charon.handlers;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
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
import java.util.stream.Collectors;

public class BrightspaceMigratorHandler extends BasePortalHandler {

    private static final String URL_FRAGMENT = "brightspace-migrator";
    private static final String SESSION_KEY_ALLOWED_TO_MIGRATE = "NYU_ALLOWED_TO_MIGRATE";
    private SqlService sqlService;
    private static Log M_log = LogFactory.getLog(BrightspaceMigratorHandler.class);

    private AuthzGroupService authzGroupService = (AuthzGroupService)ComponentManager.get("org.sakaiproject.authz.api.AuthzGroupService");

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
                if (!isAllowedToMigrateSitesToBrightspace()) {
                    return NEXT;
                }

                JSONObject obj = new JSONObject();

                JSONArray sitesJSON = new JSONArray();
                Set<String> terms = new HashSet<>();

                String termFilter = StringUtils.trimToNull(req.getParameter("term"));
                String queryFilter = StringUtils.trimToNull(req.getParameter("q"));

                for (SiteToArchive site : instructorSites()) {
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

                    JSONArray rostersJSON = new JSONArray();
                    for (String roster : site.rosters) {
                        rostersJSON.add(roster);
                    }
                    siteJSON.put("rosters", rostersJSON);

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
                if (!isAllowedToMigrateSitesToBrightspace()) {
                    return NEXT;
                }

                String siteId = StringUtils.trimToNull(req.getParameter("site_id"));
                if (siteId != null && SecurityService.unlock("site.upd", "/site/" + siteId)) {
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

    public boolean isAllowedToMigrateSitesToBrightspace() {
        if (!"true".equals(HotReloadConfigurationService.getString("brightspace.selfservice.enabled", "true"))) {
            // Self-service is disabled!
            return false;
        }

        Session session = SessionManager.getCurrentSession();
        if (session.getAttribute(SESSION_KEY_ALLOWED_TO_MIGRATE) != null) {
            return (boolean)session.getAttribute(SESSION_KEY_ALLOWED_TO_MIGRATE);
        }

        long timerStartTime = System.currentTimeMillis();

        boolean result = false;

        String netid = UserDirectoryService.getCurrentUser().getEid();

        try {
            Connection db = sqlService.borrowConnection();

            try {
                // First, let's check if the netid has been approved
                PreparedStatement ps = db.prepareStatement("select count(1) " +
                        " from NYU_T_SELF_SERVICE_ACCESS ssa" +
                        " where ssa.netid = ?" +
                        " and ssa.rule_type = 'netid'");
                ps.setString(1, netid);

                ResultSet rs = ps.executeQuery();
                try {
                    if (rs.next()) {
                        result = rs.getInt(1) > 0;
                    }
                } finally {
                    rs.close();
                    ps.close();
                }

                if (!result) {
                    // Secondly, let's check if the user is an instructor of any approved schools/departments
                    RuleSet ruleSet = new RuleSet();

                    ps = db.prepareStatement("select * " +
                            " from NYU_T_SELF_SERVICE_ACCESS ssa" +
                            " where ssa.rule_type != 'netid'");

                    rs = ps.executeQuery();
                    try {
                        while (rs.next()) {
                            ruleSet.addRule(rs.getString("school"), rs.getString("department"));
                        }
                    } finally {
                        rs.close();
                        ps.close();
                    }

                    result = instructorSites().stream().anyMatch(s -> ruleSet.matches(s));
                }
            } finally {
                sqlService.returnConnection(db);
            }
        } catch (SQLException e) {
            M_log.warn(this + ".isInstructor: " + e);
        }

        session.setAttribute(SESSION_KEY_ALLOWED_TO_MIGRATE, result);

        long timerEndTime = System.currentTimeMillis();

        M_log.info(String.format("isAllowedToMigrateSitesToBrightspace took %d ms to complete", timerEndTime - timerStartTime));

        return result;
    }


    private List<SiteToArchive> instructorSites() {
        Map<String, SiteToArchive> results = new LinkedHashMap<>();
        Set<String> allowedTermEids = allowedTermEids();

        Connection db = null;
        try {
            db = sqlService.borrowConnection();

            int start = 1;
            int pageSize = 100;
            int last = start + pageSize - 1;

            PagingPosition paging = new PagingPosition();
            while (true) {
                paging.setPosition(start, last);
                List<Site> userSites = SiteService.getSites(org.sakaiproject.site.api.SiteService.SelectionType.UPDATE, null, null, null, org.sakaiproject.site.api.SiteService.SortType.CREATED_ON_DESC, paging);
                userSites = userSites.stream().filter(s -> {
                                String termEid = s.getProperties().getProperty("term_eid");
                                return termEid == null || allowedTermEids.contains(termEid);
                            }).collect(Collectors.toList());

                for (Site userSite : userSites) {
                    SiteToArchive siteToArchive = new SiteToArchive();
                    siteToArchive.siteId = userSite.getId();
                    siteToArchive.title = userSite.getTitle();
                    siteToArchive.term = userSite.getProperties().getProperty("term_eid");
                    if (siteToArchive.term == null) {
                        siteToArchive.term = StringUtils.capitalize(userSite.getType());
                    }
                    siteToArchive.requests = new ArrayList<>();
                    siteToArchive.rosters = new ArrayList<>(authzGroupService.getProviderIds(String.format("/site/%s", userSite.getId())));

                    siteToArchive.school = userSite.getProperties().getProperty("School");
                    siteToArchive.department = userSite.getProperties().getProperty("Department");

                    results.put(siteToArchive.siteId, siteToArchive);
                }

                List<String> chunkSiteIds = userSites.stream().map(s -> s.getId()).collect(Collectors.toList());

                String placeholders = chunkSiteIds.stream().map(_p -> "?").collect(Collectors.joining(","));
                PreparedStatement ps = db.prepareStatement("select * from NYU_T_SITE_ARCHIVES_QUEUE where site_id in (" + placeholders + ")");

                for (int i = 0; i < chunkSiteIds.size(); i++) {
                    ps.setString(i + 1, chunkSiteIds.get(i));
                }

                ResultSet rs = ps.executeQuery();
                try {
                    while (rs.next()) {
                        String siteId = rs.getString("site_id");

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

                if (userSites.size() < pageSize) {
                    break;
                }

                start = last + 1;
                last = start + pageSize - 1;
            }
        } catch (SQLException e) {
            M_log.error(this + ".instructorSites: " + e);
        } finally {
            if (db != null) {
                sqlService.returnConnection(db);
            }
        }

        return new ArrayList<>(results.values());
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

    private Set<String> allowedTermEids() {
        String termEidsStr = HotReloadConfigurationService.getString("brightspace.selfservice.termeids", "").trim();
        return new HashSet<>(Arrays.asList(termEidsStr.split(" *, *")));
    }

    private class SiteToArchive {
        public String siteId;
        public String title;
        public String term;
        public String school;
        public String department;
        public List<SiteArchiveRequest> requests;
        public List<String> rosters;
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

    private class AccessRule {
        public String school;
        public String department;
    }

    private class RuleSet {
        public Map<String, List<AccessRule>> rules = new HashMap<>();

        public void addRule(String school, String department) {
            if (!rules.containsKey(school)) {
                rules.put(school, new ArrayList<>());
            }
            AccessRule rule = new AccessRule();
            rule.school = school;
            rule.department = department;

            rules.get(school).add(rule);
        }

        public boolean matches(SiteToArchive site) {
            if (rules.containsKey(site.school)) {
                List<AccessRule> matched = rules.get(site.school);

                return matched.stream().anyMatch(r -> {
                    return r.department == null || r.department.equals(site.department);
                });
            }

            return false;
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

