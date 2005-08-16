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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.w3c.dom.Node;

/**
 * Models the web.xml file's details ... basically just a bunch of configuration
 * details, plus the actual instances of mounted servlets.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebAppConfiguration implements ServletContext, Comparator {
    private static final String ELEM_DESCRIPTION = "description";
    private static final String ELEM_DISPLAY_NAME = "display-name";
    private static final String ELEM_SERVLET = "servlet";
    private static final String ELEM_SERVLET_MAPPING = "servlet-mapping";
    private static final String ELEM_SERVLET_NAME = "servlet-name";
    private static final String ELEM_FILTER = "filter";
    private static final String ELEM_FILTER_MAPPING = "filter-mapping";
    private static final String ELEM_FILTER_NAME = "filter-name";
    private static final String ELEM_DISPATCHER = "dispatcher";
    private static final String ELEM_URL_PATTERN = "url-pattern";
    private static final String ELEM_WELCOME_FILES = "welcome-file-list";
    private static final String ELEM_WELCOME_FILE = "welcome-file";
    private static final String ELEM_SESSION_CONFIG = "session-config";
    private static final String ELEM_SESSION_TIMEOUT = "session-timeout";
    private static final String ELEM_MIME_MAPPING = "mime-mapping";
    private static final String ELEM_MIME_EXTENSION = "extension";
    private static final String ELEM_MIME_TYPE = "mime-type";
    private static final String ELEM_CONTEXT_PARAM = "context-param";
    private static final String ELEM_PARAM_NAME = "param-name";
    private static final String ELEM_PARAM_VALUE = "param-value";
    private static final String ELEM_LISTENER = "listener";
    private static final String ELEM_LISTENER_CLASS = "listener-class";
    private static final String ELEM_DISTRIBUTABLE = "distributable";
    private static final String ELEM_ERROR_PAGE = "error-page";
    private static final String ELEM_EXCEPTION_TYPE = "exception-type";
    private static final String ELEM_ERROR_CODE = "error-code";
    private static final String ELEM_ERROR_LOCATION = "location";
    private static final String ELEM_SECURITY_CONSTRAINT = "security-constraint";
    private static final String ELEM_LOGIN_CONFIG = "login-config";
    private static final String ELEM_SECURITY_ROLE = "security-role";
    private static final String ELEM_ROLE_NAME = "role-name";
    private static final String ELEM_ENV_ENTRY = "env-entry";
    private static final String ELEM_LOCALE_ENC_MAP_LIST = "locale-encoding-mapping-list";
    private static final String ELEM_LOCALE_ENC_MAPPING = "locale-encoding-mapping";
    private static final String ELEM_LOCALE = "locale";
    private static final String ELEM_ENCODING = "encoding";
    private static final String DISPATCHER_REQUEST = "REQUEST";
    private static final String DISPATCHER_FORWARD = "FORWARD";
    private static final String DISPATCHER_INCLUDE = "INCLUDE";
    private static final String DISPATCHER_ERROR = "ERROR";
    private static final String JSP_SERVLET_NAME = "JspServlet";
    private static final String JSP_SERVLET_MAPPING = "*.jsp";
    private static final String JSP_SERVLET_LOG_LEVEL = "WARNING";
    private static final String INVOKER_SERVLET_NAME = "invoker";
    private static final String INVOKER_SERVLET_CLASS = "winstone.InvokerServlet";
    private static final String DEFAULT_INVOKER_PREFIX = "/servlet/";
    private static final String DEFAULT_SERVLET_NAME = "default";
    private static final String DEFAULT_SERVLET_CLASS = "winstone.StaticResourceServlet";
    private static final String DEFAULT_REALM_CLASS = "winstone.realm.ArgumentsRealm";
    private static final String DEFAULT_JNDI_MGR_CLASS = "winstone.jndi.WebAppJNDIManager";
    private static final String RELOADING_CL_CLASS = "winstone.classLoader.ReloadingClassLoader";
    private static final String ERROR_SERVLET_NAME = "winstoneErrorServlet";
    private static final String ERROR_SERVLET_CLASS = "winstone.ErrorServlet";
    
    static final String JSP_SERVLET_CLASS = "org.apache.jasper.servlet.JspServlet";
    
    private WinstoneResourceBundle resources;
    private WebAppGroup ownerWebappGroup;
    private Cluster cluster;
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
    private ServletContextAttributeListener contextAttributeListeners[];
    private ServletContextListener contextListeners[];
    private ServletRequestListener requestListeners[];
    private ServletRequestAttributeListener requestAttributeListeners[];
    private HttpSessionActivationListener sessionActivationListeners[];
    private HttpSessionAttributeListener sessionAttributeListeners[];
    private HttpSessionListener sessionListeners[];
    private Throwable contextStartupError;
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
    private Class[] errorPagesByExceptionKeysSorted;
    private Map errorPagesByException;
    private Map errorPagesByCode;
    private Map localeEncodingMap;
    private String defaultServletName;
    private String errorServletName;
    private JNDIManager jndiManager;

    public static boolean booleanArg(Map args, String name, boolean defaultTrue) {
        String value = (String) args.get(name);
        if (defaultTrue)
            return (value == null) || (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        else
            return (value != null) && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }

    public static String stringArg(Map args, String name, String defaultValue) {
        return (String) (args.get(name) == null ? defaultValue : args.get(name));
    }

    public static int intArg(Map args, String name, int defaultValue) {
        return Integer.parseInt(stringArg(args, name, "" + defaultValue));
    }

    /**
     * Constructor. This parses the xml and sets up for basic routing
     */
    public WebAppConfiguration(WebAppGroup ownerWebappGroup, Cluster cluster, String webRoot,
            String prefix, ObjectPool objectPool, Map startupArgs, Node elm,
            WinstoneResourceBundle resources, ClassLoader parentClassLoader,
            List parentClassPaths, String contextName) {
        this.ownerWebappGroup = ownerWebappGroup;
        this.resources = resources;
        this.webRoot = webRoot;
        this.prefix = prefix;
        this.contextName = contextName;

        // Build switch values
        boolean useDirLists = booleanArg(startupArgs, "directoryListings", true);
        boolean useJasper = booleanArg(startupArgs, "useJasper", false);
        boolean useWCL = booleanArg(startupArgs, "useWinstoneClassLoader", true);
        boolean useReloading = booleanArg(startupArgs, "useServletReloading",
                false);
        boolean useInvoker = booleanArg(startupArgs, "useInvoker", true);
        boolean useJNDI = booleanArg(startupArgs, "useJNDI", false);

        // Try to set up the reloading class loader, and if we fail, use the
        // normal one
        if (useWCL && useReloading) {
            try {
                Class reloaderClass = Class.forName(RELOADING_CL_CLASS);
                Constructor reloadConstr = reloaderClass
                        .getConstructor(new Class[] { this.getClass(),
                                ClassLoader.class, List.class, String.class,
                                WinstoneResourceBundle.class });
                this.loader = (ClassLoader) reloadConstr
                        .newInstance(new Object[] { this, parentClassLoader,
                                parentClassPaths, webRoot, this.resources });
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, resources, "WebAppConfig.CLError", err);
            }
        }

        if (this.loader == null) {
            if (useWCL) {
                this.loader = new WinstoneClassLoader(this, parentClassLoader, 
                        parentClassPaths, webRoot, this.resources);
            } else {
                this.loader = parentClassLoader;
            }
        }

        // Check jasper is available
        if (useJasper) {
            try {
                Class.forName(JSP_SERVLET_CLASS, false, this.loader);
            } catch (Throwable err) {
                Logger.log(Logger.WARNING, resources, 
                        "WebAppConfig.JasperNotFound");
                Logger.log(Logger.DEBUG, resources, 
                        "WebAppConfig.JasperLoadException", err);
                useJasper = false;
            }
        }

        this.attributes = new Hashtable();
        this.initParameters = new HashMap();
        this.sessions = new Hashtable();

        this.servletInstances = new HashMap();
        this.filterInstances = new HashMap();

        List contextAttributeListeners = new ArrayList();
        List contextListeners = new ArrayList();
        List requestListeners = new ArrayList();
        List requestAttributeListeners = new ArrayList();
        List sessionActivationListeners = new ArrayList();
        List sessionAttributeListeners = new ArrayList();
        List sessionListeners = new ArrayList();

        this.errorPagesByException = new HashMap();
        this.errorPagesByCode = new HashMap();
        boolean distributable = false;

        this.exactServletMatchMounts = new Hashtable();
        List localFolderPatterns = new ArrayList();
        List localExtensionPatterns = new ArrayList();

        List lfpRequest = new ArrayList();
        List lfpForward = new ArrayList();
        List lfpInclude = new ArrayList();
        List lfpError = new ArrayList();

        List localWelcomeFiles = new ArrayList();
        List startupServlets = new ArrayList();

        Set rolesAllowed = new HashSet();
        List constraintNodes = new ArrayList();
        List envEntryNodes = new ArrayList();
        List localErrorPagesByExceptionList = new ArrayList();

        Node loginConfigNode = null;

        // init mimeTypes set
        this.mimeTypes = new Hashtable();
        String allTypes = this.resources.getString("WebAppConfig.DefaultMimeTypes");
        StringTokenizer mappingST = new StringTokenizer(allTypes, ":", false);
        for (; mappingST.hasMoreTokens();) {
            String mapping = mappingST.nextToken();
            int delimPos = mapping.indexOf('=');
            if (delimPos == -1)
                continue;
            String extension = mapping.substring(0, delimPos);
            String mimeType = mapping.substring(delimPos + 1);
            this.mimeTypes.put(extension.toLowerCase(), mimeType);
        }

        this.localeEncodingMap = new HashMap();
        String encodingMapSet = this.resources
                .getString("WebAppConfig.EncodingMap");
        StringTokenizer st = new StringTokenizer(encodingMapSet, ";");
        for (; st.hasMoreTokens();) {
            String token = st.nextToken();
            int delimPos = token.indexOf("=");
            if (delimPos == -1)
                continue;
            this.localeEncodingMap.put(token.substring(0, delimPos), token
                    .substring(delimPos + 1));
        }

        // Add required context atttributes
        this.attributes.put("javax.servlet.context.tempdir", new File(System
                .getProperty("java.io.tmpdir")));

        // Parse the web.xml file
        if (elm != null)
            for (int n = 0; n < elm.getChildNodes().getLength(); n++) {
                Node child = elm.getChildNodes().item(n);
                if (child.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                String nodeName = child.getNodeName();

                if (nodeName.equals(ELEM_DISPLAY_NAME))
                    this.displayName = child.getFirstChild().getNodeValue()
                            .trim();

                else if (nodeName.equals(ELEM_DISTRIBUTABLE))
                    distributable = true;

                else if (nodeName.equals(ELEM_SECURITY_CONSTRAINT))
                    constraintNodes.add(child);

                else if (nodeName.equals(ELEM_ENV_ENTRY))
                    envEntryNodes.add(child);

                else if (nodeName.equals(ELEM_LOGIN_CONFIG))
                    loginConfigNode = child;

                // Session config elements
                else if (nodeName.equals(ELEM_SESSION_CONFIG)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node timeoutElm = child.getChildNodes().item(m);
                        if ((timeoutElm.getNodeType() == Node.ELEMENT_NODE)
                                && (timeoutElm.getNodeName()
                                        .equals(ELEM_SESSION_TIMEOUT)))
                            this.sessionTimeout = new Integer(timeoutElm
                                    .getFirstChild().getNodeValue().trim());
                    }
                }

                // Construct the security roles
                else if (child.getNodeName().equals(ELEM_SECURITY_ROLE)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node roleElm = child.getChildNodes().item(m);
                        if ((roleElm.getNodeType() == Node.ELEMENT_NODE)
                                && (roleElm.getNodeName()
                                        .equals(ELEM_ROLE_NAME)))
                            rolesAllowed.add(roleElm.getFirstChild()
                                    .getNodeValue().trim());
                    }
                }

                // Construct the servlet instances
                else if (nodeName.equals(ELEM_SERVLET)) {
                    ServletConfiguration instance = new ServletConfiguration(
                            this, this.resources, child);
                    this.servletInstances.put(instance.getServletName(),
                            instance);
                    if (instance.getLoadOnStartup() >= 0)
                        startupServlets.add(instance);
                }

                // Construct the servlet instances
                else if (nodeName.equals(ELEM_FILTER)) {
                    FilterConfiguration instance = new FilterConfiguration(
                            this, this.loader, this.resources, child);
                    this.filterInstances
                            .put(instance.getFilterName(), instance);
                }

                // Construct the servlet instances
                else if (nodeName.equals(ELEM_LISTENER)) {
                    String listenerClass = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node listenerElm = child.getChildNodes().item(m);
                        if ((listenerElm.getNodeType() == Node.ELEMENT_NODE)
                                && (listenerElm.getNodeName()
                                        .equals(ELEM_LISTENER_CLASS)))
                            listenerClass = listenerElm.getFirstChild()
                                    .getNodeValue().trim();
                    }
                    if (listenerClass != null)
                        try {
                            Class listener = Class.forName(listenerClass, true,
                                    this.loader);
                            Object listenerInstance = listener.newInstance();
                            if (listenerInstance instanceof ServletContextAttributeListener)
                                contextAttributeListeners.add(listenerInstance);
                            if (listenerInstance instanceof ServletContextListener)
                                contextListeners.add(listenerInstance);
                            if (listenerInstance instanceof ServletRequestAttributeListener)
                                requestAttributeListeners.add(listenerInstance);
                            if (listenerInstance instanceof ServletRequestListener)
                                requestListeners.add(listenerInstance);
                            if (listenerInstance instanceof HttpSessionActivationListener)
                                sessionActivationListeners
                                        .add(listenerInstance);
                            if (listenerInstance instanceof HttpSessionAttributeListener)
                                sessionAttributeListeners.add(listenerInstance);
                            if (listenerInstance instanceof HttpSessionListener)
                                sessionListeners.add(listenerInstance);
                            Logger.log(Logger.DEBUG, this.resources,
                                    "WebAppConfig.AddListener", listenerClass);
                        } catch (Throwable err) {
                            Logger.log(Logger.WARNING, this.resources,
                                    "WebAppConfig.InvalidListener",
                                    listenerClass);
                        }
                }

                // Process the servlet mappings
                else if (nodeName.equals(ELEM_SERVLET_MAPPING)) {
                    String name = null;
                    String pattern = null;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                        Node mapChild = child.getChildNodes().item(k);
                        if (mapChild.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        String mapNodeName = mapChild.getNodeName();
                        if (mapNodeName.equals(ELEM_SERVLET_NAME))
                            name = mapChild.getFirstChild().getNodeValue()
                                    .trim();
                        else if (mapNodeName.equals(ELEM_URL_PATTERN))
                            pattern = mapChild.getFirstChild().getNodeValue()
                                    .trim();
                    }
                    processMapping(name, pattern, this.exactServletMatchMounts,
                            localFolderPatterns, localExtensionPatterns);
                }

                // Process the filter mappings
                else if (nodeName.equals(ELEM_FILTER_MAPPING)) {
                    String filterName = null;
                    String servletName = null;
                    String urlPattern = null;
                    boolean onRequest = false;
                    boolean onForward = false;
                    boolean onInclude = false;
                    boolean onError = false;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
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
                        else if (mapNodeName.equals(ELEM_DISPATCHER)) {
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
                    if (servletName != null) {
                        mapping = Mapping.createFromLink(filterName, servletName, resources);
                    } else if (urlPattern != null) {
                        mapping = Mapping.createFromURL(filterName, urlPattern, resources);
                    } else {
                        throw new WinstoneException("Error in filter mapping - no pattern and no servlet name");
                    }

                    if (onRequest)
                        lfpRequest.add(mapping);
                    if (onForward)
                        lfpForward.add(mapping);
                    if (onInclude)
                        lfpInclude.add(mapping);
                    if (onError)
                        lfpError.add(mapping);
                }

                // Process the list of welcome files
                else if (nodeName.equals(ELEM_WELCOME_FILES))
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node welcomeFile = child.getChildNodes().item(m);
                        if ((welcomeFile.getNodeType() == Node.ELEMENT_NODE)
                                && welcomeFile.getNodeName().equals(ELEM_WELCOME_FILE)
                                && (welcomeFile.getFirstChild() != null)) {
                            localWelcomeFiles.add(welcomeFile.getFirstChild()
                                    .getNodeValue().trim());
                        }
                    }

                // Process the error pages
                else if (nodeName.equals(ELEM_ERROR_PAGE)) {
                    String code = null;
                    String exception = null;
                    String location = null;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                        Node errorChild = child.getChildNodes().item(k);
                        if (errorChild.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        String errorChildName = errorChild.getNodeName();
                        if (errorChildName.equals(ELEM_ERROR_CODE))
                            code = errorChild.getFirstChild().getNodeValue()
                                    .trim();
                        else if (errorChildName.equals(ELEM_EXCEPTION_TYPE))
                            exception = errorChild.getFirstChild()
                                    .getNodeValue().trim();
                        else if (errorChildName.equals(ELEM_ERROR_LOCATION))
                            location = errorChild.getFirstChild()
                                    .getNodeValue().trim();
                    }
                    if ((code != null) && (location != null))
                        this.errorPagesByCode.put(code.trim(), location.trim());
                    if ((exception != null) && (location != null))
                        try {
                            Class exceptionClass = Class.forName(exception
                                    .trim(), false, this.loader);
                            localErrorPagesByExceptionList.add(exceptionClass);
                            this.errorPagesByException.put(exceptionClass,
                                    location.trim());
                        } catch (ClassNotFoundException err) {
                            Logger
                                    .log(Logger.ERROR, resources,
                                            "WebAppConfig.ExceptionNotFound",
                                            exception);
                        }
                }

                // Process the list of welcome files
                else if (nodeName.equals(ELEM_MIME_MAPPING)) {
                    String extension = null;
                    String mimeType = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node mimeTypeNode = child.getChildNodes().item(m);
                        if (mimeTypeNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (mimeTypeNode.getNodeName().equals(
                                ELEM_MIME_EXTENSION))
                            extension = mimeTypeNode.getFirstChild()
                                    .getNodeValue().trim();
                        else if (mimeTypeNode.getNodeName().equals(
                                ELEM_MIME_TYPE))
                            mimeType = mimeTypeNode.getFirstChild()
                                    .getNodeValue().trim();
                    }
                    if ((extension != null) && (mimeType != null))
                        this.mimeTypes.put(extension.toLowerCase(), mimeType);
                    else
                        Logger.log(Logger.WARNING, this.resources,
                                "WebAppConfig.InvalidMimeMapping",
                                new String[] { extension, mimeType });
                }

                // Process the list of welcome files
                else if (nodeName.equals(ELEM_CONTEXT_PARAM)) {
                    String name = null;
                    String value = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node contextParamNode = child.getChildNodes().item(m);
                        if (contextParamNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (contextParamNode.getNodeName().equals(
                                ELEM_PARAM_NAME))
                            name = contextParamNode.getFirstChild()
                                    .getNodeValue().trim();
                        else if (contextParamNode.getNodeName().equals(
                                ELEM_PARAM_VALUE))
                            value = contextParamNode.getFirstChild()
                                    .getNodeValue().trim();
                    }
                    if ((name != null) && (value != null))
                        this.initParameters.put(name, value);
                    else
                        Logger.log(Logger.WARNING, this.resources,
                                "WebAppConfig.InvalidInitParam", new String[] {
                                        name, value });
                }

                // Process locale encoding mapping elements
                else if (nodeName.equals(ELEM_LOCALE_ENC_MAP_LIST)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node mappingNode = child.getChildNodes().item(m);
                        if (mappingNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (mappingNode.getNodeName().equals(
                                ELEM_LOCALE_ENC_MAPPING)) {
                            String localeName = null;
                            String encoding = null;
                            for (int l = 0; l < child.getChildNodes()
                                    .getLength(); l++) {
                                Node mappingChildNode = child.getChildNodes()
                                        .item(l);
                                if (mappingChildNode.getNodeType() != Node.ELEMENT_NODE)
                                    continue;
                                else if (mappingChildNode.getNodeName().equals(
                                        ELEM_LOCALE))
                                    localeName = mappingChildNode
                                            .getFirstChild().getNodeValue();
                                else if (mappingChildNode.getNodeName().equals(
                                        ELEM_ENCODING))
                                    encoding = mappingChildNode.getFirstChild()
                                            .getNodeValue();
                            }
                            if ((encoding != null) && (localeName != null))
                                this.localeEncodingMap
                                        .put(localeName, encoding);
                        }
                    }
                }
            }
        
        // If not distributable, remove the cluster reference
        if (!distributable && (cluster != null)) {
            Logger.log(Logger.INFO, this.resources,
                    "WebAppConfig.ClusterOffNotDistributable", this.contextName);
        } else {
            this.cluster = cluster;
        }


        // Build the login/security role instance
        if (!constraintNodes.isEmpty() && (loginConfigNode != null)) {
            String authMethod = null;
            for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++)
                if (loginConfigNode.getChildNodes().item(n).getNodeName()
                        .equals("auth-method"))
                    authMethod = loginConfigNode.getChildNodes().item(n)
                            .getFirstChild().getNodeValue();

            // Load the appropriate auth class
            if (authMethod == null)
                authMethod = "BASIC";
            else
                authMethod = WinstoneResourceBundle.globalReplace(authMethod,
                        "-", "");
            String realmClassName = stringArg(startupArgs, "realmClass",
                    DEFAULT_REALM_CLASS);
            String authClassName = "winstone.auth."
                    + authMethod.substring(0, 1).toUpperCase()
                    + authMethod.substring(1).toLowerCase()
                    + "AuthenticationHandler";
            try {
                // Build the realm
                Class realmClass = Class.forName(realmClassName);
                Constructor realmConstr = realmClass
                        .getConstructor(new Class[] {
                                WinstoneResourceBundle.class, Set.class,
                                Map.class });
                this.authenticationRealm = (AuthenticationRealm) realmConstr
                        .newInstance(new Object[] { resources, rolesAllowed,
                                startupArgs });

                // Build the authentication handler
                Class authClass = Class.forName(authClassName);
                Constructor authConstr = authClass
                        .getConstructor(new Class[] { Node.class, List.class,
                                Set.class, WinstoneResourceBundle.class,
                                AuthenticationRealm.class });
                this.authenticationHandler = (AuthenticationHandler) authConstr
                        .newInstance(new Object[] { loginConfigNode,
                                constraintNodes, rolesAllowed, resources,
                                authenticationRealm });
            } catch (ClassNotFoundException err) {
                Logger.log(Logger.DEBUG, this.resources,
                        "WebAppConfig.AuthDisabled", authMethod);
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, this.resources,
                        "WebAppConfig.AuthError", new String[] { authClassName,
                                realmClassName }, err);
            }
        }

        // Instantiate the JNDI manager
        String jndiMgrClassName = stringArg(startupArgs, "jndiClassName",
                DEFAULT_JNDI_MGR_CLASS);
        if (useJNDI)
            try {
                // Build the realm
                Class jndiMgrClass = Class.forName(jndiMgrClassName, true, this.loader);
                Constructor jndiMgrConstr = jndiMgrClass.getConstructor(new Class[] { 
                        Map.class, List.class, ClassLoader.class });
                this.jndiManager = (JNDIManager) jndiMgrConstr.newInstance(new Object[] { 
                        startupArgs, envEntryNodes, this.loader });
                if (this.jndiManager != null)
                    this.jndiManager.setup();
            } catch (ClassNotFoundException err) {
                Logger.log(Logger.DEBUG, this.resources,
                        "WebAppConfig.JNDIDisabled");
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, this.resources,
                        "WebAppConfig.JNDIError", jndiMgrClassName, err);
            }

        // Add the default index.html welcomeFile if none are supplied
        if (localWelcomeFiles.isEmpty()) {
            if (useJasper)
                localWelcomeFiles.add("index.jsp");
            localWelcomeFiles.add("index.html");
        }

        // Put the name filters after the url filters, then convert to string
        // arrays
        this.filterPatternsRequest = (Mapping[]) lfpRequest
                .toArray(new Mapping[lfpRequest.size()]);
        this.filterPatternsForward = (Mapping[]) lfpForward
                .toArray(new Mapping[lfpForward.size()]);
        this.filterPatternsInclude = (Mapping[]) lfpInclude
                .toArray(new Mapping[lfpInclude.size()]);
        this.filterPatternsError = (Mapping[]) lfpError
                .toArray(new Mapping[lfpError.size()]);

        if (this.filterPatternsRequest.length > 0)
            Arrays.sort(this.filterPatternsRequest,
                    this.filterPatternsRequest[0]);
        if (this.filterPatternsForward.length > 0)
            Arrays.sort(this.filterPatternsForward,
                    this.filterPatternsForward[0]);
        if (this.filterPatternsInclude.length > 0)
            Arrays.sort(this.filterPatternsInclude,
                    this.filterPatternsInclude[0]);
        if (this.filterPatternsError.length > 0)
            Arrays.sort(this.filterPatternsError, this.filterPatternsError[0]);

        this.welcomeFiles = (String[]) localWelcomeFiles
                .toArray(new String[localWelcomeFiles.size()]);
        this.errorPagesByExceptionKeysSorted = (Class[]) localErrorPagesByExceptionList
                .toArray(new Class[localErrorPagesByExceptionList.size()]);
        Arrays.sort(this.errorPagesByExceptionKeysSorted, this);

        // Put the listeners into their arrays
        this.contextAttributeListeners = (ServletContextAttributeListener[]) contextAttributeListeners
                .toArray(new ServletContextAttributeListener[contextAttributeListeners
                        .size()]);
        this.contextListeners = (ServletContextListener[]) contextListeners
                .toArray(new ServletContextListener[contextListeners.size()]);
        this.requestListeners = (ServletRequestListener[]) requestListeners
                .toArray(new ServletRequestListener[requestListeners.size()]);
        this.requestAttributeListeners = (ServletRequestAttributeListener[]) requestAttributeListeners
                .toArray(new ServletRequestAttributeListener[requestAttributeListeners.size()]);
        this.sessionActivationListeners = (HttpSessionActivationListener[]) sessionActivationListeners
                .toArray(new HttpSessionActivationListener[sessionActivationListeners.size()]);
        this.sessionAttributeListeners = (HttpSessionAttributeListener[]) sessionAttributeListeners
                .toArray(new HttpSessionAttributeListener[sessionAttributeListeners.size()]);
        this.sessionListeners = (HttpSessionListener[]) sessionListeners
                .toArray(new HttpSessionListener[sessionListeners.size()]);

        // If we haven't explicitly mapped the default servlet, map it here
        if (this.defaultServletName == null)
            this.defaultServletName = DEFAULT_SERVLET_NAME;
        if (this.errorServletName == null)
            this.errorServletName = ERROR_SERVLET_NAME;

        // If we don't have an instance of the default servlet, mount the inbuilt one
        if (this.servletInstances.get(this.defaultServletName) == null) {
            Map staticParams = new Hashtable();
            staticParams.put("webRoot", webRoot);
            staticParams.put("prefix", this.prefix);
            staticParams.put("directoryList", "" + useDirLists);
            ServletConfiguration defaultServlet = new ServletConfiguration(
                    this, this.resources,  this.defaultServletName, DEFAULT_SERVLET_CLASS,
                    staticParams, 0);
            // commented cause it should be called during startup servlet
//          defaultServlet.getRequestDispatcher(this.filterInstances); 
            this.servletInstances.put(this.defaultServletName, defaultServlet);
            startupServlets.add(defaultServlet);
        }

        // If we don't have an instance of the default servlet, mount the inbuilt one
        if (this.servletInstances.get(this.errorServletName) == null) {
            ServletConfiguration errorServlet = new ServletConfiguration(
                    this, this.resources,  this.errorServletName, ERROR_SERVLET_CLASS,
                    new HashMap(), 0);
            // commented cause it should be called during startup servlet
//          errorServlet.getRequestDispatcher(this.filterInstances); 
            this.servletInstances.put(this.errorServletName, errorServlet);
            startupServlets.add(errorServlet);
        }
        
        // Initialise jasper servlet if requested
        if (useJasper) {
            setAttribute("org.apache.catalina.classloader", this.loader);
            // Logger.log(Logger.DEBUG, "Setting JSP classpath: " +
            // this.loader.getClasspath());
            if (useWCL)
                setAttribute("org.apache.catalina.jsp_classpath",
                        ((WinstoneClassLoader) this.loader).getClasspath());

            Map jspParams = new HashMap();
            addJspServletParams(jspParams);
            ServletConfiguration sc = new ServletConfiguration(this,
                    this.resources, JSP_SERVLET_NAME, JSP_SERVLET_CLASS, jspParams, 3);
            this.servletInstances.put(JSP_SERVLET_NAME, sc);
            startupServlets.add(sc);
            processMapping(JSP_SERVLET_NAME, JSP_SERVLET_MAPPING,
                    this.exactServletMatchMounts, localFolderPatterns,
                    localExtensionPatterns);
        }

        // Initialise invoker servlet if requested
        if (useInvoker) {
            // Get generic options
            String invokerPrefix = stringArg(startupArgs, "invokerPrefix",
                    DEFAULT_INVOKER_PREFIX);
            Map invokerParams = new HashMap();
            invokerParams.put("prefix", this.prefix);
            invokerParams.put("invokerPrefix", invokerPrefix);
            ServletConfiguration sc = new ServletConfiguration(this,
                    this.resources,  INVOKER_SERVLET_NAME, INVOKER_SERVLET_CLASS, 
                    invokerParams, 3);
            this.servletInstances.put(INVOKER_SERVLET_NAME, sc);
            processMapping(INVOKER_SERVLET_NAME, invokerPrefix + Mapping.STAR,
                    this.exactServletMatchMounts, localFolderPatterns,
                    localExtensionPatterns);
        }

        // Sort the folder patterns so the longest paths are first
        localFolderPatterns.addAll(localExtensionPatterns);
        this.patternMatches = (Mapping[]) localFolderPatterns
                .toArray(new Mapping[localFolderPatterns.size()]);
        if (this.patternMatches.length > 0)
            Arrays.sort(this.patternMatches, this.patternMatches[0]);

        // Send init notifies
        try {
            for (int n = 0; n < this.contextListeners.length; n++)
                this.contextListeners[n].contextInitialized(new ServletContextEvent(this));
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, resources, "WebAppConfig.ContextStartupError", this.contextName, err);
            this.contextStartupError = err;
        }

        if (this.contextStartupError == null) {
            // Initialise all the filters
            for (Iterator i = this.filterInstances.values().iterator(); i.hasNext();) {
                FilterConfiguration config = (FilterConfiguration) i.next();
                try {
                    config.getFilter();
                } catch (ServletException err) {
                    Logger.log(Logger.ERROR, resources, "WebAppConfig.FilterStartupError", 
                            config.getFilterName(), err);
                }
            }

            // Initialise load on startup servlets
            Object autoStarters[] = startupServlets.toArray();
            Arrays.sort(autoStarters);
            for (int n = 0; n < autoStarters.length; n++) {
                ((ServletConfiguration) autoStarters[n]).ensureInitialization();
            }
        }
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getWebroot() {
        return this.webRoot;
    }

    public ClassLoader getLoader() {
        return this.loader;
    }

    public Map getFilters() {
        return this.filterInstances;
    }
    
    public String getContextName() {
        return this.contextName;
    }

    public Class[] getErrorPageExceptions() {
        return this.errorPagesByExceptionKeysSorted;
    }

    public Map getErrorPagesByException() {
        return this.errorPagesByException;
    }

    public Map getErrorPagesByCode() {
        return this.errorPagesByCode;
    }

    public Map getLocaleEncodingMap() {
        return this.localeEncodingMap;
    }

    public String[] getWelcomeFiles() {
        return this.welcomeFiles;
    }

//    public boolean isDistributable() {
//        return this.distributable;
//    }

    public ServletRequestListener[] getRequestListeners() {
        return this.requestListeners;
    }

    public ServletRequestAttributeListener[] getRequestAttributeListeners() {
        return this.requestAttributeListeners;
    }

    public static void addJspServletParams(Map jspParams) {
        jspParams.put("logVerbosityLevel", JSP_SERVLET_LOG_LEVEL);
        jspParams.put("fork", "false");
    }

    public int compare(Object one, Object two) {
        if (!(one instanceof Class) || !(two instanceof Class))
            throw new IllegalArgumentException(
                    "This comparator is only for sorting classes");
        Class classOne = (Class) one;
        Class classTwo = (Class) two;
        if (classOne.isAssignableFrom(classTwo))
            return 1;
        else if (classTwo.isAssignableFrom(classOne))
            return -1;
        else
            return 0;
    }

    public String getServletURIFromRequestURI(String requestURI) {
        if (prefix.equals("")) {
            return requestURI;
        } else if (requestURI.startsWith(prefix)) {
            return requestURI.substring(prefix.length());
        } else {
            throw new WinstoneException("This shouldn't happen, " +
                    "since we aborted earlier if we didn't match");
        }
    }
    
    /**
     * Iterates through each of the servlets/filters and calls destroy on them
     */
    public void destroy() {
        Collection filterInstances = new ArrayList(this.filterInstances.values());
        for (Iterator i = filterInstances.iterator(); i.hasNext();) {
            try {
                ((FilterConfiguration) i.next()).destroy();
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, resources, "WebAppConfig.ShutdownError", err);
            }
        }
        this.filterInstances.clear();
        
        Collection servletInstances = new ArrayList(this.servletInstances.values());
        for (Iterator i = servletInstances.iterator(); i.hasNext();) {
            try {
                ((ServletConfiguration) i.next()).destroy();
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, resources, "WebAppConfig.ShutdownError", err);
            }
        }
        this.servletInstances.clear();

        // Drop all sessions
        Collection sessions = new ArrayList(this.sessions.values());
        for (Iterator i = sessions.iterator(); i.hasNext();) {
            try {
                ((WinstoneSession) i.next()).invalidate();
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, resources, "WebAppConfig.ShutdownError", err);
            }
        }
        this.sessions.clear();

        // Send destroy notifies - backwards
        for (int n = this.contextListeners.length - 1; n >= 0; n--) {
            try {
                this.contextListeners[n].contextDestroyed(new ServletContextEvent(this));
                this.contextListeners[n] = null;
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, resources, "WebAppConfig.ShutdownError", err);
            }
        }
        this.contextListeners = null;
        
        // Terminate class loader reloading thread if running
        if (this.loader instanceof WinstoneClassLoader) {
            ((WinstoneClassLoader) this.loader).destroy();
            this.loader = null;
        }

        // Kill JNDI manager if we have one
        if (this.jndiManager != null) {
            this.jndiManager.tearDown();
            this.jndiManager = null;
        }
    }

    /**
     * Triggered by the admin thread on the reloading class loader. This will
     * cause a full shutdown and reinstantiation of the web app - not real
     * graceful, but you shouldn't have reloading turned on in high load
     * environments.
     */
    public void resetClassLoader() throws IOException {
        this.ownerWebappGroup.reloadWebApp(getPrefix());
    }

    /**
     * Here we process url patterns into the exactMatch and patternMatch lists
     */
    private void processMapping(String name, String pattern, Map exactPatterns,
            List folderPatterns, List extensionPatterns) {
        
        // Compatibility hack - add a leading slash if one is not found and not 
        // an extension mapping
        if (!pattern.equals("") && !pattern.startsWith(Mapping.STAR) && 
                !pattern.startsWith(Mapping.SLASH)) {
            pattern = Mapping.SLASH + pattern;
        }
        
        Mapping urlPattern = null;
        try {
            urlPattern = Mapping.createFromURL(name, pattern, resources);
        } catch (WinstoneException err) {
            Logger.log(Logger.WARNING, resources, "WebAppConfig.ErrorMapURL",
                    err.getMessage());
            return;
        }

        // put the pattern in the correct list
        if (urlPattern.getPatternType() == Mapping.EXACT_PATTERN) {
            exactPatterns.put(urlPattern.getUrlPattern(), name);
        } else if (urlPattern.getPatternType() == Mapping.FOLDER_PATTERN) {
            folderPatterns.add(urlPattern);
        } else if (urlPattern.getPatternType() == Mapping.EXTENSION_PATTERN) {
            extensionPatterns.add(urlPattern);
        } else if (urlPattern.getPatternType() == Mapping.DEFAULT_SERVLET) {
            this.defaultServletName = name;
        } else {
            Logger.log(Logger.WARNING, resources, "WebAppConfig.InvalidMount",
                    new String[] { name, pattern });
        }
    }

    /**
     * Execute the pattern match, and try to return a servlet that matches this
     * URL
     */
    private ServletConfiguration urlMatch(String path,
            StringBuffer servletPath, StringBuffer pathInfo) {
        Logger.log(Logger.FULL_DEBUG, resources, "WebAppConfig.URLMatch", path);

        // Check exact matches first
        String exact = (String) this.exactServletMatchMounts.get(path);
        if (exact != null) {
            if (this.servletInstances.get(exact) != null) {
                servletPath.append(path);
                // pathInfo.append(""); // a hack - empty becomes null later
                return (ServletConfiguration) this.servletInstances.get(exact);
            }
        }

        // Inexact mount check
        for (int n = 0; n < this.patternMatches.length; n++) {
            Mapping urlPattern = this.patternMatches[n];
            if (urlPattern.match(path, servletPath, pathInfo) && 
                    (this.servletInstances.get(urlPattern.getMappedTo()) != null)) {
                return (ServletConfiguration) this.servletInstances
                        .get(urlPattern.getMappedTo());
            }
        }

        // return default servlet
        // servletPath.append(""); // unneeded
        if (this.servletInstances.get(this.defaultServletName) == null)
            throw new WinstoneException(resources.getString(
                    "WebAppConfig.MatchedNonExistServlet",
                    this.defaultServletName));
        pathInfo.append(path);
        return (ServletConfiguration) this.servletInstances.get(this.defaultServletName);
    }

    /**
     * Constructs a session instance with the given sessionId
     * 
     * @param sessionId The sessionID for the new session
     * @return A valid session object
     */
    public WinstoneSession makeNewSession(String sessionId) {
        WinstoneSession ws = new WinstoneSession(sessionId, this, (this.cluster != null));
        setSessionListeners(ws);
        if ((this.sessionTimeout != null)
                && (this.sessionTimeout.intValue() > 0))
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
    public WinstoneSession getSessionById(String sessionId, boolean localOnly) {
        if (sessionId == null) {
            return null;
        }
        WinstoneSession session = (WinstoneSession) this.sessions.get(sessionId);
        if (session != null) {
            return session;
        }

        // If I'm distributable ... check remotely
        if ((this.cluster != null) && !localOnly) {
            session = this.cluster.askClusterForSession(sessionId, this);
            if (session != null) {
                this.sessions.put(sessionId, session);
            }
            return session;
        } else {
            return null;
        }
    }

    /**
     * Remove the session from the collection
     */
    public void removeSessionById(String sessionId) {
        this.sessions.remove(sessionId);
    }

    public void setSessionListeners(WinstoneSession session) {
        session.setSessionActivationListeners(this.sessionActivationListeners);
        session.setSessionAttributeListeners(this.sessionAttributeListeners);
        session.setSessionListeners(this.sessionListeners);
        session.setResources(this.resources);
    }

    public void removeServletConfigurationAndMappings(ServletConfiguration config) {
        this.servletInstances.remove(config.getServletName());
        // The urlMatch method will only match to non-null mappings, so we don't need
        // to remove anything here
    }
    
    /***************************************************************************
     * 
     * OK ... from here to the end is the interface implementation methods for
     * the servletContext interface.
     * 
     **************************************************************************/

    // Application level attributes
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        return Collections.enumeration(this.attributes.keySet());
    }

    public void removeAttribute(String name) {
        Object me = this.attributes.get(name);
        this.attributes.remove(name);
        if (me != null)
            for (int n = 0; n < this.contextAttributeListeners.length; n++)
                this.contextAttributeListeners[n]
                        .attributeRemoved(new ServletContextAttributeEvent(
                                this, name, me));
    }

    public void setAttribute(String name, Object object) {
        if (object == null) {
            removeAttribute(name);
        } else {
            Object me = this.attributes.get(name);
            this.attributes.put(name, object);
            if (me != null) {
                for (int n = 0; n < this.contextAttributeListeners.length; n++)
                    this.contextAttributeListeners[n]
                            .attributeReplaced(new ServletContextAttributeEvent(
                                    this, name, me));
            } else {
                for (int n = 0; n < this.contextAttributeListeners.length; n++)
                    this.contextAttributeListeners[n]
                            .attributeAdded(new ServletContextAttributeEvent(this,
                                    name, object));
            }
        }
    }

    // Application level init parameters
    public String getInitParameter(String name) {
        return (String) this.initParameters.get(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }

    // Server info
    public String getServerInfo() {
        return resources.getString("ServerVersion");
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 4;
    }

    // Weird mostly deprecated crap to do with getting servlet instances
    public javax.servlet.ServletContext getContext(String uri) {
        return this.ownerWebappGroup.getWebAppByURI(uri);
    }

    public String getServletContextName() {
        return this.displayName;
    }

    /**
     * Look up the map of mimeType extensions, and return the type that matches
     */
    public String getMimeType(String fileName) {
        int dotPos = fileName.lastIndexOf('.');
        if ((dotPos != -1) && (dotPos != fileName.length() - 1)) {
            String extension = fileName.substring(dotPos + 1).toLowerCase();
            String mimeType = (String) this.mimeTypes.get(extension);
            return mimeType;
        } else
            return null;
    }

    // Context level log statements
    public void log(String message) {
        Logger.logDirectMessage(Logger.INFO, this.contextName, message, null);
    }

    public void log(String message, Throwable throwable) {
        Logger.logDirectMessage(Logger.ERROR, this.contextName, message, throwable);
    }

    /**
     * Named dispatcher - this basically gets us a simple exact dispatcher (no
     * url matching, no request attributes and no security)
     */
    public javax.servlet.RequestDispatcher getNamedDispatcher(String name) {
        ServletConfiguration servlet = (ServletConfiguration) this.servletInstances.get(name);
        if (servlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, servlet, this.resources);
            if (rd != null) {
                rd.setForNamedDispatcher(this.filterPatternsForward, this.filterPatternsInclude);
                return rd;
            }
        }
        return null;
    }

    /**
     * Gets a dispatcher, which sets the request attributes, etc on a
     * forward/include. Doesn't execute security though.
     */
    public javax.servlet.RequestDispatcher getRequestDispatcher(
            String uriInsideWebapp) {
        if (uriInsideWebapp == null) {
            return null;
        } else if (!uriInsideWebapp.startsWith("/")) {
            return null;
        }

        // Parse the url for query string, etc
        String queryString = "";
        int questionPos = uriInsideWebapp.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != uriInsideWebapp.length() - 1)
                queryString = uriInsideWebapp.substring(questionPos + 1);
            uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
        }

        // Return the dispatcher
        StringBuffer servletPath = new StringBuffer();
        StringBuffer pathInfo = new StringBuffer();
        ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
        if (servlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, servlet, this.resources);
            if (rd != null) {
                rd.setForURLDispatcher(servletPath.toString(), pathInfo.toString()
                        .equals("") ? null : pathInfo.toString(), queryString,
                        uriInsideWebapp, this.filterPatternsForward,
                        this.filterPatternsInclude);
                return rd;
            }
        }
        return null;
    }

    /**
     * Creates the dispatcher that corresponds to a request level dispatch (ie
     * the initial entry point). The difference here is that we need to set up
     * the dispatcher so that on a forward, it executes the security checks and
     * the request filters, while not setting any of the request attributes for
     * a forward. Also, we can't return a null dispatcher in error case - instead 
     * we have to return a dispatcher pre-init'd for showing an error page (eg 404).
     * A null dispatcher is interpreted to mean a successful 302 has occurred. 
     */
    public RequestDispatcher getInitialDispatcher(String uriInsideWebapp,
            WinstoneRequest request, WinstoneResponse response)
            throws IOException {
        if (!uriInsideWebapp.equals("") && !uriInsideWebapp.startsWith("/")) {
            return this.getErrorDispatcherByCode(
                    HttpServletResponse.SC_BAD_REQUEST,
                    resources.getString("WebAppConfig.InvalidURI", uriInsideWebapp),
                    null);
        } else if (this.contextStartupError != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            this.contextStartupError.printStackTrace(pw);
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                    resources.getString("WebAppConfig.ErrorDuringStartup", sw
//                            .toString()));
//            return null;
            return this.getErrorDispatcherByCode(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    resources.getString("WebAppConfig.ErrorDuringStartup", sw.toString()), 
                    this.contextStartupError);
        }

        // Parse the url for query string, etc 
        String queryString = "";
        int questionPos = uriInsideWebapp.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != uriInsideWebapp.length() - 1)
                queryString = uriInsideWebapp.substring(questionPos + 1);
            uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
        }

        // Return the dispatcher
        StringBuffer servletPath = new StringBuffer();
        StringBuffer pathInfo = new StringBuffer();
        ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath,
                pathInfo);
        if (servlet != null) {
            // If the default servlet was returned, we should check for welcome files
            if (servlet.equals(this.servletInstances.get(this.defaultServletName))) {
                // Is path a directory ?
                String directoryPath = pathInfo.toString();
                if (directoryPath.endsWith("/")) {
                    directoryPath = directoryPath.substring(0, directoryPath.length() - 1);
                }
                if (directoryPath.startsWith("/")) {
                    directoryPath = directoryPath.substring(1);
                }

                File res = new File(webRoot, directoryPath);
                if (res.exists() && res.isDirectory()) {
                    // Check for the send back with slash case
                    if (!pathInfo.toString().endsWith("/")) {
                        Logger.log(Logger.FULL_DEBUG, resources,
                                "WebAppConfig.FoundNonSlashDirectory", pathInfo
                                        .toString());
                        response.sendRedirect(this.prefix
                                + servletPath.toString()
                                + pathInfo.toString()
                                + "/"
                                + (queryString.equals("") ? "" : "?" + queryString));
                        return null;
                    }

                    // Check for welcome files
                    Logger.log(Logger.FULL_DEBUG, resources,
                            "WebAppConfig.CheckWelcomeFile", servletPath
                                    .toString()
                                    + pathInfo.toString());
                    String welcomeFile = matchWelcomeFiles(servletPath
                            .toString()
                            + pathInfo.toString(), request);
                    if (welcomeFile != null) {
                        response.sendRedirect(this.prefix
                                + servletPath.toString()
                                + pathInfo.toString()
                                + welcomeFile
                                + (queryString.equals("") ? "" : "?" + queryString));
                        return null;
                    }
                }
            }

            RequestDispatcher rd = new RequestDispatcher(this, servlet, this.resources);
            rd.setForInitialDispatcher(servletPath.toString(), 
                    pathInfo.toString().equals("") ? null : pathInfo.toString(), queryString,
                    uriInsideWebapp, this.filterPatternsRequest, this.authenticationHandler);
            return rd;
        }
        
        // If we are here, return a 404
        return this.getErrorDispatcherByCode(HttpServletResponse.SC_NOT_FOUND,
                resources.getString("StaticResourceServlet.PathNotFound", 
                        uriInsideWebapp), null);
    }

    /**
     * Gets a dispatcher, set up for error dispatch.
     */
    public RequestDispatcher getErrorDispatcherByClass(
            Throwable exception) {

        // Check for exception class match
        Class exceptionClasses[] = this.errorPagesByExceptionKeysSorted;
        Throwable errWrapper = new ServletException("For loop condition's sake only", exception);
        while (errWrapper instanceof ServletException) {
            errWrapper = ((ServletException) errWrapper).getRootCause();
            for (int n = 0; n < exceptionClasses.length; n++) {

                Logger.log(Logger.FULL_DEBUG, resources,
                        "WinstoneResponse.TestingException",
                        new String[] {this.errorPagesByExceptionKeysSorted[n].getName(),
                            errWrapper.getClass().getName()});
                if (exceptionClasses[n].isInstance(errWrapper)) {
                    String errorURI = (String) this.errorPagesByException.get(exceptionClasses[n]);
                    if (errorURI != null) {
                        RequestDispatcher rd = buildErrorDispatcher(errorURI, 
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                errWrapper.getMessage(), errWrapper);
                        if (rd != null) {
                            return rd;
                        }
                    } else {
                        Logger.log(Logger.WARNING, resources, 
                                "WinstoneResponse.SkippingException",
                                new String[] {exceptionClasses[n].getName(),
                                    (String) this.errorPagesByException.get(exceptionClasses[n]) });
                    }
                } else {
                    Logger.log(Logger.WARNING, resources, 
                            "WinstoneResponse.ExceptionNotMatched", 
                            exceptionClasses[n].getName());
                }
            }
        }
        
        // Otherwise throw a code error
        return getErrorDispatcherByCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                exception.getMessage(), exception);
    }
    
    public RequestDispatcher getErrorDispatcherByCode(
            int statusCode, String summaryMessage, Throwable exception) {
        // Check for status code match
        String errorURI = (String) getErrorPagesByCode().get("" + statusCode);
        if (errorURI != null) {
            RequestDispatcher rd = buildErrorDispatcher(errorURI, statusCode, 
                    summaryMessage, exception);
            if (rd != null) {
                return rd;
            }
        }
        
        // If no dispatcher available, return a dispatcher to the default error formatter
        ServletConfiguration errorServlet = (ServletConfiguration) 
                    this.servletInstances.get(this.errorServletName);
        if (errorServlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, errorServlet, this.resources);
            if (rd != null) {
                rd.setForErrorDispatcher(null, null, null, statusCode, 
                        summaryMessage, exception, null, this.filterPatternsError);
                return rd;
            }
        }
        
        // Otherwise log and return null
        Logger.log(Logger.ERROR, resources, "WebAppConfig.NoErrorServlet", "" + statusCode, exception);
        return null;
    }

    /**
     * Build a dispatcher to the error handler if it's available. If it fails, return null.
     */
    private RequestDispatcher buildErrorDispatcher(String errorURI, int statusCode, 
            String summaryMessage, Throwable exception) {
        // Parse the url for query string, etc 
        String queryString = "";
        int questionPos = errorURI.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != errorURI.length() - 1) {
                queryString = errorURI.substring(questionPos + 1);
            }
            errorURI = errorURI.substring(0, questionPos);
        }

        // Return the dispatcher
        StringBuffer servletPath = new StringBuffer();
        StringBuffer pathInfo = new StringBuffer();
        ServletConfiguration servlet = urlMatch(errorURI, servletPath, pathInfo);
        if (servlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, servlet, this.resources);
            if (rd != null) {
                rd.setForErrorDispatcher(servletPath.toString(), 
                        pathInfo.toString().equals("") ? null : pathInfo.toString(), 
                                queryString, statusCode, summaryMessage, 
                                exception, errorURI, this.filterPatternsError);
                return rd;
            }
        }
        return null;
    }

    /**
     * Check if any of the welcome files under this path are available. Returns the 
     * name of the file if found, null otherwise
     */
    private String matchWelcomeFiles(String path, WinstoneRequest request) {
        Set subfiles = getResourcePaths(path);
        for (int n = 0; n < this.welcomeFiles.length; n++) {
            String exact = (String) this.exactServletMatchMounts.get(path);
            if (exact != null)
                return this.welcomeFiles[n];

            // Inexact folder mount check - note folder mounts only
            for (int j = 0; j < this.patternMatches.length; j++) {
                Mapping urlPattern = this.patternMatches[j];
                if ((urlPattern.getPatternType() == Mapping.FOLDER_PATTERN)
                        && urlPattern.match(path + this.welcomeFiles[n], null,
                                null))
                    return this.welcomeFiles[n];
            }

            if (subfiles.contains(path + this.welcomeFiles[n]))
                return this.welcomeFiles[n];
        }
        return null;
    }

    // Getting resources via the classloader
    public URL getResource(String path) throws MalformedURLException {
        if (path == null) {
            return null;
        } else if (!path.startsWith("/")) {
            throw new MalformedURLException(resources.getString(
                    "WebAppConfig.BadResourcePath", path));
        } else if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        File res = new File(webRoot, path.substring(1));
        return (res != null) && res.exists() ? res.toURL() : null;
    }

    public InputStream getResourceAsStream(String path) {
        try {
            URL res = this.getResource(path);
            return res == null ? null : res.openStream();
        } catch (IOException err) {
            throw new WinstoneException(resources
                    .getString("WebAppConfig.ErrorOpeningStream"), err);
        }
    }

    public String getRealPath(String path) {
        // Trim the prefix
        if (path == null)
            return null;
        else {
            try {
                File res = new File(this.webRoot, path);
                if (res.isDirectory())
                    return res.getCanonicalPath() + "/";
                else
                    return res.getCanonicalPath();
            } catch (IOException err) {
                return null;
            }
        }
    }

    public Set getResourcePaths(String path) {
        // Trim the prefix
        if (path == null)
            return null;
        else if (!path.startsWith("/"))
            throw new WinstoneException(resources.getString(
                    "WebAppConfig.BadResourcePath", path));
        else {
            String workingPath = null;
            if (path.equals("/"))
                workingPath = "";
            else {
                boolean lastCharIsSlash = path.charAt(path.length() - 1) == '/';
                workingPath = path.substring(1, path.length()
                        - (lastCharIsSlash ? 1 : 0));
            }
            File inPath = new File(this.webRoot, workingPath.equals("") ? "."
                    : workingPath);
            if (!inPath.exists())
                return null;
            else if (!inPath.isDirectory())
                return null;

            // Find all the files in this folder
            File children[] = inPath.listFiles();
            Set out = new HashSet();
            for (int n = 0; n < children.length; n++) {
                // Write the entry as subpath + child element
                String entry = //this.prefix + 
                "/" + (workingPath.length() != 0 ? workingPath + "/" : "")
                        + children[n].getName()
                        + (children[n].isDirectory() ? "/" : "");
                out.add(entry);
            }
            return out;
        }
    }

    /**
     * @deprecated
     */
    public javax.servlet.Servlet getServlet(String name) {
        return null;
    }

    /**
     * @deprecated
     */
    public Enumeration getServletNames() {
        return Collections.enumeration(new ArrayList());
    }

    /**
     * @deprecated
     */
    public Enumeration getServlets() {
        return Collections.enumeration(new ArrayList());
    }

    /**
     * @deprecated
     */
    public void log(Exception exception, String msg) {
        this.log(msg, exception);
    }

}
