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
package winstone.ajp13;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.cert.*;

import winstone.*;

/**
 * Implements the main listener daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Ajp13Listener implements Listener, Runnable
{
  private int LISTENER_TIMEOUT = 5000; // every 5s reset the listener socket
  private int DEFAULT_PORT = 8009;
  private int CONNECTION_TIMEOUT = 60000;

  private int KEEP_ALIVE_TIMEOUT   = -1;
  private int KEEP_ALIVE_SLEEP     = 50;
  private int KEEP_ALIVE_SLEEP_MAX = 500;

  private static final String LOCAL_RESOURCE_FILE    = "winstone.ajp13.LocalStrings";

  private WinstoneResourceBundle mainResources;
  private WinstoneResourceBundle localResources;
  private ObjectPool objectPool;
  private int listenPort;
  private boolean interrupted;

  /**
   * Constructor
   */
  public Ajp13Listener(Map args, WinstoneResourceBundle resources, ObjectPool objectPool)
  {
    // Load resources
    this.mainResources = resources;
    this.localResources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    this.objectPool = objectPool;

    //this.arguments = args;
    this.listenPort = (args.get("ajp13Port") == null ? DEFAULT_PORT
                          : Integer.parseInt((String) args.get("ajp13Port")));

    if (this.listenPort < 0)
      throw new WinstoneException("disabling ajp13 connector");

    this.interrupted = false;

    // Start me running
    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * The main run method. This handles the normal thread processing.
   */
  public void run()
  {
    try
    {
      ServerSocket ss = new ServerSocket(this.listenPort);
      ss.setSoTimeout(LISTENER_TIMEOUT);
      Logger.log(Logger.INFO, localResources.getString("Ajp13Listener.StartupOK",
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
          this.objectPool.handleRequest(s, this);
      }

      // Close server socket
      ss.close();
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, localResources.getString("Ajp13Listener.ShutdownError"), err);}

    Logger.log(Logger.INFO, localResources.getString("Ajp13Listener.ShutdownOK"));
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
    WinstoneRequest req = this.objectPool.getRequestFromPool();
    WinstoneResponse rsp = this.objectPool.getResponseFromPool();
    req.setListener(this);
    rsp.setRequest(req);
    rsp.updateContentTypeHeader("text/html");

    if (iAmFirst || (KEEP_ALIVE_TIMEOUT == -1))
      socket.setSoTimeout(CONNECTION_TIMEOUT);
    else
      socket.setSoTimeout(KEEP_ALIVE_TIMEOUT);
    Ajp13IncomingPacket headers = new Ajp13IncomingPacket(inSocket, localResources, handler);
    socket.setSoTimeout(CONNECTION_TIMEOUT);

    if (headers.getPacketLength() > 0)
    {
      headers.parsePacket("8859_1");
      parseSocketInfo(headers, req);
      String servletURI = parseURILine(headers, req);
      req.parseHeaders(Arrays.asList(headers.getHeaders()));

      // If content-length present and non-zero, download the other packets
      WinstoneInputStream inData = null;
      int contentLength = req.getContentLength();
      if (contentLength > 0)
      {
        byte bodyContent[] = new byte[contentLength];
        int position = 0;
        while (position < contentLength)
        {
          outSocket.write(getBodyRequestPacket(Math.min(contentLength - position, 8184)));
          position = getBodyResponsePacket(inSocket, bodyContent, position);
          Logger.log(Logger.FULL_DEBUG,
          localResources.getString("Ajp13Listener.ReadBodyProgress",
                    "[#position]", "" + position, "[#total]", "" + contentLength));

        }
        inData = new WinstoneInputStream(bodyContent, mainResources);
        inData.setContentLength(contentLength);
      }
      else
        inData = new WinstoneInputStream(new byte[0], mainResources);
      req.setInputStream(inData);

      // Build input/output streams, plus request/response
      WinstoneOutputStream outData = new Ajp13OutputStream(socket.getOutputStream(),
          mainResources, localResources, "8859_1");
      outData.setResponse(rsp);
      rsp.setOutputStream(outData);

      // Set the handler's member variables so it can execute the servlet
      handler.setRequest(req);
      handler.setResponse(rsp);
      handler.setInStream(inData);
      handler.setOutStream(outData);
    }
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
    if (req != null)
      this.objectPool.releaseRequestToPool(req);
    if (rsp != null)
      this.objectPool.releaseResponseToPool(rsp);
  }

  /**
   * This is kind of a hack, since we have already parsed the uri to get the
   * input stream. Just pass back the request uri
   */
  public String parseURI(RequestHandlerThread handler, WinstoneRequest req, 
    WinstoneInputStream inData, Socket socket, boolean iAmFirst) throws IOException
    {return req.getServletPath();}

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
    //Logger.log(Logger.FULL_DEBUG, "Releasing socket: " + Thread.currentThread().getName());
    inSocket.close();
    outSocket.close();
    socket.close();
  }

  /**
   * Extract the header details relating to socket stuff from the ajp13 header packet
   */
  private void parseSocketInfo(Ajp13IncomingPacket headers, WinstoneRequest req)
  {
    req.setServerPort(headers.getServerPort());
    req.setRemoteIP(headers.getRemoteAddress());
    req.setServerName(headers.getServerName());
    req.setLocalPort(headers.getServerPort());
    req.setLocalAddr(headers.getServerName());
    req.setRemoteIP(headers.getRemoteAddress());
    if ((headers.getRemoteHost() != null) && !headers.getRemoteHost().equals(""))
      req.setRemoteName(headers.getRemoteHost());
    else
      req.setRemoteName(headers.getRemoteAddress());
    req.setScheme(headers.isSSL() ? "https" : "http");
    req.setIsSecure(headers.isSSL());
  }

  /**
   * Extract the header details relating to protocol, uri, etc from the ajp13
   * header packet
   */
  private String parseURILine(Ajp13IncomingPacket headers, WinstoneRequest req)
    throws UnsupportedEncodingException
  {
    req.setMethod(headers.getMethod());
    req.setProtocol(headers.getProtocol());
    //req.setServletPath(headers.getURI());
    //req.setRequestURI(headers.getURI());

    // Get query string if supplied
    for (Iterator i = headers.getAttributes().keySet().iterator(); i.hasNext(); )
    {
      String attName = (String) i.next();
      if (attName.equals("query_string"))
      {
        String qs = (String) headers.getAttributes().get("query_string");
        req.setQueryString(qs);
        //req.getParameters().putAll(WinstoneRequest.extractParameters(qs, req.getEncoding(), mainResources));
        req.setRequestURI(headers.getURI() + "?" + qs);
      }
      else if (attName.equals("ssl_cert"))
      {
        String certValue = (String) headers.getAttributes().get("ssl_cert");
        InputStream certStream = new ByteArrayInputStream(certValue.getBytes("8859_1"));
        X509Certificate certificateArray[] = new X509Certificate[1];
        try
          {certificateArray[0] = (X509Certificate) CertificateFactory.getInstance("X.509")
                                                    .generateCertificate(certStream);}
        catch (CertificateException err)
          {Logger.log(Logger.DEBUG, "Skipping invalid SSL certificate: " + certValue);}
        req.setAttribute("javax.servlet.request.X509Certificate", certificateArray);
        req.setIsSecure(true);
      }
      else if (attName.equals("ssl_cipher"))
      {
        req.setAttribute("javax.servlet.request.cipher_suite",
                         headers.getAttributes().get("ssl_cipher"));
        req.setIsSecure(true);
      }
      else if (attName.equals("ssl_session"))
      {
        req.setAttribute("javax.servlet.request.ssl_session",
                         headers.getAttributes().get("ssl_session"));
        req.setIsSecure(true);
      }
    }
    return headers.getURI();

  }  

  /**
   * Tries to wait for extra requests on the same socket. If any are found
   * before the timeout expires, it exits with a true, indicating a new
   * request is waiting. If the timeout expires, return a false, instructing
   * the handler thread to begin shutting down the socket and relase itself.
   */
  public boolean processKeepAlive(WinstoneRequest request,
                                  WinstoneResponse response,
                                  InputStream inSocket)
    throws IOException, InterruptedException
    {return true;}

  /**
   * Build the packet needed for asking for a body chunk
   */
  private byte[] getBodyRequestPacket(int desiredPacketLength)
  {
    byte getBodyRequestPacket[] = new byte[] {0x41, 0x42, 0x00, 0x03, 0x06, 0x00, 0x00};
    Ajp13OutputStream.setIntBlock(desiredPacketLength, getBodyRequestPacket, 5);
    return getBodyRequestPacket;
  }

  /**
   * Process the server response to a get_body_chunk request. This loads the
   * packet from the stream, and unpacks it into the buffer at the right place.
   */
  private int getBodyResponsePacket(InputStream in, byte buffer[], int offset)
    throws IOException
  {
    // Get the incoming packet flag
    byte headerBuffer[] = new byte[4];
    int headerBytesRead = in.read(headerBuffer);
    if (headerBytesRead != 4)
      throw new WinstoneException(localResources.getString("Ajp13Listener.InvalidHeader"));
    else if ((headerBuffer[0] != 0x12) || (headerBuffer[1] != 0x34))
      throw new WinstoneException(localResources.getString("Ajp13Listener.InvalidHeader"));

    // Read in the whole packet
    int packetLength = ((headerBuffer[2] & 0xFF) << 8) + (headerBuffer[3] & 0xFF);
    if (packetLength == 0)
      return offset;

    // Look for packet length
    byte bodyLengthBuffer[] = new byte[2];
    in.read(bodyLengthBuffer);
    int bodyLength = ((bodyLengthBuffer[0] & 0xFF) << 8) + (bodyLengthBuffer[1] & 0xFF);
    int packetBytesRead = in.read(buffer, offset, bodyLength);

    if (packetBytesRead < bodyLength)
      throw new WinstoneException(localResources.getString("Ajp13Listener.ShortPacket"));
    else
      return packetBytesRead + offset;
  }

  /**
   * Useful method for dumping out the contents of a packet in hex form
   */
/*
  public static void packetDump(byte packetBytes[], int packetLength)
  {
    String dump = "";
    for (int n = 0; n < packetLength; n+=16)
    {
      String line = Integer.toHexString((n >> 4) & 0xF) + "0:";
      for (int j = 0; j < Math.min(packetLength - n, 16); j++)
        line = line + " " + ((packetBytes[n + j] & 0xFF) < 16 ? "0" : "") +
                        Integer.toHexString(packetBytes[n + j] & 0xFF);

      line = line + "    ";
      for (int j = 0; j < Math.min(packetLength - n, 16); j++)
      {
        byte me = (byte) (packetBytes[n + j] & 0xFF);
        line = line + (((me > 32) && (me < 123)) ? (char) me : '.');
      }
      dump = dump + line + "\r\n";
    }
    System.out.println(dump);
  }
*/
}

