/*
 * Winstone Servlet Container
 * Copyright (C) 2003 Rick Knowles
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
