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
import java.security.Principal;

/**
 * Interface definition for http requests.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpServletRequest extends javax.servlet.ServletRequest
{
  public static final String BASIC_AUTH        = "BASIC";
  public static final String CLIENT_CERT_AUTH  = "CLIENT_CERT";
  public static final String DIGEST_AUTH       = "DIGEST";
  public static final String FORM_AUTH         = "FORM";

  public String getAuthType();
  public String getContextPath();
  public Cookie[] getCookies();
  public long getDateHeader(String name);
  public String getHeader(String name);
  public Enumeration getHeaderNames();
  public Enumeration getHeaders(String name);
  public int getIntHeader(String name);
  public String getMethod();
  public String getPathInfo();
  public String getPathTranslated();
  public String getQueryString();
  public String getRemoteUser();
  public String getRequestedSessionId();
  public String getRequestURI();
  public StringBuffer getRequestURL();
  public String getServletPath();
  public HttpSession getSession();
  public HttpSession getSession(boolean create);
  public Principal getUserPrincipal();
  public boolean isRequestedSessionIdFromCookie();
  public boolean isRequestedSessionIdFromURL();
  public boolean isRequestedSessionIdValid();
  public boolean isUserInRole(String role);

  /**
   * @deprecated As of Version 2.1 of the Java Servlet API, use
   *        isRequestedSessionIdFromURL() instead.
   */
  public boolean isRequestedSessionIdFromUrl();

}

