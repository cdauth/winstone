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

import java.io.IOException;
import java.util.Locale;
import java.io.ByteArrayOutputStream;

import javax.servlet.http.*;
import javax.servlet.*;

/**
 * Wraps a normal response, suppressing the headers, and only adding the
 * body. This is useful for Include dispatches, because we just want the body. 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class IncludeResponse extends HttpServletResponseWrapper
{
  private ByteArrayOutputStream includedBody;
  private WinstoneOutputStream servletOutputStream;
  private WinstoneResourceBundle resources;
  
  public IncludeResponse(ServletResponse response, WinstoneResourceBundle resources)
  {
    super((HttpServletResponse) response);
    this.resources = resources;
    this.includedBody = new ByteArrayOutputStream();
    this.servletOutputStream = new WinstoneOutputStream(this.includedBody, true, resources);
  }

  public ServletOutputStream getOutputStream() throws IOException
    {return this.servletOutputStream;}

  public void finish() throws IOException
  {
    this.servletOutputStream.commit();
    this.servletOutputStream.close();
    this.includedBody.flush();
    String underlyingEncoding = getResponse().getCharacterEncoding();
    String bodyBlock = (underlyingEncoding == null
            ? new String(this.includedBody.toByteArray(), underlyingEncoding)
            : new String(this.includedBody.toByteArray()));
    getResponse().getWriter().write(bodyBlock);
  }
  
  public void addCookie(Cookie cookie)  {}
  public void addDateHeader(String name, long date) {}
  public void addHeader(String name, String value) {}
  public void addIntHeader(String name, int value) {}

  public void sendError(int sc, String msg) throws IOException  
    {Logger.log(Logger.ERROR, "Error in include: " + sc + " " + msg);}
  public void sendError(int sc) throws IOException 
    {Logger.log(Logger.ERROR, "Error in include: " + sc);}
  public void sendRedirect(String location) throws IOException
    {Logger.log(Logger.ERROR, "Redirect in include: " + location);}

  public void setDateHeader(String name, long date)  {}
  public void setHeader(String name, String value) {}
  public void setIntHeader(String name, int value) {}

  /**
   * @deprecated
   */
  public void setStatus(int sc, String sm) {}
  public void setStatus(int sc) {}

  public void setLocale(Locale loc) {}
  public void setContentLength(int len) {}
  public void setContentType(String type) {}

  public void setBufferSize(int size) {}
  public void reset() throws IllegalStateException {}
  public void resetBuffer() throws IllegalStateException  {}
  public void flushBuffer() throws IOException {}
}
