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
package winstone.auth; 

import java.io.*;

import winstone.*;

/**
 * This class has been included so that we can handle caching a request object
 * without taking it out of circulation. This class just caches and replays the
 * crucial data from a request, making sure the original request is ok to be 
 * returned to the pool. 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class CachedRequest extends WinstoneRequest
{
  /**
   * Constructor - dumps the input request contents here, so that they can 
   * be retrieved by transferContent() later.
   * 
   * @param resources Resource bundle for error messages
   * @param request The source request to copy from
   * @throws IOException If there are any problems with reading from the stream
   */
  public CachedRequest(WinstoneRequest request, WinstoneResourceBundle resources) 
    throws IOException
  {
    super(resources);
    
    // Stash the relevant pieces of info
    this.attributes.putAll(request.getAttributes());
    this.parameters.putAll((request.getParameters()));
    this.locales = request.getListLocales();
    this.method = request.getMethod();
    this.scheme = request.getScheme();
    this.serverName = request.getServerName();
    this.requestURI = request.getRequestURI();
    this.servletPath = request.getServletPath();
    this.pathInfo = request.getPathInfo();
    this.queryString = request.getQueryString();
    this.protocol = request.getProtocol();
    this.contentLength = request.getContentLength();
    this.contentType = request.getContentType();
    this.serverPort = request.getServerPort();
    this.sessionCookie = request.getSessionCookie();
    this.encoding = request.getEncoding();
    this.parsedParameters = request.getParsedParameters();
    InputStream in = request.getInputStream();
    ByteArrayOutputStream inBackup = new ByteArrayOutputStream();
    if (this.method.equals("POST"))
    {
      byte buffer[] = new byte[8192];
      int inputCounter = 0;
      int readBytes = in.read(buffer);
      while ((readBytes != -1) && 
             ((inputCounter < this.contentLength) || (this.contentLength == -1)))
      {
        inputCounter += readBytes;
        inBackup.write(buffer, 0, readBytes);
        readBytes = in.read(buffer);
      }
    }
    this.inputData = new WinstoneInputStream(inBackup.toByteArray(), resources);
  }
  
  /**
   * Copies the contents we stashed earlier into a new request
   * @param request The request to write to
   */
  public void transferContent(WinstoneRequest request)
  {
    request.getAttributes().putAll(this.attributes);
    request.getParameters().putAll(this.parameters);
    request.setLocales(this.locales);
    request.setMethod(this.method);
    request.setScheme(this.scheme);
    request.setServerName(this.serverName);
    request.setRequestURI(this.requestURI);
    request.setServletPath(this.servletPath);
    request.setPathInfo(this.pathInfo);
    request.setQueryString(this.queryString);
    request.setProtocol(this.protocol);
    request.setContentLength(this.contentLength);
    request.setContentType(this.contentType);
    request.setServerPort(this.serverPort);
    request.setSessionCookie(this.sessionCookie);
    request.setEncoding(this.encoding);
    request.setParsedParameters(this.parsedParameters);
    request.setInputStream(this.inputData);
  }

}
