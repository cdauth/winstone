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

import org.w3c.dom.Node;
import javax.servlet.ServletContext;
import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Models the web.xml file's details ... basically just a bunch of configuration
 * details, plus the actual instances of mounted servlets.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebAppConfiguration implements ServletContext
{
  final String ELEM_DESCRIPTION     = "description";
  final String ELEM_DISPLAY_NAME    = "display-name";
  final String ELEM_SERVLET         = "servlet";
  final String ELEM_MAPPING         = "servlet-mapping";
  final String ELEM_SERVLET_NAME    = "servlet-name";
  final String ELEM_URL_PATTERN     = "url-pattern";
  final String ELEM_WELCOME_FILES   = "welcome-file-list";
  final String ELEM_WELCOME_FILE    = "welcome-file";
  final String ELEM_SESSION_TIMEOUT = "session-config";
  final String ELEM_SESSION_CONFIG  = "session-timeout";

  final String STAR = "*";

  final String JSP_SERVLET_NAME       = "JspServlet";
  final String JSP_SERVLET_MAPPING    = "*.jsp";
  final String JSP_SERVLET_CLASS      = "org.apache.jasper.servlet.JspServlet";
  final String JSP_SERVLET_LOG_LEVEL  = "DEBUG";
  
  private String webRoot;
  private String prefix;
  private WinstoneClassLoader loader;

  private String displayName;

  private Map   attributes;
  private Map   initParameters;
  private Map   sessions;

  private Map   servletInstances;
  private Map   exactMatchMounts;

  private String  patterns[];
  private String  patternMounts[];

  private String  welcomeFiles[];
  private Integer sessionTimeout;

  private ServletConfiguration staticResourceProcessor;

  /**
   * Constructor. This parses the xml and sets up for basic routing
   */
  public WebAppConfiguration(String webRoot,
                             String prefix,
                             boolean directoryListings,
                             boolean useJasper,
                             Node elm)
  {
    this.webRoot = webRoot;
    this.prefix = (prefix != null ? prefix : "");
    this.loader = new WinstoneClassLoader(webRoot, this.getClass().getClassLoader());

    this.attributes = new Hashtable();
    this.initParameters = new Hashtable();
    this.sessions = new Hashtable();

    this.servletInstances = new Hashtable();
    this.exactMatchMounts = new Hashtable();

    List localPatterns = new ArrayList();
    List localPatternMounts = new ArrayList();
    List localWelcomeFiles = new ArrayList();

    List startupServlets = new ArrayList();

    // Initialise jasper servlet if requested
    if (useJasper)
    {
      setAttribute("org.apache.catalina.classloader", this.loader);
      //Logger.log(Logger.DEBUG, "Setting JSP classpath: " +  this.loader.getClasspath());
      setAttribute("org.apache.catalina.jsp_classpath", this.loader.getClasspath());

      Map jspParams = new HashMap();
      jspParams.put("logVerbosityLevel", JSP_SERVLET_LOG_LEVEL);
      jspParams.put("fork", "false");
      ServletConfiguration sc = new ServletConfiguration(this, this.loader, JSP_SERVLET_NAME,
        JSP_SERVLET_CLASS, jspParams, 3);
      this.servletInstances.put(JSP_SERVLET_NAME, sc);
      startupServlets.add(sc);
      processMapping(JSP_SERVLET_NAME, JSP_SERVLET_MAPPING, localPatterns, localPatternMounts);
    }

    // Parse the web.xml file
    for (int n = 0; n < elm.getChildNodes().getLength(); n++)
    {
      Node child = elm.getChildNodes().item(n);
      if (child.getNodeType() != Node.ELEMENT_NODE)
        continue;
      String nodeName = child.getNodeName();

      if (nodeName.equals(ELEM_DISPLAY_NAME))
        this.displayName = child.getFirstChild().getNodeValue().trim();

      // Session config elements
      else if (nodeName.equals(ELEM_SESSION_CONFIG))
      {
        for (int m = 0; m < child.getChildNodes().getLength(); m++)
        {
          Node timeoutElm = (Node) child.getChildNodes().item(m);
          if ((timeoutElm.getNodeType() == Node.ELEMENT_NODE) &&
              (timeoutElm.getNodeName().equals(ELEM_SESSION_TIMEOUT)))
            this.sessionTimeout = new Integer(timeoutElm.getFirstChild().getNodeValue().trim());
        }
      }

      // Construct the servlet instances
      else if (nodeName.equals(ELEM_SERVLET))
      {
        ServletConfiguration instance = new ServletConfiguration(this, this.loader, child);
        this.servletInstances.put(instance.getServletName(), instance);
        if (instance.getLoadOnStartup() >= 0)
          startupServlets.add(instance);
      }

      // Process the servlet mappings
      else if (nodeName.equals(ELEM_MAPPING))
      {
        String name    = null;
        String pattern = null;

        // Parse the element and extract
        for (int k = 0; k < child.getChildNodes().getLength(); k++)
        {
          Node mapChild = child.getChildNodes().item(k);
          if (mapChild.getNodeType() != Node.ELEMENT_NODE)
            continue;
          String mapNodeName = mapChild.getNodeName();
          if (mapNodeName.equals(ELEM_SERVLET_NAME))
            name = mapChild.getFirstChild().getNodeValue().trim();
          else if (mapNodeName.equals(ELEM_URL_PATTERN))
            pattern = mapChild.getFirstChild().getNodeValue().trim();
        }
        processMapping(name, pattern, localPatterns, localPatternMounts);
      }

      // Process the list of welcome files
      else if (nodeName.equals(ELEM_WELCOME_FILES))
        for (int m = 0; m < child.getChildNodes().getLength(); m++)
        {
          Node welcomeFile = (Node) child.getChildNodes().item(m);
          if ((welcomeFile.getNodeType() == Node.ELEMENT_NODE) &&
              (welcomeFile.getNodeName().equals(ELEM_WELCOME_FILE)))
            localWelcomeFiles.add(welcomeFile.getFirstChild().getNodeValue().trim());
        }
    }

    // Take the elements out of the lists and build arrays
    this.welcomeFiles = (String []) localWelcomeFiles.toArray(
                                    new String[localWelcomeFiles.size()]);
    this.patterns = (String []) localPatterns.toArray(
                                    new String[localPatterns.size()]);
    this.patternMounts = (String []) localPatternMounts.toArray(
                                    new String[localPatternMounts.size()]);

    // Initialise static processor
    Map staticParams = new Hashtable();
    staticParams.put("webRoot", this.webRoot);
    staticParams.put("prefix", this.prefix);
    staticParams.put("directoryList", "" + directoryListings);
    staticParams.put("welcomeFileCount", "" + this.welcomeFiles.length);
    for (int n = 0; n < this.welcomeFiles.length; n++)
      staticParams.put("welcomeFile_" + n, this.welcomeFiles[n]);
    this.staticResourceProcessor = new ServletConfiguration(this, this.getClassLoader(),
      "StaticResourceProcessor", "com.rickknowles.winstone.StaticResourceServlet", staticParams, 0);
    this.staticResourceProcessor.getRequestDispatcher(null);

    // Initialise load on startup servlets
    Object autoStarters[] = startupServlets.toArray();
    Arrays.sort(autoStarters);
    for (int n = 0; n < autoStarters.length; n++)
      ((ServletConfiguration) autoStarters[n]).getRequestDispatcher(null);
  }

  public String getPrefix()                   {return this.prefix;}
  public String getWebroot()                  {return this.webRoot;}
  public ClassLoader getClassLoader()         {return this.loader;}
  public Map getSessions()                    {return this.sessions;}
  public String[] getWelcomeFiles()           {return this.welcomeFiles;}

  /**
   * Iterates through each of the servlets and calls destroy on them
   */
  public void destroy()
  {
    for (Iterator i = this.servletInstances.values().iterator(); i.hasNext(); )
      ((ServletConfiguration) i.next()).destroy();
  }

  /**
   * Here we process url patterns into the exactMatch and patternMatch map/list
   */
  private void processMapping(String name, String pattern, List localPatterns, List localPatternMounts)
  {
    // If pattern contains asterisk, goes in pattern match, otherwise exact
    if ((pattern == null) || (name == null))
      Logger.log(Logger.WARNING, "WebAppConfig: Invalid pattern mount for " + name +
                                 " pattern " + pattern + " - ignoring");
    // exact mount
    else if (pattern.indexOf(STAR) == -1)
      this.exactMatchMounts.put(pattern, name);
    // pattern mount - 1 star
    else if (pattern.indexOf(STAR) == pattern.lastIndexOf(STAR))
    {
      localPatterns.add(pattern);
      localPatternMounts.add(name);
    }
    // pattern mount 2 or more stars
    else
    {
      int first = pattern.indexOf(STAR);
      int last = pattern.lastIndexOf(STAR);
      if ((first != 0) || (last != pattern.length() - 1))
      {
        String newPattern = pattern.substring(first, last);
        Logger.log(Logger.WARNING, "WebAppConfig: Invalid pattern " + pattern +
                                   " - using: " + newPattern);
        localPatterns.add(newPattern);
      }
      else
        localPatterns.add(pattern);
      localPatternMounts.add(name);
    }
    Logger.log(Logger.FULL_DEBUG, "Mapped: " + name + " to " + pattern);
  }

  /**
   * Execute the pattern match, and try to return a servlet that matches this
   * URL
   */
  private ServletConfiguration urlMatch(String path)
  {
    Logger.log(Logger.FULL_DEBUG, "URL Match - path: " + path);

    // Check exact matches first
    String exact = (String) this.exactMatchMounts.get(path);
    if (exact != null)
      return (ServletConfiguration) this.servletInstances.get(exact);

    // Check pattern mounts
    for (int n = 0; n < this.patterns.length; n++)
      if (wildcardMatch(this.patterns[n], path))
        return (ServletConfiguration) this.servletInstances.get(this.patternMounts[n]);

    // return null, which indicates this should be handled as static content
    return null;
  }

  /**
   * Currently only processes simple-ish patterns (1 star at start, end, or middle,
   * and anything between 2 stars
   */
  private boolean wildcardMatch(String pattern, String path)
  {
    int first = pattern.indexOf(STAR);
    int last = pattern.lastIndexOf(STAR);
    if (pattern.equals(STAR))
      return true;
    else if (first == last)
    {
      if (first == pattern.length() - 1)
        return path.startsWith(pattern.substring(0, pattern.length() - STAR.length()));
      else if (first == 0)
        return path.endsWith(pattern.substring(STAR.length()));
      else
        return path.startsWith(pattern.substring(0, first)) &&
               path.endsWith(pattern.substring(first + STAR.length()));
    }
    else
      return (path.indexOf(pattern.substring(first, last)) != -1);
  }

  private boolean fancyWildcardMatch(int type, String pattern, String path)
  {
    if (pattern == null)
      return false;
    else if (pattern.equals("*"))
      return true;

    int wcPos = pattern.indexOf('*');

    // Where there are no wildcards left
    if (wcPos == -1)
      return path.equals(pattern);
    else if (wcPos == 0)
      return path.endsWith(pattern.substring(1));
    else if (wcPos == pattern.length())
      return path.startsWith(pattern.substring(0, wcPos));
    else
      return false; // for now - if I can find a decent recursive algorithm
                    // for this kind of pattern match, add it here
  }

  public WinstoneSession makeNewSession(String sessionId)
  {
    WinstoneSession ws = new WinstoneSession(sessionId, this);
    if ((this.sessionTimeout != null) && (this.sessionTimeout.intValue() > 0))
      ws.setMaxInactiveInterval(this.sessionTimeout.intValue() * 60);
    else
      ws.setMaxInactiveInterval(-1);
    this.sessions.put(sessionId, ws);
    return ws;
  }

  /**************************************************************************
   *
   * OK ... from here to the end is the interface implementation methods for
   * the servletContext interface ... not much useful here.
   *
   **************************************************************************/

  // Application level attributes
  public Object getAttribute(String name)               {return this.attributes.get(name);}
  public Enumeration getAttributeNames()                {return Collections.enumeration(this.attributes.keySet());}
  public void removeAttribute(String name)              {this.attributes.remove(name);}
  public void setAttribute(String name, Object object)  {this.attributes.put(name, object);}

  // Application level init parameters
  public String getInitParameter(String name) {return (String) this.initParameters.get(name);}
  public Enumeration getInitParameterNames()  {return Collections.enumeration(this.initParameters.keySet());}

  // Server info
  public String getServerInfo() {return "Winstone Server v0.1";}
  public int getMajorVersion()  {return 2;}
  public int getMinorVersion()  {return 3;}

  // Weird mostly deprecated crap to do with getting servlet instances
  public javax.servlet.ServletContext getContext(String uri)  {return this;}
  public javax.servlet.Servlet getServlet(String name)        {return null;}
  public Enumeration getServletNames()                        {return Collections.enumeration(new ArrayList());}
  public Enumeration getServlets()                            {return Collections.enumeration(new ArrayList());}
  public String getMimeType(String file)                      {return "not implemented";}
  public String getServletContextName()                       {return this.displayName;}

  // Context level log statements
  public void log(String msg)                           {Logger.log(Logger.INFO, msg);}
  public void log(String message, Throwable throwable)  {Logger.log(Logger.ERROR, message, throwable);}
  public void log(Exception exception, String msg)      {this.log(msg, exception);}

  // Getting request dispatchers
  public javax.servlet.RequestDispatcher getNamedDispatcher(String name)
  {
    ServletConfiguration servlet = (ServletConfiguration) this.servletInstances.get(name);
    return (servlet != null ? servlet.getRequestDispatcher(null) : null);
  }

  public javax.servlet.RequestDispatcher getRequestDispatcher(String path)
  {
    ServletConfiguration servlet = urlMatch(path);
    if (servlet != null)
      return servlet.getRequestDispatcher(path);
    else
      return this.staticResourceProcessor.getRequestDispatcher(path);
  }

  // Getting resources via the classloader
  public URL getResource(String path)
  {
    // Trim the prefix
    if (path == null)
      return null;
    else if (path.startsWith(this.prefix))
      path = path.substring(this.prefix.length());

    // Trim the leading /
    if (path.startsWith("/"))
      path = path.substring(1);

    try
      {return new File(webRoot, path).toURL();}
    catch (MalformedURLException err)
      {throw new WinstoneException("Bad resource path", err);}
  }
  public InputStream getResourceAsStream(String path)
  {
    try
      {return this.getResource(path).openStream();}
    catch (IOException err)
      {throw new WinstoneException("Error opening stream", err);}
  }
  public String getRealPath(String path)
  {
    // Trim the prefix
    if (path == null)
      return null;
    else if (path.startsWith(this.prefix))
      path = path.substring(this.prefix.length());

    // Trim the leading /
    if (path.startsWith("/"))
      path = path.substring(1);
    try
      {return new File(this.webRoot, path).getCanonicalPath();}
    catch (IOException err) {return null;}
  }

  public Set getResourcePaths(String path)
  {
    // Trim the prefix
    if (path == null)
      return null;

    StringBuffer workingPath = new StringBuffer(path);
    if (workingPath.toString().startsWith(this.prefix))
      workingPath.delete(0, this.prefix.length());

    // Trim the leading /
    if ((workingPath.length() > 0) && (workingPath.charAt(0) == '/'))
      workingPath.deleteCharAt(0);

    // Trim the trailing /
    if ((workingPath.length() > 0) &&
        (workingPath.charAt(workingPath.length() - 1) == '/'))
      workingPath.deleteCharAt(workingPath.length() - 1);

    // Find all the files in this folder
    File inPath = new File(this.webRoot, workingPath.toString());
    if (!inPath.exists())
      return null;
    else if (!inPath.isDirectory())
      return null;
    File children[] = inPath.listFiles();
    Set out = new HashSet();
    for (int n = 0; n < children.length; n++)
    {
      // Write the entry as prefix + subpath + child element
      String entry = this.prefix + "/" +
              (workingPath.length() != 0 ? workingPath.toString() + "/" : "") +
              children[n].getName() + (children[n].isDirectory() ? "/" : "");
      Logger.log(Logger.FULL_DEBUG, "getResourcePath: path=" + path + ", entry=" + entry);
      out.add(entry);
    }
    return out;
  }

}

 