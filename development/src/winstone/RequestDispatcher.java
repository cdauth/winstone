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
  private String prefix;
  private ClassLoader loader;
  private Object semaphore;
  private WinstoneResourceBundle resources;
  private Map filters;
  
  private String servletPath;
  private String pathInfo;
  private String queryString;
  private String requestURI;
  private String jspFile;
  private AuthenticationHandler authHandler;

  private Mapping forwardFilterPatterns[];
  private Mapping includeFilterPatterns[];
  private Mapping filterPatterns[];

  private int filterPatternsEvaluated;
  private Boolean doInclude;
  private boolean useRequestAttributes;

  /**
   * Constructor. This initializes the filter chain and sets up the details
   * needed to handle a servlet excecution, such as security constraints,
   * filters, etc.
   */
  public RequestDispatcher(ServletConfiguration config, Servlet instance, 
    String name, ClassLoader loader, Object semaphore, String prefix, String jspFile, 
    Map filters, WinstoneResourceBundle resources)
  {
    this.config = config;
    this.resources = resources;
    this.instance = instance;
    this.name = name;
    this.loader = loader;
    this.semaphore = semaphore;
    this.prefix = prefix;
    this.jspFile = jspFile;
    this.filters = filters;

    this.filterPatternsEvaluated = 0;
  }
  
  public void setForNamedDispatcher(Mapping forwardFilterPatterns[], 
      Mapping includeFilterPatterns[])
  {
    this.forwardFilterPatterns = forwardFilterPatterns;
    this.includeFilterPatterns = includeFilterPatterns;
    this.filterPatterns = null; // set after the call to forward or include
    this.useRequestAttributes = false;
  }
  
  public void setForURLDispatcher(String servletPath, 
      String pathInfo, String queryString, String requestURI,
      Mapping forwardFilterPatterns[], Mapping includeFilterPatterns[])
  {
    this.servletPath = servletPath;
    this.pathInfo = pathInfo;
    this.queryString = queryString;
    this.requestURI = requestURI;
    
    this.forwardFilterPatterns = forwardFilterPatterns;
    this.includeFilterPatterns = includeFilterPatterns;
    this.filterPatterns = null; // set after the call to forward or include
    this.useRequestAttributes = true;
  }
  
  public void setForInitialDispatcher(String servletPath, String pathInfo, 
      String requestURI, Mapping requestFilterPatterns[], AuthenticationHandler authHandler)
  {
    this.servletPath = servletPath;
    this.pathInfo = pathInfo;
    this.requestURI = requestURI;
    this.authHandler = authHandler;
    this.filterPatterns = requestFilterPatterns;
    this.useRequestAttributes = false;
  }

  public String getName() {return this.name;}

  /**
   * Includes the execution of a servlet into the current request
   */
  public void include(ServletRequest request,
                      ServletResponse response)
    throws ServletException, IOException
  {
    ServletRequest includedRequest = request;
    ServletResponse includedResponse = response;
    
    // On the first call, log and initialise the filter chain
    if (this.doInclude == null)
    {
      Logger.log(Logger.DEBUG, "INCLUDE: servlet=" + this.name + ", path=" + this.requestURI);
      includedResponse = new IncludeResponse(response, this.resources);

      // Set request attributes
      if (useRequestAttributes)
        includedRequest = new DispatchRequestWrapper(request, this.resources, true,
	          this.prefix + this.requestURI, this.prefix, this.servletPath, this.pathInfo, 
	          this.queryString);
    }
    if (this.filterPatterns == null)
      this.filterPatterns = this.includeFilterPatterns;
    this.doInclude = Boolean.TRUE;
    
    // Make sure the filter chain is exhausted first
    if ((this.filterPatterns.length > 0) &&
        (this.filterPatternsEvaluated < this.filterPatterns.length))
      doFilter(includedRequest, includedResponse);
    else
    {
      
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(this.loader);
      if (this.jspFile != null)
        request.setAttribute(JSP_FILE, this.jspFile);
      
      if (this.instance instanceof SingleThreadModel)
        synchronized (this.semaphore)
          {this.instance.service(includedRequest, includedResponse);}
      else
        this.instance.service(includedRequest, includedResponse);

      // TODO: insert error handling here, to suppress servletExceptions
      
      Thread.currentThread().setContextClassLoader(cl);
      
      // unwrap down to the included response
      ServletResponse originalResponse = includedResponse;
      while ((originalResponse instanceof ServletResponseWrapper) &&
          	 !(originalResponse instanceof IncludeResponse))
        originalResponse = ((ServletResponseWrapper) originalResponse).getResponse();

      // Finish the response off (ie commit the body to the parent response
      if (originalResponse instanceof IncludeResponse)
        ((IncludeResponse) includedResponse).finish();
    }
  }

  /**
   * Forwards to another servlet, and when it's finished executing
   * that other servlet, cut off execution.
   */
  public void forward(javax.servlet.ServletRequest request,
                      javax.servlet.ServletResponse response)
    throws ServletException, IOException
  {
    // because this is a forward, we can overwrite the request if it's ours
    ServletRequest workingRequest = request;
    ServletResponse workingResponse = response;

    // Only on the first call to forward, we should strip the req/rsp back to bare
    if (this.doInclude == null)
    {
      Logger.log(Logger.DEBUG, "FORWARD: servlet=" + this.name + ", path=" + this.requestURI);
      if (response.isCommitted())
        throw new IllegalStateException(resources.getString("RequestDispatcher.ForwardCommitted"));
      response.resetBuffer();

      // Strip back to bare request/response - set up for filters
      while (workingRequest instanceof ServletRequestWrapper)
        workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
      while (workingResponse instanceof ServletResponseWrapper)
        workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
      
      if (workingRequest instanceof WinstoneRequest)
      {
        WinstoneRequest req = (WinstoneRequest) workingRequest;
        req.setServletPath(this.servletPath);
        req.setPathInfo(this.pathInfo);
        req.setRequestURI(this.prefix + this.requestURI);
      }
      
      // Check security - if we should not continue, return
      if (!continueAfterSecurityCheck(workingRequest, workingResponse))
        return;

      // Set request attributes (because it's the first time)
      if (useRequestAttributes)
        workingRequest = new DispatchRequestWrapper(workingRequest, this.resources, false,
            this.prefix + this.requestURI, this.prefix, this.servletPath, this.pathInfo, 
	          this.queryString);
    }

    // If we haven't set up the filter pattern set yet
    if (this.filterPatterns == null)
      this.filterPatterns = this.forwardFilterPatterns;
    this.doInclude = Boolean.FALSE;

    // Make sure the filter chain is exhausted first
    if ((this.filterPatterns.length > 0) &&
        (this.filterPatternsEvaluated < this.filterPatterns.length))
      doFilter(workingRequest, workingResponse);
    else
    {
      // Execute
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(this.loader);
      if (this.jspFile != null)
        request.setAttribute(JSP_FILE, this.jspFile);

      if (this.instance instanceof SingleThreadModel)
        synchronized (this.semaphore)
        	{this.instance.service(workingRequest, workingResponse);}
      else
        this.instance.service(workingRequest, workingResponse);
      // TODO: insert error handling here, to suppress servletExceptions

      
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  private boolean continueAfterSecurityCheck(ServletRequest request, ServletResponse response)
  	throws IOException, ServletException
  {
    // Have we eval'd security constraints yet ?
    if (this.authHandler != null)
      return this.authHandler.processAuthentication(request, response, this.servletPath);
    else
      return true;
  }
  
  /**
   * Handles the processing of the chain of filters, so that we process them all,
   * then pass on to the main servlet
   */
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    // Loop through the filter mappings until we hit the end
    while ((this.filterPatterns.length > 0) &&
           (this.filterPatternsEvaluated < this.filterPatterns.length))
    {
      // Get the pattern and eval it, bumping up the eval'd count
      Mapping filterPattern = this.filterPatterns[this.filterPatternsEvaluated++];
      String fullPath = this.servletPath + (this.pathInfo == null ? "" : this.pathInfo);
      
      // If the servlet name matches this name, execute it
      if ((filterPattern.getLinkName() != null) &&
          filterPattern.getLinkName().equals(this.name))
      {
        FilterConfiguration filter = (FilterConfiguration) this.filters.get(filterPattern.getMappedTo());
        Logger.log(Logger.DEBUG, this.resources.getString(
          "RequestDispatcher.ExecutingFilter", "[#filterName]", filterPattern.getMappedTo()));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);
        filter.getFilter().doFilter(request, response, this);
        Thread.currentThread().setContextClassLoader(cl);
        return;
      }
      else if ((filterPattern.getLinkName() == null) &&
          		 (this.servletPath != null) &&
          		 filterPattern.match(fullPath, null, null))
      {
        FilterConfiguration filter = (FilterConfiguration) this.filters.get(filterPattern.getMappedTo());
        Logger.log(Logger.DEBUG, this.resources.getString(
          "RequestDispatcher.ExecutingFilter", "[#filterName]", filterPattern.getMappedTo()));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);
        filter.getFilter().doFilter(request, response, this);
        Thread.currentThread().setContextClassLoader(cl);
        return;
      }
      else
        Logger.log(Logger.FULL_DEBUG, this.resources.getString(
          "RequestDispatcher.BypassingFilter",
            "[#filterPattern]", filterPattern.toString(), "[#path]", fullPath));
    }

    // Forward / include as requested in the beginning
    if (this.doInclude == null)
      return; // will never happen, because we can't call doFilter before forward/include
    else if (this.doInclude.booleanValue())
      include(request, response);
    else
      forward(request, response);
  }
}

