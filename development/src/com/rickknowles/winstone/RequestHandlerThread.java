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

  private Socket socket;
  private boolean killMe;

  private WebAppConfiguration webAppConfig;
  private Launcher launcher;
  private String prefix;
  private HttpProtocol protocol;

  private WinstoneResourceBundle resources;

  /**
   * Constructor - this is called by the handler pool, and just sets up
   * for when a real request comes along.
   */
  public RequestHandlerThread(WebAppConfiguration webAppConfig, Launcher launcher,
                              WinstoneResourceBundle resources, int threadIndex)
  {
    this.resources = resources;
    this.webAppConfig = webAppConfig;
    this.prefix = webAppConfig.getPrefix();
    this.launcher = launcher;
    this.killMe = false;

    // allocate a thread to run on this object
    Thread thread = new Thread(this, resources.getString("RequestHandlerThread.ThreadName", "[#threadNo]", "" + threadIndex));
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * The main thread execution code. This will
   */
  public void run()
  {
    while (!this.killMe)
    {
      Logger.log(Logger.FULL_DEBUG, this.resources.getString("RequestHandlerThread.EnterWaitState", "[#threadName]", Thread.currentThread().getName()));

      // Suspend this thread until we get assigned and woken up
      try
        {synchronized(this) {wait();}}
      catch (Throwable err) {}
      Logger.log(Logger.FULL_DEBUG, this.resources.getString("RequestHandlerThread.WakingUp", "[#threadName]", Thread.currentThread().getName()));

      // Check for thread destroy
      if (this.killMe) break;

      // Start request processing
      int thisPort = this.socket.getPort();
      try
      {
        Logger.log(Logger.FULL_DEBUG, resources.getString("RequestHandlerThread.OpenedPort") + thisPort);

        // Set the stream time out, so that if the client hangs, we don't lock up too
        this.socket.setSoTimeout(CONNECTION_TIMEOUT);
        InputStream in = this.socket.getInputStream();
        OutputStream out = this.socket.getOutputStream();

        boolean continueFlag = true;
        while (continueFlag)
        {
          try
          {
            long requestId = System.currentTimeMillis();
            Logger.log(Logger.FULL_DEBUG, resources.getString("RequestHandlerThread.StartRequest") + requestId);

            // Actually process the request
            WinstoneInputStream inData = new WinstoneInputStream(in, this.resources);
            byte uriBuffer[] = inData.readLine();
            String uriLine = new String(uriBuffer);

            // Make request / response
            WinstoneRequest req = new WinstoneRequest(inData, this.protocol, this.webAppConfig, this.resources);
            WinstoneResponse rsp = new WinstoneResponse(req, out, this.protocol, resources);

            this.protocol.parseSocketInfo(this.socket, req);
            String servletURI = this.protocol.parseURILine(uriLine, req);
            this.protocol.parseHeaders(req, inData);

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
              Logger.log(Logger.WARNING,
                         resources.getString("RequestHandlerThread.NotInPrefix",
                                             "[#url]", servletURI,
                                             "[#prefix]", this.prefix));
              rsp.sendError(WinstoneResponse.SC_NOT_FOUND,
                            resources.getString("RequestHandlerThread.NotInPrefixPage",
                                                "[#url]", servletURI));
              rsp.flushBuffer();
              rsp.verifyContentLength();
              continueFlag = false;
              continue;
            }

            // Handle with the dispatcher we found
            processRequest(req, rsp, path);

            Logger.log(Logger.FULL_DEBUG, resources.getString("RequestHandlerThread.StartRequest") + requestId);
            continueFlag = !protocol.closeAfterRequest(req, rsp);

            // Try keep alive if allowed
            if (continueFlag)
            {
              // Wait for some input
              long lastRequestDate = System.currentTimeMillis();
              while ((in.available() == 0) &&
                    (System.currentTimeMillis() < lastRequestDate + KEEP_ALIVE_TIMEOUT))
                Thread.currentThread().sleep(KEEP_ALIVE_SLEEP);
              if (in.available() == 0)
                continueFlag = false;
            }
          }
          catch (InterruptedIOException errIO)
          {
            continueFlag = false;
            Logger.log(Logger.FULL_DEBUG, resources.getString("RequestHandlerThread.SocketTimeout"));
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
        Logger.log(Logger.ERROR, resources.getString("RequestHandlerThread.RequestError"), err);
      }
      Logger.log(Logger.FULL_DEBUG, resources.getString("RequestHandlerThread.ClosedPort") + thisPort);
      this.socket = null;
      this.launcher.releaseRequestHandler(this);
    }
    Logger.log(Logger.FULL_DEBUG, this.resources.getString("RequestHandlerThread.ThreadExit", "[#threadName]", Thread.currentThread().getName()));
  }

  /**
   * Actually process the request. This takes the request and response, and feeds
   * them to the desired servlet, which then processes them or throws them off to
   * another servlet.
   */
  private void processRequest(WinstoneRequest req, WinstoneResponse rsp, String path)
    throws IOException, ServletException
  {
    try
    {
      javax.servlet.RequestDispatcher rd = this.webAppConfig.getRequestDispatcher(path);
      if (rd != null)
      {
        Logger.log(Logger.FULL_DEBUG, resources.getString("RequestHandlerThread.HandlingRD") + ((RequestDispatcher) rd).getName());
        rd.forward(new HttpServletRequestWrapper(req), new HttpServletResponseWrapper(rsp));
      }
      else
        Logger.log(Logger.ERROR, resources.getString("RequestHandlerThread.NullRD"));
    }
    catch (Throwable err)
    {
      Logger.log(Logger.WARNING, resources.getString("RequestHandlerThread.UntrappedError"), err);

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      err.printStackTrace(pw);
      rsp.resetBuffer();
      rsp.sendError(rsp.SC_INTERNAL_SERVER_ERROR, resources.getString("RequestHandlerThread.ServletExceptionPage", "[#stackTrace]", sw.toString()));
    }
    rsp.flushBuffer();
    rsp.verifyContentLength();
  }

  /**
   * Assign a socket to the handler
   */
  public void commenceRequestHandling(Socket socket, HttpProtocol protocol)
  {
    this.socket = socket;
    this.protocol = protocol;
    synchronized (this) {notifyAll();}
  }

  /**
   * Trigger the thread destruction for this handler
   */
  public void destroy()
  {
    this.killMe = true;
    synchronized (this) {notifyAll();}
  }
}

