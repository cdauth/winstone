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

import java.net.Socket;
import java.net.SocketException;
import java.io.*;

/**
 * Interface that defines the necessary methods for being a connection listener
 * within winstone.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface Listener
{
  /**
   * Interrupts the listener thread. This will trigger a listener shutdown once
   * the so timeout has passed.
   */
  public void destroy();

  /**
   * Called by the request handler thread, because it needs specific setup code
   * for this connection's protocol (ie construction of request/response objects,
   * in/out streams, etc).
   */
  public void allocateRequestResponse(Socket socket, InputStream inSocket,
    OutputStream outSocket, RequestHandlerThread handler, boolean iAmFirst)
    throws SocketException, IOException;

  /**
   * Called by the request handler thread, because it needs specific shutdown code
   * for this connection's protocol (ie releasing input/output streams, etc).
   */
  public void deallocateRequestResponse(RequestHandlerThread handler,
    WinstoneRequest req, WinstoneResponse rsp, WinstoneInputStream inData,
    WinstoneOutputStream outData)
    throws IOException;

  /**
   * Called by the request handler thread, because it needs specific shutdown code
   * for this connection's protocol if the keep-alive period expires (ie closing
   * sockets, etc).
   */
  public String parseURI(WinstoneRequest req, WinstoneInputStream inData,
    Socket socket, boolean iAmFirst) throws IOException;

  /**
   * Called by the request handler thread, because it needs specific shutdown code
   * for this connection's protocol if the keep-alive period expires (ie closing
   * sockets, etc).
   */
  public void releaseSocket(Socket socket, InputStream inSocket, OutputStream outSocket)
    throws IOException;

  /**
   * Tries to wait for extra requests on the same socket. If any are found
   * before the timeout expires, it exits with a true, indicating a new
   * request is waiting. If the timeout expires, return a false, instructing
   * the handler thread to begin shutting down the socket and relase itself.
   */
  public boolean processKeepAlive(WinstoneRequest request,
                                  WinstoneResponse response,
                                  InputStream inSocket,
                                  HttpProtocol protocol)
    throws IOException, InterruptedException;
}

 