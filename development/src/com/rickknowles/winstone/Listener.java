
// Copyright (c) 2003 
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

 