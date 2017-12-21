/**********************************************************************************
 *
 * Copyright (c) 2015 The Sakai Foundation
 *
 * Original developers:
 *
 *   New York University
 *   Eric Lin
 *   Bhavesh Vasandani
 *   Steven Adam
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.stealth.impl.rest;

import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.lang.reflect.Array;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.stealth.api.Errors;
import org.sakaiproject.stealth.api.Stealth;
import org.sakaiproject.stealth.api.StealthException;
import org.sakaiproject.stealth.api.User;
import org.sakaiproject.stealth.api.Site;
import org.sakaiproject.stealth.api.StealthTool;
import org.sakaiproject.stealth.api.StealthRules;
import org.sakaiproject.tool.cover.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Web services for managing popups and banners.  Intended for administrator use.
 */
public class StealthAdminEntityProvider implements EntityProvider, AutoRegisterEntityProvider, ActionsExecutable, Outputable, Describeable {

    private static final Logger LOG = LoggerFactory.getLogger(StealthAdminEntityProvider.class);
    private static final String ADMIN_SITE_REALM = "/site/!admin";
    private static final String SAKAI_SESSION_TOKEN_PROPERTY = "sakai.stealth-admin.token";
    private static final String REQUEST_SESSION_PARAMETER = "session";
    private static final String[] EMPTY_ARRAY = new String[0];

    protected DeveloperHelperService developerHelperService;
    private EntityProviderManager entityProviderManager;

    @Override
    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON };
    }

    @Override
    public String getEntityPrefix() {
        return "stealth-admin";
    }

    @EntityCustomAction(action = "searchNetid", viewKey = EntityView.VIEW_LIST)
    public String searchNetid(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            String netID = view.getPathSegment(2);

            if (netID != null) {
                if(netID.length() > 1) {
                    List<User> list_user = stealth().getUsers().getNetIdList(netID);
                    JSONArray data = new JSONArray();
                    for (User u : list_user) {
                        JSONObject row = new JSONObject();
                        row.put("id", u.getNetId());
                        row.put("text", u.getNetId());
                        data.add(row);
                    }
                    result.put("results", data);
                } else {
                    result.put("results", new JSONArray());
                }

            } else {
                result.put("results", "[\"Error\"]");
            }
            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "searchSiteid", viewKey = EntityView.VIEW_LIST)
    public String validateSite(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            String siteID = view.getPathSegment(2);

            if (siteID != null) {
                if(siteID.length() > 4){
                    List<Site> list_sites = stealth().getSites().getSiteIdList(siteID);
                    JSONArray data = new JSONArray();
                    for (Site s : list_sites) {
                        JSONObject row = new JSONObject();
                        row.put("id", s.getSiteId());
                        row.put("text", s.getSiteId());
                        data.add(row);
                    }
                    result.put("results", data);
                }
                else {
                    result.put("results", new JSONArray());
                }
            }
            else {
                result.put("results", "[\"Error\"]");
            }
            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "getTerms", viewKey = EntityView.VIEW_LIST)
    public String getTerms(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            JSONArray data = new JSONArray();
            JSONObject row = new JSONObject();

            switch(currentMonth){
                case 1:
                    row.put("id", "Summner " + (currentYear - 1));
                    row.put("text", "Summner " + (currentYear - 1));
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Fall " + (currentYear - 1));
                    row.put("text", "Fall " + (currentYear - 1));
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Winter " + currentYear);
                    row.put("text", "Winter " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Spring " + currentYear);
                    row.put("text", "Spring " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Summer " + currentYear);
                    row.put("text", "Summer " + currentYear);
                    data.add(row);
                    break;
                case 2: case 3: case 4: case 5:
                    row.put("id", "Fall " + (currentYear - 1));
                    row.put("text", "Fall " + (currentYear - 1));
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Winter " + currentYear);
                    row.put("text", "Winter " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Spring " + currentYear);
                    row.put("text", "Spring " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Summer " + currentYear);
                    row.put("text", "Summer " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Fall " + currentYear);
                    row.put("text", "Fall " + currentYear);
                    data.add(row);
                    break;
                case 6: case 7: case 8:
                    row.put("id", "Winter " + currentYear);
                    row.put("text", "Winter " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Spring " + currentYear);
                    row.put("text", "Spring " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Summer " + currentYear);
                    row.put("text", "Summer " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Fall " + currentYear);
                    row.put("text", "Fall " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Winter " + (currentYear + 1));
                    row.put("text", "Winter " + (currentYear + 1));
                    data.add(row);
                    break;
                default:
                    row.put("id", "Spring " + currentYear);
                    row.put("text", "Spring " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Summer " + currentYear);
                    row.put("text", "Summer " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Fall " + currentYear);
                    row.put("text", "Fall " + currentYear);
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Winter " + (currentYear + 1));
                    row.put("text", "Winter " + (currentYear + 1));
                    data.add(row);
                    row = new JSONObject();
                    row.put("id", "Spring " + (currentYear + 1));
                    row.put("text", "Spring " + (currentYear + 1));
                    data.add(row);
                    break;
            }
            result.put("results", data);

            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "getTools", viewKey = EntityView.VIEW_LIST)
    public String getTools(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            List<StealthTool> list_tools = stealth().getRules().getAllStealthTools();

            JSONArray data = new JSONArray();
            for (StealthTool t : list_tools) {
                JSONObject row = new JSONObject();
                row.put("id", t.getToolId());
                row.put("text", t.getToolName());
                data.add(row);
            }
            result.put("results", data);

            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "getRuleByUser", viewKey = EntityView.VIEW_NEW)
    public String getRuleByUser(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            WrappedParams wp = new WrappedParams(params);
            JSONArray data = new JSONArray();
            String netID = wp.getString("rule-by-netid");
            List<StealthRules> list_rules;
            Map<String, StealthRules> list_unique = new HashMap<>();

            if (netID != null) {
                list_rules = stealth().getRules().searchByNetId(netID);
                for (StealthRules rule : list_rules) {
                    if(list_unique.containsKey(rule.getSiteId())){
                        list_unique.put(rule.getSiteId(),
                        new StealthRules(rule.getNetId(), rule.getCourseTitle(), rule.getSiteId(), rule.getToolName()
                        + "," + list_unique.get(rule.getSiteId()).getToolName()));
                    }else{
                        list_unique.put(rule.getSiteId(),
                        new StealthRules(rule.getNetId(), rule.getCourseTitle(), rule.getSiteId(), rule.getToolName()));
                    }
                }
                for (Map.Entry<String, StealthRules> entry : list_unique.entrySet()) {
                    JSONArray row = new JSONArray();
                    row.add(entry.getValue().getNetId());
                    row.add(entry.getValue().getCourseTitle());
                    row.add(entry.getValue().getSiteId());
                    row.add(entry.getValue().getToolName());
                    data.add(row);
                }
            } else {
                return (new JSONArray()).toJSONString();
            }
            return data.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "getRuleBySite", viewKey = EntityView.VIEW_NEW)
    public String getRuleBySite(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            WrappedParams wp = new WrappedParams(params);
            JSONArray data = new JSONArray();
            String siteID = wp.getString("rule-by-siteid");
            List<StealthRules> list_rules;
            Map<String, StealthRules> list_unique = new HashMap<>();

            if (siteID != null) {
                list_rules = stealth().getRules().searchBySiteId(siteID);
                for (StealthRules rule : list_rules) {
                    if(list_unique.containsKey(rule.getSiteId())){
                        list_unique.put(rule.getSiteId(),
                        new StealthRules(rule.getNetId(), rule.getCourseTitle(), rule.getSiteId(), rule.getToolName()
                        + "," + list_unique.get(rule.getSiteId()).getToolName()));
                    }else{
                        list_unique.put(rule.getSiteId(),
                        new StealthRules(rule.getNetId(), rule.getCourseTitle(), rule.getSiteId(), rule.getToolName()));
                    }
                }
                for (Map.Entry<String, StealthRules> entry : list_unique.entrySet()) {
                    JSONArray row = new JSONArray();
                    row.add(entry.getValue().getNetId());
                    row.add(entry.getValue().getCourseTitle());
                    row.add(entry.getValue().getSiteId());
                    row.add(entry.getValue().getToolName());
                    data.add(row);
                }
            } else {
                return (new JSONArray()).toJSONString();
            }
            return data.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "handleAddForm", viewKey = EntityView.VIEW_NEW)
    public String handleAddForm(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            WrappedParams wp = new WrappedParams(params);
            String[] netid = wp.getList("netid");
            String[] siteid = wp.getList("siteid");
            String[] term = wp.getList("term");
            String[] tool = wp.getList("tool");

            if(netid.length == 0 && siteid.length == 0) {
                result.put("error", "No netid or siteid provided");
                return result.toJSONString();
            }

            JSONArray data = new JSONArray();
            for (String n : netid) {
                if(term.length > 0) {
                    for (String tm : term) {
                        for (String t : tool) {
                            JSONObject entry = new JSONObject();
                            entry.put("netid", n);
                            entry.put("term", tm);
                            entry.put("tool", t);
                            stealth().getRules().addPermissionByUser(n, tm, t);
                            data.add(entry);
                        }
                    }
                } else {
                    for (String t : tool) {
                        JSONObject entry = new JSONObject();
                        entry.put("netid", n);
                        entry.put("term", "NULL");
                        entry.put("tool", t);
                        stealth().getRules().addPermissionByUser(n, "NULL", t);
                        data.add(entry);
                    }
                }
            }

            for (String s : siteid) {
                for (String t : tool) {
                    JSONObject entry = new JSONObject();
                    entry.put("siteid", s);
                    entry.put("tool", t);
                    stealth().getRules().addPermissionBySite(s, t);
                    data.add(entry);
                }
            }
            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "testPost", viewKey = EntityView.VIEW_NEW)
    public String testPost(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();

            for(Map.Entry<String, Object> entry : params.entrySet()) {
                if(entry.getValue() instanceof Collection<?>){
                    JSONArray data = new JSONArray();
                    for(Object o : (Collection<?>) entry.getValue()){
                        data.add(o.toString());
                    }
                    result.put(entry.getKey(), data);
                } else if (entry.getValue().getClass().isArray()) { // String[], int[]
                    Object array = entry.getValue();
                    JSONArray data = new JSONArray();
                    for(int i = 0; i < Array.getLength(array); i++){
                        data.add(Array.get(array, i).toString());
                    }
                    result.put(entry.getKey(), data);
                } else {
                    JSONArray data = new JSONArray();
                    data.add(entry.getValue());
                    result.put(entry.getKey(), data);
                }
            }

            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    private String respondWithError(Exception e) {
        JSONObject result = new JSONObject();
        result.put("status", "ERROR");
        result.put("message", e.getMessage());

        LOG.error("Caught an error while handling a request", e);

        return result.toJSONString();
    }

    private String respondWithError(Errors e) {
        JSONObject result = new JSONObject();
        result.put("status", "ERROR");
        result.put("message", e.toMap());

        return result.toJSONString();
    }

    private void assertPermission() {
        if (!SecurityService.unlock("stealth.manage", ADMIN_SITE_REALM)) {
            LOG.error("assertPermission denied access to user " + SessionManager.getCurrentSessionUserId());
            throw new StealthException("Access denied, no permission");
        }
    }

    private Stealth stealth() {
        return (Stealth) ComponentManager.get(Stealth.class);
    }

    public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
        this.entityProviderManager = entityProviderManager;
    }

    public void setDeveloperHelperService(DeveloperHelperService developerHelperService) {
        this.developerHelperService = developerHelperService;
    }

    private class WrappedParams {

        private final Map<String, Object> params;

        public WrappedParams(Map<String, Object> params) {
            this.params = params;
        }

        public String[] getList(String name){
            try{
                if(!params.containsKey(name)){
                    return EMPTY_ARRAY;
                }else if(params.get(name).getClass().isArray()){
                    return (String[]) params.get(name);
                }else{
                    String[] result = new String[1];
                    result[0] = (String) params.get(name);
                    return result;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Error converting argument " + name + " to array.");
            }
        }

        public String getString(String name) {
            String result = (String) params.get(name);
            if (result == null) {
                throw new IllegalArgumentException("Parameter " + name + " cannot be null.");
            }
            return result;
        }

        public String getString(String name, String defaultValue) {
            if (containsKey(name)) {
                return getString(name);
            } else {
                return defaultValue;
            }
        }

        public boolean getBoolean(String name) {
            return Boolean.valueOf(getString(name));
        }

        public boolean containsKey(String name) {
            return this.params.containsKey(name);
        }
    }
}
