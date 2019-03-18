package org.sakaiproject.content.googledrive;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class GoogleDriveFilter implements Filter {
    protected String GOOGLE_DRIVE_PATH = "/google-drive";

    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (((HttpServletRequest) servletRequest).getPathInfo().startsWith(GOOGLE_DRIVE_PATH)) {
            new GoogleDriveServlet().service(servletRequest, servletResponse);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
