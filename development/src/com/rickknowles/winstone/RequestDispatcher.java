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
import java.util.Map;

/**
 * Implements the sending of a request to a specific servlet instance,
 * or routes off to the static content servlet.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestDispatcher implements javax.servlet.RequestDispatcher, javax.servlet.FilterChain
{
  final String JSP_FILE = "org.apache.catalina.jsp_file";

  private Servlet instance;
  private String name;
  private ClassLoader loader;
  private Object semaphore;
  private String requestedPath;
  private WinstoneResourceBundle resources;
  private Map filters;
  private String filterPatterns[];
  private int filterPatternsEvaluated;
  private int filterPatternCount;
  private boolean doInclude;

  public RequestDispatcher(Servlet instance, String name, ClassLoader loader,
    Object semaphore, String requestedPath, WinstoneResourceBundle resources,
    Map filters, String filterPatterns[])
  {
    this.resources = resources;
    this.instance = instance;
    this.name = name;
    this.loader = loader;
    this.semaphore = semaphore;
    this.requestedPath = requestedPath;
    this.filters = filters;
    this.filterPatterns = filterPatterns;
    this.filterPatternsEvaluated = 0;
    this.filterPatternCount = (filterPatterns == null ? 0: filterPatterns.length);
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


    // Make sure the filter chain is exhausted first
    if ((this.filterPatternCount > 0) &&
        (this.filterPatternsEvaluated < this.filterPatternCount))
    {
      this.doInclude = true;
      doFilter(request, response);
    }
    else
    {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(this.loader);
      if (this.instance instanceof SingleThreadModel)
        synchronized (this.semaphore)
          {this.instance.service(request, response);}
      else
        this.instance.service(request, response);
      Thread.currentThread().setContextClassLoader(cl);
    }
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
      throw new IllegalStateException(resources.getString("RequestDispatcher.ForwardCommitted"));
    response.resetBuffer();

    // Make sure the filter chain is exhausted first
    if ((this.filterPatternCount > 0) &&
        (this.filterPatternsEvaluated < this.filterPatternCount))
      doFilter(request, response);
    else
    {
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

  /**
   * Handles the processing of the chain of filters, so that we process them all,
   * then pass on to the main servlet
   */
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    // Loop through the filter mappings until we hit the end
    while ((this.filterPatternCount > 0) &&
           (this.filterPatternsEvaluated < this.filterPatternCount))
    {
      // Get the pattern and eval it, bumping up the eval'd count
      String filterPattern = this.filterPatterns[this.filterPatternsEvaluated++];
      int delimPos = filterPattern.indexOf("] F:[");

      // If the servlet name matches this name, execute it
      if ((delimPos == -1) || !filterPattern.endsWith("]"))
        Logger.log(Logger.DEBUG, this.resources.getString(
          "RequestDispatcher.InvalidMapping", "[#filterPattern]", filterPattern));
      else if (filterPattern.startsWith("S:[") &&
               filterPattern.substring(3, delimPos).equals(this.name))
      {
        String filterName = filterPattern.substring(delimPos + 5, filterPattern.length() - 1);
        FilterConfiguration filter = (FilterConfiguration) this.filters.get(filterName);
        Logger.log(Logger.DEBUG, this.resources.getString(
          "RequestDispatcher.ExecutingFilter", "[#filterName]", filterName));
        filter.getFilter().doFilter(request, response, this);
        return;
      }
      else if (filterPattern.startsWith("U:[") &&
               WebAppConfiguration.wildcardMatch(filterPattern.substring(3, delimPos),
                                                 this.requestedPath))
      {
        String filterName = filterPattern.substring(delimPos + 5, filterPattern.length() - 1);
        FilterConfiguration filter = (FilterConfiguration) this.filters.get(filterName);
        Logger.log(Logger.DEBUG, this.resources.getString(
          "RequestDispatcher.ExecutingFilter", "[#filterName]", filterName));
        filter.getFilter().doFilter(request, response, this);
        return;
      }
      else
        Logger.log(Logger.FULL_DEBUG, this.resources.getString(
          "RequestDispatcher.BypassingFilter",
            "[#filterPattern]", filterPattern, "[#path]", this.requestedPath));
    }

    // Forward / include as requested in the beginning
    if (this.doInclude)
      include(request, response);
    else
      forward(request, response);
  }
}

