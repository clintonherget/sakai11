package edu.nyu.classes.seats.handlers;

import edu.nyu.classes.seats.models.Meeting;
import edu.nyu.classes.seats.models.SeatAssignment;
import edu.nyu.classes.seats.models.SeatSection;
import edu.nyu.classes.seats.storage.DBConnection;
import edu.nyu.classes.seats.storage.Locks;
import edu.nyu.classes.seats.storage.SeatsStorage;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitSectionHandler implements Handler {

    protected String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            JSONObject result = new JSONObject();

            DBConnection db = (DBConnection)context.get("db");

            RequestParams p = new RequestParams(request);

            String sectionId = p.getString("sectionId", null);
            if (sectionId == null) {
                throw new RuntimeException("Need argument: sectionId");
            }

            String numberOfGroupsParam = p.getString("numberOfGroups", null);
            if (numberOfGroupsParam == null) {
                throw new RuntimeException("Need argument: numberOfGroups");
            }
            Integer numberOfGroups = Integer.parseInt(numberOfGroupsParam);

            String selectionTypeParam = p.getString("selectionType", null);
            SeatsStorage.SelectionType selectionType = null;
            try {
                selectionType = SeatsStorage.SelectionType.valueOf(selectionTypeParam);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Need argument: selectionType");
            }

            String siteId = (String)context.get("siteId");
            Locks.lockSiteForUpdate(siteId);
            try {
                SeatSection seatSection = SeatsStorage.getSeatSection(db, sectionId, siteId);
                SeatsStorage.bootstrapGroupsForSection(db, seatSection, numberOfGroups, selectionType);
                SeatsStorage.markSectionAsSplit(db, seatSection);
            } finally {
                Locks.unlockSiteForUpdate(siteId);
            }

            try {
                response.getWriter().write(result.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean hasTemplate() {
        return false;
    }

    @Override
    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }

    public Errors getErrors() {
        return null;
    }

    public boolean hasRedirect() {
        return false;
    }

    public String getRedirect() {
        return "";
    }
}


