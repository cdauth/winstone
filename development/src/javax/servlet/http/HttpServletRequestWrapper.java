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
 * Wraps HttpServletRequest objects in a decorator pattern
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class HttpServletRequestWrapper extends javax.servlet.ServletRequestWrapper
  implements HttpServletRequest
{
  private HttpServletRequest httpRequest;
  public HttpServletRequestWrapper(HttpServletRequest request)
  {
    super(request);
    this.httpRequest = request;
  }
  public void setRequest(javax.servlet.ServletRequest request)
  {
    if (request instanceof HttpServletRequest)
    {
      super.setRequest(request);
      this.httpRequest = (HttpServletRequest) request;
    }
    else
      throw new IllegalArgumentException("Not an HttpServletRequest");
  }

  public String getAuthType()             {return this.httpRequest.getAuthType();}
  public String getContextPath()          {return this.httpRequest.getContextPath();}
  public Cookie[] getCookies()            {return this.httpRequest.getCookies();}

  public long getDateHeader(String name)     {return this.httpRequest.getDateHeader(name);}
  public String getHeader(String name)       {return this.httpRequest.getHeader(name);}
  public Enumeration getHeaderNames()        {return this.httpRequest.getHeaderNames();}
  public Enumeration getHeaders(String name) {return this.httpRequest.getHeaders(name);}
  public int getIntHeader(String name)       {return this.httpRequest.getIntHeader(name);}

  public String getMethod()               {return this.httpRequest.getMethod();}
  public String getPathInfo()             {return this.httpRequest.getPathInfo();}
  public String getPathTranslated()       {return this.httpRequest.getPathTranslated();}
  public String getQueryString()          {return this.httpRequest.getQueryString();}
  public String getRemoteUser()           {return this.httpRequest.getRemoteUser();}
  public String getRequestedSessionId()   {return this.httpRequest.getRequestedSessionId();}
  public String getRequestURI()           {return this.httpRequest.getRequestURI();}
  public String getServletPath()          {return this.httpRequest.getServletPath();}
  public StringBuffer getRequestURL()     {return this.httpRequest.getRequestURL();}

  public HttpSession getSession()               {return this.httpRequest.getSession();}
  public HttpSession getSession(boolean create) {return this.httpRequest.getSession(create);}

  public Principal getUserPrincipal()               {return this.httpRequest.getUserPrincipal();}

  public boolean isRequestedSessionIdFromCookie()   {return this.httpRequest.isRequestedSessionIdFromCookie();}
  public boolean isRequestedSessionIdFromUrl()      {return this.httpRequest.isRequestedSessionIdFromUrl();}
  public boolean isRequestedSessionIdFromURL()      {return this.httpRequest.isRequestedSessionIdFromURL();}
  public boolean isRequestedSessionIdValid()        {return this.httpRequest.isRequestedSessionIdValid();}
  public boolean isUserInRole(String role)          {return this.httpRequest.isUserInRole(role);}
}

