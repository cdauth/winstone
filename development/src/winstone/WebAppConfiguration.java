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
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

import org.w3c.dom.Node;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import java.io.IOException;

/**
 * Models the web.xml file's details ... basically just a bunch of configuration
 * details, plus the actual instances of mounted servlets.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebAppConfiguration implements ServletContext
{
  static final String ELEM_DESCRIPTION         = "description";
  static final String ELEM_DISPLAY_NAME        = "display-name";
  static final String ELEM_SERVLET             = "servlet";
  static final String ELEM_SERVLET_MAPPING     = "servlet-mapping";
  static final String ELEM_SERVLET_NAME        = "servlet-name";
  static final String ELEM_FILTER              = "filter";
  static final String ELEM_FILTER_MAPPING      = "filter-mapping";
  static final String ELEM_FILTER_NAME         = "filter-name";
  static final String ELEM_DISPATCHER					 = "dispatcher";
  static final String ELEM_URL_PATTERN         = "url-pattern";
  static final String ELEM_WELCOME_FILES       = "welcome-file-list";
  static final String ELEM_WELCOME_FILE        = "welcome-file";
  static final String ELEM_SESSION_CONFIG      = "session-config";
  static final String ELEM_SESSION_TIMEOUT     = "session-timeout";
  static final String ELEM_MIME_MAPPING        = "mime-mapping";
  static final String ELEM_MIME_EXTENSION      = "extension";
  static final String ELEM_MIME_TYPE           = "mime-type";
  static final String ELEM_CONTEXT_PARAM       = "context-param";
  static final String ELEM_PARAM_NAME          = "param-name";
  static final String ELEM_PARAM_VALUE         = "param-value";
  static final String ELEM_LISTENER            = "listener";
  static final String ELEM_LISTENER_CLASS      = "listener-class";
  static final String ELEM_DISTRIBUTABLE       = "distributable";
  static final String ELEM_ERROR_PAGE          = "error-page";
  static final String ELEM_EXCEPTION_TYPE      = "exception-type";
  static final String ELEM_ERROR_CODE          = "error-code";
  static final String ELEM_ERROR_LOCATION      = "location";
  static final String ELEM_SECURITY_CONSTRAINT = "security-constraint";
  static final String ELEM_LOGIN_CONFIG        = "login-config";
  static final String ELEM_SECURITY_ROLE       = "security-role";
  static final String ELEM_ROLE_NAME           = "role-name";
  static final String ELEM_ENV_ENTRY           = "env-entry";
  static final String ELEM_LOCALE_ENC_MAP_LIST = "locale-encoding-mapping-list";
  static final String ELEM_LOCALE_ENC_MAPPING  = "locale-encoding-mapping";
  static final String ELEM_LOCALE              = "locale";
  static final String ELEM_ENCODING            = "encoding";

  static final String DISPATCHER_REQUEST = "REQUEST";
  static final String DISPATCHER_FORWARD = "FORWARD";
  static final String DISPATCHER_INCLUDE = "INCLUDE";
  static final String DISPATCHER_ERROR   = "ERROR";
  
  static final String WEBAPP_LOGSTREAM = "WebApp";
  
  static final String JSP_SERVLET_NAME       = "JspServlet";
  static final String JSP_SERVLET_MAPPING    = "*.jsp";
  public static final String JSP_SERVLET_CLASS = "org.apache.jasper.servlet.JspServlet";
  static final String JSP_SERVLET_LOG_LEVEL  = "WARNING";

  static final String INVOKER_SERVLET_NAME   = "invoker";
  static final String INVOKER_SERVLET_CLASS  = "winstone.InvokerServlet";
  static final String DEFAULT_INVOKER_PREFIX = "/servlet/";

  static final String DEFAULT_SERVLET_NAME    = "default";
  static final String DEFAULT_SERVLET_CLASS   = "winstone.StaticResourceServlet";

  static final String DEFAULT_REALM_CLASS    = "winstone.realm.ArgumentsRealm";
  static final String DEFAULT_JNDI_MGR_CLASS = "winstone.jndi.WebAppJNDIManager";

  static final String RELOADING_CL_CLASS     = "winstone.classLoader.ReloadingClassLoader";

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
  private List requestListeners;
  private List requestAttributeListeners;
  private List sessionActivationListeners;
  private List sessionAttributeListeners;
  private List sessionListeners;

  private Map exactServletMatchMounts;
  private Mapping patternMatches[];

  private Mapping filterPatternsRequest[];
  private Mapping filterPatternsForward[];
  private Mapping filterPatternsInclude[];
  private Mapping filterPatternsError[];

  private AuthenticationHandler authenticationHandler;
  private AuthenticationRealm authenticationRealm;

  private String welcomeFiles[];
  private Integer sessionTimeout;
  private boolean distributable;

  private Map errorPagesByException;
  private Map errorPagesByCode;
  private Map localeEncodingMap;
  
  //private ServletConfiguration staticResourceProcessor;
  private String defaultServletName;
  private JNDIManager jndiManager;

  private static boolean booleanArg(Map args, String name, boolean defaultTrue) 
  {
    String value = (String) args.get(name);
    if (defaultTrue)
      return (value == null) || (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    else
      return (value != null) && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
  }

  private static String stringArg(Map args, String name, String defaultValue)
  	{return (String) (args.get(name) == null ? defaultValue : args.get(name));}
  
  /**
   * Constructor. This parses the xml and sets up for basic routing
   */
  public WebAppConfiguration(Launcher launcher,
                             String webRoot,
                             String prefix,
                             ObjectPool objectPool,
                             Map startupArgs,
                             Node elm,
                             WinstoneResourceBundle resources)
  {
    this.launcher = launcher;
    this.resources = resources;
    this.webRoot = webRoot;
    this.prefix = (prefix != null ? prefix : "");
    this.contextName = WEBAPP_LOGSTREAM;

    // Build switch values
    boolean useDirLists  = booleanArg(startupArgs, "directoryListings", true);
    boolean useJasper  	  = booleanArg(startupArgs, "useJasper", false);
    boolean useWCL  	 	  = booleanArg(startupArgs, "useWinstoneClassLoader", true);
    boolean useReloading = booleanArg(startupArgs, "useServletReloading", false);
    boolean useInvoker   = booleanArg(startupArgs, "useInvoker", false);
    boolean useJNDI      = booleanArg(startupArgs, "useJNDI", false);
    
    // Try to set up the reloading class loader, and if we fail, use the normal one
    if (useWCL && useReloading)
    try
    {
      Class reloaderClass = Class.forName(RELOADING_CL_CLASS);
      Constructor reloadConstr = reloaderClass.getConstructor(new Class[] 
        {this.getClass(), ClassLoader.class, WinstoneResourceBundle.class});
      this.loader = (ClassLoader) reloadConstr.newInstance(new Object[] 
        {this, this.getClass().getClassLoader(), this.resources});
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, "Erroring setting class loader", err);}

    if (this.loader == null)
      this.loader = (useWCL
        ? new WinstoneClassLoader(this, this.getClass().getClassLoader(), this.resources)
        : this.getClass().getClassLoader());

    this.attributes = new Hashtable();
    this.initParameters = new HashMap();
    this.sessions = new Hashtable();

    this.servletInstances = new HashMap();
    this.filterInstances = new HashMap();

    this.contextAttributeListeners = new ArrayList();
    this.contextListeners = new ArrayList();
    this.requestListeners = new ArrayList();
    this.requestAttributeListeners = new ArrayList();
    this.sessionActivationListeners = new ArrayList();
    this.sessionAttributeListeners = new ArrayList();
    this.sessionListeners = new ArrayList();

    this.errorPagesByException = new HashMap();
    this.errorPagesByCode = new HashMap();
    this.distributable = false;

    this.exactServletMatchMounts = new Hashtable();
    List localFolderPatterns = new ArrayList();
    List localExtensionPatterns = new ArrayList();

    List lfpRequest = new ArrayList();
    List lfpForward = new ArrayList();
    List lfpInclude = new ArrayList();
    List lfpError   = new ArrayList();

    List localWelcomeFiles = new ArrayList();
    List startupServlets = new ArrayList();

    List rolesAllowed = new ArrayList();
    List constraintNodes = new ArrayList();
    List envEntryNodes = new ArrayList();
    Node loginConfigNode = null;

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

    this.localeEncodingMap = new HashMap();
    String encodingMapSet = this.resources.getString("WinstoneResponse.EncodingMap");
    StringTokenizer st = new StringTokenizer(encodingMapSet, ";");
    for (; st.hasMoreTokens(); )
    {
      String token = st.nextToken();
      int delimPos = token.indexOf("=");
      if (delimPos == -1)
        continue;
      this.localeEncodingMap.put(token.substring(0, delimPos), token.substring(delimPos + 1));
    }

    // Add required context atttributes
    this.attributes.put("javax.servlet.context.tempdir", new File(System.getProperty("java.io.tmpdir")));
    
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
          ServletConfiguration instance = new ServletConfiguration(this, this.loader, this.resources, this.prefix, child);
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
            if (listenerInstance instanceof ServletRequestAttributeListener)
              this.requestAttributeListeners.add(listenerInstance);
            if (listenerInstance instanceof ServletRequestListener)
              this.requestListeners.add(listenerInstance);
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
          processMapping(name, pattern, this.exactServletMatchMounts, 
              localFolderPatterns, localExtensionPatterns);
        }

        // Process the filter mappings
        else if (nodeName.equals(ELEM_FILTER_MAPPING))
        {
          String filterName  = null;
          String servletName = null;
          String urlPattern  = null;
          boolean onRequest = false;
          boolean onForward = false;
          boolean onInclude = false;
          boolean onError   = false;

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
            else if (mapNodeName.equals(ELEM_DISPATCHER))
            {
            	String dispatcherValue = mapChild.getFirstChild().getNodeValue().trim();
            	if (dispatcherValue.equals(DISPATCHER_REQUEST))
            		onRequest = true;
            	else if (dispatcherValue.equals(DISPATCHER_FORWARD))
            		onForward = true;
            	else if (dispatcherValue.equals(DISPATCHER_INCLUDE))
            		onInclude = true;
            	else if (dispatcherValue.equals(DISPATCHER_ERROR))
            		onError = true;
            }
          }
          if (!onRequest && !onInclude && !onForward && !onError)
          	onRequest = true;
          
        	Mapping mapping = null;
        	if (servletName != null)
          	mapping = Mapping.createFromLink(filterName, servletName, resources);
          else if (urlPattern != null)
          	mapping = Mapping.createFromURL(filterName, urlPattern, resources);
          else
            throw new WinstoneException("Error in filter mapping - no pattern and no servlet name");

        	if (onRequest) lfpRequest.add(mapping);
         	if (onForward) lfpForward.add(mapping);
         	if (onInclude) lfpInclude.add(mapping);
         	if (onError)   lfpError.add(mapping);
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
        
        // Process locale encoding mapping elements
        else if (nodeName.equals(ELEM_LOCALE_ENC_MAP_LIST))
        {
          for (int m = 0; m < child.getChildNodes().getLength(); m++)
          {
            Node mappingNode = (Node) child.getChildNodes().item(m);
            if (mappingNode.getNodeType() != Node.ELEMENT_NODE)
              continue;
            else if (mappingNode.getNodeName().equals(ELEM_LOCALE_ENC_MAPPING))
            {
            	String localeName = null;
            	String encoding = null;
              for (int l = 0; l < child.getChildNodes().getLength(); l++)
              {
                Node mappingChildNode = (Node) child.getChildNodes().item(l);
                if (mappingChildNode.getNodeType() != Node.ELEMENT_NODE)
                  continue;
                else if (mappingChildNode.getNodeName().equals(ELEM_LOCALE))
                	localeName = mappingChildNode.getFirstChild().getNodeValue();
                else if (mappingChildNode.getNodeName().equals(ELEM_ENCODING))
                	encoding = mappingChildNode.getFirstChild().getNodeValue();
              }
              if ((encoding != null) && (localeName != null))
              	this.localeEncodingMap.put(localeName, encoding);
            }
          }
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
      String realmClassName = stringArg(startupArgs, "realmClass", DEFAULT_REALM_CLASS);
      String authClassName = "winstone.auth." + 
                             authMethod.substring(0, 1).toUpperCase() + 
                             authMethod.substring(1).toLowerCase() + "AuthenticationHandler";
      try
      {
        // Build the realm
        Class realmClass = Class.forName(realmClassName);
        Constructor realmConstr = realmClass.getConstructor(new Class[] {WinstoneResourceBundle.class, Map.class});
        this.authenticationRealm = (AuthenticationRealm) realmConstr.newInstance(new Object[] {resources, startupArgs});

        // Build the authentication handler
        Class authClass = Class.forName(authClassName);
        Constructor authConstr = authClass.getConstructor(new Class[] 
          {Node.class, List.class, WinstoneResourceBundle.class, AuthenticationRealm.class});
        this.authenticationHandler = (AuthenticationHandler) authConstr.newInstance(new Object[] 
          {loginConfigNode, constraintNodes, resources, authenticationRealm});
      }
      catch (ClassNotFoundException err)
        {Logger.log(Logger.DEBUG, this.resources.getString("WebAppConfig.AuthDisabled", "[#type]", authMethod));}
      catch (Throwable err)
        {Logger.log(Logger.ERROR, this.resources.getString("WebAppConfig.AuthError", 
          "[#authClassName]", authClassName, "[#realmClassName]", realmClassName), err);}
    }

    // Instantiate the JNDI manager
    String jndiMgrClassName = stringArg(startupArgs, "jndiClassName", DEFAULT_JNDI_MGR_CLASS);
    if (useJNDI)
    try
    {
      // Build the realm
      Class jndiMgrClass = Class.forName(jndiMgrClassName, true, this.loader);
      Constructor jndiMgrConstr = jndiMgrClass.getConstructor(new Class[] {Map.class, List.class, ClassLoader.class});
      this.jndiManager = (JNDIManager) jndiMgrConstr.newInstance(new Object[] {startupArgs, envEntryNodes, this.loader});
      if (this.jndiManager != null)
        this.jndiManager.setup();
    }
    catch (ClassNotFoundException err)
      {Logger.log(Logger.DEBUG, this.resources.getString("WebAppConfig.JNDIDisabled"));}
    catch (Throwable err)
      {Logger.log(Logger.ERROR, this.resources.getString("WebAppConfig.JNDIError", 
        "[#jndiClassName]", jndiMgrClassName), err);}

    // Add the default index.html welcomeFile if none are supplied
    if (localWelcomeFiles.isEmpty())
    {
      if (useJasper)
        localWelcomeFiles.add("index.jsp");
      localWelcomeFiles.add("index.html");
    }
    
    // Put the name filters after the url filters, then convert to string arrays
    this.filterPatternsRequest = (Mapping []) lfpRequest.toArray(new Mapping[lfpRequest.size()]);
    this.filterPatternsForward = (Mapping []) lfpForward.toArray(new Mapping[lfpForward.size()]);
    this.filterPatternsInclude = (Mapping []) lfpInclude.toArray(new Mapping[lfpInclude.size()]);
    this.filterPatternsError   = (Mapping []) lfpError.toArray(new Mapping[lfpError.size()]);
    
    if (this.filterPatternsRequest.length > 0)
      Arrays.sort(this.filterPatternsRequest, (Comparator) this.filterPatternsRequest[0]);
    if (this.filterPatternsForward.length > 0)
      Arrays.sort(this.filterPatternsForward, (Comparator) this.filterPatternsForward[0]);
    if (this.filterPatternsInclude.length > 0)
      Arrays.sort(this.filterPatternsInclude, (Comparator) this.filterPatternsInclude[0]);
    if (this.filterPatternsError.length > 0)
      Arrays.sort(this.filterPatternsError, (Comparator) this.filterPatternsError[0]);
    
    this.welcomeFiles 	= (String []) localWelcomeFiles.toArray(
        new String[localWelcomeFiles.size()]);

    // If we haven't explicitly mapped the default servlet, map it here
    if (this.defaultServletName == null)
      this.defaultServletName = DEFAULT_SERVLET_NAME;

    // If we don't have an instance of the default servlet, mount the inbuilt one
    if (this.servletInstances.get(this.defaultServletName) == null)
    {
    	Map staticParams = new Hashtable();
    	staticParams.put("webRoot", this.webRoot);
    	staticParams.put("prefix", this.prefix);
    	staticParams.put("directoryList", "" + useDirLists);
    	staticParams.put("welcomeFileCount", "" + this.welcomeFiles.length);
    	for (int n = 0; n < this.welcomeFiles.length; n++)
    		staticParams.put("welcomeFile_" + n, this.welcomeFiles[n]);
    	ServletConfiguration defaultServlet = new ServletConfiguration(this, this.loader,
    		this.resources, this.prefix, this.defaultServletName, DEFAULT_SERVLET_CLASS, staticParams, 0);
    	defaultServlet.getRequestDispatcher(this.filterInstances);
    	this.servletInstances.put(this.defaultServletName, defaultServlet);
    	startupServlets.add(defaultServlet);
    }

    // Initialise jasper servlet if requested
    if (useJasper)
    {
      setAttribute("org.apache.catalina.classloader", this.loader);
      //Logger.log(Logger.DEBUG, "Setting JSP classpath: " +  this.loader.getClasspath());
      if (useWCL)
        setAttribute("org.apache.catalina.jsp_classpath",
                        ((WinstoneClassLoader) this.loader).getClasspath());

      Map jspParams = new HashMap();
      addJspServletParams(jspParams);
      ServletConfiguration sc = new ServletConfiguration(this, this.loader, this.resources,
        this.prefix, JSP_SERVLET_NAME, JSP_SERVLET_CLASS, jspParams, 3);
      this.servletInstances.put(JSP_SERVLET_NAME, sc);
      startupServlets.add(sc);
      processMapping(JSP_SERVLET_NAME, JSP_SERVLET_MAPPING, this.exactServletMatchMounts, 
          localFolderPatterns, localExtensionPatterns);
    }

    // Initialise invoker servlet if requested
    if (useInvoker)
    {
      // Get generic options
      String invokerPrefix = stringArg(startupArgs, "invokerPrefix", DEFAULT_INVOKER_PREFIX);
      Map invokerParams = new HashMap();
      invokerParams.put("prefix", this.prefix);
      invokerParams.put("invokerPrefix", invokerPrefix);
      ServletConfiguration sc = new ServletConfiguration(this, this.loader, this.resources,
          this.prefix, INVOKER_SERVLET_NAME, INVOKER_SERVLET_CLASS, invokerParams, 3);
      this.servletInstances.put(INVOKER_SERVLET_NAME, sc);
      processMapping(INVOKER_SERVLET_NAME, invokerPrefix + Mapping.STAR, this.exactServletMatchMounts, 
          localFolderPatterns, localExtensionPatterns);
    }

    // Sort the folder patterns so the longest paths are first
    localFolderPatterns.addAll(localExtensionPatterns);
    this.patternMatches = (Mapping []) localFolderPatterns.toArray(
				new Mapping[localFolderPatterns.size()]);
    if (this.patternMatches.length > 0)
      Arrays.sort(this.patternMatches, (Comparator) this.patternMatches[0]);
    
    // Send init notifies
    for (Iterator i = this.contextListeners.iterator(); i.hasNext(); )
      ((ServletContextListener) i.next()).contextInitialized(new ServletContextEvent(this));

    // Initialise load on startup servlets
    Object autoStarters[] = startupServlets.toArray();
    Arrays.sort(autoStarters);
    for (int n = 0; n < autoStarters.length; n++)
      ((ServletConfiguration) autoStarters[n]).getRequestDispatcher(this.filterInstances);
  }

  public String getPrefix()             {return this.prefix;}
  public String getWebroot()            {return this.webRoot;}
  public Map getErrorPagesByException() {return this.errorPagesByException;}
  public Map getErrorPagesByCode()      {return this.errorPagesByCode;}
  public Map getLocaleEncodingMap()     {return this.localeEncodingMap;}
  public String[] getWelcomeFiles()     {return this.welcomeFiles;}
  public boolean isDistributable()     {return this.distributable;}

  public static void addJspServletParams(Map jspParams)
  {
    jspParams.put("logVerbosityLevel", JSP_SERVLET_LOG_LEVEL);
    jspParams.put("fork", "false");
  }
  
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
   * Marks a request/response as using this context
   */
  public void setRequestResponse(WinstoneRequest req, WinstoneResponse rsp)
  {
    req.setWebAppConfig(this);
    rsp.setWebAppConfig(this);
    
    // Set listeners on the request
    req.setRequestListeners(this.requestListeners);
    req.setRequestAttributeListeners(this.requestAttributeListeners);
  }

  /**
   * Here we process url patterns into the exactMatch and patternMatch lists
   */
  private void processMapping(String name, String pattern, Map exactPatterns, 
      List folderPatterns, List extensionPatterns)
  {
    Mapping urlPattern = null;
    try 
    	{urlPattern = Mapping.createFromURL(name, pattern, resources);}
    catch (WinstoneException err)
    {
      Logger.log(Logger.WARNING, err.getMessage());
      return;
    }
    
    // put the pattern in the correct list
    if (urlPattern.getPatternType() == Mapping.EXACT_PATTERN)
      exactPatterns.put(urlPattern.getUrlPattern(), name);
    else if (urlPattern.getPatternType() == Mapping.FOLDER_PATTERN)
      folderPatterns.add(urlPattern);
    else if (urlPattern.getPatternType() == Mapping.EXTENSION_PATTERN)
      extensionPatterns.add(urlPattern);
    else if (urlPattern.getPatternType() == Mapping.DEFAULT_SERVLET)
      this.defaultServletName = name;
    else
      Logger.log(Logger.WARNING, resources.getString("WebAppConfig.InvalidMount",
          																	"[#name]", name, "[#pattern]", pattern));
  }

  /**
   * Execute the pattern match, and try to return a servlet that matches this
   * URL
   */
  private ServletConfiguration urlMatch(String path, StringBuffer servletPath, StringBuffer pathInfo)
  {
    Logger.log(Logger.FULL_DEBUG, resources.getString("WebAppConfig.URLMatch",
                                          "[#path]", path));

    // Check exact matches first
    String exact = (String) this.exactServletMatchMounts.get(path);
    if (exact != null)
    {
      servletPath.append(path);
      // pathInfo.append(""); // a hack - empty becomes null later
      return (ServletConfiguration) this.servletInstances.get(exact);
    }
    
    // Inexact mount check
    for (int n = 0; n < this.patternMatches.length; n++)
    {
      Mapping urlPattern = (Mapping) this.patternMatches[n];
      if (urlPattern.match(path, servletPath, pathInfo))
        return (ServletConfiguration) this.servletInstances.get(urlPattern.getMappedTo());
    }

    // return null, which indicates this should be handled by default servlet
    //servletPath.append("");  // unneeded
    pathInfo.append(path);
    return (ServletConfiguration) this.servletInstances.get(this.defaultServletName);
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
    ws.setLastAccessedDate(System.currentTimeMillis());
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

  /**
   * Named dispatcher - this basically gets us a simple exact dispatcher
   * (no url matching, no request attributes and no security)
   */
  public javax.servlet.RequestDispatcher getNamedDispatcher(String name)
  {
    ServletConfiguration servlet = (ServletConfiguration) this.servletInstances.get(name);
    if (servlet != null)
    {
      RequestDispatcher rd = servlet.getRequestDispatcher(this.filterInstances);
      rd.setForNamedDispatcher(this.filterPatternsForward, this.filterPatternsInclude);
      return rd;
    }
    else return null;
  }

  /**
   * Gets a dispatcher, which sets the request attributes, etc on a forward/include.
   * Doesn't execute security though.
   */
  public javax.servlet.RequestDispatcher getRequestDispatcher(String uriInsideWebapp)
  {
    if (!uriInsideWebapp.startsWith("/"))
      return null;
  	
    // Parse the url for query string, etc 
    String queryString = "";
  	int questionPos = uriInsideWebapp.indexOf('?');
  	if (questionPos != -1)
  	{
  	  if (questionPos != uriInsideWebapp.length() - 1)
  	    queryString = uriInsideWebapp.substring(questionPos + 1);
  	  uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
  	}
  	
  	// Return the dispatcher
  	StringBuffer servletPath = new StringBuffer();
  	StringBuffer pathInfo = new StringBuffer();
    ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
    if (servlet != null)
    {
      RequestDispatcher rd = servlet.getRequestDispatcher(this.filterInstances);
      rd.setForURLDispatcher(servletPath.toString(), 
          pathInfo.toString().equals("") ? null : pathInfo.toString(), 
          queryString, uriInsideWebapp, this.filterPatternsForward, 
          this.filterPatternsInclude);
      return rd;
    }
    else return null;
  }

  /**
   * Creates the dispatcher that corresponds to a request level dispatch (ie the initial
   * entry point). The difference here is that we need to set up the dispatcher so 
   * that on a forward, it executes the security checks and the request filters, while not
   * setting any of the request attributes for a forward.
   */
  public javax.servlet.RequestDispatcher getInitialDispatcher(String uriInsideWebapp,
      WinstoneRequest request)
  {
    if (!uriInsideWebapp.equals("") && !uriInsideWebapp.startsWith("/"))
      return null;
  	
    // Parse the url for query string, etc 
    String queryString = "";
  	int questionPos = uriInsideWebapp.indexOf('?');
  	if (questionPos != -1)
  	{
  	  if (questionPos != uriInsideWebapp.length() - 1)
  	    queryString = uriInsideWebapp.substring(questionPos + 1);
  	  uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
  	}
  	
  	// Return the dispatcher
  	StringBuffer servletPath = new StringBuffer();
  	StringBuffer pathInfo = new StringBuffer();
    ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
    if (servlet != null)
    {
      request.setQueryString(queryString);
      // parse params here ? yes for now
      request.getParameters().putAll(WinstoneRequest.extractParameters(queryString, request.getEncoding(), resources));
      request.setServletPath(servletPath.toString());
      request.setPathInfo(pathInfo.toString().equals("") ? null : pathInfo.toString());
      request.setRequestURI(this.prefix + uriInsideWebapp);
      RequestDispatcher rd = servlet.getRequestDispatcher(this.filterInstances);
      rd.setForInitialDispatcher(request.getServletPath(), request.getPathInfo(), 
          uriInsideWebapp, this.filterPatternsRequest, this.authenticationHandler);
      return rd;
    }
    else return null;
  }

  // Getting resources via the classloader
  public URL getResource(String path)
  {
    if (path == null)
      return null;
    else if (!path.startsWith("/"))
      throw new WinstoneException(resources.getString("WebAppConfig.BadResourcePath", "[#path]", path));      
  	else try
      {return new File(webRoot, path.substring(1)).toURL();}
    catch (MalformedURLException err)
      {throw new WinstoneException(resources.getString("WebAppConfig.BadResourcePath", "[#path]", path), err);}
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
    else if (!path.startsWith("/"))
      return null;
    else try
    {
   	  File res = new File(this.webRoot, path);
   	  if (res.isDirectory())
      	return res.getCanonicalPath() + "/";
      else
      	return res.getCanonicalPath();
    }
    catch (IOException err) {return null;}
  }

  public Set getResourcePaths(String path)
  {
    // Trim the prefix
    if (path == null)
      return null;
    else if (!path.startsWith("/"))
      throw new WinstoneException(resources.getString("WebAppConfig.BadResourcePath", "[#path]", path));      
    else
    {
      String workingPath = null;
      if (path.equals("/"))
        workingPath = "";
      else
      {
        boolean lastCharIsSlash = path.charAt(path.length() - 1) == '/';
        workingPath = path.substring(1, path.length() - (lastCharIsSlash ? 1 : 0));
      }
      File inPath = new File(this.webRoot, workingPath.equals("") ? "." : workingPath);
      if (!inPath.exists())
        return null;
      else if (!inPath.isDirectory())
        return null;
      
      // Find all the files in this folder
      File children[] = inPath.listFiles();
      Set out = new HashSet();
      for (int n = 0; n < children.length; n++)
      {
        // Write the entry as prefix + subpath + child element
        String entry = //this.prefix + 
        			"/" + (workingPath.length() != 0 ? workingPath + "/" : "") +
              children[n].getName() + (children[n].isDirectory() ? "/" : "");
        out.add(entry);
      }
      return out;
    }
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

