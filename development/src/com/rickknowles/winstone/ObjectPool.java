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

import java.util.*;
import java.io.*;
import java.net.*;
import javax.servlet.UnavailableException;

/**
 * Holds the object pooling code for Winstone. Presently this is only responses
 * and requests, but may increase.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ObjectPool
{
  private int STARTUP_REQUEST_HANDLERS_IN_POOL = 5;
  private int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 50;
  private int MAX_REQUEST_HANDLERS_IN_POOL = 300;

  private int START_REQUESTS_IN_POOL  = 10;
  private int MAX_REQUESTS_IN_POOL    = 100;

  private int START_RESPONSES_IN_POOL = 10;
  private int MAX_RESPONSES_IN_POOL   = 100;

  private List unusedRequestHandlerThreads;
  private List usedRequestHandlerThreads;
  private List usedRequestPool;
  private List unusedRequestPool;
  private List usedResponsePool;
  private List unusedResponsePool;

  private Object requestHandlerSemaphore = new Boolean(true);
  private Object requestPoolSemaphore    = new Boolean(true);
  private Object responsePoolSemaphore   = new Boolean(true);

  private int threadIndex = 0;
  private WinstoneResourceBundle resources;
  private WebAppConfiguration webAppConfig;
  //private Launcher launcher;
  
  /**
   * 
   */
  public ObjectPool(Map args, WinstoneResourceBundle resources, 
    WebAppConfiguration webAppConfig) throws IOException
  {
    this.resources = resources;
    this.webAppConfig = webAppConfig;
    
    // Build the initial pool of handler threads
    this.unusedRequestHandlerThreads = new Vector();
    this.usedRequestHandlerThreads = new Vector();

    // Build the request/response pools
    this.usedRequestPool    = new ArrayList();
    this.usedResponsePool   = new ArrayList();
    this.unusedRequestPool  = new ArrayList();
    this.unusedResponsePool = new ArrayList();

    // Get handler pool options
    if (args.get("handlerCountStartup") != null)
      STARTUP_REQUEST_HANDLERS_IN_POOL = Integer.parseInt((String) args.get("handlerCountStartup"));
    if (args.get("handlerCountMax") != null)
      MAX_IDLE_REQUEST_HANDLERS_IN_POOL = Integer.parseInt((String) args.get("handlerCountMax"));
    if (args.get("handlerCountMaxIdle") != null)
      MAX_IDLE_REQUEST_HANDLERS_IN_POOL = Integer.parseInt((String) args.get("handlerCountMaxIdle"));

    // Start the base set of handler threads
    for (int n = 0; n < STARTUP_REQUEST_HANDLERS_IN_POOL; n++)
      this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++));

    // Initialise the request/response pools
    for (int n = 0; n < START_REQUESTS_IN_POOL; n++)
      this.unusedRequestPool.add(new WinstoneRequest(resources));
    for (int n = 0; n < START_RESPONSES_IN_POOL; n++)
      this.unusedResponsePool.add(new WinstoneResponse(resources));
  }

  public void removeUnusedRequestHandlers()
  {
    // Check max idle requestHandler count
    synchronized (this.requestHandlerSemaphore)
    {
      // If we have too many idle request handlers
      while (this.unusedRequestHandlerThreads.size() > MAX_IDLE_REQUEST_HANDLERS_IN_POOL)
      {
        RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
        rh.destroy();
        this.unusedRequestHandlerThreads.remove(rh);
      }
    }
  }

  public void destroy()
  {
    for (Iterator i = this.usedRequestHandlerThreads.iterator(); i.hasNext(); )
      releaseRequestHandler((RequestHandlerThread) i.next());
    for (Iterator i = this.unusedRequestHandlerThreads.iterator(); i.hasNext(); )
      ((RequestHandlerThread) i.next()).destroy();
  }

  /**
   * Once the socket request comes in, this method is called. It reserves a
   * request handler, then delegates the socket to that class. When it finishes,
   * the handler is released back into the pool.
   */
  public void handleRequest(Socket socket, Listener listener)
    throws IOException, UnavailableException, InterruptedException
  {
    synchronized (this.requestHandlerSemaphore)
    {
      // If we have any spare, get it from the pool
      if (this.unusedRequestHandlerThreads.size() > 0)
      {
        RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.UsingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
        rh.commenceRequestHandling(socket, listener);
      }

      // If we are out (and not over our limit), allocate a new one
      else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL)
      {
        RequestHandlerThread rh = new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.NewRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
        rh.commenceRequestHandling(socket, listener);
      }

      // otherwise throw fail message - we've blown our limit
      else
      {
        // Possibly insert a second chance here ? Delay and one retry ?
        // Remember to release the lock first
        socket.close();
        //throw new UnavailableException("NoHandlersAvailable");
      }
    }
  }

  /**
   * Release the handler back into the pool
   */
  public void releaseRequestHandler(RequestHandlerThread rh)
  {
    synchronized (this.requestHandlerSemaphore)
    {
      if (this.usedRequestHandlerThreads.contains(rh))
      {
        this.usedRequestHandlerThreads.remove(rh);
        this.unusedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.ReleasingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
      }
      else
        Logger.log(Logger.WARNING, resources.getString("ObjectPool.UnknownRHPoolThread"));
    }
  }

  /**
   * An attempt at pooling request objects for reuse.
   */
  public WinstoneRequest getRequestFromPool() throws IOException
  {
    WinstoneRequest req = null;
    synchronized (this.requestPoolSemaphore)
    {
      // If we have any spare, get it from the pool
      if (this.unusedRequestPool.size() > 0)
      {
        req = (WinstoneRequest) this.unusedRequestPool.get(0);
        this.unusedRequestPool.remove(req);
        this.usedRequestPool.add(req);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.UsingRequestFromPool",
            "[#unused]", "" + this.unusedRequestPool.size()));
      }
      // If we are out, allocate a new one
      else if (this.usedRequestPool.size() < MAX_REQUESTS_IN_POOL)
      {
        req = new WinstoneRequest(this.resources);
        this.usedRequestPool.add(req);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.NewRequestForPool",
            "[#used]", "" + this.usedRequestPool.size()));
      }
      else
        throw new WinstoneException(this.resources.getString("ObjectPool.PoolRequestLimitExceeded"));
    }
    return req;
  }

  public void releaseRequestToPool(WinstoneRequest req)
  {
    req.cleanUp();
    synchronized (this.requestPoolSemaphore)
    {
      this.usedRequestPool.remove(req);
      this.unusedRequestPool.add(req);
      Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.RequestReleased",
          "[#unused]", "" + this.unusedRequestPool.size()));
    }
  }

  /**
   * An attempt at pooling request objects for reuse.
   */
  public WinstoneResponse getResponseFromPool() throws IOException
  {
    WinstoneResponse rsp = null;
    synchronized (this.responsePoolSemaphore)
    {
      // If we have any spare, get it from the pool
      if (this.unusedResponsePool.size() > 0)
      {
        rsp = (WinstoneResponse) this.unusedResponsePool.get(0);
        this.unusedResponsePool.remove(rsp);
        this.usedResponsePool.add(rsp);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.UsingResponseFromPool",
            "[#unused]", "" + this.unusedResponsePool.size()));
      }
      // If we are out, allocate a new one
      else if (this.usedResponsePool.size() < MAX_RESPONSES_IN_POOL)
      {
        rsp = new WinstoneResponse(this.resources);
        this.usedResponsePool.add(rsp);
        Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.NewResponseForPool",
            "[#used]", "" + this.usedResponsePool.size()));
      }
      else
        throw new WinstoneException(this.resources.getString("ObjectPool.PoolResponseLimitExceeded"));
    }
    return rsp;
  }

  public void releaseResponseToPool(WinstoneResponse rsp)
  {
    rsp.cleanUp();
    synchronized (this.responsePoolSemaphore)
    {
      this.usedResponsePool.remove(rsp);
      this.unusedResponsePool.add(rsp);
      Logger.log(Logger.FULL_DEBUG, resources.getString("ObjectPool.ResponseReleased",
          "[#unused]", "" + this.unusedResponsePool.size()));
    }
  }

}