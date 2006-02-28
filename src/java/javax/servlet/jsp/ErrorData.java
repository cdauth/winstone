/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public final class ErrorData {
    private String requestURI;
    private String servletName;
    private int statusCode;
    private Throwable throwable;

    public ErrorData(Throwable throwable, int statusCode, String uri,
            String servletName) {
        this.throwable = throwable;
        this.statusCode = statusCode;
        this.requestURI = uri;
        this.servletName = servletName;
    }

    public String getRequestURI() {
        return this.requestURI;
    }

    public String getServletName() {
        return this.servletName;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }
}
