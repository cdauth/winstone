/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class JspFactory {
    private static JspFactory defaultFactory = null;

    public static JspFactory getDefaultFactory() {
        return defaultFactory;
    }

    public static void setDefaultFactory(JspFactory deflt) {
        defaultFactory = deflt;
    }

    public abstract JspEngineInfo getEngineInfo();

    public abstract PageContext getPageContext(Servlet servlet,
            ServletRequest request, ServletResponse response,
            String errorPageURL, boolean needsSession, int buffer,
            boolean autoflush);

    public abstract void releasePageContext(PageContext pc);
}
