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

import javax.servlet.*;
import java.io.IOException;
import java.util.Map;

/**
 * Implements the sending of a request to a specific servlet instance,
 * or routes off to the static content servlet.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestDispatcher implements javax.servlet.RequestDispatcher, javax.servlet.FilterChain
{
  final String JSP_FILE = "org.apache.catalina.jsp_file";

  private ServletConfiguration config;
  private Servlet instance;
  private String name;
  //private String prefix;
  private ClassLoader loader;
  private Object semaphore;
  private String requestedPath;
  private String jspFile;
  private WinstoneResourceBundle resources;
  private Map filters;
  private String forwardFilterPatterns[];
  private String includeFilterPatterns[];
  private String filterPatterns[];
  private int filterPatternsEvaluated;
  private int filterPatternCount;
  //private boolean doInclude;
  private boolean securityChecked;
  private AuthenticationHandler authHandler;

  static final String INCLUDE_REQUEST_URI  = "javax.servlet.include.request_uri";
  static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
  static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
  static final String INCLUDE_PATH_INFO    = "javax.servlet.include.path_info";
  static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

  static final String FORWARD_REQUEST_URI  = "javax.servlet.forward.request_uri";
  static final String FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
  static final String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
  static final String FORWARD_PATH_INFO    = "javax.servlet.forward.path_info";
  static final String FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";

  /**
   * Constructor. This initializes the filter chain and sets up the details
   * needed to handle a servlet excecution, such as security constraints,
   * filters, etc.
   */
  public RequestDispatcher(ServletConfiguration config, Servlet instance, 
    String name, ClassLoader loader, Object semaphore, String requestedPath, 
    WinstoneResourceBundle resources, Map filters, String forwardFilterPatterns[], 
    String includeFilterPatterns[], AuthenticationHandler authHandler, //String prefix, 
    String jspFile)
  {
    this.config = config;
    this.resources = resources;
    this.instance = instance;
    this.name = name;
    this.loader = loader;
    this.semaphore = semaphore;
    this.requestedPath = requestedPath;
    this.jspFile = jspFile;
    this.authHandler = authHandler;
    //this.prefix = prefix;
    this.filters = filters;
    this.forwardFilterPatterns = forwardFilterPatterns;
    this.includeFilterPatterns = includeFilterPatterns;

    this.filterPatternsEvaluated = 0;
    this.filterPatternCount = -1; //(forwardFilterPatterns == null ? 0: forwardFilterPatterns.length);
  }

  public String getName() {return this.name;}

  /**
   * Includes the execution of a servlet into the current request
   */
  public void include(ServletRequest request,
                      ServletResponse response)
    throws ServletException, IOException
  {
    
    // On the first call
    if (this.filterPatternsEvaluated == 0)
      Logger.log(Logger.DEBUG, "INCLUDE: servlet=" + this.name + ", path=" + this.requestedPath);

    // Have we eval'd security constraints yet ?
    boolean continueAfterSecurityCheck = true;
    if (!this.securityChecked)
    {
      this.securityChecked = true;
      if (this.authHandler != null)
        continueAfterSecurityCheck = this.authHandler.processAuthentication
                                              (request, response, this.requestedPath);
    }

    // Make sure that failed attempts get routed through to login page
    if (!continueAfterSecurityCheck)
      return;

    // Make sure the filter chain is exhausted first
    //else if ((this.filterPatternCount > 0) &&
    //    (this.filterPatternsEvaluated < this.filterPatternCount))
    //{
    //  this.doInclude = true;
    //  doFilter(request, response);
    //}
    else
    {
      IncludeResponse includer = new IncludeResponse(response, this.resources);
      request.setAttribute("winstone.requestDispatcher.include", "true");
      
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(this.loader);
      if (this.jspFile != null)
        request.setAttribute(JSP_FILE, this.jspFile);

      // Set request attributes
      request.setAttribute(INCLUDE_REQUEST_URI, "");
      request.setAttribute(INCLUDE_CONTEXT_PATH, "");
      request.setAttribute(INCLUDE_SERVLET_PATH, "");
      request.setAttribute(INCLUDE_PATH_INFO, "");
      request.setAttribute(INCLUDE_QUERY_STRING, "");
      
      if (this.instance instanceof SingleThreadModel)
        synchronized (this.semaphore)
          {this.instance.service(request, includer);}
      else
        this.instance.service(request, includer);

      Thread.currentThread().setContextClassLoader(cl);
      includer.finish();
    }
  }

  /**
   * Forwards to another servlet, and when it's finished executing
   * that other servlet, cut off execution.
   * 
   */
  public void forward(javax.servlet.ServletRequest request,
                      javax.servlet.ServletResponse response)
    throws ServletException, IOException
  {
    ServletRequest bareRequest = request;
    ServletResponse bareResponse = response;

    // On the first call
    if (this.filterPatternsEvaluated == 0)
    {
      Logger.log(Logger.DEBUG, "FORWARD: servlet=" + this.name + ", path=" + this.requestedPath);
      
      if (response.isCommitted())
        throw new IllegalStateException(resources.getString("RequestDispatcher.ForwardCommitted"));
      response.resetBuffer();

      // Strip back to bare request/response - set up for filters
      if (request instanceof ServletRequestWrapper)
        bareRequest = ((ServletRequestWrapper) request).getRequest();
      if (request instanceof ServletResponseWrapper)
        bareResponse = ((ServletResponseWrapper) response).getResponse();
      
      if (bareRequest instanceof WinstoneRequest)
      {
        WinstoneRequest req = (WinstoneRequest) bareRequest;
        req.setServletPath(this.requestedPath);
        //req.setRequestURI(this.prefix + this.requestedPath);
      }
    }

    // Have we eval'd security constraints yet ?
    boolean continueAfterSecurityCheck = true;
    if (!this.securityChecked)
    {
      this.securityChecked = true;
      if (this.authHandler != null)
        continueAfterSecurityCheck = this.authHandler.processAuthentication
                                       (bareRequest, bareResponse, this.requestedPath);
    }

    // Make sure that failed attempts get routed through to login page
    if (!continueAfterSecurityCheck)
      return;

    // Make sure the filter chain is exhausted first
    else if ((this.filterPatternCount > 0) &&
        (this.filterPatternsEvaluated < this.filterPatternCount))
      doFilter(bareRequest, bareResponse);
    else
    {
      bareRequest.setAttribute("winstone.requestDispatcher.include", "false");

      // Execute
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(this.loader);
      if (this.jspFile != null)
        request.setAttribute(JSP_FILE, this.jspFile);

      // Set request attributes
      request.setAttribute(FORWARD_REQUEST_URI, "");
      request.setAttribute(FORWARD_CONTEXT_PATH, "");
      request.setAttribute(FORWARD_SERVLET_PATH, "");
      request.setAttribute(FORWARD_PATH_INFO, "");
      request.setAttribute(FORWARD_QUERY_STRING, "");

      if (this.instance instanceof SingleThreadModel)
        synchronized (this.semaphore)
        	{this.instance.service(bareRequest, bareResponse);}
      else
        this.instance.service(bareRequest, bareResponse);
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
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);
        filter.getFilter().doFilter(request, response, this);
        Thread.currentThread().setContextClassLoader(cl);
        return;
      }
      else
        Logger.log(Logger.FULL_DEBUG, this.resources.getString(
          "RequestDispatcher.BypassingFilter",
            "[#filterPattern]", filterPattern, "[#path]", this.requestedPath));
    }

    // Forward / include as requested in the beginning
//    if (this.doInclude)
//      include(request, response);
//    else
      forward(request, response);
  }
}

