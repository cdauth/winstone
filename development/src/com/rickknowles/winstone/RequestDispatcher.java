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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SingleThreadModel;
import java.io.IOException;

/**
 * Implements the sending of a request to a specific servlet instance,
 * or routes off to the static content servlet.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestDispatcher implements javax.servlet.RequestDispatcher
{
  final String JSP_FILE = "org.apache.catalina.jsp_file";

  private Servlet instance;
  private String name;
  private ClassLoader loader;
  private Object semaphore;
  private String requestedPath;

  public RequestDispatcher(Servlet instance, String name, ClassLoader loader,
    Object semaphore, String requestedPath)
  {
    this.instance = instance;
    this.name = name;
    this.loader = loader;
    this.semaphore = semaphore;
    this.requestedPath = requestedPath;
  }

  public String getName() {return this.name;}

  /**
   * Includes the execution of a servlet into the current request
   */
  public void include(ServletRequest request,
                      ServletResponse response)
    throws ServletException, IOException
  {
    Logger.log(Logger.FULL_DEBUG, "INCLUDE: " + this.name);
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(this.loader);
    if (this.instance instanceof SingleThreadModel)
      synchronized (this.semaphore)
        {this.instance.service(request, response);}
    else
      this.instance.service(request, response);
    Thread.currentThread().setContextClassLoader(cl);
  }

  /**
   * Forwards to another servlet, and when it's finished executing
   * that other servlet, cut off execution.
   * Note: current implementation is actually just an include
   */
  public void forward(javax.servlet.ServletRequest request,
                      javax.servlet.ServletResponse response)
    throws ServletException, IOException
  {
    Logger.log(Logger.FULL_DEBUG, "FORWARD: " + this.name);
    if (response.isCommitted())
      throw new IllegalStateException("Called RequestDispatcher.forward() on committed response");

    response.resetBuffer();

    // Execute
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(this.loader);
    if (this.requestedPath != null)
      request.setAttribute(JSP_FILE, this.requestedPath);
    if (this.instance instanceof SingleThreadModel)
      synchronized (this.semaphore)
        {this.instance.service(request, response);}
    else
      this.instance.service(request, response);
    Thread.currentThread().setContextClassLoader(cl);
  }
}

