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
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Response for servlet
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneResponse implements HttpServletResponse
{
  protected static DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  static  {df.setTimeZone(TimeZone.getTimeZone("GMT"));}

  static final String CONTENT_LENGTH_HEADER  = "Content-Length";
  static final String CONTENT_TYPE_HEADER    = "Content-Type";

  // Response header constants
  static final String KEEP_ALIVE_HEADER      = "Connection";
  static final String ENCODING_HEADER        = "Transfer-Encoding";
  static final String KEEP_ALIVE_OPEN        = "Keep-Alive";
  static final String KEEP_ALIVE_CLOSE       = "Close";
  static final String DATE_HEADER            = "Date";
  static final String SERVER_HEADER          = "Server";
  static final String LOCATION_HEADER        = "Location";
  static final String OUT_COOKIE_HEADER1     = "Set-Cookie";
  static final String OUT_COOKIE_HEADER2     = "Set-Cookie2";
  static final String X_POWERED_BY_HEADER		 = "X-Powered-By";

  static final String POWERED_BY_WINSTONE = "Servlet/2.4 (Winstone/0.6)";
  
  private int statusCode;
  private WinstoneRequest req;
  private WebAppConfiguration webAppConfig;
  private WinstoneOutputStream outputStream;
  private List headers;
  private String encoding;
  private List cookies;
  private Writer outWriter;
  private Locale locale;
  private Map encodingMap;
  private String protocol;
  private String reqKeepAliveHeader;

  private WinstoneResourceBundle resources;

  /**
   * Constructor
   */
  public WinstoneResponse(WinstoneResourceBundle resources)
  {
    this.resources = resources;
    this.headers = new ArrayList();
    this.cookies = new ArrayList();
    this.encodingMap = new HashMap();

    this.statusCode = SC_OK;
    this.locale = Locale.getDefault();
    this.encoding = null;
    this.protocol = null;
    this.reqKeepAliveHeader = null;
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
    this.cookies.clear();
    this.outWriter = null;
    this.protocol = null;
    this.reqKeepAliveHeader = null;

    this.statusCode = SC_OK;
    this.locale = Locale.getDefault();
    this.encoding = null;
  }

  private String getEncodingFromLocale(Locale loc)
  {
  	Map encMap = this.webAppConfig.getLocaleEncodingMap();
    String fullMatch = (String) encMap.get(loc.getLanguage() + "_" + loc.getCountry());
    if (fullMatch != null)
      return fullMatch;
    else
      return (String) encMap.get(loc.getLanguage());
  }
  public void setOutputStream(WinstoneOutputStream outData) {this.outputStream = outData;}
  public void setWebAppConfig(WebAppConfiguration webAppConfig) {this.webAppConfig = webAppConfig;}

  public String getProtocol()   {return this.protocol;}
  public void setProtocol(String protocol) {this.protocol = protocol;}
  
  public void extractRequestKeepAliveHeader(WinstoneRequest req) 
  	{this.reqKeepAliveHeader = req.getHeader(KEEP_ALIVE_HEADER);}
  
  public List   getHeaders()    {return this.headers;}
  public List   getCookies()    {return this.cookies;}

  public WinstoneRequest getRequest() {return this.req;}
  public void setRequest(WinstoneRequest req) {this.req = req;}
  
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
    String header = sb.toString().substring(0, sb.length() - 1);
    if (header.startsWith("text/"))
      header += ";charset=" + getCharacterEncoding();
    setHeader(CONTENT_TYPE_HEADER, header);
  }

  /**
   * A check to ensure correct content length values
   */
  public void verifyContentLength()
  {
    String length = getHeader(CONTENT_LENGTH_HEADER);
    if (length != null)
    {
      int contentLength = 0;
      try {contentLength = Integer.parseInt(length);}
      catch (Throwable err) {return;}
      int bodyBytes = this.outputStream.getBytesWritten();
      if (contentLength != bodyBytes)
        Logger.log(Logger.DEBUG, resources, "WinstoneResponse.ShortOutput",
          new String[] {contentLength + "", bodyBytes + ""});
    }
  }
  
  /**
   * This ensures the bare minimum correct http headers are present
   */
  public void validateHeaders()
  {
    //rsp.setHeader(ENCODING_HEADER, "chunked");
    String contentLength = this.getHeader(CONTENT_LENGTH_HEADER);
    this.setHeader(KEEP_ALIVE_HEADER,
                  (contentLength != null) && !closeAfterRequest()
                            ? KEEP_ALIVE_OPEN : KEEP_ALIVE_CLOSE);
    if (this.getHeader(DATE_HEADER) == null)
      this.setDateHeader(DATE_HEADER, System.currentTimeMillis());
    if (this.getHeader(X_POWERED_BY_HEADER) == null)
      this.setHeader(X_POWERED_BY_HEADER, POWERED_BY_WINSTONE);

    // If we don't have a webappConfig, exit here, cause we definitely don't have a session
    WinstoneRequest req = this.getRequest();
    if (req.getWebAppConfig() == null)
      return;

    // Write out the new session cookie if it's present
    String sessionCookie = req.getSessionCookie();
    if (sessionCookie != null)
    {
      WinstoneSession session = req.getWebAppConfig().getSessionById(sessionCookie, false);
      if ((session != null) && session.isNew())
      {
        session.setIsNew(false);
        Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, sessionCookie);
        cookie.setMaxAge(-1);
        cookie.setSecure(req.isSecure());
        cookie.setVersion(req.isSecure() ? 1 : 0);
        cookie.setPath(req.getWebAppConfig().getPrefix().equals("") ? "/" : req.getWebAppConfig().getPrefix());
        this.addCookie(cookie);
      }
    }
  }

  /**
   * Writes out the http header for a single cookie
   */
  public String writeCookie(Cookie cookie) throws IOException
  {
    StringBuffer out = new StringBuffer();
    
    // Set-Cookie or Set-Cookie2
    if (cookie.getVersion() >= 1 )
      out.append(OUT_COOKIE_HEADER2).append(": ");
    else
      out.append(OUT_COOKIE_HEADER1).append(": ");

    // name/value pair
    if (cookie.getVersion() == 0)
      out.append(cookie.getName()).append("=").append(cookie.getValue());
    else
    {
      out.append(cookie.getName()).append("=");
      quote(cookie.getValue(), out);
    }
    
    if (cookie.getVersion() >= 1) 
    {
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
      if (cookie.getDomain() != null)
      {
        out.append("; Domain=");
        out.append(cookie.getDomain());
      }
      if (cookie.getMaxAge() >= 0)
      {
        long expiryMS = System.currentTimeMillis() + (long) (1000 * (long) cookie.getMaxAge());
        Date expiryDate = new Date(expiryMS);
        out.append("; Expires=").append(df.format(expiryDate));
      }
      if (cookie.getPath() != null)
        out.append("; Path=").append(cookie.getPath());
      if (cookie.getSecure())
        out.append("; Secure");
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

  final String specialCharacters = "()<>@,;:\\\"/[]?={} \t";  

  /**
   * Based on request/response headers and the protocol, determine whether
   * or not this connection should operate in keep-alive mode.
   */
  public boolean closeAfterRequest()
  {
    String inKeepAliveHeader = this.reqKeepAliveHeader;
    String outKeepAliveHeader = this.getHeader(KEEP_ALIVE_HEADER);
    if (this.protocol.startsWith("HTTP/0"))
      return true;
    else if ((inKeepAliveHeader == null) && (outKeepAliveHeader == null))
      return this.protocol.equals("HTTP/1.0") ? true : false;
    else if (outKeepAliveHeader != null)
      return outKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE);
    else if (inKeepAliveHeader != null)
      return inKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE);
    else
      return false;
  }

  // ServletResponse interface methods
  public void flushBuffer() throws IOException
  {
    try {this.outWriter.flush();} catch (Throwable err) {}
    this.outputStream.commit();
  }
  public void setBufferSize(int size)  {this.outputStream.setBufferSize(size);}
  public int getBufferSize()  {return this.outputStream.getBufferSize();}

  public String getCharacterEncoding() {return this.encoding == null ? "ISO-8859-1" : this.encoding;}
  public void setCharacterEncoding(String encoding) {this.encoding = encoding;}
  public String getContentType() {return getHeader(CONTENT_TYPE_HEADER);}
  public void setContentType(String type) {updateContentTypeHeader(type);}
  public Locale getLocale() {return this.locale;}
  public void setLocale(Locale loc) 
  {
    if (this.outWriter == null)
    {
      //Logger.log(Logger.DEBUG, "Response.setLocale()");
      this.locale = loc;
      String ct = getHeader(CONTENT_TYPE_HEADER);
      String charset = getEncodingFromLocale(this.locale);
      if (ct == null)
        updateContentTypeHeader("text/html;charset=" + charset);
      else if (ct.indexOf(';') == -1)
        updateContentTypeHeader(ct + ";charset=" + charset);
     else
        updateContentTypeHeader(ct.substring(0, ct.indexOf(';')) + ";charset=" + charset);
        
    }
    else
      Logger.log(Logger.WARNING, resources, "WinstoneResponse.SetLocaleTooLate");
  }

  public ServletOutputStream getOutputStream() throws IOException
    {return this.outputStream;}

  public PrintWriter getWriter() throws IOException
  {
    String encoding = getCharacterEncoding();
    if (this.outWriter != null)
      return new PrintWriter(this.outWriter, true);
    else try
    {
      this.outWriter = new OutputStreamWriter(getOutputStream(), encoding);
      return new PrintWriter(this.outWriter, true);
    }
    catch (UnsupportedEncodingException err)
    {
      throw new WinstoneException(resources.getString("WinstoneResponse.WriterError") +
                                      encoding, err);
    }
  }

  public boolean isCommitted() {return this.outputStream.isCommitted();}
  public void reset() 
  {
    resetBuffer();
    this.statusCode = SC_OK;
    this.headers.clear();
    this.cookies.clear();
  }
  public void resetBuffer() 
  {
    if (isCommitted())
      throw new IllegalStateException(resources.getString("WinstoneResponse.ResponseCommitted"));
    this.outWriter = null; 
    this.outputStream.reset();
  }
  public void setContentLength(int len) {setIntHeader(CONTENT_LENGTH_HEADER, len);}

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

  public String encodeRedirectURL(String url) {return url;}
  public String encodeURL(String url)         {return url;}
  public int getStatus()                      {return this.statusCode;}
  public void setStatus(int sc)               {this.statusCode = sc;}

  public void sendError(int sc) throws IOException
    {sendError(sc, null);}
  public void sendError(int sc, String msg) throws IOException
  {
    Logger.log(Logger.DEBUG, this.resources, "WinstoneResponse.SendingError", 
            new String[] {"" + sc, msg});
    
    boolean found = false;
    if ((this.webAppConfig != null) && (this.req != null) &&
        (this.webAppConfig.getErrorPagesByCode().get("" + sc) != null))
    {
      String errorPage = (String) this.webAppConfig.getErrorPagesByCode().get("" + sc);
      javax.servlet.RequestDispatcher rd = this.webAppConfig.getErrorDispatcher(
          errorPage, new Integer(sc), null, null, getRequest().getRequestURI());
      try
      {
        rd.forward(this.req, this);
        found = true;
      }
      catch (IllegalStateException err) {throw err;}
      catch (IOException err) {throw err;}
      catch (Throwable err)
        {Logger.log(Logger.WARNING, this.resources, "WinstoneResponse.ErrorInErrorPage",
              new String[] {errorPage, sc + ""}, err);}
    }

    // If all fails, show the default page
    if (!found)
    {
      this.statusCode = sc;

      String output = resources.getString("WinstoneResponse.ErrorPage", new String[] {
          sc + "",
          (msg == null ? "" : msg),
          resources.getString("ServerVersion"),
          "" + new Date()});
      setContentLength(output.getBytes(getCharacterEncoding()).length);
      Writer out = getWriter();
      out.write(output);
      out.close();
    }
  }

  public void sendRedirect(String location) throws IOException
  {
    // Build location
    StringBuffer fullLocation = new StringBuffer();
    if (location.startsWith("http://"))
      fullLocation.append(location);
    else 
    {
      fullLocation.append(this.req.getScheme()).append("://");
      fullLocation.append(this.req.getServerName());
      if (!((this.req.getServerPort() == 80) && this.req.getScheme().equals("http")) &&
          !((this.req.getServerPort() == 443) && this.req.getScheme().equals("https")))
        fullLocation.append(':').append(this.req.getServerPort());
      if (location.startsWith("/"))
      {
        if (!this.webAppConfig.getPrefix().equals("") &&
            !location.startsWith(this.webAppConfig.getPrefix()))
          fullLocation.append(this.webAppConfig.getPrefix());
        fullLocation.append(location);
      }
      else 
      {
        fullLocation.append(this.req.getRequestURI());
        if (fullLocation.toString().indexOf("?") != -1)
          fullLocation.delete(fullLocation.toString().indexOf("?"), fullLocation.length());
        fullLocation.delete(fullLocation.toString().lastIndexOf("/") + 1, fullLocation.length());
        fullLocation.append(location);
      }
    }
    
    // If body not parsed
    if (this.req.getParsedParameters() == null)
      this.req.getParameterNames(); // dummy to force parsing of the request
    else if (this.req.getParsedParameters().equals(Boolean.FALSE))
    {
      // read full stream length
      InputStream in = this.req.getInputStream();
      while (in.read() != -1);
    }
        
    this.statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
    setHeader(LOCATION_HEADER, fullLocation.toString());
    setContentLength(0);
    getWriter().close();
  }

  /**
   * Method used for handling untrapped errors. It looks to see if there are any
   * errorPage directives for this class first, then falls through to the
   * default error page if there are none.
   */
  public void sendUntrappedError(Throwable err,
      WinstoneRequest req, String throwingServletName) throws IOException
  {
    boolean found = false;
    if ((this.webAppConfig != null) &&
        !this.webAppConfig.getErrorPagesByException().isEmpty())
    {
      Class exceptionClasses[] = this.webAppConfig.getErrorPageExceptions();
      Map errorPages = this.webAppConfig.getErrorPagesByException();
      Throwable errWrapper = new ServletException("First time only", err);
      while (!found && (errWrapper instanceof ServletException))
      {
        errWrapper = ((ServletException) errWrapper).getRootCause();
        for (int n = 0; n < exceptionClasses.length  && !found; n++)
        {
        
          Logger.log(Logger.FULL_DEBUG, resources, "WinstoneResponse.TestingException", 
            exceptionClasses[n] + "");
          String errorPage = null;
          try
          {
            if (exceptionClasses[n].isInstance(err))
            {
              errorPage = (String) errorPages.get(exceptionClasses[n]);
              javax.servlet.RequestDispatcher rd = this.webAppConfig
                .getErrorDispatcher(errorPage,
                  new Integer(SC_INTERNAL_SERVER_ERROR), err, throwingServletName,
                  req.getRequestURI());
              if (rd != null)
              {
                found = true;
                rd.forward(req, this);
              }
              else
                Logger.log(Logger.WARNING, resources, "WinstoneResponse.SkippingException", 
                     new String[] {exceptionClasses[n].getName(), 
                     (String) errorPages.get(exceptionClasses[n])});
            }
          }
          catch (Throwable err2) 
          	{Logger.log(Logger.WARNING, this.resources, "WinstoneResponse.ErrorInErrorPage",
                  new String[] {errorPage, "" + 500}, err2);}
        }
      }
    }

    // Fall through to the default page if no webapp or no errorPage tags
    if (!found)
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      err.printStackTrace(pw);
      sendError(SC_INTERNAL_SERVER_ERROR,
        resources.getString("WinstoneResponse.ServletExceptionPage", sw.toString()));
    }
  }

  /**
   * @deprecated
   */
  public String encodeRedirectUrl(String url) {return encodeRedirectURL(url);}
  /**
   * @deprecated
   */
  public String encodeUrl(String url)         {return encodeURL(url);}
  /**
   * @deprecated
   */
  public void setStatus(int sc, String sm)    {setStatus(sc);}
}

