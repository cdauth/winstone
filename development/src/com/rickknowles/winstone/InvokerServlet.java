
// Copyright (c) 2003 
package com.rickknowles.winstone;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.util.*;

/**
 * If a URI matches a servlet class name, mount an instance of that servlet, and
 * try to process the request using that servlet.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class InvokerServlet extends HttpServlet
{
  final String RESOURCE_FILE    = "com.rickknowles.winstone.LocalStrings";

  private WinstoneResourceBundle resources;
  private Map mountedInstances;
  private String prefix;
  private String invokerPrefix;

  /**
   * Set up a blank map of servlet configuration instances
   */
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    this.resources = new WinstoneResourceBundle(RESOURCE_FILE);
    this.mountedInstances = new Hashtable();
    this.prefix = config.getInitParameter("prefix");
    this.invokerPrefix = config.getInitParameter("invokerPrefix");
  }

  private String trimHostName(String input)
  {
    if (input == null)
      return null;
    else if (input.startsWith("/"))
      return input;

    int hostStart = input.indexOf("://");
    if (hostStart == -1)
      return input;
    String hostName = input.substring(hostStart + 3);
    int pathStart = hostName.indexOf('/');
    if (pathStart == -1)
      return "/";
    else
      return hostName.substring(pathStart);
  }

  private String trimQueryString(String input)
  {
    if (input == null)
      return null;

    int questionPos = input.indexOf('?');
    if (questionPos == -1)
      return input;
    else
      return input.substring(0, questionPos);
  }

  /**
   * Destroy any mounted instances we might be holding, then destroy myself
   */
  public void destroy()
  {
    if (this.mountedInstances != null)
      synchronized (this.mountedInstances)
      {
        for (Iterator i = this.mountedInstances.values().iterator(); i.hasNext(); )
          ((ServletConfiguration) i.next()).destroy();
      }
    super.destroy();
  }

  /**
   * Take the URI, and retrieve the part that is relevant to this servlet
   */
  protected String extractLocalPath(String fullURI)
  {
    String pathOnly = trimHostName(trimQueryString(fullURI));
    if (!pathOnly.startsWith(this.prefix))
      throw new IllegalArgumentException(this.resources.getString("InvokerServlet.NotInPrefix",
          "[#fullURI]", fullURI));
    else
      return pathOnly.substring(this.prefix.length());
  }

  /**
   * Take the local path
   */
  protected ServletConfiguration getInvokableInstance(String pathName)
    throws ServletException, IOException
  {
    ServletConfiguration sc = null;
    synchronized (this.mountedInstances)
    {
      if (this.mountedInstances.containsKey(pathName))
        sc = (ServletConfiguration) this.mountedInstances.get(pathName);
    }

    if (sc == null)
    {
      // Search for a class
      if (pathName.startsWith(this.invokerPrefix))
      {
        String className = pathName.substring(this.invokerPrefix.length());

        // If found, mount an instance
        try
        {
          Class servletClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
          sc = new ServletConfiguration(this.getServletContext(),
              Thread.currentThread().getContextClassLoader(), this.resources,
              getServletConfig().getServletName() + ":" + pathName, className,
              new Hashtable(), -1);
          this.mountedInstances.put(pathName, sc);
          Logger.log(Logger.DEBUG, this.resources.getString("InvokerServlet.MountingServlet",
              "[#className]", className));
          sc.getRequestDispatcher(pathName);
        }
        catch (Throwable err) {/* Ignore, just return a null instance */}
      }
    }
    return sc;
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    // Get the servlet instance if possible
    String localPath = extractLocalPath(req.getRequestURI());
    ServletConfiguration invokedServlet = getInvokableInstance(localPath);

    if (invokedServlet == null)
    {
      String errMsg = this.resources.getString("InvokerServlet.NoMatchingServletFound",
                                 "[#requestURI]", req.getRequestURI());
      Logger.log(Logger.WARNING, errMsg);
      rsp.sendError(rsp.SC_NOT_FOUND, errMsg);
    }
    else
      invokedServlet.getRequestDispatcher(localPath).forward(req, rsp);
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    // Get the servlet instance if possible
    String localPath = extractLocalPath(req.getRequestURI());
    ServletConfiguration invokedServlet = getInvokableInstance(localPath);

    if (invokedServlet == null)
      rsp.sendError(rsp.SC_NOT_FOUND,
        this.resources.getString("InvokerServlet.NoMatchingServletFound",
                                 "[#requestURI]", req.getRequestURI()));
    else
      invokedServlet.getRequestDispatcher(localPath).forward(req, rsp);
  }

}

