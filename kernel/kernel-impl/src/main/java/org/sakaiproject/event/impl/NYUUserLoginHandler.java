package org.sakaiproject.event.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class NYUUserLoginHandler {

    private final static String OUR_ADDED_PROPERTY = "nyu-login-handler-added";
    private static Logger LOG = LoggerFactory.getLogger(NYUUserLoginHandler.class);


    public void invoke(Session sakaiSession) {
        String userId = sakaiSession.getUserId();

        try {
            long startTime = System.currentTimeMillis();

            String instructorToolsStr = HotReloadConfigurationService.getString("nyu.instructor-auto-tools", "");

            if ("".equals(instructorToolsStr.trim())) {
                return;
            }

            String[] instructorTools = instructorToolsStr.split(", *");
            User user = UserDirectoryService.getUser(userId);

            // Creates the workspace if it doesn't already exist
            Site workspace = SiteService.getSite("~" + userId);

            if (isInstructor(user)) {
                addToolsIfMissing(workspace, instructorTools);
            } else {
                removeToolsIfPresent(workspace, instructorTools);
            }

            LOG.info("Processed user tools in " + (System.currentTimeMillis() - startTime) + " ms");
        } catch (Exception e) {
            LOG.error("Failure while applying auto tools: " + e);
            e.printStackTrace();
        }
    }


    private void addToolsIfMissing(Site workspace, String[] instructorTools) throws Exception {
        Set<String> foundTools = new HashSet<>(16);

        // Add missing
        for (SitePage page : workspace.getPages()) {
            for (ToolConfiguration tool : page.getTools()) {
                foundTools.add(tool.getToolId());
            }
        }

        boolean updated = false;

        for (String registration : instructorTools) {
            if (!foundTools.contains(registration)) {
                Tool tool = ToolManager.getTool(registration);

                LOG.info("Adding tool '{}' to workspace {}", registration, workspace.getId());
                SitePage page = workspace.addPage();
                page.setTitle(tool.getTitle());
                ResourcePropertiesEdit properties = page.getPropertiesEdit();
                properties.addProperty(OUR_ADDED_PROPERTY, "true");

                page.addTool(tool);
                updated = true;
            }
        }

        if (updated) {
            SiteService.save(workspace);
        }
    }

    private void removeToolsIfPresent(Site workspace, String[] instructorTools) throws Exception {
        List<SitePage> toRemove = new ArrayList<>();

        for (SitePage page : workspace.getPages()) {
            if (!"true".equals(page.getProperties().getProperty("nyu-login-handler-added"))) {
                continue;
            }

            List<ToolConfiguration> tools = page.getTools();

            ToolConfiguration tool = tools.get(0);
            for (String registration : instructorTools) {
                if (registration.equals(tool.getToolId())) {
                    LOG.info("Removing tool '{}' from workspace {}", registration, workspace.getId());
                    toRemove.add(page);
                    break;
                }
            }
        }

        if (!toRemove.isEmpty()) {
            for (SitePage page : toRemove) {
                workspace.removePage(page);
            }

            SiteService.save(workspace);
        }
    }

    private boolean isInstructor(User user) throws SQLException {
        String eid = user.getEid();

        if (eid == null) {
            return false;
        }

        String pretendInstructors = HotReloadConfigurationService.getString("nyu.pretend-instructors", "");
        if (("," + pretendInstructors.trim().replace(" ", "") + ",").indexOf("," + eid + ",") >= 0) {
            return true;
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = SqlService.borrowConnection();

            if (SqlService.getVendor().equals("oracle")) {
                ps = connection.prepareStatement("select 1 from nyu_t_instructors where netid = ? AND rownum <= 1");
            } else {
                // MySQL
                ps = connection.prepareStatement("select 1 from nyu_t_instructors where netid = ? LIMIT 1");
            }
            ps.setString(1, eid);
            rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            }

            return false;
        } finally {
            if (ps != null) {
                try { ps.close (); } catch (Exception e) {}
            }
            if (rs != null) {
                try { rs.close (); } catch (Exception e) {}
            }

            SqlService.returnConnection (connection);
        }
    }

}
