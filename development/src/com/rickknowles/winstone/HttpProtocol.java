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
import java.util.*;
import javax.servlet.http.Cookie;

/**
 * Encapsulates the http specific logic of building a request object, ie the
 * http protocol parsing etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpProtocol
{
  final char CR = '\r';
  final char LF = '\n';
  public static final String CR_LF = "\r\n";
  final String specialCharacters = "()<>@,;:\\\"/[]?={} \t";

  // Request header constants
  final String SESSION_COOKIE_NAME    = "WinstoneHttpSessionId";
  final String CONTENT_LENGTH_HEADER  = "Content-Length";
  final String CONTENT_TYPE_HEADER    = "Content-Type";
  final String AUTHORIZATION_HEADER   = "Authorization";
  final String LOCALE_HEADER          = "Accept-Language";
  final String HOST_HEADER            = "Host";

  final String IN_COOKIE_HEADER1      = "Cookie";
  final String IN_COOKIE_HEADER2      = "Cookie2";
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
  final String LOCATION_HEADER        = "Location";
  final String OUT_COOKIE_HEADER1     = "Set-Cookie";
  final String OUT_COOKIE_HEADER2     = "Set-Cookie2";

  private WinstoneResourceBundle resources;

  public HttpProtocol(WinstoneResourceBundle resources) {this.resources = resources;}

  public String parseURILine(String uriLine, WinstoneRequest req)
  {
    // Method
    int spacePos = uriLine.indexOf(' ');
    if (spacePos == -1)
      throw new WinstoneException(resources.getString("HttpProtocol.ErrorUriLine") + uriLine);
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
   * Parse the incoming stream into a list of headers (stopping at the first
   * blank line), then call the parseHeaders(req, list) method on that list.
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
        {
          headerList.add(headerLine);
          Logger.log(Logger.FULL_DEBUG, "Header:" + headerLine.trim());
        }
        headerBuffer = inData.readLine();
        headerLine = new String(headerBuffer);
      }
    }

    // If no headers available, parse an empty list
    parseHeaders(req, headerList);
  }

  /**
   * Go through the list of headers, and build the headers/cookies arrays for
   * the request object.
   */
  public void parseHeaders(WinstoneRequest req, List headerList)
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
        req.setAuthorization(value);
      else if (name.equalsIgnoreCase(LOCALE_HEADER))
        req.setLocales(parseLocales(value));
      else if (name.equalsIgnoreCase(CONTENT_LENGTH_HEADER))
        req.setContentLength(Integer.parseInt(value));
      else if (name.equalsIgnoreCase(HOST_HEADER))
        req.setServerName(nextColonPos == -1 ? value : value.substring(0, nextColonPos));
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
            thisCookie.setVersion(name.equals(IN_COOKIE_HEADER2) || req.isSecure() ? 1 : 0);
            thisCookie.setSecure(req.isSecure());
            cookieList.add(thisCookie);
            Logger.log(Logger.FULL_DEBUG, resources.getString("HttpProtocol.CookieFound",
                  "[#cookieName]", thisCookie.getName(), "[#cookieValue]", thisCookie.getValue()));
            if (thisCookie.getName().equals(SESSION_COOKIE_NAME))
            {
              req.setSessionCookie(thisCookie.getValue());
              Logger.log(Logger.FULL_DEBUG, resources.getString("HttpProtocol.SessionCookieFound", "[#cookieValue]", thisCookie.getValue()));
            }
          }
        }
      }
    }
    req.setHeaders((String []) outHeaderList.toArray(new String[outHeaderList.size()]));
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
  public Map extractParameters(String urlEncodedParams)
  {
    Logger.log(Logger.FULL_DEBUG, resources.getString("HttpProtocol.ParsingParameters") + urlEncodedParams);
    Map params = new Hashtable();
    StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
    while (st.hasMoreTokens())
    {
      String token = st.nextToken();
      int equalPos = token.indexOf('=');
      if (equalPos == -1)
        Logger.log(Logger.WARNING, resources.getString("HttpProtocol.IgnoringBadParameter") + token);
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
          Logger.log(Logger.WARNING, resources.getString("HttpProtocol.UnknownParameterType",
              "[#name]", decodedName + " = " + decodedValue.getClass()));
      }
      catch (Exception err)
        {Logger.log(Logger.ERROR, resources.getString("HttpProtocol.ErrorParameters"), err);}
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
      Logger.log(Logger.WARNING, resources.getString("HttpProtocol.BothMethodsCalled"));
      return new Boolean(true);
    }
    else if (parsedParameters == null)
    try
    {
      Logger.log(Logger.FULL_DEBUG, resources.getString("HttpProtocol.ParsingBodyParameters"));
      if (method.equals(METHOD_POST) &&
          (contentType != null) && contentType.equals(POST_PARAMETERS))
      {
        // Parse params
        byte paramBuffer[] = new byte[contentLength];
        int readCount = inData.read(paramBuffer);
        if (readCount != contentLength)
          Logger.log(Logger.WARNING, resources.getString("HttpProtocol.IncorrectContentLength",
            "[#contentLength]", contentLength + "", "[#readCount]", readCount + ""));
        String paramLine = new String(paramBuffer);
        Map postParams = extractParameters(paramLine.trim());
        Logger.log(Logger.FULL_DEBUG, resources.getString("HttpProtocol.ParamLine") + postParams);
        params.putAll(postParams);
      }
      return new Boolean(true);
    }
    catch (Throwable err)
    {
      Logger.log(Logger.ERROR, resources.getString("HttpProtocol.ErrorBodyParameters"), err);
      return null;
    }
    else
      return parsedParameters;
  }

  /**
   * Based on request/response headers and the protocol, determine whether
   * or not this connection should operate in keep-alive mode.
   */
  public boolean closeAfterRequest(WinstoneRequest req, WinstoneResponse rsp)
  {
    String inKeepAliveHeader = req.getHeader(KEEP_ALIVE_HEADER);
    String outKeepAliveHeader = rsp.getHeader(KEEP_ALIVE_HEADER);
    if (req.getProtocol().startsWith("HTTP/0"))
      return true;
    else if ((inKeepAliveHeader == null) && (outKeepAliveHeader == null))
      return req.getProtocol().equals("HTTP/1.0") ? true : false;
    else if (outKeepAliveHeader != null)
      return outKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE);
    else if (inKeepAliveHeader != null)
      return inKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE);
    else
      return false;
  }

  /**
   * This ensures the bare minimum correct http headers are present
   */
  public void validateHeaders(WinstoneResponse rsp)
  {
    //rsp.setHeader(ENCODING_HEADER, "chunked");
    String contentLength = rsp.getHeader(CONTENT_LENGTH_HEADER);
    rsp.setHeader(KEEP_ALIVE_HEADER,
                  (contentLength != null) && !closeAfterRequest(rsp.getRequest(), rsp)
                            ? KEEP_ALIVE_OPEN : KEEP_ALIVE_CLOSE);
    if (rsp.getHeader(DATE_HEADER) == null)
      rsp.setDateHeader(DATE_HEADER, System.currentTimeMillis());

    // If we don't have a webappConfig, exit here, cause we definitely don't have a session
    WinstoneRequest req = rsp.getRequest();
    if (req.getWebAppConfig() == null)
      return;

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
        cookie.setSecure(req.isSecure());
        cookie.setVersion(req.isSecure() ? 1 : 0);
        cookie.setPath(req.getWebAppConfig().getPrefix());
        rsp.addCookie(cookie);
      }
    }
  }

  /**
   * Writes out the http header for a single cookie
   */
  public String writeCookie(Cookie cookie) throws IOException
  {
    StringBuffer out = new StringBuffer();
    if (cookie.getVersion() == 1)
    {
      out.append(OUT_COOKIE_HEADER2).append(": ");
      out.append(cookie.getName()).append("=");
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
      out.append(OUT_COOKIE_HEADER1).append(": ");
      out.append(cookie.getName()).append("=").append(cookie.getValue());
      if (cookie.getMaxAge() >= 0)
        out.append("; Max-Age=").append(cookie.getMaxAge());
      else
        out.append("; Discard");
      if (cookie.getPath() != null)
        out.append("; Path=").append(cookie.getPath());
    }
    return out.toString();
  }

  /**
   * Quotes the necessary strings in a cookie header. The quoting is only
   * applied if the string contains special characters.
   */
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

