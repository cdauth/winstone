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
import java.net.*;
import java.util.*;

/**
 * Implements the main listener daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpListener implements Listener, Runnable
{
  private int LISTENER_TIMEOUT = 500; // every 500ms reset the listener socket
  private int DEFAULT_PORT = 8080;
  private int CONNECTION_TIMEOUT = 60000;
  private boolean DEFAULT_HNL = true;

  private int KEEP_ALIVE_TIMEOUT   = 10000;
  private int KEEP_ALIVE_SLEEP     = 20;
  private int KEEP_ALIVE_SLEEP_MAX = 500;

  private WinstoneResourceBundle resources;
  private Launcher launcher;
  private boolean doHostnameLookups;
  private int listenPort;
  private HttpProtocol protocol;
  private boolean interrupted;

  protected HttpListener() {}

  /**
   * Constructor
   */
  public HttpListener(Map args, WinstoneResourceBundle resources, Launcher launcher)
  {
    // Load resources
    this.resources = resources;
    this.launcher = launcher;
    this.listenPort = (args.get("httpPort") == null ? DEFAULT_PORT
                          : Integer.parseInt((String) args.get("httpPort")));

    if (this.listenPort < 0)
      throw new WinstoneException("disabling http connector");

    String hnl = (String) args.get("httpDoHostnameLookups");
    this.doHostnameLookups = (hnl == null ? DEFAULT_HNL : (hnl.equalsIgnoreCase("yes") || hnl.equalsIgnoreCase("true")));
    this.protocol = new HttpProtocol(this.resources);
    this.interrupted = false;

    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * The main run method. This continually listens for incoming connections,
   * and allocates any that it finds to a request handler thread, before going
   * back to listen again.
   */
  public void run()
  {
    try
    {
      ServerSocket ss = new ServerSocket(this.listenPort);
      ss.setSoTimeout(LISTENER_TIMEOUT);
      Logger.log(Logger.INFO, resources.getString("HttpListener.StartupOK",
                              "[#port]", this.listenPort + ""));

      // Enter the main loop
      while (!interrupted)
      {
        // Get the listener
        Socket s = null;
        try
          {s = ss.accept();}
        catch (java.io.InterruptedIOException err) {s = null;}

        // if we actually got a socket, process it. Otherwise go around again
        if (s != null)
          this.launcher.handleRequest(s, this, this.protocol);
      }

      // Close server socket
      ss.close();
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, resources.getString("HttpListener.ShutdownError"), err);}

    Logger.log(Logger.INFO, resources.getString("HttpListener.ShutdownOK"));
  }

  /**
   * Interrupts the listener thread. This will trigger a listener shutdown once
   * the so timeout has passed.
   */
  public void destroy() {this.interrupted = true;}

  /**
   * Called by the request handler thread, because it needs specific setup code
   * for this connection's protocol (ie construction of request/response objects,
   * in/out streams, etc).
   *
   * This implementation parses incoming AJP13 packets, and builds an outputstream
   * that is capable of writing back the response in AJP13 packets.
   */
  public void allocateRequestResponse(Socket socket, InputStream inSocket,
    OutputStream outSocket, RequestHandlerThread handler, boolean iAmFirst)
    throws SocketException, IOException
  {
    Logger.log(Logger.FULL_DEBUG, "Allocating request/response: " + Thread.currentThread().getName());
    socket.setSoTimeout(CONNECTION_TIMEOUT);

    // Build input/output streams, plus request/response
    WinstoneInputStream inData = new WinstoneInputStream(inSocket, this.resources);
    WinstoneOutputStream outData = new WinstoneOutputStream(outSocket, resources, this.protocol);
    WinstoneRequest req = new WinstoneRequest(this.protocol, this, this.resources);
    WinstoneResponse rsp = new WinstoneResponse(req, this.protocol, resources);
    outData.setResponse(rsp);
    req.setInputStream(inData);
    rsp.setOutputStream(outData);

    // Set the handler's member variables so it can execute the servlet
    handler.setRequest(req);
    handler.setResponse(rsp);
    handler.setInStream(inData);
    handler.setOutStream(outData);
  }

  /**
   * Called by the request handler thread, because it needs specific shutdown code
   * for this connection's protocol (ie releasing input/output streams, etc).
   */
  public void deallocateRequestResponse(RequestHandlerThread handler,
    WinstoneRequest req, WinstoneResponse rsp, WinstoneInputStream inData,
    WinstoneOutputStream outData)
    throws IOException
  {
    handler.setInStream(null);
    handler.setOutStream(null);
    handler.setRequest(null);
    handler.setResponse(null);
  }

  public String parseURI(WinstoneRequest req, WinstoneInputStream inData,
    Socket socket, boolean iAmFirst) throws IOException
  {
    parseSocketInfo(socket, req);

    // Read the header line (because this is the first line of the request,
    // apply keep-alive timeouts to it if we are not the first request)
    if (!iAmFirst)
      socket.setSoTimeout(KEEP_ALIVE_TIMEOUT);
    byte uriBuffer[] = inData.readLine();
    socket.setSoTimeout(CONNECTION_TIMEOUT);

    // Get header data (eg protocol, method, uri, headers, etc)
    String uriLine = new String(uriBuffer);
    String servletURI = this.protocol.parseURILine(uriLine, req);
    this.protocol.parseHeaders(req, inData);
    int contentLength = req.getContentLength();
    if (contentLength != -1)
      inData.setContentLength(contentLength);
    return servletURI;
  }

  /**
   * Called by the request handler thread, because it needs specific shutdown code
   * for this connection's protocol if the keep-alive period expires (ie closing
   * sockets, etc).
   *
   * This implementation simply shuts down the socket and streams.
   */
  public void releaseSocket(Socket socket, InputStream inSocket, OutputStream outSocket)
    throws IOException
  {
    Logger.log(Logger.FULL_DEBUG, "Releasing socket: " + Thread.currentThread().getName());
    inSocket.close();
    outSocket.close();
    socket.close();
  }

  private void parseSocketInfo(Socket socket, WinstoneRequest req)
  {
    req.setScheme("http");
    req.setServerPort(socket.getLocalPort());
    req.setRemoteIP(socket.getInetAddress().getHostAddress());
    if (this.doHostnameLookups)
    {
      req.setServerName(socket.getLocalAddress().getHostName());
      req.setRemoteName(socket.getInetAddress().getHostName());
    }
    else
    {
      req.setServerName(socket.getLocalAddress().getHostAddress());
      req.setRemoteName(socket.getInetAddress().getHostAddress());
    }
  }

  /**
   * Tries to wait for extra requests on the same socket. If any are found
   * before the timeout expires, it exits with a true, indicating a new
   * request is waiting. If the protocol does not support keep-alives, or
   * the request instructed us to close the connection, or the timeout expires,
   * return a false, instructing the handler thread to begin shutting down the
   * socket and relase itself. 
   */
  public boolean processKeepAlive(WinstoneRequest request,
                                  WinstoneResponse response,
                                  InputStream inSocket,
                                  HttpProtocol protocol)
    throws IOException, InterruptedException
  {
    // Try keep alive if allowed
    boolean continueFlag = !protocol.closeAfterRequest(request, response);
    return continueFlag;
  }

}

