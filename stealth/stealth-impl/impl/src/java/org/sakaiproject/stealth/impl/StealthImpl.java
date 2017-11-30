/**********************************************************************************
 *
 * Copyright (c) 2015 The Sakai Foundation
 *
 * Original developers:
 *
 *   New York University
 *   Payten Giles
 *   Mark Triggs
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

package org.sakaiproject.stealth.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.github.jknack.handlebars.cache.TemplateCache;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.authz.cover.FunctionManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.stealth.api.SiteService;
import org.sakaiproject.stealth.api.Site;
import org.sakaiproject.stealth.api.UserService;
import org.sakaiproject.stealth.api.User;
import org.sakaiproject.stealth.api.ToolService;
import org.sakaiproject.stealth.api.ToolsBySite;
import org.sakaiproject.stealth.api.ToolsByUser;
import org.sakaiproject.stealth.api.I18n;
import org.sakaiproject.stealth.api.Stealth;
import org.sakaiproject.stealth.api.StealthException;
import org.sakaiproject.stealth.impl.common.SakaiI18n;
import org.sakaiproject.stealth.impl.handlebars.ForeverTemplateCache;
import org.sakaiproject.stealth.impl.user.NetIdStorage;
import org.sakaiproject.stealth.impl.site.SiteIdStorage;
import org.sakaiproject.stealth.impl.stealthtools.StealthRulesStorage;
import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the Stealth service.  Provides system initialization
 * and access to the Stealth System
 */
class StealthImpl implements Stealth {

    private static final Logger LOG = LoggerFactory.getLogger(StealthImpl.class);

    private TemplateCache cache;

    @Override
    public void init() {
        if (ServerConfigurationService.getBoolean("auto.ddl", false) || ServerConfigurationService.getBoolean("stealth.auto.ddl", false)) {
            // runDBMigration(ServerConfigurationService.getString("vendor@org.sakaiproject.db.api.SqlService"));
        }

        // No need to parse each time.
        cache = new ForeverTemplateCache();

        FunctionManager.registerFunction("stealth.manage");
    }

    @Override
    public void destroy() {
    }

    @Override
    public UserService getUsers() {
        return new NetIdStorage();
    }

    @Override
    public SiteService getSites() {
        return new SiteIdStorage();
    }

    @Override
    public ToolService getRules() {
        return new StealthRulesStorage();
    }

    @Override
    public I18n getI18n(ClassLoader loader, String resourceBase) {
        return new SakaiI18n(loader, resourceBase);
    }

    private Handlebars loadHandleBars(final I18n i18n) {
        Handlebars handlebars = new Handlebars()
                .with(cache);

        handlebars.registerHelper("t", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String key = options.param(0);
                return i18n.t(key);
            }
        });

        return handlebars;
    }

}
