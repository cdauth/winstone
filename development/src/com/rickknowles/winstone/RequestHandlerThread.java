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

import java.net.*;
import java.io.*;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * The threads to which incoming requests get allocated.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestHandlerThread implements Runnable
{
  final static int KEEP_ALIVE_TIMEOUT   = 10000;
  final static int KEEP_ALIVE_SLEEP     = 20;
  final static int KEEP_ALIVE_SLEEP_MAX = 500;

  final static int CONNECTION_TIMEOUT = 60000;

  private Thread thread;
  private Socket socket;
  private boolean continueFlag;

  private WebAppConfiguration webAppConfig;
  private Listener listener;
  private String prefix;
  private HttpConnector connector;

  /**
   * Constructor - this is called by the handler pool, and just sets up
   * for when a real request comes along.
   */
  public RequestHandlerThread(WebAppConfiguration webAppConfig, Listener listener, HttpConnector connector)
  {
    this.webAppConfig = webAppConfig;
    this.prefix = webAppConfig.getPrefix();
    this.listener = listener;
    this.connector = connector;

    // allocate a thread to run on this object
    this.thread = new Thread(this);
    this.thread.setDaemon(true);
  }

  /**
   * The main thread execution code.
   */
  public void run()
  {
    try
    {
      Logger.log(Logger.FULL_DEBUG, "New socket opened - remotePort " + this.socket.getPort());

      // Set the stream time out, so that if
      this.socket.setSoTimeout(CONNECTION_TIMEOUT);
      InputStream in = this.socket.getInputStream();
      OutputStream out = this.socket.getOutputStream();

      continueFlag = true;
      while (continueFlag)
      {
        try
        {
          long requestId = System.currentTimeMillis();
          Logger.log(Logger.FULL_DEBUG, "Starting request id: " + requestId);

          // Actually process the request
          WinstoneInputStream inData = new WinstoneInputStream(in);
          byte uriBuffer[] = inData.readLine();
          String uriLine = new String(uriBuffer);
          WinstoneRequest req = new WinstoneRequest(inData,
                                                  this.connector,
                                                  this.webAppConfig);
          this.connector.parseSocketInfo(this.socket, req);
          String servletURI = this.connector.parseURILine(uriLine, req);
          this.connector.parseHeaders(req, inData);

          int contentLength = req.getContentLength();
          if (contentLength != -1)
            inData.setContentLength(contentLength);

          // Get the URI from the servlet, check for prefix, then match it to a
          // requestDispatcher
          String path = null;
          if (this.prefix.equals(""))
            path = servletURI;
          else if (servletURI.startsWith(this.prefix))
            path = servletURI.substring(this.prefix.length());
          else
          {
            Logger.log(Logger.WARNING, "Request URL " + servletURI + " not in prefix " + this.prefix);
            WinstoneResponse rsp = new WinstoneResponse(req, out, this.connector);
            rsp.sendError(WinstoneResponse.SC_NOT_FOUND, "Request URL " + servletURI + " not found.<br><br>");
            rsp.flushBuffer();
            rsp.verifyContentLength();
            continueFlag = false;
            continue;
          }

          // Handle with the dispatcher we found
          WinstoneResponse rsp = new WinstoneResponse(req, out, this.connector);
          processRequest(req, rsp, path);

          Logger.log(Logger.FULL_DEBUG, "Finishing request id: " + requestId);
          continueFlag = !connector.closeAfterRequest(req, rsp);

          // Try keep alive if allowed
          if (continueFlag)
          {
            // Wait for some input
            long lastRequestDate = System.currentTimeMillis();
            int sleepPeriod = KEEP_ALIVE_SLEEP;
            while ((in.available() == 0) && (System.currentTimeMillis() < lastRequestDate + KEEP_ALIVE_TIMEOUT))
            {
              Logger.log(Logger.FULL_DEBUG, "Sleep time: (port " +
                          this.socket.getPort() + ") - " + (lastRequestDate +
                          KEEP_ALIVE_TIMEOUT - System.currentTimeMillis()) +
                          "ms - sleep=" + sleepPeriod + "ms");
              Thread.currentThread().sleep(sleepPeriod);
              sleepPeriod = Math.min(sleepPeriod * 2, KEEP_ALIVE_SLEEP_MAX);
            }
            if (in.available() == 0)
              continueFlag = false;
          }
        }
        catch (InterruptedIOException errIO)
        {
          continueFlag = false;
          Logger.log(Logger.FULL_DEBUG, "Socket read timed out - exiting request handler thread");
        }
        catch (SocketException errIO) {continueFlag = false;}
      }
      in.close();
      out.close();
      this.socket.close();
    }
    catch (Throwable err)
    {
      try {this.socket.close();} catch (IOException errIO) {}
      Logger.log(Logger.ERROR, "Error within request handler thread", err);
    }
    Logger.log(Logger.FULL_DEBUG, "Closed socket - remotePort " + this.socket.getPort());
    this.socket = null;

    // Before finishing, allocate another thread to run on this object
    this.thread = new Thread(this);
    this.thread.setDaemon(true);
    this.listener.releaseRequestHandler(this);
  }

  private void processRequest(WinstoneRequest req, WinstoneResponse rsp, String path)
    throws IOException, ServletException
  {
    try
    {
      javax.servlet.RequestDispatcher rd = this.webAppConfig.getRequestDispatcher(path);
      if (rd != null)
      {
        Logger.log(Logger.FULL_DEBUG, "Processing with RD: " + ((RequestDispatcher) rd).getName());
        rd.forward(new HttpServletRequestWrapper(req), new HttpServletResponseWrapper(rsp));
      }
      else
        Logger.log(Logger.ERROR, "ERROR: Null request handler");
    }
    catch (Throwable err)
    {
      Logger.log(Logger.INFO, "Untrapped Error in Servlet", err);

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      pw.print("<h3>Untrapped error in servlet</h3><br>");
      pw.print("<pre>");
      err.printStackTrace(pw);
      pw.print("</pre>");

      rsp.resetBuffer();
      rsp.sendError(rsp.SC_INTERNAL_SERVER_ERROR, sw.toString());
    }
    rsp.flushBuffer();
    rsp.verifyContentLength();
  }

  /**
   * Assign a socket to the handler
   */
  public void commenceRequestHandling(Socket s)
  {
    this.socket = s;
    this.thread.start();
  }

  public void destroy() {this.continueFlag = false;}
}
