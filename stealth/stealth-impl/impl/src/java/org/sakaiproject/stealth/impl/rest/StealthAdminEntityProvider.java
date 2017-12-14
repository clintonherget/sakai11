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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @EntityCustomAction(action = "validateSite", viewKey = EntityView.VIEW_LIST)
    public String validateSite(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            String siteID = view.getPathSegment(2);
            List<Site> list_sites;

            if (siteID != null) {
                if(siteID.length()>1){
                    list_sites = stealth().getSites().getSiteIdList(siteID);
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
            JSONArray data = new JSONArray();

            // Generate two years worth of terms
            for (int i = -2; i <= 2; i ++){
                JSONObject row = new JSONObject();
                row.put("id", "Winter " + (currentYear + i));
                row.put("text", "Winter " + (currentYear + i));
                data.add(row);

                row = new JSONObject();
                row.put("id", "Spring " + (currentYear + i));
                row.put("text", "Spring " + (currentYear + i));
                data.add(row);

                row = new JSONObject();
                row.put("id", "Summer " + (currentYear + i));
                row.put("text", "Summer " + (currentYear + i));
                data.add(row);

                row = new JSONObject();
                row.put("id", "Fall " + (currentYear + i));
                row.put("text", "Fall " + (currentYear + i));
                data.add(row);
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

            result.put("results", list_tools);

            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "getRuleByUser", viewKey = EntityView.VIEW_LIST)
    public String getRuleByUser(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            String netID = view.getPathSegment(2);
            List<StealthRules> list_user = null;

            if (netID != null) {
                list_user = stealth().getRules().searchByNetId(netID);
                JSONArray data = new JSONArray();
                for (StealthRules rule : list_user) {
                    JSONObject row = new JSONObject();
                    row.put("netid", rule.getNetId());
                    row.put("term", rule.getTerm());
                    row.put("toolid", rule.getToolId());
                    data.add(row);
                }
                result.put("results", data);
            } else {
                result.put("results", "[\"Error\"]");
            }
            return result.toJSONString();
        } catch (Exception e) {
            return respondWithError(e);
        }
    }

    @EntityCustomAction(action = "getRuleBySite", viewKey = EntityView.VIEW_LIST)
    public String getRuleBySite(EntityView view, Map<String, Object> params) {
        try {
            assertPermission();
            JSONObject result = new JSONObject();
            String siteID = view.getPathSegment(2);
            List<StealthRules> list_sites = null;

            if (siteID != null) {
                result.put("query", siteID);
                list_sites = stealth().getRules().searchBySiteId(siteID);
                JSONArray data = new JSONArray();
                for (StealthRules rule : list_sites) {
                    JSONObject row = new JSONObject();
                    row.put("siteid", rule.getSiteId());
                    row.put("toolid", rule.getToolId());
                    data.add(row);
                }
                result.put("results", data);
            } else {
                result.put("results", "[\"Error\"]");
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

        public String getString(String name) {
            String result = (String)params.get(name);

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

        public long getEpochMS(String name) {
            return Long.valueOf(getString(name));
        }

        public boolean getBoolean(String name) {
            return Boolean.valueOf(getString(name));
        }

        public boolean containsKey(String name) {
            return this.params.containsKey(name);
        }
    }
}
