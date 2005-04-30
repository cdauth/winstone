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
