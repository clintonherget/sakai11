package edu.nyu.classes.seats;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.cover.PreferencesService;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.nyu.classes.seats.handlers.*;

public class ToolServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ToolServlet.class);

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        I18n i18n = new I18n(this.getClass().getClassLoader(), "edu.nyu.classes.seats.i18n.seats");

        URL toolBaseURL = determineBaseURL();
        Handlebars handlebars = loadHandlebars(toolBaseURL, i18n);

        try {
            Map<String, Object> context = new HashMap<String, Object>();

            context.put("baseURL", toolBaseURL);
            context.put("layout", true);
            context.put("skinRepo", ServerConfigurationService.getString("skin.repo", ""));
            context.put("randomSakaiHeadStuff", request.getAttribute("sakai.html.head"));

            if (ToolManager.getCurrentPlacement() != null) {
                context.put("siteId", ToolManager.getCurrentPlacement().getContext());
            }

            Handler handler = handlerForRequest(request);

            handler.handle(request, response, context);

            response.setHeader("Content-Type", handler.getContentType());

            if (handler.hasRedirect()) {
                if (handler.getRedirect().startsWith("http")) {
                    response.sendRedirect(handler.getRedirect());
                } else {
                    response.sendRedirect(toolBaseURL + handler.getRedirect());
                }
            } else if (handler.hasTemplate()) {
                if (Boolean.TRUE.equals(context.get("layout"))) {
                    Template template = handlebars.compile("edu/nyu/classes/seats/views/layout");
                    response.getWriter().write(template.apply(context));
                } else {
                    Template template = handlebars.compile("edu/nyu/classes/seats/views/" + context.get("subpage"));
                    response.getWriter().write(template.apply(context));
                }
            }
        } catch (IOException e) {
            LOG.warn("Write failed", e);
        }
    }

    private Handler handlerForRequest(HttpServletRequest request) {
        String path = request.getPathInfo();

        if (path == null) {
            path = "";
        }

        // FIXME
        return new HomeHandler();
    }

    private URL determineBaseURL() {
        try {
            return new URL(ServerConfigurationService.getPortalUrl() + getBaseURI() + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Couldn't determine tool URL", e);
        }
    }

    private String getBaseURI() {
        String result = "";

        String siteId = null;
        String toolId = null;

        if (ToolManager.getCurrentPlacement() != null) {
            siteId = ToolManager.getCurrentPlacement().getContext();
            toolId = ToolManager.getCurrentPlacement().getId();
        }

        if (siteId != null) {
            result += "/site/" + siteId;
            if (toolId != null) {
                result += "/tool/" + toolId;
            }
        }

        return result;
    }

    private Handlebars loadHandlebars(final URL baseURL, final I18n i18n) {
        Handlebars handlebars = new Handlebars();

        handlebars.setInfiniteLoops(true);

        handlebars.registerHelper("subpage", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String subpage = options.param(0);
                try {
                    Template template = handlebars.compile("edu/nyu/classes/seats/views/" + subpage);
                    return template.apply(context);
                } catch (IOException e) {
                    LOG.warn("IOException while loading subpage", e);
                    return "";
                }
            }
        });

        handlebars.registerHelper(Handlebars.HELPER_MISSING, new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) throws IOException {
                throw new RuntimeException("Failed to find a match for: " + options.fn.text());
            }
        });

        handlebars.registerHelper("show-time", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                long utcTime = options.param(0) == null ? 0 : options.param(0);

                if (utcTime == 0) {
                    return "-";
                }

                Time time = TimeService.newTime(utcTime);

                return time.toStringLocalFull();
            }
        });

        handlebars.registerHelper("actionURL", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String type = options.param(0);
                String uuid = options.param(1);
                String action = options.param(2);

                try {
                    return new URL(baseURL, type + "/" + uuid + "/" + action).toString();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed while building action URL", e);
                }
            }
        });

        handlebars.registerHelper("newURL", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String type = options.param(0);
                String action = options.param(1);

                try {
                    return new URL(baseURL, type + "/" + action).toString();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed while building newURL", e);
                }
            }
        });

        handlebars.registerHelper("t", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String key = Arrays.stream(options.params).map(Object::toString).collect(Collectors.joining("_"));
                return i18n.t(key);
            }
        });

        handlebars.registerHelper("selected", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String option = options.param(0);
                String value = options.param(1);

                return option.equals(value) ? "selected" : "";
            }
        });

        return handlebars;
    }
}