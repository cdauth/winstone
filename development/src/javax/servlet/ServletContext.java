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
package javax.servlet;

import java.util.Enumeration;
import java.net.URL;
import java.io.InputStream;
import java.util.Set;

/**
 * Models the web application concept as an interface.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletContext
{
  public Object getAttribute(String name);
  public Enumeration getAttributeNames();

  public String getInitParameter(String name);
  public Enumeration getInitParameterNames();

  public String getServletContextName();
  public ServletContext getContext(String uripath);

  public String getServerInfo();
  public String getMimeType(String file);
  public int getMajorVersion();
  public int getMinorVersion();

  public RequestDispatcher getRequestDispatcher(String path);
  public RequestDispatcher getNamedDispatcher(String name);

  public String getRealPath(String path);
  public URL getResource(String path) throws java.net.MalformedURLException;
  public InputStream getResourceAsStream(String path);
  public Set getResourcePaths(String path);

  /**
   * @deprecated As of Java Servlet API 2.1, with no direct replacement.
   */
  public Servlet getServlet(String name);

  /**
   * @deprecated As of Java Servlet API 2.1, with no replacement.
   */
  public Enumeration getServletNames();

  /**
   * @deprecated As of Java Servlet API 2.0, with no replacement.
   */
  public Enumeration getServlets();

  /**
   * @deprecated As of Java Servlet API 2.1, use log(String message,
   * Throwable throwable) instead.
   */
  public void log(Exception exception, String msg);

  public void log(String msg);
  public void log(String message, Throwable throwable);
  public void removeAttribute(String name);
  public void setAttribute(String name, Object object);
}

