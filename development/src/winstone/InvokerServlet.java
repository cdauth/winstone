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

import javax.servlet.http.*;
import javax.servlet.*;


import java.io.IOException;
import java.util.*;

/**
 * If a URI matches a servlet class name, mount an instance of that servlet, and
 * try to process the request using that servlet.
 *
 * @author  <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class InvokerServlet extends HttpServlet
{
  final String RESOURCE_FILE     = "winstone.LocalStrings";
  final String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
  final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

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
/*
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
*/
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
/*  protected String extractLocalPath(String fullURI)
  {
    String pathOnly = trimHostName(trimQueryString(fullURI));
    if (!pathOnly.startsWith(this.prefix))
      throw new IllegalArgumentException(this.resources.getString("InvokerServlet.NotInPrefix",
          "[#fullURI]", fullURI));
    else
      return pathOnly.substring(this.prefix.length());
  }
*/
  /**
   * Get an instance of the servlet configuration object
   */
  protected ServletConfiguration getInvokableInstance(String servletName)
    throws ServletException, IOException
  {
    ServletConfiguration sc = null;
    synchronized (this.mountedInstances)
    {
      if (this.mountedInstances.containsKey(servletName))
        sc = (ServletConfiguration) this.mountedInstances.get(servletName);
    }

    if (sc == null)
    {
      // If found, mount an instance
      try
      {
        Class servletClass = Class.forName(servletName, true, Thread.currentThread().getContextClassLoader());
        sc = new ServletConfiguration(this.getServletContext(),
            Thread.currentThread().getContextClassLoader(), this.resources, this.prefix,
            getServletConfig().getServletName() + ":" + servletName, 
            servletName, new Hashtable(), -1);
        this.mountedInstances.put(servletName, sc);
        Logger.log(Logger.DEBUG, this.resources.getString("InvokerServlet.MountingServlet",
            "[#className]", servletName, "[#invokerName]", getServletConfig().getServletName()));
        sc.getRequestDispatcher(new HashMap()); // just to trigger the servlet.init()
      }
      catch (Throwable err)  {sc = null;}
    }
    return sc;
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    boolean isInclude = (req.getAttribute(INCLUDE_PATH_INFO) != null);
    boolean isForward = (req.getAttribute(FORWARD_PATH_INFO) != null);
    String servletName = null;
    
    if (isInclude)
    	servletName = (String) req.getAttribute(INCLUDE_PATH_INFO);
    else if (isForward)
    	servletName = (String) req.getAttribute(FORWARD_PATH_INFO);
    else if (req.getPathInfo() != null)
    	servletName = req.getPathInfo();
    else
    	servletName = "";
    if (servletName.startsWith("/"))
      servletName = servletName.substring(1);
    ServletConfiguration invokedServlet = getInvokableInstance(servletName);

    if (invokedServlet == null)
    {
      String errMsg = this.resources.getString("InvokerServlet.NoMatchingServletFound",
                                 "[#requestURI]", servletName);
      Logger.log(Logger.WARNING, errMsg);
      rsp.sendError(HttpServletResponse.SC_NOT_FOUND, errMsg);
    }
    else
    {
      RequestDispatcher rd = invokedServlet.getRequestDispatcher(new HashMap());
      rd.setForNamedDispatcher(new Mapping[0], new Mapping[0]);
      rd.forward(req, rsp);
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
    {doGet(req, rsp);}
}

