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
package com.rickknowles.winstone;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import javax.servlet.http.Cookie;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpUtils;

/**
 * Implements the request interface required by the servlet spec.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneRequest implements HttpServletRequest
{
  protected static DateFormat headerDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  static  {headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));}

  private Map attributes;
  private Map parameters;
  private String headers[];
  private Cookie cookies[];
  private String method;
  private String requestURI;
  private String servletPath;
  private String queryString;
  private String protocol;
  private int contentLength;
  private String contentType;
  private String encoding;
  private WinstoneInputStream inputData;
  private ServletConfiguration servletConfig;
  private WebAppConfiguration webappConfig;
  private HttpConnector connector;
  private String serverName;
  private int serverPort;
  private String remoteIP;
  private String remoteName;
  private Boolean parsedParameters;
  private String sessionCookie;
  private List locales;
  private String authorization;

  /**
   * InputStream factory method.
   */
  public WinstoneRequest(WinstoneInputStream inData,
                         HttpConnector connector,
                         WebAppConfiguration webappConfig)
    throws IOException
  {
    this.webappConfig = webappConfig;
    this.inputData = inData;
    this.connector = connector;
    this.attributes = new Hashtable();
    this.parameters = new Hashtable();
    this.locales = new ArrayList();
    this.contentLength = -1;
  }

  /**
   * Steps through the header array, searching for the first header matching
   */
  private String extractFirstHeader(String name)
  {
    for (int n = 0; n < this.headers.length; n++)
      if (this.headers[n].startsWith(name))
        return this.headers[n].substring(name.length() + 1).trim(); // 1 for colon
    return null;
  }

  private Collection extractHeaderNameList()
  {
    Collection headerNames = new HashSet();
    for (int n = 0; n < this.headers.length; n++)
    {
      String name = (String) this.headers[n];
      int colonPos = name.indexOf(':');
      headerNames.add(name.substring(0, colonPos));
    }
    return headerNames;
  }

  public Map getParameters() {return this.parameters;}
  public String getSessionCookie() {return this.sessionCookie;}
  public WebAppConfiguration getWebAppConfig() {return this.webappConfig;}

  public void setServerPort(int port) {this.serverPort = port;}
  public void setRemoteIP(String remoteIP)  {this.remoteIP = remoteIP;}
  public void setRemoteName(String remoteName) {this.remoteName = remoteName;}

  public void setMethod(String method) {this.method = method;}
  public void setQueryString(String queryString) {this.queryString = queryString;}
  public void setRequestURI(String requestURI) {this.requestURI = requestURI;}
  public void setServletPath(String servletPath) {this.servletPath = servletPath;}
  public void setProtocol(String protocol) {this.protocol = protocol;}

  public void setHeaders(String headers[]) {this.headers = headers;}
  public void setCookies(Cookie cookies[]) {this.cookies = cookies;}
  public void setContentLength(int len) {this.contentLength = len;}
  public void setContentType(String type) {this.contentType = type;}
  public void setAuthorization(String auth) {this.authorization = authorization;}
  public void setLocales(List locales) {this.locales = locales;}
  public void setSessionCookie(String sessionCookie) {this.sessionCookie = sessionCookie;}

  // Implementation methods for the servlet request stuff
  public Object getAttribute(String name)         {return this.attributes.get(name);}
  public Enumeration getAttributeNames()          {return Collections.enumeration(this.attributes.keySet());}
  public void removeAttribute(String name)        {this.attributes.remove(name);}
  public void setAttribute(String name, Object o) {this.attributes.put(name, o);}

  public String getCharacterEncoding()  {return this.encoding;}
  public void setCharacterEncoding(String encoding) {this.encoding = encoding;}

  public int getContentLength()  {return this.contentLength;}
  public String getContentType() {return this.contentType;}

  public Locale getLocale()
    {return this.locales.isEmpty() ? null : (Locale) this.locales.get(0);}

  public Enumeration getLocales() {return Collections.enumeration(this.locales);}

  public String getProtocol()             {return this.protocol;}
  public String getScheme()               {return this.connector.getScheme();}
  public boolean isSecure()               {return false;}

  public BufferedReader getReader() throws IOException
  {
    if (this.encoding != null)
      return new BufferedReader(new InputStreamReader(getInputStream(), this.encoding));
    else
      return new BufferedReader(new InputStreamReader(getInputStream()));
  }

  public ServletInputStream getInputStream() throws IOException
  {
    if ((this.parsedParameters != null) && this.parsedParameters.booleanValue())
      Logger.log(Logger.ERROR, "Called getInputStream after getParameter ... error");
    else
      this.parsedParameters = new Boolean(false);
    return this.inputData;
  }

  public String getParameter(String name)
  {
    Boolean parsed = this.connector.parseRequestParameters(this.inputData,
                                                           this.parsedParameters,
                                                           this.method,
                                                           this.contentType,
                                                           this.contentLength,
                                                           this.parameters);
    this.parsedParameters = parsed;
    Object param = this.parameters.get(name);
    if (param == null)
      return null;
    else if (param instanceof String)
      return (String) param;
    else if (param instanceof String[])
      return ((String []) param)[0];
    else
      return param.toString();
  }

  public Enumeration getParameterNames()
  {
    Boolean parsed = this.connector.parseRequestParameters(this.inputData,
                                                           this.parsedParameters,
                                                           this.method,
                                                           this.contentType,
                                                           this.contentLength,
                                                           this.parameters);
    this.parsedParameters = parsed;
    return Collections.enumeration(this.parameters.keySet());
  }

  public String[] getParameterValues(String name)
  {
    Boolean parsed = this.connector.parseRequestParameters(this.inputData,
                                                           this.parsedParameters,
                                                           this.method,
                                                           this.contentType,
                                                           this.contentLength,
                                                           this.parameters);
    this.parsedParameters = parsed;
    Object param = this.parameters.get(name);
    if (param == null)
      return null;
    else if (param instanceof String)
    {
      String arr[] = new String[1];
      arr[0] = (String) param;
      return arr;
    }
    else if (param instanceof String[])
      return (String []) param;
    else
      throw new WinstoneException("Unknown parameter type: " + name);
  }

  public Map getParameterMap()
  {
    Boolean parsed = this.connector.parseRequestParameters(this.inputData,
                                                           this.parsedParameters,
                                                           this.method,
                                                           this.contentType,
                                                           this.contentLength,
                                                           this.parameters);
    this.parsedParameters = parsed;
    return new Hashtable(this.parameters);
  }

  public String getServerName() {return this.serverName;}
  public int getServerPort()    {return this.serverPort;}
  public String getRemoteAddr() {return this.remoteIP;}
  public String getRemoteHost() {return this.remoteName;}

  public javax.servlet.RequestDispatcher getRequestDispatcher(String path)
    {return this.webappConfig.getRequestDispatcher(path);}
  public String getRealPath(String path)
    {return this.webappConfig.getRealPath(path);}

  // Now the stuff for HttpServletRequest
  public String getAuthType() {return null;}
  public String getContextPath()  {return this.webappConfig.getPrefix();}
  public Cookie[] getCookies() {return this.cookies;}
  public long getDateHeader(String name)
  {
    String dateHeader = getHeader(name);
    if (dateHeader == null)
      return -1;
    else try
      {return headerDF.parse(dateHeader).getTime();}
    catch (java.text.ParseException err)
      {throw new IllegalArgumentException("Can't convert to date - " + dateHeader);}
  }
  public String getHeader(String name)          {return extractFirstHeader(name);}
  public Enumeration getHeaderNames()           {return Collections.enumeration(extractHeaderNameList());}
  public Enumeration getHeaders(String name)    {return null;}
  public int getIntHeader(String name)          {return Integer.parseInt(getHeader(name));}
  public String getMethod()                     {return this.method;}
  public String getPathInfo()                   {return null;}
  public String getPathTranslated()             {return null;}
  public String getQueryString()                {return this.queryString;}
  public String getRemoteUser()                 {return null;}
  public String getRequestedSessionId()         {return null;}
  public String getRequestURI()                 {return this.requestURI;}
  public String getServletPath()                {return this.servletPath;}
  public StringBuffer getRequestURL()
  {
    StringBuffer url = new StringBuffer();
    url.append(getScheme()).append("://");
    url.append(getServerName());
    if (!((getServerPort() == 80) && getScheme().equals("http")) &&
        !((getServerPort() == 443) && getScheme().equals("https")))
      url.append(':').append(getServerPort());
    url.append(getRequestURI());
    return url;
  }

  public java.security.Principal getUserPrincipal() {return null;}
  public boolean isUserInRole(String role)          {return true;}

  public boolean isRequestedSessionIdFromCookie()   {return true;}
  public boolean isRequestedSessionIdFromUrl()      {return isRequestedSessionIdFromURL();}
  public boolean isRequestedSessionIdFromURL()      {return false;}
  public boolean isRequestedSessionIdValid()
    {return this.webappConfig.getSessions().containsKey(getSessionId());}

  public HttpSession getSession()               {return getSession(true);}
  public HttpSession getSession(boolean create)
  {
    String cookieValue = getSessionId();

    // Handle the null case
    if (cookieValue == null)
    {
      if (this.sessionCookie != null)
        cookieValue = this.sessionCookie;
      else if (!create)
        return null;
      else
        cookieValue = makeNewSession();
    }

    // Now get the session object
    long nowDate = System.currentTimeMillis();
    WinstoneSession session = (WinstoneSession) this.webappConfig.getSessions().get(cookieValue);
    if (session != null)
    {
      // check if it's expired yet
      if ((session.getMaxInactiveInterval() > 0) &&
          (session.getMaxInactiveInterval() + (session.getLastAccessedTime() * 1000) > nowDate))
      {
        session.invalidate();
        Logger.log(Logger.DEBUG, "Invalidating session: " + session.getId());
        if (create)
          session = (WinstoneSession) this.webappConfig.getSessions().get(makeNewSession());
        else
          session = null;
      }
    }
    else if (create)
      session = (WinstoneSession) this.webappConfig.getSessions().get(makeNewSession());

    // Set last accessed time
    if (session != null)
      session.setLastAccessedDate(nowDate);
    return session;

  }

  private String getSessionId() {return this.sessionCookie;}

  /**
   * Make a new session, and return the id
   */
  private String makeNewSession()
  {
    String cookieValue = "Winstone_" + this.remoteIP + "_" + this.serverPort + "_" +
                    System.currentTimeMillis();
    this.sessionCookie = cookieValue;
    this.webappConfig.makeNewSession(cookieValue);
    return cookieValue;
  }
}

