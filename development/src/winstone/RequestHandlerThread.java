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

import java.net.*;
import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;

/**
 * The threads to which incoming requests get allocated.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestHandlerThread implements Runnable
{
  private boolean interrupted;
  private Thread thread;

  private WebAppConfiguration webAppConfig;
  private ObjectPool objectPool;
  private String prefix;

  private WinstoneInputStream inData;
  private WinstoneOutputStream outData;
  private WinstoneRequest req;
  private WinstoneResponse rsp;
  private Listener listener;
  private Socket socket;
  private String threadName;
  private WinstoneResourceBundle resources;
  private long requestStartTime;

  public Object startupMonitor = new Boolean(true);
  private Object processingMonitor = new Boolean(true);

  /**
   * Constructor - this is called by the handler pool, and just sets up
   * for when a real request comes along.
   */
  public RequestHandlerThread(WebAppConfiguration webAppConfig, ObjectPool objectPool,
                              WinstoneResourceBundle resources, int threadIndex)
  {
    this.resources = resources;
    this.webAppConfig = webAppConfig;
    this.prefix = webAppConfig.getPrefix();
    this.objectPool = objectPool;
    this.interrupted = false;
    this.threadName = resources.getString("RequestHandlerThread.ThreadName", "" + threadIndex);

    // allocate a thread to run on this object
    this.thread = new Thread(this, threadName);
    this.thread.setDaemon(true);
  }

  /**
   * The main thread execution code.
   */
  public void run()
  {
    ServletRequestListener requestListeners[] = 
        this.webAppConfig.getRequestListeners();
    ServletRequestAttributeListener requestAttributeListeners[] = 
        this.webAppConfig.getRequestAttributeListeners();
    while (!interrupted)
    {
      // Start request processing
      InputStream inSocket = null;
      OutputStream outSocket = null;
      boolean iAmFirst = true;
      try
      {
        // Get input/output streams
        inSocket = socket.getInputStream();
        outSocket = socket.getOutputStream();

        boolean continueFlag = true;
        while (continueFlag && !interrupted)
        {
          try
          {
            long requestId = System.currentTimeMillis();
            this.listener.allocateRequestResponse(socket, inSocket, outSocket, this, iAmFirst);
            if (this.req == null)
            {
              // Dead request - happens sometimes with ajp13  - discard
              this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
              continue;
            }
            String servletURI = this.listener.parseURI(this, this.req, this.rsp, this.inData, this.socket, iAmFirst);
            long headerParseTime = getRequestProcessTime();
            iAmFirst = false;

            Logger.log(Logger.FULL_DEBUG, resources, "RequestHandlerThread.StartRequest",
              new String[] {"" + requestId, Thread.currentThread().getName()});

            // Get the URI from the request, check for prefix, then match it to a
            // requestDispatcher
            String path = null;
            if (this.prefix.equals(""))
              path = servletURI;
            else if (servletURI.startsWith(this.prefix))
              path = servletURI.substring(this.prefix.length());
            else
            {
              Logger.log(Logger.WARNING, resources, 
                  "RequestHandlerThread.NotInPrefix", new String[] {servletURI, this.prefix});
              rsp.sendError(WinstoneResponse.SC_NOT_FOUND,
                            resources.getString("RequestHandlerThread.NotInPrefixPage", servletURI));
              rsp.flushBuffer();
              rsp.verifyContentLength();

              // Process keep-alive
              continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);
              this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
              Logger.log(Logger.FULL_DEBUG, resources, "RequestHandlerThread.FinishRequest",
                  new String[] {"" + requestId, Thread.currentThread().getName()});
              Logger.log(Logger.SPEED, resources, "RequestHandlerThread.RequestTime",
                  new String[] {servletURI, "" + headerParseTime,
                  "" + getRequestProcessTime()});
              continue;
            }

            // Now we've verified it's in the right webapp, send request in scope notify
            for (int n = 0; n < requestListeners.length; n++)
              requestListeners[n].requestInitialized(new ServletRequestEvent(this.webAppConfig, req));
            
            // Lookup a dispatcher, then process with it
            req.setWebAppConfig(this.webAppConfig);
            rsp.setWebAppConfig(this.webAppConfig);
            req.setRequestAttributeListeners(requestAttributeListeners);
            processRequest(req, rsp, path);
            this.outData.finishResponse();
            this.inData.finishRequest();

            Logger.log(Logger.FULL_DEBUG, resources, "RequestHandlerThread.FinishRequest",
              new String[] {"" + requestId, Thread.currentThread().getName()});

            // Process keep-alive
            continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);

            // Set last accessed time on session as start of this request
            WinstoneSession session = (WinstoneSession) req.getSession(false);
            if (session != null)
              session.setLastAccessedDate(this.requestStartTime);
            
            // send request listener notifies
            for (int n = 0; n < requestListeners.length; n++)
              requestListeners[n].requestDestroyed(new ServletRequestEvent(this.webAppConfig, req));

            req.setWebAppConfig(null);
            rsp.setWebAppConfig(null);
            req.setRequestAttributeListeners(null);

            this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
            Logger.log(Logger.SPEED, resources, "RequestHandlerThread.RequestTime",
              new String[] {servletURI, "" + headerParseTime,
              "" + getRequestProcessTime()});
          }
          catch (InterruptedIOException errIO)
          {
            continueFlag = false;
            Logger.log(Logger.FULL_DEBUG, resources, "RequestHandlerThread.SocketTimeout");
          }
          catch (SocketException errIO) {continueFlag = false;}
        }
        this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
        this.listener.releaseSocket(this.socket, inSocket, outSocket); //shut sockets
      }
      catch (Throwable err)
      {
        try
        {
          this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
          this.listener.releaseSocket(this.socket, inSocket, outSocket); //shut sockets
        }
        catch (Throwable errClose) {}
        Logger.log(Logger.ERROR, resources, "RequestHandlerThread.RequestError", err);
      }

      this.objectPool.releaseRequestHandler(this);

      // Suspend this thread until we get assigned and woken up
      Logger.log(Logger.FULL_DEBUG, this.resources, "RequestHandlerThread.EnterWaitState", Thread.currentThread().getName());
      try
        {synchronized(this.processingMonitor) {this.processingMonitor.wait();}}
      catch (Throwable err) {}
      Logger.log(Logger.FULL_DEBUG, this.resources, "RequestHandlerThread.WakingUp", Thread.currentThread().getName());

      // Check for thread destroy
      if (interrupted) break;

    }
    Logger.log(Logger.FULL_DEBUG, this.resources, "RequestHandlerThread.ThreadExit", Thread.currentThread().getName());
  }

  /**
   * Actually process the request. This takes the request and response, and feeds
   * them to the desired servlet, which then processes them or throws them off to
   * another servlet.
   */
  private void processRequest(WinstoneRequest req, WinstoneResponse rsp, String path)
    throws IOException, ServletException
  {
    RequestDispatcher rd = null;
    try
    {
      rd = this.webAppConfig.getInitialDispatcher(path, req, rsp);
      
      // Null RD means we have been redirected to a welcome page, so ignore quietly
      if (rd != null)
      {
        Logger.log(Logger.FULL_DEBUG, resources, "RequestHandlerThread.HandlingRD", ((RequestDispatcher) rd).getName());
        rd.forward(req, rsp);
      }
    }
    catch (Throwable err)
    {
      Logger.log(Logger.WARNING, resources, "RequestHandlerThread.UntrappedError", err);
      rsp.resetBuffer();
      rsp.sendUntrappedError(err, req, rd != null ? rd.getName() : null);
    }
    rsp.flushBuffer();
    rsp.verifyContentLength();
  }

  /**
   * Assign a socket to the handler
   */
  public void commenceRequestHandling(Socket socket,
                                      Listener listener)
  {
    this.listener = listener;
    this.socket = socket;
    if (this.thread.isAlive())
      synchronized (this.processingMonitor) {this.processingMonitor.notifyAll();}
    else
      this.thread.start();
  }

  public void setRequest(WinstoneRequest request)     {this.req = request;}
  public void setResponse(WinstoneResponse response)  {this.rsp = response;}

  public void setInStream(WinstoneInputStream inStream)    {this.inData  = inStream;}
  public void setOutStream(WinstoneOutputStream outStream) {this.outData = outStream;}

  public void setRequestStartTime() {this.requestStartTime = System.currentTimeMillis();}
  public long getRequestProcessTime() {return System.currentTimeMillis() - this.requestStartTime;}
  
  /**
   * Trigger the thread destruction for this handler
   */
  public void destroy()
  {
    this.interrupted = true;
    if (this.thread.isAlive())
      synchronized (this.processingMonitor) {this.processingMonitor.notifyAll();}
  }
}

