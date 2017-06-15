// This was originally written as an Axis web service, but Axis 1.4 is fairly
// long in the tooth these days and doesn't work (reliably) under Java 1.8.
//
// The original plan was to migrate to CXF, but the systems that consume this
// service are somewhat particular about how the XML looks--wanting different
// SoapAction and URI prefix in different environments and that sort of thing.
//
// Rather than risk regressions stemming from incompatibilities between SOAP
// libraries I've just taken the small number of requests and responses we care
// about and produced templates based on the responses from the original
// implementation.

package edu.nyu.classes.nyugrades.ws;

import edu.nyu.classes.nyugrades.api.AuditLogException;
import edu.nyu.classes.nyugrades.api.Grade;
import edu.nyu.classes.nyugrades.api.GradePullDisabledException;
import edu.nyu.classes.nyugrades.api.GradeSet;
import edu.nyu.classes.nyugrades.api.MultipleSectionsMatchedException;
import edu.nyu.classes.nyugrades.api.MultipleSitesFoundForSectionException;
import edu.nyu.classes.nyugrades.api.NYUGradesService;
import edu.nyu.classes.nyugrades.api.NYUGradesSessionService;
import edu.nyu.classes.nyugrades.api.SectionNotFoundException;
import edu.nyu.classes.nyugrades.api.SiteNotFoundForSectionException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class NYUGradesWS extends HttpServlet
{
    String GRADES_ADMIN_USER = "admin";

    private static final Log LOG = LogFactory.getLog(NYUGradesWS.class);

    private UserDirectoryService userDirectoryService;
    private ServerConfigurationService serverConfigurationService;
    private SessionManager sakaiSessionManager;
    private NYUGradesSessionService nyuGradesSessions;
    private NYUGradesService nyuGrades;


    private String[] permittedUsernames;


    public NYUGradesWS()
    {
        serverConfigurationService = (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class.getName());
        userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class.getName());
        sakaiSessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());

        nyuGradesSessions = (NYUGradesSessionService) ComponentManager.get("edu.nyu.classes.nyugrades.api.NYUGradesSessionService");
        nyuGrades = (NYUGradesService) ComponentManager.get("edu.nyu.classes.nyugrades.api.NYUGradesService");

        permittedUsernames = serverConfigurationService.getString("nyu.grades-service.allowed_users", "admin").split(", *");
    }


    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        new RequestDispatcher(request, response).dispatch();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        new RequestDispatcher(request, response).dispatch();
    }

    private class RequestDispatcher {
        private HttpServletRequest request;
        private HttpServletResponse response;

        public RequestDispatcher(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        private void dispatch() throws ServletException, IOException {
            // Served the WSDL if requested
            if (request.getQueryString() != null && request.getQueryString().toLowerCase().indexOf("wsdl") >= 0) {
                try {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setHeader("Content-Type", "text/xml");
                    respondWithTemplate("wsdl", new String[] { "REQUEST_SUFFIX", request.getRequestURI().endsWith("Production.jws") ? "Production" : "" });
                } catch (Exception e) {
                    LOG.error("Error while serving WSDL: " + e);
                    e.printStackTrace();
                }

                return;
            }

            String action = getSoapAction();

            if (action == null) {
                throw new ServletException("You must provide a SoapAction header");
            }

            try {
                SOAPRequest soapRequest = new SOAPRequest(request);

                // We'll use startsWith here because SIS requests "loginProduction" in
                // production environment, but just "login" in others.
                if (action.startsWith("login")) {
                    String sessionId = login(soapRequest.get("username"), soapRequest.get("password"));

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setHeader("Content-Type", "text/xml");
                    respondWithTemplate("login_response", new String[] { "SESSION", sessionId });
                } else if (action.startsWith("logout")) {
                    String status = logout(soapRequest.get("sessionId"));

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setHeader("Content-Type", "text/xml");
                    respondWithTemplate("logout_response", new String[] { "STATUS", status });
                } else if (action.startsWith("getGradesForSite")) {
                    GradeSet grades = getGradesForSite(soapRequest.get("sessionId"),
                            soapRequest.get("courseId"),
                            soapRequest.get("strm"),
                            soapRequest.get("sessionCode"),
                            soapRequest.get("classSection"));

                    respondWithGrades(grades);
                } else {
                    throw new ServletException("Unrecognized SoapAction header: " + action);
                }
            } catch (Exception e) {
                LOG.error(e);
                e.printStackTrace();

                try {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    respondWithTemplate("error_response", new String[] { "ERROR_MESSAGE", e.getMessage() });
                } catch (Exception e2) {
                    LOG.error("Additionally, failed to write an error response with exception: " + e2);
                    e2.printStackTrace();
                }
            }
        }

        private String getSoapAction() {
            String action = request.getHeader("SoapAction");

            if (action == null) {
                return null;
            } else {
                return action.replace("\"", "");
            }
        }

        private void respondWithGrades(GradeSet grades)
            throws Exception {
            StringBuilder gradeString = new StringBuilder();

            for (Grade g : grades) {
                gradeString.append(fillTemplate("single_grade", new String[] {
                            "NETID", g.netId,
                            "EMPLID", g.emplId,
                            "GRADELETTER", g.gradeletter
                        }));
            }

            respondWithTemplate("grades_response", new String[] {
                        "GRADES", gradeString.toString()
                    });
        }

        private String fillTemplate(String templateName, String[] keysAndValues) throws Exception {
            URL templateResource = this.getClass().getResource("/edu/nyu/classes/nyugrades/ws/response_templates/" + templateName + ".xml");

            if (templateResource == null) {
                throw new ServletException("Internal error: failed to load template for: " + templateName);
            }

            String templateContent = new String(Files.readAllBytes(Paths.get(templateResource.toURI())),
                    "UTF-8");

            if ((keysAndValues.length % 2) != 0) {
                throw new ServletException("Internal error: keysAndValues should have an even number of elements.");
            }

            for (int i = 0; i < keysAndValues.length; i += 2) {
                templateContent = templateContent.replace("{{" + keysAndValues[i] + "}}",
                        StringEscapeUtils.escapeXml(keysAndValues[i + 1]));
            }

            return templateContent;
        }

        private void respondWithTemplate(String template, String[] keysAndValues) throws Exception {
            String templateContent = fillTemplate(template, keysAndValues);

            // Some specials
            templateContent = templateContent.replace("{{BASE_URL}}", serverConfigurationService.getServerUrl() + request.getRequestURI());
            templateContent = templateContent.replace("{{SERVER_HOST}}", serverConfigurationService.getServerName());
            if (getSoapAction() != null) {
                templateContent = templateContent.replace("{{SOAP_ACTION}}", getSoapAction());
            }

            response.getWriter().write(templateContent);
        }

        private boolean passwordValid(String username, String password)
        {
            return (userDirectoryService.authenticate(username, password) != null);
        }


        private boolean usernameIsPermitted(String username)
        {
            for (String permittedUsername : permittedUsernames) {
                if (permittedUsername.equalsIgnoreCase(username)) {
                    return true;
                }
            }

            return false;
        }


        private String login(String username, String password) throws RequestFailedException
        {
            if (!passwordValid(username, password) || !usernameIsPermitted(username)) {
                LOG.warn("Rejected request from " + username);
                throw new RequestFailedException("Permission denied");
            }

            nyuGradesSessions.expireSessions();
            return nyuGradesSessions.createSession(username);
        }


        private String logout(String sessionId) throws RequestFailedException
        {
            nyuGradesSessions.deleteSession(sessionId);

            return "OK";
        }


        private GradeSet getGradesForSite(String sessionId,
                                          String courseId,
                                          String term,
                                          String sessionCode,
                                          String classSection)
            throws RequestFailedException, AuditLogException
        {
            if (!nyuGradesSessions.checkSession(sessionId)) {
                LOG.warn("Rejected invalid sessionId");
                throw new RequestFailedException("Permission denied");
            }

            Session sakaiSession = sakaiSessionManager.startSession();
            try {
                sakaiSessionManager.setCurrentSession(sakaiSession);

                sakaiSession.setUserId(GRADES_ADMIN_USER);
                sakaiSession.setUserEid(GRADES_ADMIN_USER);

                String sectionEid = null;
                try {
                    sectionEid = nyuGrades.findSingleSection(courseId, term, sessionCode, classSection);
                    return nyuGrades.getGradesForSection(sectionEid);
                } catch (SectionNotFoundException e) {
                    throw new RequestFailedException(String.format("Failed to find a section for CRSE_ID; STRM; SESSION_CODE; CLASS_SECTION = %s; %s; %s; %s",
                            courseId, term, sessionCode, classSection));
                } catch (SiteNotFoundForSectionException e) {
                    throw new RequestFailedException(String.format("Failed to find site for section: %s",
                            sectionEid));
                } catch (MultipleSectionsMatchedException e) {
                    throw new RequestFailedException(String.format("Multiple sections matched for CRSE_ID; STRM; SESSION_CODE; CLASS_SECTION = %s; %s; %s; %s",
                            courseId, term, sessionCode, classSection));
                } catch (MultipleSitesFoundForSectionException e) {
                    throw new RequestFailedException(String.format("Multiple sites found for section: %s",
                            sectionEid));
                } catch (GradePullDisabledException e) {
                    throw new RequestFailedException(String.format("Grade pull is currently disabled for section: %s",
                            sectionEid));
                }
            } finally {
                sakaiSession.invalidate();
            }
        }

    }

    private class RequestFailedException extends Exception {
        public RequestFailedException(String message) {
            super(message);
        }
    }

    private class SOAPRequest {
        private Document doc;
        private XPath xpath;

        public SOAPRequest(HttpServletRequest request) throws Exception {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            doc = builder.parse(request.getInputStream());
            xpath = XPathFactory.newInstance().newXPath();
        }

        public String get(String parameter) throws Exception {
            XPathExpression expr = xpath.compile(String.format("//*[local-name()='Body']/*/*[local-name()='%s']",
                    parameter));
            NodeList matches = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);

            if (matches.getLength() == 1) {
                return matches.item(0).getTextContent();
            } else {
                throw new RequestFailedException("Missing value for required parameter: " + parameter);
            }
        }
    }
}
