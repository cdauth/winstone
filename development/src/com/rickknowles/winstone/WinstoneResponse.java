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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Response for servlet
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneResponse implements HttpServletResponse
{
  protected static DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  static  {df.setTimeZone(TimeZone.getTimeZone("GMT"));}

  private int statusCode;
  private WinstoneRequest req;
  private WebAppConfiguration webAppConfig;
  private HttpProtocol protocolClass;
  private WinstoneOutputStream outputStream;
  private List headers;
  private String encoding;
  private List cookies;
  //private int contentLength;
  private Writer outWriter;

  private WinstoneResourceBundle resources;

  /**
   * Constructor
   */
  public WinstoneResponse(WinstoneResourceBundle resources)
  {
    this.resources = resources;
    this.protocolClass = protocolClass;
    this.headers = new ArrayList();
    this.cookies = new ArrayList();
    this.encoding = null; // default

    this.statusCode = SC_OK;
  }

  /**
   * Resets the request to be reused
   */
  public void cleanUp()
  {
    this.req = null;
    this.webAppConfig = null;
    this.outputStream = null;
    this.headers.clear();
    this.encoding = null;
    this.cookies.clear();
    this.outWriter = null;

    this.statusCode = SC_OK;
  }

  public void setOutputStream(WinstoneOutputStream outData) {this.outputStream = outData;}
  public void setWebAppConfig(WebAppConfiguration webAppConfig) {this.webAppConfig = webAppConfig;}
  public void setProtocolClass(HttpProtocol protocolClass) {this.protocolClass = protocolClass;}
  public void setRequest(WinstoneRequest req) {this.req = req;}

  public String getProtocol()   {return this.req.getProtocol();}
  public List   getHeaders()    {return this.headers;}
  public List   getCookies()    {return this.cookies;}

  public WinstoneRequest getRequest() {return this.req;}
  
  public void updateContentTypeHeader(String type)
  {
    // Parse type to set encoding if needed
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(type, ";");
    while (st.hasMoreTokens())
    {
      String clause = st.nextToken().trim();
      if (clause.startsWith("charset="))
        this.encoding = clause.substring(8);
      else
        sb.append(clause).append(";");
    }
    String header = sb.toString().substring(0, sb.length() - 1) +
                   (this.encoding == null ? "" : ";charset=" + this.encoding);
    setHeader(this.protocolClass.CONTENT_TYPE_HEADER, header);
  }

  /**
   * A check to ensure correct content length values
   */
  public void verifyContentLength()
  {
    String length = getHeader(this.protocolClass.CONTENT_LENGTH_HEADER);
    if (length != null)
    {
      int contentLength = 0;
      try {contentLength = Integer.parseInt(length);}
      catch (Throwable err) {return;}
      int bodyBytes = this.outputStream.getBytesWritten();
      if (contentLength != bodyBytes)
        Logger.log(Logger.WARNING, resources.getString("WinstoneResponse.ShortOutput",
          "[#contentLength]", contentLength + "", "[#bodyBytes]", bodyBytes + ""));
    }
  }

  public void updateSessionCookie() throws IOException
  {
    // Write out the new session cookie if it's present
    String sessionCookie = req.getSessionCookie();
    if (sessionCookie != null)
    {
      WinstoneSession session = (WinstoneSession) req.getWebAppConfig().getSessions().get(sessionCookie);
      if ((session != null) && session.isNew())
      {
        session.setIsNew(false);
        Cookie cookie = new Cookie(this.protocolClass.SESSION_COOKIE_NAME, sessionCookie);
        cookie.setMaxAge(-1);
        cookie.setPath(req.getWebAppConfig().getPrefix() + "/");
        addCookie(cookie);
      }
    }
  }
  /*public void writeHeaders(PrintStream writeTo) throws IOException
  {
    this.protocolClass.writeHeaders(this.req, this, this.req.getWebAppConfig().getPrefix(),
                                writeTo, this.headers, this.cookies);
  }*/

  // ServletResponse interface methods
  public void flushBuffer() throws IOException
  {
    try {this.outWriter.flush();} catch (Throwable err) {}
    this.outputStream.commit();
  }
  public void setBufferSize(int size)  {this.outputStream.setBufferSize(size);}
  public int getBufferSize()  {return this.outputStream.getBufferSize();}

  public String getCharacterEncoding() {return this.encoding;}
  public void setContentType(String type) {updateContentTypeHeader(type);}
  public void setLocale(Locale loc) {Logger.log(Logger.WARNING, "Response locales not implemented");}
  public Locale getLocale() {Logger.log(Logger.WARNING, "Response locales not implemented"); return null;}

  public ServletOutputStream getOutputStream() throws IOException
    {return this.outputStream;}

  public PrintWriter getWriter() throws IOException
  {
    if (this.outWriter != null)
      return new PrintWriter(this.outWriter, true);
    else try
    {
      if (this.encoding != null)
        this.outWriter = new OutputStreamWriter(getOutputStream(), this.encoding);
      else
        this.outWriter = new OutputStreamWriter(getOutputStream());
      return new PrintWriter(this.outWriter, true);
    }
    catch (UnsupportedEncodingException err)
    {
      throw new WinstoneException(resources.getString("WinstoneResponse.WriterError") +
                                      this.encoding, err);
    }
  }

  public boolean isCommitted() {return this.outputStream.isCommitted();}
  public void reset() {this.outWriter = null; this.outputStream.reset();}
  public void resetBuffer() {reset();}
  public void setContentLength(int len) {setIntHeader(this.protocolClass.CONTENT_LENGTH_HEADER, len);}

  // HttpServletResponse interface methods
  public void addCookie(Cookie cookie) {this.cookies.add(cookie);}

  public boolean containsHeader(String name)
  {
    for (int n = 0; n < this.headers.size(); n++)
      if (((String)this.headers.get(n)).startsWith(name))
        return true;
    return false;
  }

  public void addDateHeader(String name, long date) {addHeader(name, df.format(new Date(date)));} //df.format(new Date(date)));}
  public void addIntHeader(String name, int value)  {addHeader(name, "" + value);}
  public void addHeader(String name, String value)  {this.headers.add(name + ": " + value);}
  public void setDateHeader(String name, long date) {setHeader(name, df.format(new Date(date)));}
  public void setIntHeader(String name, int value)  {setHeader(name, "" + value);}
  public void setHeader(String name, String value)
  {
    boolean found = false;
    for (int n = 0; n < this.headers.size(); n++ )
    {
      String header = (String) this.headers.get(n);
      if (header.startsWith(name))
      {
        this.headers.set(n, name + ": " + value);
        found = true;
      }
    }
    if (!found) addHeader(name, value);
  }

  public String getHeader(String name)
  {
    for (int n = 0; n < this.headers.size(); n++ )
    {
      String header = (String) this.headers.get(n);
      if (header.startsWith(name + ": "))
        return header.substring(name.length() + 2);
    }
    return null;
  }
 
  public String encodeRedirectUrl(String url) {return encodeRedirectURL(url);}
  public String encodeRedirectURL(String url) {return url;}
  public String encodeUrl(String url)         {return encodeURL(url);}
  public String encodeURL(String url)         {return url;}
  public int getStatus()                      {return this.statusCode;}
  public void setStatus(int sc)               {this.statusCode = sc;}
  public void setStatus(int sc, String sm)    {setStatus(sc);}

  public void sendError(int sc) throws IOException
    {sendError(sc, null);}
  public void sendError(int sc, String msg) throws IOException
  {
    boolean found = false;
    if ((this.webAppConfig != null) && (this.req != null) &&
        (this.webAppConfig.getErrorPagesByCode().get("" + sc) != null))
    {
      String errorPage = (String) this.webAppConfig.getErrorPagesByCode().get("" + sc);
      javax.servlet.RequestDispatcher rd = this.webAppConfig.getRequestDispatcher(errorPage);
      try
      {
        rd.forward(this.req, this);
        found = true;
      }
      catch (IllegalStateException err) {throw err;}
      catch (IOException err) {throw err;}
      catch (Throwable err)
        {Logger.log(Logger.WARNING, this.resources.getString("WinstoneResponse.ErrorInErrorPage",
              "[#path]", errorPage, "[#code]", sc + ""), err);}
    }

    // If all fails, show the default page
    if (!found)
    {
      this.statusCode = sc;
      Map params = new HashMap();
      params.put("[#statusCode]", sc + "");
      params.put("[#msg]", msg);
      params.put("[#serverVersion]", resources.getString("ServerVersion"));
      params.put("[#date]", "" + new Date());

      String output = resources.getString("WinstoneResponse.ErrorPage", params);
      if (this.encoding == null)
        setContentLength(output.getBytes().length);
      else
        setContentLength(output.getBytes(encoding).length);
      Writer out = getWriter();
      out.write(output);
      out.close();
    }
  }

  public void sendRedirect(String location) throws IOException
  {
    this.statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
    setHeader(this.protocolClass.LOCATION_HEADER, location);
    setContentLength(0);
    getWriter().close();
  }

  /**
   * Method used for handling untrapped errors. It looks to see if there are any
   * errorPage directives for this class first, then falls through to the
   * default error page if there are none.
   */
  public void sendUntrappedError(Throwable err,
      WinstoneRequest req) throws IOException
  {
    boolean found = false;
    if ((this.webAppConfig != null) &&
        !this.webAppConfig.getErrorPagesByException().isEmpty())
    {
      Map errorPages = this.webAppConfig.getErrorPagesByException();
      for (Iterator i = errorPages.keySet().iterator(); i.hasNext(); )
      {
        String testErrorClassName = (String) i.next();
        try
        {
          Class testErrorClass = Class.forName(testErrorClassName);
          if (testErrorClass.isInstance(err))
          {
            javax.servlet.RequestDispatcher rd = this.webAppConfig
              .getRequestDispatcher((String) errorPages.get(testErrorClassName));
            found = true;
            rd.forward(req, this);
          }
        }
        catch (Throwable err2) {/* Skipping */}
      }
    }

    // Fall through to the default page if no webapp or no errorPage tags
    if (!found)
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      err.printStackTrace(pw);
      sendError(SC_INTERNAL_SERVER_ERROR, resources.getString("WinstoneResponse.ServletExceptionPage", "[#stackTrace]", sw.toString()));
    }
  }

}

