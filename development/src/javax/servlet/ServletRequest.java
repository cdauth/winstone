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
import java.util.Locale;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Base request object interface definition.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletRequest
{
  public Object getAttribute(String name);
  public Enumeration getAttributeNames();
  public String getCharacterEncoding();
  public int getContentLength();
  public String getContentType();
  public ServletInputStream getInputStream() throws IOException;
  public Locale getLocale();
  public Enumeration getLocales();
  public String getParameter(String name);
  public Map getParameterMap();
  public Enumeration getParameterNames();
  public String[] getParameterValues(String name);
  public String getProtocol();
  public BufferedReader getReader() throws IOException;
  public String getRemoteAddr();
  public String getRemoteHost();
  public RequestDispatcher getRequestDispatcher(String path);
  public String getScheme();
  public String getServerName();
  public int getServerPort();
  public boolean isSecure();
  public void removeAttribute(String name);
  public void setAttribute(String name, Object o);
  public void setCharacterEncoding(String env);

  /**
   * @deprecated As of Version 2.1 of the Java Servlet API, use
   *               ServletContext.getRealPath(String) instead.
   */
  public String getRealPath(String path);
}

