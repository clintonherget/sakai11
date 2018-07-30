package edu.nyu.classes.syllabusfeed;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;


public class SyllabusFeedServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(SyllabusFeedServlet.class);

    private static final String BASE_JOINS =
        (" from nyu_t_course_catalog cc " +
         " inner join sakai_realm_provider srp on srp.provider_id = replace(cc.stem_name, ':', '_')" +
         " inner join sakai_realm sr on sr.realm_key = srp.realm_key " +
         " inner join sakai_site ss on concat('/site/', ss.site_id) = sr.realm_id" +
         " inner join sakai_syllabus_item ssi on ssi.contextId = ss.site_id" +
         " inner join sakai_syllabus_data ssd on ssd.surrogateKey = ssi.id AND ssd.status = 'posted'" +
         " inner join sakai_syllabus_attach ssa on ssa.syllabusId = ssd.id AND export = 1"
         );


    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        if (request.getParameter("get") == null) {
            handleListing(request, response);
        } else {
            handleFetch(request, response);
        }
    }

    private void handleFetch(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String stemName = request.getParameter("get");

        List<String> errors = new ArrayList<>();

        if (stemName.isEmpty()) {
            errors.add("Missing parameter 'get': expected a valid roster stem name");
        }

        if (reportErrors(errors, response)) {
            return;
        }

        Connection conn = null;

        try {
            conn = SqlService.borrowConnection();

            try (PreparedStatement ps = conn.prepareStatement("select ssa.syllabusAttachName, ssa.syllabusAttachType, ssa.attachmentId" +
                                                              BASE_JOINS +
                                                              " where cc.stem_name = ?")) {
                ps.setString(1, stemName);
                try(ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String filename = rs.getString("syllabusAttachName");
                        String mimeType = rs.getString("syllabusAttachType");
                        String contentId = rs.getString("attachmentId");

                        SecurityService.pushAdvisor(new SecurityAdvisor() {
                            public SecurityAdvice isAllowed(String userId, String function, String reference) {
                                if (("/content" + contentId).equals(reference)) {
                                    return SecurityAdvice.ALLOWED;
                                } else {
                                    return SecurityAdvice.PASS;
                                }
                            }
                        });

                        try {
                            ContentResource resource = ContentHostingService.getResource(contentId);
                            InputStream stream = resource.streamContent();

                            response.setContentType(mimeType);
                            response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));

                            OutputStream out = response.getOutputStream();
                            byte[] buf = new byte[4096];

                            int len;
                            while ((len = stream.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        } catch (PermissionException | IdUnusedException | ServerOverloadException | TypeException e) {
                            String msg = String.format("Error fetching resource for stem_name=%s: ",
                                                       stemName);
                            LOG.error(msg, e);
                            throw new ServletException(msg, e);
                        } finally {
                            SecurityService.popAdvisor();
                        }
                    } else {
                        response.setStatus(404);
                        response.setContentType("text/plain");
                        response.getWriter().write("Document not found");
                    }
                }
            }
        } catch (SQLException e) {
            String msg = String.format("Error processing request for stem_name=%s: ",
                                       stemName);
            LOG.error(msg, e);
            throw new ServletException(msg, e);
        } finally {
            if (conn != null) {
                SqlService.returnConnection(conn);
            }
        }
    }

    private void handleListing(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String location = request.getParameter("location");
        String strm = request.getParameter("strm");

        List<String> errors = new ArrayList<>();

        if (location == null) {
            errors.add("Missing parameter 'location': expected a comma-separated list of location codes");
        }

        if (strm == null) {
            errors.add("Missing parameter 'strm': expected a single strm STRM number (e.g. 1186)");
        } else {
            try {
                Long.valueOf(strm);
            } catch (NumberFormatException e) {
                errors.add("Invalid parameter 'strm': expected a single strm STRM number (e.g. 1186)");
            }
        }

        if (reportErrors(errors, response)) {
            return;
        }

        handleQuery(strm, Arrays.asList(location.split(" *, *")), response);
    }


    private boolean reportErrors(List<String> errors, HttpServletResponse response)
        throws IOException {
        if (errors.isEmpty()) {
            return false;
        }

        response.setContentType("text/plain");
        response.setStatus(400);
        Writer out = response.getWriter();
        out.write("Request contained the following errors:\n");
        for (String error: errors) {
            out.write("  * " + error);
            out.write("\n");
        }

        return true;
    }

    private void handleQuery(String strm,
                             List<String> locationCodes,
                             HttpServletResponse response)
        throws ServletException, IOException {
        Connection conn = null;

        response.setContentType("text/json; charset=utf-8");

        try {
            conn = SqlService.borrowConnection();

            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator json = jsonFactory.createGenerator(response.getOutputStream(), JsonEncoding.UTF8);

            String locationPlaceholders = locationCodes.stream().map(l -> "?").collect(Collectors.joining(", "));

            // FIXME: limit to published/selected attachments
            // TODO: touch the attachment mtime on publish too
            try (PreparedStatement ps = conn.prepareStatement("select cc.stem_name," +
                                                              "  cc.crse_id," +
                                                              "  cc.class_section," +
                                                              "  cc.catalog_nbr," +
                                                              "  cc.strm," +
                                                              "  cc.location," +
                                                              "  ssa.lastModifiedTime syllabus_last_modified," +
                                                              "  sr.modifiedon realm_modifiedon" +
                                                              BASE_JOINS +
                                                              " where cc.location in (" + locationPlaceholders + ") AND cc.strm = ?"
                                                              )) {
                int i = 0;
                for (; i < locationCodes.size(); i++) {
                    ps.setString(i + 1, locationCodes.get(i));
                }
                ps.setString(i + 1, strm);

                try (ResultSet rs = ps.executeQuery()) {
                    json.writeStartArray();
                    while (rs.next()) {
                        json.writeStartObject();
                        json.writeStringField("stem_name", rs.getString("stem_name"));
                        json.writeStringField("crse_id", rs.getString("crse_id"));
                        json.writeStringField("class_section", rs.getString("class_section"));
                        json.writeStringField("catalog_nbr", rs.getString("catalog_nbr"));
                        json.writeStringField("strm", rs.getString("strm"));
                        json.writeStringField("location", rs.getString("location"));

                        // URL, last_modified
                        long syllabusLastModified = rs.getLong("syllabus_last_modified");
                        long realmLastModified = rs.getDate("realm_modifiedon") == null ? 0 : rs.getDate("realm_modifiedon").getTime();

                        json.writeNumberField("last_modified", Math.max(syllabusLastModified, realmLastModified));
                        json.writeStringField("url",
                                              String.format("%s/syllabus-feed?get=%s",
                                                            ServerConfigurationService.getServerUrl(),
                                                            URLEncoder.encode(rs.getString("stem_name"), "UTF-8")));


                        json.writeEndObject();
                    }
                    json.writeEndArray();
                    json.close();
                }
            }
        } catch (SQLException e) {
            String msg = String.format("Error processing request for strm=%s; locations=%s: ",
                                       strm, locationCodes);
            LOG.error(msg, e);
            throw new ServletException(msg, e);
        } finally {
            if (conn != null) {
                SqlService.returnConnection(conn);
            }
        }
    }
}
