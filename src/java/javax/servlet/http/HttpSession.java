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
package javax.servlet.http;

import java.util.Enumeration;

import javax.servlet.ServletContext;

/**
 * Interface for http sessions on the server.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpSession {
    public Object getAttribute(String name);

    public Enumeration getAttributeNames();

    public long getCreationTime();

    public String getId();

    public long getLastAccessedTime();

    public int getMaxInactiveInterval();

    public ServletContext getServletContext();

    public void invalidate();

    public boolean isNew();

    public void removeAttribute(String name);

    public void setAttribute(String name, Object value);

    public void setMaxInactiveInterval(int interval);

    /**
     * @deprecated As of Version 2.1, this method is deprecated and has no
     *             replacement. It will be removed in a future version of the
     *             Java Servlet API.
     */
    public HttpSessionContext getSessionContext();

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             getAttribute(java.lang.String).
     */
    public Object getValue(String name);

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             getAttributeNames()
     */
    public String[] getValueNames();

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             setAttribute(java.lang.String, java.lang.Object)
     */
    public void putValue(String name, Object value);

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             removeAttribute(java.lang.String)
     */
    public void removeValue(String name);
}
