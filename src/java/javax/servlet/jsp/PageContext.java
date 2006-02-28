/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.tagext.BodyContent;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class PageContext extends JspContext {
    public static final int PAGE_SCOPE = 1;
    public static final int REQUEST_SCOPE = 2;
    public static final int SESSION_SCOPE = 3;
    public static final int APPLICATION_SCOPE = 4;

    public static final String APPLICATION = "javax.servlet.jsp.jspApplication";
    public static final String CONFIG = "javax.servlet.jsp.jspConfig";
    public static final String EXCEPTION = "javax.servlet.jsp.jspException";
    public static final String OUT = "javax.servlet.jsp.jspOut";
    public static final String PAGE = "javax.servlet.jsp.jspPage";
    public static final String PAGECONTEXT = "javax.servlet.jsp.jspPageContext";
    public static final String REQUEST = "javax.servlet.jsp.jspRequest";
    public static final String RESPONSE = "javax.servlet.jsp.jspResponse";
    public static final String SESSION = "javax.servlet.jsp.jspSession";

    private static final String SERVLET_ERROR = "javax.servlet.error.exception";
    private static final String SERVLET_CODE = "javax.servlet.error.status_code";
    private static final String SERVLET_URI = "javax.servlet.error.request_uri";
    private static final String SERVLET_NAME = "javax.servlet.error.servlet_name";

    public ErrorData getErrorData() {
        ServletRequest request = getRequest();
        Throwable error = (Throwable) request.getAttribute(SERVLET_ERROR);
        Integer code = (Integer) request.getAttribute(SERVLET_CODE);
        String uri = (String) request.getAttribute(SERVLET_URI);
        String name = (String) request.getAttribute(SERVLET_NAME);
        return new ErrorData(error, code.intValue(), uri, name);
    }

    public abstract void forward(String relativeUrlPath) throws ServletException, IOException;

    public abstract void include(String relativeUrlPath) throws ServletException, IOException;

    public abstract void include(String relativeUrlPath, boolean flush)
            throws ServletException, IOException;

    public abstract Exception getException();

    public abstract Object getPage();

    public abstract ServletRequest getRequest();

    public abstract ServletResponse getResponse();

    public abstract ServletConfig getServletConfig();

    public abstract ServletContext getServletContext();

    public abstract HttpSession getSession();

    public abstract void handlePageException(Exception e)
            throws ServletException, IOException;

    public abstract void handlePageException(Throwable t)
            throws ServletException, IOException;

    public abstract void initialize(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException;

    public BodyContent pushBody() {
        return null;
    }

    public abstract void release();

}
