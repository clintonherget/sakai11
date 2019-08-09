package org.sakaiproject.assignment.datemanager.handlers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class AssignmentsFeedHandler implements Handler {
    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String siteId = (String)context.get("siteId");

            AssignmentService assignmentService = ComponentManager.get(AssignmentService.class);
            Collection<Assignment> assignments = assignmentService.getAssignmentsForContext(siteId);

            JSONArray result = new JSONArray();

            for (Assignment assignment : assignments) {
                JSONObject assobj = new JSONObject();
                assobj.put("title", assignment.getTitle());
                result.add(assobj);
            }

            response.getWriter().write(result.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasRedirect() {
        return (redirectTo != null);
    }

    public String getRedirect() {
        return redirectTo;
    }

    public Errors getErrors() {
        return null;
    }

    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean hasTemplate() {
        return false;
    }
}
