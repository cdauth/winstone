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
import java.lang.reflect.*;

import org.w3c.dom.Node;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
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
  final String ELEM_DESCRIPTION         = "description";
  final String ELEM_DISPLAY_NAME        = "display-name";
  final String ELEM_SERVLET             = "servlet";
  final String ELEM_SERVLET_MAPPING     = "servlet-mapping";
  final String ELEM_SERVLET_NAME        = "servlet-name";
  final String ELEM_FILTER              = "filter";
  final String ELEM_FILTER_MAPPING      = "filter-mapping";
  final String ELEM_FILTER_NAME         = "filter-name";
  final String ELEM_URL_PATTERN         = "url-pattern";
  final String ELEM_WELCOME_FILES       = "welcome-file-list";
  final String ELEM_WELCOME_FILE        = "welcome-file";
  final String ELEM_SESSION_CONFIG      = "session-config";
  final String ELEM_SESSION_TIMEOUT     = "session-timeout";
  final String ELEM_MIME_MAPPING        = "mime-mapping";
  final String ELEM_MIME_EXTENSION      = "extension";
  final String ELEM_MIME_TYPE           = "mime-type";
  final String ELEM_CONTEXT_PARAM       = "context-param";
  final String ELEM_PARAM_NAME          = "param-name";
  final String ELEM_PARAM_VALUE         = "param-value";
  final String ELEM_LISTENER            = "listener";
  final String ELEM_LISTENER_CLASS      = "listener-class";
  final String ELEM_DISTRIBUTABLE       = "distributable";
  final String ELEM_ERROR_PAGE          = "error-page";
  final String ELEM_EXCEPTION_TYPE      = "exception-type";
  final String ELEM_ERROR_CODE          = "error-code";
  final String ELEM_ERROR_LOCATION      = "location";
  final String ELEM_SECURITY_CONSTRAINT = "security-constraint";
  final String ELEM_LOGIN_CONFIG        = "login-config";
  final String ELEM_SECURITY_ROLE       = "security-role";
  final String ELEM_ROLE_NAME           = "role-name";
  final String ELEM_ENV_ENTRY           = "env-entry";

  static final String STAR = "*";
  final String WEBAPP_LOGSTREAM = "WebApp";
  
  final String JSP_SERVLET_NAME       = "JspServlet";
  final String JSP_SERVLET_MAPPING    = "*.jsp";
  final String JSP_SERVLET_CLASS      = "org.apache.jasper.servlet.JspServlet";
  final String JSP_SERVLET_LOG_LEVEL  = "WARNING";

  final String INVOKER_SERVLET_NAME   = "invoker";
  final String INVOKER_SERVLET_CLASS  = "com.rickknowles.winstone.InvokerServlet";

  final String STATIC_SERVLET_NAME    = "default";
  final String STATIC_SERVLET_CLASS   = "com.rickknowles.winstone.StaticResourceServlet";

  final String DEFAULT_REALM_CLASS    = "com.rickknowles.winstone.realm.ArgumentsRealm";
  final String DEFAULT_JNDI_MGR_CLASS = "com.rickknowles.winstone.jndi.WebAppJNDIManager";

  private WinstoneResourceBundle resources;
  private Launcher launcher;
  private String webRoot;
  private String prefix;
  private String contextName;
  private ClassLoader loader;

  private String displayName;

  private Map attributes;
  private Map initParameters;
  private Map sessions;
  private Map mimeTypes;

  private Map servletInstances;
  private Map filterInstances;

  private List contextAttributeListeners;
  private List contextListeners;
  private List sessionActivationListeners;
  private List sessionAttributeListeners;
  private List sessionListeners;

  private Map exactServletMatchMounts;
  private String servletPatterns[];
  private String servletPatternMounts[];
  private String filterPatterns[];

  private AuthenticationHandler authenticationHandler;
  private AuthenticationRealm authenticationRealm;

  private String welcomeFiles[];
  private Integer sessionTimeout;
  private boolean distributable;

  private Map errorPagesByException;
  private Map errorPagesByCode;

  private ServletConfiguration staticResourceProcessor;
  private JNDIManager jndiManager;

  /**
   * Constructor. This parses the xml and sets up for basic routing
   */
  public WebAppConfiguration(Launcher launcher,
                             String webRoot,
                             String prefix,
                             boolean directoryListings,
                             boolean useJasper,
                             boolean useWinstoneClassLoader,
                             boolean servletReloading,
                             String invokerPrefix,
                             boolean useJNDI,
                             Node elm,
                             Map argsForSecurityJNDI,
                             WinstoneResourceBundle resources)
  {
    this.launcher = launcher;
    this.resources = resources;
    this.webRoot = webRoot;
    this.prefix = (prefix != null ? prefix : "");
    this.contextName = WEBAPP_LOGSTREAM;
    this.loader = (useWinstoneClassLoader
      ? new WinstoneClassLoader(this, this.getClass().getClassLoader(), this.resources, servletReloading)
      : this.getClass().getClassLoader());

    this.attributes = new Hashtable();
    this.initParameters = new HashMap();
    this.sessions = new Hashtable();

    this.servletInstances = new HashMap();
    this.filterInstances = new HashMap();

    this.contextAttributeListeners = new ArrayList();
    this.contextListeners = new ArrayList();
    this.sessionActivationListeners = new ArrayList();
    this.sessionAttributeListeners = new ArrayList();
    this.sessionListeners = new ArrayList();

    this.errorPagesByException = new HashMap();
    this.errorPagesByCode = new HashMap();
    this.distributable = false;

    this.exactServletMatchMounts = new Hashtable();
    List localServletPatterns = new ArrayList();
    List localServletPatternMounts = new ArrayList();
    List localFilterPatterns = new ArrayList();

    List localWelcomeFiles = new ArrayList();
    List startupServlets = new ArrayList();

    List rolesAllowed = new ArrayList();
    List constraintNodes = new ArrayList();
    List envEntryNodes = new ArrayList();
    Node loginConfigNode = null;

    // Initialise jasper servlet if requested
    if (useJasper)
    {
      setAttribute("org.apache.catalina.classloader", this.loader);
      //Logger.log(Logger.DEBUG, "Setting JSP classpath: " +  this.loader.getClasspath());
      if (useWinstoneClassLoader)
        setAttribute("org.apache.catalina.jsp_classpath",
                        ((WinstoneClassLoader) this.loader).getClasspath());

      Map jspParams = new HashMap();
      jspParams.put("logVerbosityLevel", JSP_SERVLET_LOG_LEVEL);
      jspParams.put("fork", "false");
      ServletConfiguration sc = new ServletConfiguration(this, this.loader, this.resources,
        JSP_SERVLET_NAME, JSP_SERVLET_CLASS, jspParams, 3);
      this.servletInstances.put(JSP_SERVLET_NAME, sc);
      startupServlets.add(sc);
      processMapping(JSP_SERVLET_NAME, JSP_SERVLET_MAPPING, this.exactServletMatchMounts,
                     localServletPatterns, localServletPatternMounts);
    }

    // Initialise invoker servlet if requested
    if (invokerPrefix != null)
    {
      Map invokerParams = new HashMap();
      invokerParams.put("prefix", this.prefix);
      invokerParams.put("invokerPrefix", invokerPrefix);
      ServletConfiguration sc = new ServletConfiguration(this, this.loader, this.resources,
        INVOKER_SERVLET_NAME, INVOKER_SERVLET_CLASS, invokerParams, 3);
      this.servletInstances.put(INVOKER_SERVLET_NAME, sc);
      processMapping(INVOKER_SERVLET_NAME, invokerPrefix + STAR, this.exactServletMatchMounts,
                     localServletPatterns, localServletPatternMounts);
    }

    // init mimeTypes set
    this.mimeTypes = new Hashtable();
    String allTypes = this.resources.getString("WebAppConfig.DefaultMimeTypes");
    StringTokenizer mappingST = new StringTokenizer(allTypes, ":", false);
    for ( ; mappingST.hasMoreTokens(); )
    {
      String mapping = mappingST.nextToken();
      int delimPos = mapping.indexOf('=');
      if (delimPos == -1) continue;
      String extension = mapping.substring(0, delimPos);
      String mimeType = mapping.substring(delimPos + 1);
      this.mimeTypes.put(mapping.substring(0, delimPos).toLowerCase(), mapping.substring(delimPos + 1));
    }

    // Parse the web.xml file
    if (elm != null)
      for (int n = 0; n < elm.getChildNodes().getLength(); n++)
      {
        Node child = elm.getChildNodes().item(n);
        if (child.getNodeType() != Node.ELEMENT_NODE)
          continue;
        String nodeName = child.getNodeName();

        if (nodeName.equals(ELEM_DISPLAY_NAME))
          this.displayName = child.getFirstChild().getNodeValue().trim();

        else if (nodeName.equals(ELEM_DISTRIBUTABLE))
          this.distributable = true;

        else if (nodeName.equals(ELEM_SECURITY_CONSTRAINT))
          constraintNodes.add(child);

        else if (nodeName.equals(ELEM_ENV_ENTRY))
          envEntryNodes.add(child);

        else if (nodeName.equals(ELEM_LOGIN_CONFIG))
          loginConfigNode = child;

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

        // Construct the security roles
        else if (child.getNodeName().equals(ELEM_SECURITY_ROLE))
        {
          for (int m = 0; m < child.getChildNodes().getLength(); m++)
          {
            Node roleElm = (Node) child.getChildNodes().item(m);
            if ((roleElm.getNodeType() == Node.ELEMENT_NODE) &&
                (roleElm.getNodeName().equals(ELEM_ROLE_NAME)))
              rolesAllowed.add(roleElm.getFirstChild().getNodeValue().trim());
          }
        }

        // Construct the servlet instances
        else if (nodeName.equals(ELEM_SERVLET))
        {
          ServletConfiguration instance = new ServletConfiguration(this, this.loader, this.resources, child);
          this.servletInstances.put(instance.getServletName(), instance);
          if (instance.getLoadOnStartup() >= 0)
            startupServlets.add(instance);
        }

        // Construct the servlet instances
        else if (nodeName.equals(ELEM_FILTER))
        {
          FilterConfiguration instance = new FilterConfiguration(this, this.loader, this.resources, child);
          this.filterInstances.put(instance.getFilterName(), instance);
        }

        // Construct the servlet instances
        else if (nodeName.equals(ELEM_LISTENER))
        {
          String listenerClass = null;
          for (int m = 0; m < child.getChildNodes().getLength(); m++)
          {
            Node listenerElm = (Node) child.getChildNodes().item(m);
            if ((listenerElm.getNodeType() == Node.ELEMENT_NODE) &&
                (listenerElm.getNodeName().equals(ELEM_LISTENER_CLASS)))
              listenerClass = listenerElm .getFirstChild().getNodeValue().trim();
          }
          if (listenerClass != null)
          try
          {
            Class listener = Class.forName(listenerClass, true, this.loader);
            Object listenerInstance = listener.newInstance();
            if (listenerInstance instanceof ServletContextAttributeListener)
              this.contextAttributeListeners.add(listenerInstance);
            if (listenerInstance instanceof ServletContextListener)
              this.contextListeners.add(listenerInstance);
            if (listenerInstance instanceof HttpSessionActivationListener)
              this.sessionActivationListeners.add(listenerInstance);
            if (listenerInstance instanceof HttpSessionAttributeListener)
              this.sessionAttributeListeners.add(listenerInstance);
            if (listenerInstance instanceof HttpSessionListener)
              this.sessionListeners.add(listenerInstance);
            Logger.log(Logger.DEBUG, this.resources.getString("WebAppConfig.AddListener", "[#class]", listenerClass));
          }
          catch (Throwable err)
            {Logger.log(Logger.WARNING, this.resources.getString("WebAppConfig.InvalidListener", "[#class]", listenerClass));}
        }

        // Process the servlet mappings
        else if (nodeName.equals(ELEM_SERVLET_MAPPING))
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
          processMapping(name, pattern, this.exactServletMatchMounts, localServletPatterns, localServletPatternMounts);
        }

        // Process the filter mappings
        else if (nodeName.equals(ELEM_FILTER_MAPPING))
        {
          String filterName  = null;
          String servletName = null;
          String urlPattern  = null;

          // Parse the element and extract
          for (int k = 0; k < child.getChildNodes().getLength(); k++)
          {
            Node mapChild = child.getChildNodes().item(k);
            if (mapChild.getNodeType() != Node.ELEMENT_NODE)
              continue;
            String mapNodeName = mapChild.getNodeName();
            if (mapNodeName.equals(ELEM_FILTER_NAME))
              filterName = mapChild.getFirstChild().getNodeValue().trim();
            else if (mapNodeName.equals(ELEM_SERVLET_NAME))
              servletName = mapChild.getFirstChild().getNodeValue().trim();
            else if (mapNodeName.equals(ELEM_URL_PATTERN))
              urlPattern = mapChild.getFirstChild().getNodeValue().trim();
          }
          localFilterPatterns.add((servletName == null ? "U:[" + urlPattern : "S:[" + servletName)
                                  + "] F:[" + filterName + "]");
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

        // Process the error pages
        else if (nodeName.equals(ELEM_ERROR_PAGE))
        {
          String code      = null;
          String exception = null;
          String location  = null;

          // Parse the element and extract
          for (int k = 0; k < child.getChildNodes().getLength(); k++)
          {
            Node errorChild = child.getChildNodes().item(k);
            if (errorChild.getNodeType() != Node.ELEMENT_NODE)
              continue;
            String errorChildName = errorChild.getNodeName();
            if (errorChildName.equals(ELEM_ERROR_CODE))
              code = errorChild.getFirstChild().getNodeValue().trim();
            else if (errorChildName.equals(ELEM_EXCEPTION_TYPE))
              exception = errorChild.getFirstChild().getNodeValue().trim();
            else if (errorChildName.equals(ELEM_ERROR_LOCATION))
              location = errorChild.getFirstChild().getNodeValue().trim();
          }
          if ((code != null) && (location != null))
            this.errorPagesByCode.put(code.trim(), location.trim());
          if ((exception != null) && (location != null))
            this.errorPagesByException.put(exception.trim(), location.trim());
        }

        // Process the list of welcome files
        else if (nodeName.equals(ELEM_MIME_MAPPING))
        {
          String extension = null;
          String mimeType  = null;
          for (int m = 0; m < child.getChildNodes().getLength(); m++)
          {
            Node mimeTypeNode = (Node) child.getChildNodes().item(m);
            if (mimeTypeNode.getNodeType() != Node.ELEMENT_NODE)
              continue;
            else if (mimeTypeNode.getNodeName().equals(ELEM_MIME_EXTENSION))
              extension = mimeTypeNode.getFirstChild().getNodeValue().trim();
            else if (mimeTypeNode.getNodeName().equals(ELEM_MIME_TYPE))
              mimeType = mimeTypeNode.getFirstChild().getNodeValue().trim();
          }
          if ((extension != null) && (mimeType != null))
            this.mimeTypes.put(extension.toLowerCase(), mimeType);
          else
            Logger.log(Logger.WARNING, this.resources.getString("WebAppConfig.InvalidMimeMapping",
                "[#extension]", extension, "[#mimeType]", mimeType));
        }

        // Process the list of welcome files
        else if (nodeName.equals(ELEM_CONTEXT_PARAM))
        {
          String name = null;
          String value  = null;
          for (int m = 0; m < child.getChildNodes().getLength(); m++)
          {
            Node contextParamNode = (Node) child.getChildNodes().item(m);
            if (contextParamNode.getNodeType() != Node.ELEMENT_NODE)
              continue;
            else if (contextParamNode.getNodeName().equals(ELEM_PARAM_NAME))
              name = contextParamNode.getFirstChild().getNodeValue().trim();
            else if (contextParamNode.getNodeName().equals(ELEM_PARAM_VALUE))
              value = contextParamNode.getFirstChild().getNodeValue().trim();
          }
          if ((name != null) && (value != null))
            this.initParameters.put(name, value);
          else
            Logger.log(Logger.WARNING, this.resources.getString("WebAppConfig.InvalidInitParam",
                "[#name]", name, "[#value]", value));
        }
      }

    // Build the login/security role instance
    if (!constraintNodes.isEmpty() && (loginConfigNode != null))
    {
      String authMethod = null;
      for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++)
        if (loginConfigNode.getChildNodes().item(n).getNodeName().equals("auth-method"))
          authMethod = loginConfigNode.getChildNodes().item(n).getFirstChild().getNodeValue();

      // Load the appropriate auth class
      if (authMethod == null) 
        authMethod = "BASIC";
      else
        authMethod = WinstoneResourceBundle.globalReplace(authMethod, "-", "");
      String realmClassName = argsForSecurityJNDI.get("realmClass") == null ? DEFAULT_REALM_CLASS : (String) argsForSecurityJNDI.get("realmClass");
      String authClassName = "com.rickknowles.winstone.auth." + 
                             authMethod.substring(0, 1).toUpperCase() + 
                             authMethod.substring(1).toLowerCase() + "AuthenticationHandler";
      try
      {
        // Build the realm
        Class realmClass = Class.forName(realmClassName);
        Constructor realmConstr = realmClass.getConstructor(new Class[] {WinstoneResourceBundle.class, Map.class});
        this.authenticationRealm = (AuthenticationRealm) realmConstr.newInstance(new Object[] {resources, argsForSecurityJNDI});

        // Build the authentication handler
        Class authClass = Class.forName(authClassName);
        Constructor authConstr = authClass.getConstructor(new Class[] 
          {Node.class, List.class, WinstoneResourceBundle.class, AuthenticationRealm.class});
        this.authenticationHandler = (AuthenticationHandler) authConstr.newInstance(new Object[] 
          {loginConfigNode, constraintNodes, resources, authenticationRealm});
      }
      catch (ClassNotFoundException err)
        {Logger.log(Logger.DEBUG, this.resources.getString("WebAppConfiguration.AuthDisabled", "[#auth]", authMethod));}
      catch (Throwable err)
        {Logger.log(Logger.ERROR, this.resources.getString("WebAppConfiguration.AuthError", 
          "[#authClassName]", authClassName, "[#realmClassName]", realmClassName), err);}
    }

    // Instantiate the JNDI manager
    String jndiMgrClassName = (argsForSecurityJNDI.get("jndiClassName") == null ?
      DEFAULT_JNDI_MGR_CLASS : (String) argsForSecurityJNDI.get("jndiClassName"));
    if (useJNDI)
    try
    {
      // Build the realm
      Class jndiMgrClass = Class.forName(jndiMgrClassName, true, this.loader);
      Constructor jndiMgrConstr = jndiMgrClass.getConstructor(new Class[] {Map.class, List.class, ClassLoader.class});
      this.jndiManager = (JNDIManager) jndiMgrConstr.newInstance(new Object[] {argsForSecurityJNDI, envEntryNodes, this.loader});
      if (this.jndiManager != null)
        this.jndiManager.setup();
    }
    catch (ClassNotFoundException err)
      {Logger.log(Logger.DEBUG, this.resources.getString("WebAppConfiguration.JNDIDisabled"));}
    catch (Throwable err)
      {Logger.log(Logger.ERROR, this.resources.getString("WebAppConfiguration.JNDIError", 
        "[#jndiClassName]", jndiMgrClassName), err);}

    
    // Add the default index.html welcomeFile if none are supplied
    if (localWelcomeFiles.isEmpty())
    {
      if (useJasper)
        localWelcomeFiles.add("index.jsp");
      localWelcomeFiles.add("index.html");
    }

    // Take the elements out of the lists and build arrays
    this.welcomeFiles = (String []) localWelcomeFiles.toArray(
                                    new String[localWelcomeFiles.size()]);
    this.servletPatterns = (String []) localServletPatterns.toArray(
                                    new String[localServletPatterns.size()]);
    this.servletPatternMounts = (String []) localServletPatternMounts.toArray(
                                    new String[localServletPatternMounts.size()]);
    this.filterPatterns = (String []) localFilterPatterns.toArray(
                                    new String[localFilterPatterns.size()]);

    // Initialise static processor
    Map staticParams = new Hashtable();
    staticParams.put("webRoot", this.webRoot);
    staticParams.put("prefix", this.prefix);
    staticParams.put("directoryList", "" + directoryListings);
    staticParams.put("welcomeFileCount", "" + this.welcomeFiles.length);
    for (int n = 0; n < this.welcomeFiles.length; n++)
      staticParams.put("welcomeFile_" + n, this.welcomeFiles[n]);
    this.staticResourceProcessor = new ServletConfiguration(this, this.loader,
      this.resources, STATIC_SERVLET_NAME, STATIC_SERVLET_CLASS, staticParams, 0);
    this.staticResourceProcessor.getRequestDispatcher(null, this.filterInstances,
      this.filterPatterns, this.authenticationHandler);

    // Initialise load on startup servlets
    Object autoStarters[] = startupServlets.toArray();
    Arrays.sort(autoStarters);
    for (int n = 0; n < autoStarters.length; n++)
      ((ServletConfiguration) autoStarters[n]).getRequestDispatcher(null,
                  this.filterInstances, this.filterPatterns, this.authenticationHandler);

    // Send init notifies
    for (Iterator i = this.contextListeners.iterator(); i.hasNext(); )
      ((ServletContextListener) i.next()).contextInitialized(new ServletContextEvent(this));
  }

  public String getPrefix()             {return this.prefix;}
  public String getWebroot()            {return this.webRoot;}
  public Map getErrorPagesByException() {return this.errorPagesByException;}
  public Map getErrorPagesByCode()      {return this.errorPagesByCode;}
  public String[] getWelcomeFiles()     {return this.welcomeFiles;}
  public boolean isDistributable()      {return this.distributable;}

  /**
   * Iterates through each of the servlets/filters and calls destroy on them
   */
  public void destroy()
  {
    for (Iterator i = this.filterInstances.values().iterator(); i.hasNext(); )
      ((FilterConfiguration) i.next()).destroy();
    for (Iterator i = this.servletInstances.values().iterator(); i.hasNext(); )
      ((ServletConfiguration) i.next()).destroy();

    // Send destroy notifies
    for (Iterator i = this.contextListeners.iterator(); i.hasNext(); )
      ((ServletContextListener) i.next()).contextDestroyed(new ServletContextEvent(this));

    // Terminate class loader reloading thread if running
    if (this.loader instanceof WinstoneClassLoader)
      ((WinstoneClassLoader) this.loader).destroy();
  
    // Kill JNDI manager if we have one
    if (this.jndiManager != null)
      this.jndiManager.tearDown();

    // Drop all sessions
    Collection sessions = new ArrayList(this.sessions.values());
    for (Iterator i = sessions.iterator(); i.hasNext(); )
      ((WinstoneSession) i.next()).invalidate();
  }

  /**
   * Triggered by the admin thread on the reloading class loader. This
   * will cause a full shutdown and reinstantiation of the web app - not real
   * graceful, but you shouldn't have reloading turned on in high load environments.
   */
  public void resetClassLoader() throws IOException
  {
    this.launcher.destroyWebApp(this);
    this.launcher.initWebApp(this.prefix, new File(this.webRoot));
  }

  /**
   * Here we process url patterns into the exactMatch and patternMatch map/list
   */
  private void processMapping(String name, String pattern,
      Map exactPatterns, List localPatterns, List localPatternMounts)
  {
    // If pattern contains asterisk, goes in pattern match, otherwise exact
    if ((pattern == null) || (name == null))
      Logger.log(Logger.WARNING, resources.getString("WebAppConfig.InvalidMount",
                                          "[#name]", name, "[#pattern]", pattern));
    // exact mount
    else if (pattern.indexOf(STAR) == -1)
      exactPatterns.put(pattern, name);
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
        Logger.log(Logger.WARNING, resources.getString("WebAppConfig.InvalidPattern",
                                          "[#newPattern]", newPattern, "[#pattern]", pattern));
        localPatterns.add(newPattern);
      }
      else
        localPatterns.add(pattern);
      localPatternMounts.add(name);
    }
    Logger.log(Logger.FULL_DEBUG, resources.getString("WebAppConfig.MappedPattern",
                                          "[#name]", name, "[#pattern]", pattern));
  }

  /**
   * Execute the pattern match, and try to return a servlet that matches this
   * URL
   */
  private ServletConfiguration urlMatch(String path)
  {
    Logger.log(Logger.FULL_DEBUG, resources.getString("WebAppConfig.URLMatch",
                                          "[#path]", path));

    // Check exact matches first
    String exact = (String) this.exactServletMatchMounts.get(path);
    if (exact != null)
      return (ServletConfiguration) this.servletInstances.get(exact);

    // Check pattern mounts
    for (int n = 0; n < this.servletPatterns.length; n++)
      if (wildcardMatch(this.servletPatterns[n], path))
        return (ServletConfiguration) this.servletInstances.get(this.servletPatternMounts[n]);

    // return null, which indicates this should be handled as static content
    return null;
  }

  /**
   * Currently only processes simple-ish patterns (1 star at start, end, or middle,
   * and anything between 2 stars
   */
  public static boolean wildcardMatch(String pattern, String path)
  {
    int first = pattern.indexOf(STAR);
    int last = pattern.lastIndexOf(STAR);
    if (pattern.equals(STAR))
      return true;
    else if (first == last)
    {
      if (first == (pattern.length() - 1))
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

  /**
   * Constructs a session instance with the given sessionId
   * 
   * @param sessionId The sessionID for the new session
   * @return A valid session object
   */
  public WinstoneSession makeNewSession(String sessionId)
  {
    WinstoneSession ws = new WinstoneSession(sessionId, this, this.distributable);
    setSessionListeners(ws);
    if ((this.sessionTimeout != null) && (this.sessionTimeout.intValue() > 0))
      ws.setMaxInactiveInterval(this.sessionTimeout.intValue() * 60);
    else
      ws.setMaxInactiveInterval(-1);
    ws.sendCreatedNotifies();
    this.sessions.put(sessionId, ws);
    return ws;
  }

  /**
   * Retrieves the session by id. If the web app is distributable, it asks the
   * other members of the cluster if it doesn't have it itself.
   * 
   * @param sessionId The id of the session we want
   * @return A valid session instance
   */
  public WinstoneSession getSessionById(String sessionId, boolean localOnly)
  {
    WinstoneSession session = (WinstoneSession) this.sessions.get(sessionId);
    if (session != null)
      return session;
    
    // If I'm distributable ...
    if (this.distributable && !localOnly && (this.launcher.getCluster() != null))
    {
      session = this.launcher.getCluster().askClusterForSession(sessionId, this);
      if (session != null)
        this.sessions.put(sessionId, session);
      return session;
    }
    else 
      return null;
  }

  /**
   * Remove the session from the collection
   */
  public void removeSessionById(String sessionId) 
    {this.sessions.remove(sessionId);}
  
  public void setSessionListeners(WinstoneSession session)
  {
    session.setSessionActivationListeners(this.sessionActivationListeners);
    session.setSessionAttributeListeners(this.sessionAttributeListeners);
    session.setSessionListeners(this.sessionListeners);
    session.setResources(this.resources);
  }
  
  /**************************************************************************
   *
   * OK ... from here to the end is the interface implementation methods for
   * the servletContext interface.
   *
   **************************************************************************/

  // Application level attributes
  public Object getAttribute(String name)               {return this.attributes.get(name);}
  public Enumeration getAttributeNames()                {return Collections.enumeration(this.attributes.keySet());}
  public void removeAttribute(String name)
  {
    Object me = this.attributes.get(name);
    this.attributes.remove(name);
    if (me != null)
      for (Iterator i = this.contextAttributeListeners.iterator(); i.hasNext(); )
        ((ServletContextAttributeListener) i.next()).attributeRemoved(new ServletContextAttributeEvent(this, name, me));
  }
  
  public void setAttribute(String name, Object object)
  {
    Object me = this.attributes.get(name);
    this.attributes.put(name, object);
    if (me != null)
      for (Iterator i = this.contextAttributeListeners.iterator(); i.hasNext(); )
        ((ServletContextAttributeListener) i.next()).attributeReplaced(new ServletContextAttributeEvent(this, name, me));
    else
      for (Iterator i = this.contextAttributeListeners.iterator(); i.hasNext(); )
        ((ServletContextAttributeListener) i.next()).attributeAdded(new ServletContextAttributeEvent(this, name, object));
  }

  // Application level init parameters
  public String getInitParameter(String name) {return (String) this.initParameters.get(name);}
  public Enumeration getInitParameterNames()  {return Collections.enumeration(this.initParameters.keySet());}

  // Server info
  public String getServerInfo() {return resources.getString("ServerVersion");}
  public int getMajorVersion()  {return 2;}
  public int getMinorVersion()  {return 3;}

  // Weird mostly deprecated crap to do with getting servlet instances
  public javax.servlet.ServletContext getContext(String uri)  {return this;}
  public String getServletContextName()                       {return this.displayName;}

  /**
   * Look up the map of mimeType extensions, and return the type that matches
   */
  public String getMimeType(String fileName)
  {
    int dotPos = fileName.lastIndexOf('.');
    if ((dotPos != -1) && (dotPos != fileName.length() - 1))
    {
      String extension = fileName.substring(dotPos + 1).toLowerCase();
      String mimeType = (String) this.mimeTypes.get(extension);
      return mimeType;
    }
    else
      return null;
  }

  // Context level log statements
  public void log(String msg)
    {Logger.log(Logger.INFO, this.contextName, msg);}
  public void log(String message, Throwable throwable)
    {Logger.log(Logger.ERROR, this.contextName, message, throwable);}

  // Getting request dispatchers
  public javax.servlet.RequestDispatcher getNamedDispatcher(String name)
  {
    ServletConfiguration servlet = (ServletConfiguration) this.servletInstances.get(name);
    return (servlet != null
      ? servlet.getRequestDispatcher(null, this.filterInstances,
                                    this.filterPatterns, this.authenticationHandler)
      : null);
  }

  public javax.servlet.RequestDispatcher getRequestDispatcher(String path)
  {
    ServletConfiguration servlet = urlMatch(path);
    if (servlet != null)
      return servlet.getRequestDispatcher(path, this.filterInstances,
                        this.filterPatterns, this.authenticationHandler);
    else
      return this.staticResourceProcessor.getRequestDispatcher(path,
            this.filterInstances, this.filterPatterns, this.authenticationHandler);
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
      {throw new WinstoneException(resources.getString("WebAppConfig.BadResourcePath"), err);}
  }

  public InputStream getResourceAsStream(String path)
  {
    try
      {return this.getResource(path).openStream();}
    catch (IOException err)
      {throw new WinstoneException(resources.getString("WebAppConfig.ErrorOpeningStream"), err);}
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
      out.add(entry);
    }
    return out;
  }
  
  /**
   * @deprecated
   */
  public javax.servlet.Servlet getServlet(String name) {return null;}
  
  /**
   * @deprecated
   */
  public Enumeration getServletNames() {return Collections.enumeration(new ArrayList());}
  
  /**
   * @deprecated
   */
  public Enumeration getServlets() {return Collections.enumeration(new ArrayList());}
  
  /**
   * @deprecated
   */
  public void log(Exception exception, String msg)      {this.log(msg, exception);}
}

