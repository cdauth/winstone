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
package winstone;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.security.*;

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

  // Request header constants
  static final String SESSION_COOKIE_NAME    = "WinstoneHttpSessionId";
  static final String CONTENT_LENGTH_HEADER  = "Content-Length";
  static final String CONTENT_TYPE_HEADER    = "Content-Type";
  static final String AUTHORIZATION_HEADER   = "Authorization";
  static final String LOCALE_HEADER          = "Accept-Language";
  static final String HOST_HEADER            = "Host";
  static final String IN_COOKIE_HEADER1      = "Cookie";
  static final String IN_COOKIE_HEADER2      = "Cookie2";
  static final String METHOD_POST            = "POST";
  static final String POST_PARAMETERS        = "application/x-www-form-urlencoded";

  protected Map attributes;
  protected Map parameters;
  protected String headers[];
  protected Cookie cookies[];
  protected String method;
  protected String scheme;
  protected String serverName;
  protected String requestURI;
  protected String servletPath;
  protected String queryString;
  protected String protocol;
  protected int contentLength;
  protected String contentType;
  protected String encoding;
  protected WinstoneInputStream inputData;
  protected ServletConfiguration servletConfig;
  protected WebAppConfiguration webappConfig;
  protected Listener listener;
  protected int serverPort;
  protected String remoteIP;
  protected String remoteName;
  protected Boolean parsedParameters;
  protected String sessionCookie;
  protected List locales;
  protected String authorization;
  protected boolean isSecure;
  protected AuthenticationPrincipal authenticatedUser;

  private WinstoneResourceBundle resources;
  private MessageDigest md5Digester;

  /**
   * InputStream factory method.
   */
  public WinstoneRequest(WinstoneResourceBundle resources) throws IOException
  {
    this.resources = resources;
    this.attributes = new Hashtable();
    this.parameters = new Hashtable();
    this.locales = new ArrayList();
    this.contentLength = -1;
    this.isSecure = false;
    try
      {this.md5Digester = MessageDigest.getInstance("MD5");}
    catch (NoSuchAlgorithmException err)
      {throw new WinstoneException("MD5 digester unavailable - what the ...?");}
  }

  /**
   * Resets the request to be reused
   */
  public void cleanUp()
  {
    this.listener = null;
    this.attributes.clear();
    this.parameters.clear();
    this.headers = null;
    this.cookies = null;
    this.method = null;
    this.scheme = null;
    this.serverName = null;
    this.requestURI = null;
    this.servletPath = null;
    this.queryString = null;
    this.protocol = null;
    this.contentLength = -1;
    this.contentType = null;
    this.encoding = null;
    this.inputData = null;
    this.servletConfig = null;
    this.webappConfig = null;
    this.serverPort = -1;
    this.remoteIP = null;
    this.remoteName = null;
    this.parsedParameters = null;
    this.sessionCookie = null;
    this.locales.clear();
    this.authorization = null;
    this.isSecure = false;
    this.authenticatedUser = null;
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

  public Map getAttributes()                    {return this.attributes;}
  public Map getParameters()                    {return this.parameters;}
  public String getSessionCookie()              {return this.sessionCookie;}
  public WebAppConfiguration getWebAppConfig()  {return this.webappConfig;}
  public String getEncoding()                   {return this.encoding;}
  public Boolean getParsedParameters()          {return this.parsedParameters;}
  public List getListLocales()                  {return this.locales;}

  public void setInputStream(WinstoneInputStream inputData) {this.inputData = inputData;}
  public void setWebAppConfig(WebAppConfiguration webappConfig)  {this.webappConfig = webappConfig;}
  public void setListener(Listener listener) {this.listener = listener;}

  public void setServerPort(int port)       {this.serverPort = port;}
  public void setRemoteIP(String remoteIP)  {this.remoteIP = remoteIP;}
  public void setRemoteName(String name)    {this.remoteName = name;}

  public void setMethod(String method)            {this.method = method;}
  public void setIsSecure(boolean isSecure)       {this.isSecure = isSecure;}
  public void setQueryString(String queryString)  {this.queryString = queryString;}
  public void setServerName(String name)          {this.serverName = name;}
  public void setRequestURI(String requestURI)    {this.requestURI = requestURI;}
  public void setScheme(String scheme)            {this.scheme = scheme;}
  public void setServletPath(String servletPath)  {this.servletPath = servletPath;}
  public void setProtocol(String protocolString)  {this.protocol = protocolString;}
  public void setRemoteUser(AuthenticationPrincipal user) {this.authenticatedUser = user;}

  public void setContentLength(int len)     {this.contentLength = len;}
  public void setContentType(String type)   {this.contentType = type;}
  public void setAuthorization(String auth) {this.authorization = auth;}
  public void setLocales(List locales)      {this.locales = locales;}
  public void setSessionCookie(String sc)   {this.sessionCookie = sc;}
  public void setEncoding(String encoding)  {this.encoding = encoding;}
  public void setParsedParameters(Boolean parsed) {this.parsedParameters = parsed;}

  /**
   * Gets parameters from the url encoded parameter string
   */
  public Map extractParameters(String urlEncodedParams)
  {
    Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneRequest.ParsingParameters") + urlEncodedParams);
    Map params = new Hashtable();
    StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
    while (st.hasMoreTokens())
    {
      String token = st.nextToken();
      int equalPos = token.indexOf('=');
      if (equalPos == -1)
        Logger.log(Logger.WARNING, resources.getString("WinstoneRequest.IgnoringBadParameter") + token);
      else try
      {
        String decodedName = decodeURLToken(token.substring(0, equalPos));
        String decodedValue = decodeURLToken(token.substring(equalPos + 1));
        Object already = params.get(decodedName);
        if (already == null)
          params.put(decodedName, decodedValue);
        else if (already instanceof String)
        {
          String pair[] = new String[2];
          pair[0] = (String) already;
          pair[1] = decodedValue;
          params.put(decodedName, pair);
        }
        else if (already instanceof String[])
        {
          String alreadyArray[] = (String []) already;
          String oneMore[] = new String[alreadyArray.length + 1];
          System.arraycopy(alreadyArray, 0, oneMore, 0, alreadyArray.length);
          oneMore[oneMore.length - 1] = decodedValue;
          params.put(decodedName, oneMore);
        }
        else
          Logger.log(Logger.WARNING, resources.getString("WinstoneRequest.UnknownParameterType",
              "[#name]", decodedName + " = " + decodedValue.getClass()));
      }
      catch (Exception err)
        {Logger.log(Logger.ERROR, resources.getString("WinstoneRequest.ErrorParameters"), err);}
    }
    return params;
  }

  /**
   * For decoding the URL encoding used on query strings
   */
  private static String decodeURLToken(String in)
  {
    StringBuffer workspace = new StringBuffer();
    for (int n = 0; n < in.length(); n++)
    {
      char thisChar = in.charAt(n);
      if (thisChar == '+')
        workspace.append(' ');
      else if (thisChar == '%')
      {
        int decoded = Integer.parseInt(in.substring(n + 1, n + 3), 16);
        workspace.append((char) decoded);
        n += 2;
      }
      else
        workspace.append(thisChar);
    }
    return workspace.toString();
  }

  /**
   * This takes the parameters in the body of the request and puts them into
   * the parameters map.
   */
  public Boolean parseRequestParameters(InputStream inData,
                                        Boolean parsedParameters,
                                        String method,
                                        String contentType,
                                        int contentLength,
                                        Map params)
  {
    if ((parsedParameters != null) && !parsedParameters.booleanValue())
    {
      Logger.log(Logger.WARNING, resources.getString("WinstoneRequest.BothMethodsCalled"));
      return new Boolean(true);
    }
    else if (parsedParameters == null)
    try
    {
      Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneRequest.ParsingBodyParameters"));
      if (method.equals(METHOD_POST) &&
          (contentType != null) && contentType.equals(POST_PARAMETERS))
      {
        // Parse params
        byte paramBuffer[] = new byte[contentLength];
        int readCount = inData.read(paramBuffer);
        if (readCount != contentLength)
          Logger.log(Logger.WARNING, resources.getString("WinstoneRequest.IncorrectContentLength",
            "[#contentLength]", contentLength + "", "[#readCount]", readCount + ""));
        String paramLine = new String(paramBuffer);
        Map postParams = extractParameters(paramLine.trim());
        Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneRequest.ParamLine") + postParams);
        params.putAll(postParams);
      }
      return new Boolean(true);
    }
    catch (Throwable err)
    {
      Logger.log(Logger.ERROR, resources.getString("WinstoneRequest.ErrorBodyParameters"), err);
      return null;
    }
    else
      return parsedParameters;
  }

  /**
   * Go through the list of headers, and build the headers/cookies arrays for
   * the request object.
   */
  public void parseHeaders(List headerList)
  {
    // Iterate through headers
    List outHeaderList = new ArrayList();
    List cookieList = new ArrayList();
    for (Iterator i = headerList.iterator(); i.hasNext(); )
    {
      String header = (String) i.next();
      int colonPos = header.indexOf(':');
      String name = header.substring(0, colonPos);
      String value = header.substring(colonPos + 1).trim();
      int nextColonPos = value.indexOf(':');

      // Add it to out headers if it's not a cookie
      if (!name.equalsIgnoreCase(IN_COOKIE_HEADER1) &&
          !name.equalsIgnoreCase(IN_COOKIE_HEADER2))
        outHeaderList.add(header);

      if (name.equalsIgnoreCase(AUTHORIZATION_HEADER))
        this.authorization = value;
      else if (name.equalsIgnoreCase(LOCALE_HEADER))
        this.locales = parseLocales(value);
      else if (name.equalsIgnoreCase(CONTENT_LENGTH_HEADER))
        this.contentLength = Integer.parseInt(value);
      else if (name.equalsIgnoreCase(HOST_HEADER))
        this.serverName = nextColonPos == -1 ? value : value.substring(0, nextColonPos);
      else if (name.equalsIgnoreCase(CONTENT_TYPE_HEADER))
      {
        this.contentType = value;
        int semicolon = value.lastIndexOf(';');
        if (semicolon != -1)
        {
          String encodingClause = value.substring(semicolon + 1).trim();
          if (encodingClause.startsWith("charset="))
            this.encoding = encodingClause.substring(8);
        }
      }
      else if (name.equalsIgnoreCase(IN_COOKIE_HEADER1) ||
               name.equalsIgnoreCase(IN_COOKIE_HEADER2))
      {
        StringTokenizer st = new StringTokenizer(value, ";", false);
        while (st.hasMoreTokens())
        {
          String cookieLine = st.nextToken().trim();
          int equalPos = cookieLine.indexOf('=');
          if (equalPos != -1)
          {
            Cookie thisCookie = new Cookie(cookieLine.substring(0, equalPos),
                                           cookieLine.substring(equalPos + 1));
            thisCookie.setVersion(name.equals(IN_COOKIE_HEADER2) || isSecure() ? 1 : 0);
            thisCookie.setSecure(isSecure());
            cookieList.add(thisCookie);
            Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneRequest.CookieFound",
                  "[#cookieName]", thisCookie.getName(), "[#cookieValue]", thisCookie.getValue()));
            if (thisCookie.getName().equals(SESSION_COOKIE_NAME))
            {
              this.sessionCookie = thisCookie.getValue();
              Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneRequest.SessionCookieFound", "[#cookieValue]", thisCookie.getValue()));
            }
          }
        }
      }
    }
    this.headers = (String []) outHeaderList.toArray(new String[outHeaderList.size()]);
    this.cookies = (Cookie []) cookieList.toArray(new Cookie[cookieList.size()]);
  }

  private List parseLocales(String header)
  {
    // Strip out the whitespace
    StringBuffer lb = new StringBuffer();
    for (int n = 0; n < header.length(); n++)
    {
      char c = header.charAt(n);
      if (!Character.isWhitespace(c))
        lb.append(c);
    }

    // Tokenize by commas
    Map localeEntries = new HashMap();
    StringTokenizer commaTK = new StringTokenizer(lb.toString(), ",", false);
    for (; commaTK.hasMoreTokens(); )
    {
      String clause = commaTK.nextToken();

      // Tokenize by semicolon
      Float quality = new Float(1);
      if (clause.indexOf(";q=") != -1)
      {
        int pos = clause.indexOf(";q=");
        try
          {quality = new Float(clause.substring(pos + 3));}
        catch (NumberFormatException err) {quality = new Float(0);}
        clause = clause.substring(0, pos);
      }

      // Build the locale
      String language = "";
      String country = "";
      String variant = "";
      int dpos = clause.indexOf('-');
      if (dpos == -1)
        language = clause;
      else
      {
        language = clause.substring(0, dpos);
        String remainder = clause.substring(dpos + 1);
        int d2pos = remainder.indexOf('-');
        if (d2pos == -1)
          country = remainder;
        else
        {
          country = remainder.substring(0, d2pos);
          variant = remainder.substring(d2pos + 1);
        }
      }
      Locale loc = new Locale(language, country, variant);

      // Put into list by quality
      List localeList = (List) localeEntries.get(quality);
      if (localeList == null)
      {
        localeList = new ArrayList();
        localeEntries.put(quality, localeList);
      }
      localeList.add(loc);
    }

    // Extract and build the list
    Float orderKeys[] = (Float []) new ArrayList(localeEntries.keySet())
                                          .toArray(new Float[localeEntries.size()]);
    Arrays.sort(orderKeys);
    List outputLocaleList = new ArrayList();
    for (int n = 0; n < orderKeys.length; n++)
    {
      // Skip backwards through the list of maps and add to the output list
      int reversedIndex = (orderKeys.length - 1) - n;
      if ((orderKeys[reversedIndex].floatValue() <= 0) || (orderKeys[reversedIndex].floatValue() > 1))
        continue;
      List localeList = (List) localeEntries.get(orderKeys[reversedIndex]);
      for (Iterator i = localeList.iterator(); i.hasNext(); )
        outputLocaleList.add(i.next());
    }

    return outputLocaleList;
  }

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
  public String getScheme()               {return this.scheme;}
  public boolean isSecure()               {return this.isSecure;}

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
      Logger.log(Logger.WARNING, resources.getString("WinstoneRequest.BothMethods"));
    else
      this.parsedParameters = new Boolean(false);
    return this.inputData;
  }

  public String getParameter(String name)
  {
    Boolean parsed = parseRequestParameters(this.inputData, this.parsedParameters, this.method,
                                            this.contentType, this.contentLength, this.parameters);
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
    Boolean parsed = parseRequestParameters(this.inputData, this.parsedParameters, this.method,
                                            this.contentType, this.contentLength, this.parameters);
    this.parsedParameters = parsed;
    return Collections.enumeration(this.parameters.keySet());
  }

  public String[] getParameterValues(String name)
  {
    Boolean parsed = parseRequestParameters(this.inputData, this.parsedParameters, this.method,
                                            this.contentType, this.contentLength, this.parameters);
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
      throw new WinstoneException(resources.getString("WinstoneRequest.UnknownParamType")
                                  + name + " - " + param.getClass());
  }

  public Map getParameterMap()
  {
    Boolean parsed = parseRequestParameters(this.inputData, this.parsedParameters, this.method,
                                            this.contentType, this.contentLength, this.parameters);
    this.parsedParameters = parsed;
    return new Hashtable(this.parameters);
  }

  public String getServerName() {return this.serverName;}
  public int getServerPort()    {return this.serverPort;}
  public String getRemoteAddr() {return this.remoteIP;}
  public String getRemoteHost() {return this.remoteName;}

  public javax.servlet.RequestDispatcher getRequestDispatcher(String path)
    {return this.webappConfig.getRequestDispatcher(path);}

  // Now the stuff for HttpServletRequest
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
      {throw new IllegalArgumentException(resources.getString("WinstoneRequest.BadDate") + dateHeader);}
  }
  public String getHeader(String name)          {return extractFirstHeader(name);}
  public Enumeration getHeaderNames()           {return Collections.enumeration(extractHeaderNameList());}
  public Enumeration getHeaders(String name)    {return null;}
  public int getIntHeader(String name)          {return Integer.parseInt(getHeader(name));}
  public String getMethod()                     {return this.method;}
  public String getPathInfo()                   {return null;}
  public String getPathTranslated()             {return null;}
  public String getQueryString()                {return this.queryString;}
  public String getRequestURI()                 {return this.requestURI;}
  public String getServletPath()                {return this.servletPath;}
  public String getRequestedSessionId()
  {
    HttpSession session = getSession(false);
    return (session == null ? null : session.getId());
  }

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

  public Principal getUserPrincipal()       {return this.authenticatedUser;}
  public boolean isUserInRole(String role)
    {return this.authenticatedUser == null ? false : this.authenticatedUser.isUserIsInRole(role);}
  public String getAuthType()
    {return this.authenticatedUser == null ? null : this.authenticatedUser.getAuthType();}
  public String getRemoteUser()
    {return this.authenticatedUser == null ? null : this.authenticatedUser.getName();}

  public boolean isRequestedSessionIdFromCookie()   {return true;}
  public boolean isRequestedSessionIdFromURL()      {return false;}
  public boolean isRequestedSessionIdValid()
  {
    WinstoneSession ws = this.webappConfig.getSessionById(getSessionId(), false);
    if (ws == null)
      return false;
    else
      return (validationCheck(ws, System.currentTimeMillis(), false) != null);
  }

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
    WinstoneSession session = this.webappConfig.getSessionById(cookieValue, false);
    if (session != null)
      session = validationCheck(session, nowDate, create);
    else if (create)
      session = this.webappConfig.getSessionById(makeNewSession(), false);

    // Set last accessed time
    if (session != null)
      session.setLastAccessedDate(nowDate);
    return session;

  }

  private String getSessionId() {return this.sessionCookie;}

  private WinstoneSession validationCheck(WinstoneSession session, long nowDate, boolean create)
  {
    // check if it's expired yet
    if ((session.getMaxInactiveInterval() > 0) &&
        (session.getMaxInactiveInterval() + (session.getLastAccessedTime() * 1000) > nowDate))
    {
      session.invalidate();
      Logger.log(Logger.DEBUG, resources.getString("WinstoneRequest.InvalidateSession") + session.getId());
      if (create)
        session = this.webappConfig.getSessionById(makeNewSession(), false);
      else
        session = null;
    }
    return session;
  }

  /**
   * Make a new session, and return the id
   */
  private String makeNewSession()
  {
    String cookieValue = "Winstone_" + this.remoteIP + "_" + this.serverPort + "_" +
                    System.currentTimeMillis();
    byte digestBytes[] = this.md5Digester.digest(cookieValue.getBytes());
    
    // Write out in hex format
    char outArray[] = new char[32];
    for (int n = 0; n < digestBytes.length; n++)
    {
      int hiNibble = (digestBytes[n] & 0xFF) >> 4;
      int loNibble = (digestBytes[n] & 0xF);
      outArray[2*n]     = (hiNibble > 9 ? (char) (hiNibble + 87) : (char) (hiNibble + 48));
      outArray[2*n + 1] = (loNibble > 9 ? (char) (loNibble + 87) : (char) (loNibble + 48));
    }
    
    this.sessionCookie = new String(outArray);
    this.webappConfig.makeNewSession(this.sessionCookie);
    return this.sessionCookie;
  }

  /**
   * @deprecated
   */
  public String getRealPath(String path)
    {return this.webappConfig.getRealPath(path);}

  /**
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl()      {return isRequestedSessionIdFromURL();}
}

