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
import java.net.Socket;
import java.util.*;
import javax.servlet.http.Cookie;

/**
 * Encapsulates the http specific logic of building a request object, ie the
 * http protocol parsing etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpConnector
{
  final char CR = '\r';
  final char LF = '\n';
  final String CR_LF = "\r\n";
  final String specialCharacters = "()<>@,;:\\\"/[]?={} \t";

  // Request header constants
  final String SESSION_COOKIE_NAME    = "WinstoneHttpSessionId";
  final String CONTENT_LENGTH_HEADER  = "Content-Length";
  final String CONTENT_TYPE_HEADER    = "Content-Type";
  final String AUTHORIZATION_HEADER   = "Authorization";
  final String LOCALE_HEADER          = "Accept-Language";

  final String IN_COOKIE_HEADER       = "Cookie";
  final String METHOD_POST            = "POST";
  final String METHOD_HEAD            = "HEAD";
  final String POST_PARAMETERS        = "application/x-www-form-urlencoded";

  // Response header constants
  final String KEEP_ALIVE_HEADER      = "Connection";
  final String ENCODING_HEADER        = "Transfer-Encoding";
  final String KEEP_ALIVE_OPEN        = "Keep-Alive";
  final String KEEP_ALIVE_CLOSE       = "Close";
  final String DATE_HEADER            = "Date";
  final String SERVER_HEADER          = "Server";
  final String OUT_COOKIE_HEADER1     = "Set-Cookie";
  final String OUT_COOKIE_HEADER2     = "Set-Cookie2";
  final String LOCATION_HEADER        = "Location";

  private boolean doHostNameLookups;
  private WinstoneResourceBundle resources;

  public HttpConnector(WinstoneResourceBundle resources, boolean doHostNameLookups)
  {
    this.doHostNameLookups = doHostNameLookups;
    this.resources = resources;
  }

  public String getScheme() {return "http";}
  
  public void parseSocketInfo(Socket socket, WinstoneRequest req)
  {
    req.setServerPort(socket.getLocalPort());
    req.setRemoteIP(socket.getInetAddress().getHostAddress());
    if (this.doHostNameLookups)
      req.setRemoteName(socket.getInetAddress().getHostName());
    else
      req.setRemoteName(socket.getInetAddress().getHostAddress());
  }

  public String parseURILine(String uriLine, WinstoneRequest req) throws Exception
  {
    // Method
    int spacePos = uriLine.indexOf(' ');
    if (spacePos == -1)
      throw new WinstoneException(resources.getString("HttpConnector.ErrorUriLine") + uriLine);
    String method = uriLine.substring(0, spacePos).toUpperCase();
    String servletURI = null;
    String fullURI = null;

    // URI
    String remainder = uriLine.substring(spacePos + 1);
    spacePos = remainder.indexOf(' ');
    if (spacePos == -1)
    {
      fullURI = trimHostName(remainder.trim());
      req.setProtocol("HTTP/0.9");
    }
    else
    {
      fullURI = trimHostName(remainder.substring(0, spacePos).trim());
      req.setProtocol(remainder.substring(spacePos + 1).trim().toUpperCase());
    }
    int questionPos = fullURI.indexOf('?');
    if ((questionPos != -1) && (method != null))
    {
      servletURI = fullURI.substring(0, questionPos);
      String queryString = fullURI.substring(questionPos + 1);
      req.setQueryString(queryString);
      req.getParameters().putAll(extractParameters(queryString));
    }
    else
      servletURI = fullURI;

    req.setMethod(method);
    req.setRequestURI(fullURI);
    req.setServletPath(servletURI);
    return servletURI;
  }

  /**
   * Go through all the headers and process them one by one until we hit an empty line
   */
  public void parseHeaders(WinstoneRequest req, WinstoneInputStream inData) throws IOException
  {
    List headerList = new ArrayList();

    if (!req.getProtocol().startsWith("HTTP/0"))
    {
      // Loop to get headers
      byte headerBuffer[] = inData.readLine();
      String headerLine = new String(headerBuffer);

      while (headerLine.trim().length() > 0)
      {
        if (headerLine.indexOf(':') != -1)
          headerList.add(headerLine);
        headerBuffer = inData.readLine();
        headerLine = new String(headerBuffer);
      }
    }

    // Iterate through headers
    List cookieList = new ArrayList();
    for (Iterator i = headerList.iterator(); i.hasNext(); )
    {
      String header = (String) i.next();
      int colonPos = header.indexOf(':');
      String name = header.substring(0, colonPos);
      String value = header.substring(colonPos + 1).trim();

      if (name.equalsIgnoreCase(AUTHORIZATION_HEADER))
        req.setAuthorization(value);
      else if (name.equalsIgnoreCase(LOCALE_HEADER))
        req.setLocales(parseLocales(value));
      else if (name.equalsIgnoreCase(CONTENT_LENGTH_HEADER))
        req.setContentLength(Integer.parseInt(value));
      else if (name.equalsIgnoreCase(CONTENT_TYPE_HEADER))
      {
        req.setContentType(value);
        int semicolon = value.lastIndexOf(';');
        if (semicolon != -1)
        {
          String encodingClause = value.substring(semicolon + 1).trim();
          if (encodingClause.startsWith("charset="))
            req.setCharacterEncoding(encodingClause.substring(8));
        }
      }
      else if (name.equalsIgnoreCase(IN_COOKIE_HEADER))
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
            cookieList.add(thisCookie);
            Logger.log(Logger.FULL_DEBUG, resources.getString("HttpConnector.CookieFound",
                  "[#cookieName]", thisCookie.getName(), "[#cookieValue]", thisCookie.getValue()));
            if (thisCookie.getName().equals(SESSION_COOKIE_NAME))
            {
              req.setSessionCookie(thisCookie.getValue());
              Logger.log(Logger.FULL_DEBUG, resources.getString("HttpConnector.SessionCookieFound", "[#cookieValue]", thisCookie.getValue()));
            }
          }
        }
      }
    }
    req.setHeaders((String []) headerList.toArray(new String[headerList.size()]));
    req.setCookies((Cookie []) cookieList.toArray(new Cookie[cookieList.size()]));
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
      if ((orderKeys[n].floatValue() <= 0) || (orderKeys[n].floatValue() > 1))
        continue;
      List localeList = (List) localeEntries.get(orderKeys[n]);
      for (Iterator i = localeList.iterator(); i.hasNext(); )
        outputLocaleList.add(i.next());
    }

    return outputLocaleList;
  }

  private String trimToOneLine(String input)
  {
    int crPos = input.indexOf(CR);
    if (crPos != -1)
      input = input.substring(0, crPos);
    int lfPos = input.indexOf(LF);
    if (lfPos != -1)
      input = input.substring(0, lfPos);
    return input;
  }

  private String trimHostName(String input)
  {
    if (input == null)
      return null;
    else if (input.startsWith("/"))
      return input;

    int hostStart = input.indexOf("://");
    if (hostStart == -1)
      return input;
    String hostName = input.substring(hostStart + 3);
    int pathStart = hostName.indexOf('/');
    if (pathStart == -1)
      return "/";
    else
      return hostName.substring(pathStart);
  }

  /**
   * Gets parameters from the url encoded parameter string
   */
  private Map extractParameters(String urlEncodedParams)
  {
    Logger.log(Logger.FULL_DEBUG, resources.getString("HttpConnector.ParsingParameters") + urlEncodedParams);
    Map params = new Hashtable();
    StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
    while (st.hasMoreTokens())
    {
      String token = st.nextToken();
      int equalPos = token.indexOf('=');
      if (equalPos == -1)
        Logger.log(Logger.WARNING, resources.getString("HttpConnector.IgnoringBadParameter") + token);
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
          Logger.log(Logger.WARNING, resources.getString("HttpConnector.UnknownParameterType",
              "[#name]", decodedName + " = " + decodedValue.getClass()));
      }
      catch (Exception err)
        {Logger.log(Logger.ERROR, resources.getString("HttpConnector.ErrorParameters"), err);}
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
      Logger.log(Logger.WARNING, resources.getString("HttpConnector.BothMethodsCalled"));
      return new Boolean(true);
    }
    else if (parsedParameters == null)
    try
    {
      Logger.log(Logger.FULL_DEBUG, resources.getString("HttpConnector.ParsingBodyParameters"));
      if (method.equals(METHOD_POST) &&
          (contentType != null) && contentType.equals(POST_PARAMETERS))
      {
        // Parse params
        byte paramBuffer[] = new byte[contentLength];
        int readCount = inData.read(paramBuffer);
        if (readCount != contentLength)
          Logger.log(Logger.WARNING, resources.getString("HttpConnector.IncorrectContentLength",
            "[#contentLength]", contentLength + "", "[#readCount]", readCount + ""));
        String paramLine = new String(paramBuffer);
        Map postParams = extractParameters(paramLine.trim());
        Logger.log(Logger.FULL_DEBUG, resources.getString("HttpConnector.ParamLine") + postParams);
        params.putAll(postParams);
      }
      return new Boolean(true);
    }
    catch (Throwable err)
    {
      Logger.log(Logger.ERROR, resources.getString("HttpConnector.ErrorBodyParameters"), err);
      return null;
    }
    else
      return parsedParameters;
  }

  public boolean closeAfterRequest(WinstoneRequest req, WinstoneResponse rsp)
  {
    String inKeepAliveHeader = req.getHeader(KEEP_ALIVE_HEADER);
    String outKeepAliveHeader = rsp.getHeader(KEEP_ALIVE_HEADER);
    if (req.getProtocol().startsWith("HTTP/0"))
      return true;
    else if ((inKeepAliveHeader == null) && (outKeepAliveHeader == null))
      return req.getProtocol().equals("HTTP/1.0") ? true : false;
    else if ((outKeepAliveHeader != null) &&
             outKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE))
      return true;
    else if (inKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE))
      return true;
    else
      return false;
  }

  /**
   * This ensures the bare minimum correct http headers are present
   */
  public void validateHeaders(WinstoneRequest req, WinstoneResponse rsp)
  {
    //setHeader(ENCODING_HEADER, "chunked");
    String contentLength = rsp.getHeader(CONTENT_LENGTH_HEADER);
    rsp.setHeader(KEEP_ALIVE_HEADER,
                  (contentLength != null) && !closeAfterRequest(req, rsp) ? KEEP_ALIVE_OPEN :
                                                                            KEEP_ALIVE_CLOSE);
    if (rsp.getHeader(DATE_HEADER) == null)
      rsp.setDateHeader(DATE_HEADER, System.currentTimeMillis());
  }

  public void writeHeaders(WinstoneRequest req, WinstoneResponse rsp,
                           PrintStream outputStream, List headers, List cookies)
    throws IOException
  {
    // Ensure bare minimum headers
    validateHeaders(req, rsp);

    // Write out the new session cookie if it's present
    String sessionCookie = req.getSessionCookie();
    if (sessionCookie != null)
    {
      WinstoneSession session = (WinstoneSession) req.getWebAppConfig().getSessions().get(sessionCookie);
      if ((session != null) && session.isNew())
      {
        session.setIsNew(false);
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionCookie);
        cookie.setMaxAge(-1);
        rsp.addCookie(cookie);
      }
    }

    StringBuffer out = new StringBuffer();
    out.append(req.getProtocol()).append(" ").append(rsp.getStatus()).append(CR_LF);

    // Write headers and cookies
    for (Iterator i = headers.iterator(); i.hasNext(); )
      out.append((String) i.next()).append(CR_LF);
    if (!cookies.isEmpty())
    {
      for (Iterator i = cookies.iterator(); i.hasNext(); )
        writeCookie((Cookie) i.next(), out);
    }
    out.append(CR_LF);
    Logger.log(Logger.FULL_DEBUG, resources.getString("HttpConnector.OutHeaders") + out.toString());
    outputStream.print(out.toString());
  }

  protected void writeCookie(Cookie cookie, StringBuffer out) throws IOException
  {
    if (cookie.getVersion() == 1)
    {
      out.append(OUT_COOKIE_HEADER2).append(": ")
         .append(cookie.getName()).append("=");
      quote(cookie.getValue(), out);
      out.append("; Version=1");
      if (cookie.getDomain() != null)
      {
        out.append("; Domain=");
        quote(cookie.getDomain(), out);
      }
      if (cookie.getSecure())
        out.append("; Secure");

      if (cookie.getMaxAge() >= 0)
        out.append("; Max-Age=").append(cookie.getMaxAge());
      else
        out.append("; Discard");
      if (cookie.getPath() != null)
      {
        out.append("; Path=");
        quote(cookie.getPath(), out);
      }
    }
    else
    {
      out.append(OUT_COOKIE_HEADER1).append(": ")
         .append(cookie.getName()).append("=").append(cookie.getValue());
      if (cookie.getMaxAge() >= 0)
        out.append("; Max-Age=").append(cookie.getMaxAge());
      else
        out.append("; Discard");
      if (cookie.getPath() != null)
        out.append("; Path=").append(cookie.getPath());
    }
    out.append(CR_LF);
  }

  protected void quote(String value, StringBuffer out)
  {
    boolean containsSpecial = false;
    for (int n = 0; n < value.length(); n++)
    {
      char thisChar = value.charAt(n);
      if ((thisChar < 32) || (thisChar >= 127) ||
          (specialCharacters.indexOf(thisChar) != -1))
      {
        containsSpecial = true;
        break;
      }
    }
    if (containsSpecial)
      out.append('"').append(value).append('"');
    else
      out.append(value);
  }

}

