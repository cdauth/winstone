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

import java.util.*;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import org.w3c.dom.Node;

/**
 * This is the one that keeps a specific servlet instance's config, as
 * well as holding the instance itself.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ServletConfiguration implements javax.servlet.ServletConfig, Comparable
{
  static final String ELEM_NAME              = "servlet-name";
  static final String ELEM_DISPLAY_NAME      = "display-name";
  static final String ELEM_CLASS             = "servlet-class";
  static final String ELEM_JSP_FILE          = "jsp-file";
  static final String ELEM_DESCRIPTION       = "description";
  static final String ELEM_INIT_PARAM        = "init-param";
  static final String ELEM_INIT_PARAM_NAME   = "param-name";
  static final String ELEM_INIT_PARAM_VALUE  = "param-value";
  static final String ELEM_LOAD_ON_STARTUP   = "load-on-startup";

  private String servletName;
  private String classFile;
  private Servlet instance;
  private Map initParameters;
  private ServletContext webAppConfig;
  private ClassLoader loader;
  private int loadOnStartup;
  private String prefix;
  private String jspFile;
  private boolean unavailableException;

  private WinstoneResourceBundle resources;
  private Object servletSemaphore = new Boolean(true);

  protected ServletConfiguration(ServletContext webAppConfig,
                                 ClassLoader loader,
                                 WinstoneResourceBundle resources,
                                 String prefix)
  {
    this.webAppConfig = webAppConfig;
    this.loader = loader;
    this.initParameters = new Hashtable();
    this.loadOnStartup = -1;
    this.resources = resources;
    this.prefix = prefix;
    this.unavailableException = false;
  }

  public ServletConfiguration(ServletContext webAppConfig,
                              ClassLoader loader,
                              WinstoneResourceBundle resources,
                              String prefix,
                              String servletName,
                              String className,
                              Map initParams,
                              int loadOnStartup)
  {
    this(webAppConfig, loader, resources, prefix);
    if (initParams != null)
      this.initParameters.putAll(initParams);
    this.servletName = servletName;
    this.classFile = className;
    this.jspFile = null;
    this.loadOnStartup = loadOnStartup;
  }

  public ServletConfiguration(WebAppConfiguration webAppConfig,
                              ClassLoader loader,
                              WinstoneResourceBundle resources,
                              String prefix,
                              Node elm)
  {
    this(webAppConfig, loader, resources, prefix);

    // Parse the web.xml file entry
    for (int n = 0; n < elm.getChildNodes().getLength(); n++)
    {
      Node child = elm.getChildNodes().item(n);
      if (child.getNodeType() != Node.ELEMENT_NODE)
        continue;
      String nodeName = child.getNodeName();

      // Construct the servlet instances
      if (nodeName.equals(ELEM_NAME))
        this.servletName = child.getFirstChild().getNodeValue().trim();
      else if (nodeName.equals(ELEM_CLASS))
        this.classFile = child.getFirstChild().getNodeValue().trim();
      else if (nodeName.equals(ELEM_JSP_FILE))
        this.jspFile = child.getFirstChild().getNodeValue().trim();
      else if (nodeName.equals(ELEM_LOAD_ON_STARTUP))
        this.loadOnStartup = Integer.parseInt(child.getFirstChild().getNodeValue().trim());
      else if (nodeName.equals(ELEM_INIT_PARAM))
      {
        String paramName = null;
        String paramValue = null;
        for (int k = 0; k < child.getChildNodes().getLength(); k++)
        {
          Node paramNode = child.getChildNodes().item(k);
          if (paramNode.getNodeType() != Node.ELEMENT_NODE)
            continue;
          else if (paramNode.getNodeName().equals(ELEM_INIT_PARAM_NAME))
            paramName = paramNode.getFirstChild().getNodeValue().trim();
          else if (paramNode.getNodeName().equals(ELEM_INIT_PARAM_VALUE))
            paramValue = paramNode.getFirstChild().getNodeValue().trim();
        }
        if ((paramName != null) && (paramValue != null))
          this.initParameters.put(paramName, paramValue);
      }
    }
    
    if ((this.jspFile != null) && (this.classFile == null))
    {
      this.classFile = WebAppConfiguration.JSP_SERVLET_CLASS;
      WebAppConfiguration.addJspServletParams(this.initParameters);
    }
    Logger.log(Logger.FULL_DEBUG, resources.getString("ServletConfiguration.DeployedInstance",
        "[#name]", this.servletName, "[#class]", this.classFile));
  }

  /**
   * Implements the first-time-init of an instance, and wraps it in a dispatcher.
   */
  public RequestDispatcher getRequestDispatcher(Map filters)
  {
    synchronized (this.servletSemaphore)
    {
      if (isUnavailable())
        throw new WinstoneException(resources.getString("ServletConfiguration.ServletUnavailable"));
      else if ((this.instance == null))
      try
      {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);

        Class servletClass = Class.forName(classFile, true, this.loader);
        this.instance = (Servlet) servletClass.newInstance();
        Logger.log(Logger.DEBUG, this.servletName + ": "
                    + resources.getString("ServletConfiguration.init")
                    //+ " (classloader: " + this.loader.getClass().getName() + ")"
                    );

        // Initialise with the correct classloader
        this.instance.init(this);
        Thread.currentThread().setContextClassLoader(cl);
      }
      catch (ClassNotFoundException err)
        {Logger.log(Logger.ERROR, resources.getString("ServletConfiguration.ClassLoadError") + this.classFile, err);}
      catch (IllegalAccessException err)
        {Logger.log(Logger.ERROR, resources.getString("ServletConfiguration.ClassLoadError") + this.classFile, err);}
      catch (InstantiationException err)
        {Logger.log(Logger.ERROR, resources.getString("ServletConfiguration.ClassLoadError") + this.classFile, err);}
      catch (javax.servlet.ServletException err)
      {
        this.instance = null;
        Logger.log(Logger.ERROR, resources.getString("ServletConfiguration.InitError") + this.servletName +
                                          " - " + this.classFile, err);
        if (err instanceof UnavailableException)
          setUnavailable();
      }
    }

    // Build filter chain
    return new RequestDispatcher(this, this.instance, this.servletName, 
        this.loader, this.servletSemaphore, this.prefix, this.jspFile, 
        filters, this.resources);
  }

  public int getLoadOnStartup()               {return this.loadOnStartup;}
  public String getInitParameter(String name) {return (String) this.initParameters.get(name);}
  public Enumeration getInitParameterNames()  {return Collections.enumeration(this.initParameters.keySet());}
  public javax.servlet.ServletContext getServletContext() {return this.webAppConfig;}
  public String getServletName()  {return this.servletName;}

  /**
   * This was included so that the servlet instances could be sorted on their
   * loadOnStartup values. Otherwise used.
   */
  public int compareTo(Object objTwo)
  {
    Integer one = new Integer(this.loadOnStartup);
    Integer two = new Integer(((ServletConfiguration) objTwo).loadOnStartup);
    return one.compareTo(two);
  }

  /**
   * Called when it's time for the container to shut this servlet down.
   */
  public void destroy()
  {
    synchronized (this.servletSemaphore)
    {
      if (this.instance != null)
      {
        Logger.log(Logger.DEBUG, this.servletName + ": " + resources.getString("ServletConfiguration.destroy"));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);
        this.instance.destroy();
        Thread.currentThread().setContextClassLoader(cl);
      }
    }
  }
  
  public boolean isUnavailable() {return this.unavailableException;}
  
  public void setUnavailable() 
  {
    this.unavailableException = true;
    if (this.instance != null)
      this.instance.destroy();
    this.instance = null;
  }
}

