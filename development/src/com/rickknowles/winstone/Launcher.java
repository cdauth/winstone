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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;
import javax.servlet.UnavailableException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implements the main launcher daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Launcher implements EntityResolver, Runnable
{
  final String DTD_2_2_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
  final String DTD_2_2_URL    = "javax/servlet/resources/web-app_2_2.dtd";

  final String DTD_2_3_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
  final String DTD_2_3_URL    = "javax/servlet/resources/web-app_2_3.dtd";

  final String HTTP_LISTENER_CLASS = "com.rickknowles.winstone.HttpListener";
  final String AJP_LISTENER_CLASS  = "com.rickknowles.winstone.ajp13.Ajp13Listener";

  static final String RESOURCE_FILE    = "com.rickknowles.winstone.LocalStrings";
  static final String WEB_ROOT = "webroot";
  static final String WEB_INF  = "WEB-INF";
  static final String WEB_XML  = "web.xml";

  private int CONTROL_TIMEOUT = 5000; // wait 5s for control connection
  private int DEFAULT_CONTROL_PORT = -1;
  private String DEFAULT_INVOKER_PREFIX = "/servlet/";

  private int STARTUP_REQUEST_HANDLERS_IN_POOL = 5;
  private int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 50;
  private int MAX_REQUEST_HANDLERS_IN_POOL = 300;

  private int START_REQUESTS_IN_POOL  = 10;
  private int MAX_REQUESTS_IN_POOL    = 100;

  private int START_RESPONSES_IN_POOL = 10;
  private int MAX_RESPONSES_IN_POOL   = 100;

  private WinstoneResourceBundle resources;
  private int controlPort;
  private List unusedRequestHandlerThreads;
  private List usedRequestHandlerThreads;
  private WebAppConfiguration webAppConfig;

  private List usedRequestPool;
  private List unusedRequestPool;
  private List usedResponsePool;
  private List unusedResponsePool;

  private Object requestPoolSemaphore  = new Boolean(true);
  private Object responsePoolSemaphore = new Boolean(true);
  private Object requestHandlerSemaphore = new Boolean(true);

  private int threadIndex = 0;

  private List listeners;
  private boolean interrupted;

  /**
   * Constructor
   */
  public Launcher(Map args, WinstoneResourceBundle resources) throws IOException
  {
    // Load resources
    this.resources = resources;
    this.controlPort = (args.get("controlPort") == null ?
                       DEFAULT_CONTROL_PORT :
                       Integer.parseInt((String) args.get("controlPort")));

    // Get the parsed webapp xml deployment descriptor
    File webRoot = getWebRoot((String) args.get("webroot"), (String) args.get("warfile"), resources);
    if (!webRoot.exists())
      throw new WinstoneException(resources.getString("Launcher.NoWebRoot") + webRoot);

    Node webXMLParentNode = null;
    File webInfFolder = new File(webRoot, WEB_INF);
    if (webInfFolder.exists())
    {
      File webXmlFile = new File(webInfFolder, WEB_XML);
      if (webXmlFile.exists())
      {
        InputStream inWebXML = new FileInputStream(webXmlFile);
        Document webXMLDoc = parseStreamToXML(inWebXML);
        inWebXML.close();
        webXMLParentNode = webXMLDoc.getDocumentElement();
      }
    }

    // Get generic options
    String dirLists = (String) args.get("directoryListings");
    String useJasper = (String) args.get("useJasper");
    String useWCL = (String) args.get("useWinstoneClassLoader");
    String useInvoker = (String) args.get("useInvoker");
    String invokerPrefix = (String) (args.get("invokerPrefix") == null
                                                ? DEFAULT_INVOKER_PREFIX
                                                : args.get("invokerPrefix"));

    // Get handler pool options
    if (args.get("handlerCountStartup") != null)
      STARTUP_REQUEST_HANDLERS_IN_POOL = Integer.parseInt((String) args.get("handlerCountStartup"));
    if (args.get("handlerCountMax") != null)
      MAX_IDLE_REQUEST_HANDLERS_IN_POOL = Integer.parseInt((String) args.get("handlerCountMax"));
    if (args.get("handlerCountMaxIdle") != null)
      MAX_IDLE_REQUEST_HANDLERS_IN_POOL = Integer.parseInt((String) args.get("handlerCountMaxIdle"));

    // Build switch values
    boolean switchOnDirLists = (dirLists == null)   || (dirLists.equalsIgnoreCase("true")   || dirLists.equalsIgnoreCase("yes"));
    boolean switchOnJasper   = (useJasper != null)  && (useJasper.equalsIgnoreCase("true")  || useJasper.equalsIgnoreCase("yes"));
    boolean switchOnWCL      = (useWCL == null)     || (useWCL.equalsIgnoreCase("true")     || useWCL.equalsIgnoreCase("yes"));
    boolean switchOnInvoker  = (useInvoker != null) && (useInvoker.equalsIgnoreCase("true") || useInvoker.equalsIgnoreCase("yes"));

    // Instantiate the webAppConfig
    this.webAppConfig = new WebAppConfiguration(webRoot.getCanonicalPath(),
                                                (String) args.get("prefix"),
                                                switchOnDirLists,
                                                switchOnJasper,
                                                switchOnWCL,
                                                switchOnInvoker ? invokerPrefix : null,
                                                webXMLParentNode,
                                                this.resources);

    // Build the initial pool of handler threads
    this.unusedRequestHandlerThreads = new Vector();
    this.usedRequestHandlerThreads = new Vector();

    // Build the request/response pools
    this.usedRequestPool    = new ArrayList();
    this.usedResponsePool   = new ArrayList();
    this.unusedRequestPool  = new ArrayList();
    this.unusedResponsePool = new ArrayList();

    // Start the base set of handler threads
    for (int n = 0; n < STARTUP_REQUEST_HANDLERS_IN_POOL; n++)
      this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++));

    // Initialise the request/response pools
    for (int n = 0; n < START_REQUESTS_IN_POOL; n++)
      this.unusedRequestPool.add(new WinstoneRequest(this.resources));
    for (int n = 0; n < START_RESPONSES_IN_POOL; n++)
      this.unusedResponsePool.add(new WinstoneResponse(this.resources));

    // Create connectors (http and ajp)
    this.listeners = new ArrayList();
    try
    {
      Class httpClass = Class.forName(HTTP_LISTENER_CLASS);
      Constructor httpConstructor = httpClass.getConstructor(new Class[] {Map.class, WinstoneResourceBundle.class, Launcher.class});
      Listener httpListener = (Listener) httpConstructor.newInstance(new Object[] {args, resources, this});
      this.listeners.add(httpListener);
    }
    catch (Throwable err)
      {Logger.log(Logger.DEBUG, this.resources.getString("Launcher.HTTPNotFound"), err);}
    try
    {
      Class ajpClass = Class.forName(AJP_LISTENER_CLASS);
      Constructor ajpConstructor = ajpClass.getConstructor(new Class[] {Map.class, WinstoneResourceBundle.class, Launcher.class});
      Listener ajpListener = (Listener) ajpConstructor.newInstance(new Object[] {args, resources, this});
      this.listeners.add(ajpListener);
    }
    catch (Throwable err)
      {Logger.log(Logger.DEBUG, this.resources.getString("Launcher.AJPNotFound"));}
  }

  public WebAppConfiguration getWebAppConfig() {return this.webAppConfig;}

  /**
   * Setup the webroot. If a warfile is supplied, extract any files that the
   * war file is newer than. If none is supplied, use the default temp directory.
   */
  protected static File getWebRoot(String webroot, String warfile,
                                   WinstoneResourceBundle resources) throws IOException
  {
    if (warfile != null)
    {
      Logger.log(Logger.INFO, resources.getString("Launcher.BeginningWarExtraction"));

      // open the war file
      File warfileRef = new File(warfile);
      if (!warfileRef.exists() || !warfileRef.isFile())
        throw new WinstoneException(resources.getString("Launcher.WarFileInvalid", "[#warfile]", warfile));

      // Get the webroot folder (or a temp dir if none supplied)
      File unzippedDir =(webroot != null ? new File(webroot) : new File(File.createTempFile("dummy", "dummy")
                                            .getParent(), "winstone/" + warfileRef.getName()));
      if (unzippedDir.exists())
      {
        if (!unzippedDir.isDirectory())
          throw new WinstoneException(resources.getString("Launcher.WebRootNotDirectory", "[#dir]", unzippedDir.getPath()));
        else
          Logger.log(Logger.DEBUG, resources.getString("Launcher.WebRootExists", "[#dir]", unzippedDir.getCanonicalPath()));
      }
      else
        unzippedDir.mkdirs();

      // Iterate through the files
      JarFile warArchive = new JarFile(warfileRef);
      for (Enumeration enum = warArchive.entries(); enum.hasMoreElements(); )
      {
        JarEntry element = (JarEntry) enum.nextElement();
        if (element.isDirectory())
          continue;
        String elemName = element.getName();

        // If archive date is newer than unzipped file, overwrite
        File outFile = new File(unzippedDir, elemName);
        if (outFile.exists() && (outFile.lastModified() > warfileRef.lastModified()))
          continue;

        outFile.getParentFile().mkdirs();
        byte buffer[] = new byte[8192];

        // Copy out the extracted file
        InputStream inContent = warArchive.getInputStream(element);
        OutputStream outStream = new FileOutputStream(outFile);
        int readBytes = inContent.read(buffer);
        while (readBytes != -1)
        {
          outStream.write(buffer, 0, readBytes);
          readBytes = inContent.read(buffer);
        }
        inContent.close();
        outStream.close();
      }

      // Return webroot
      return unzippedDir;
    }
    else
      return new File(webroot == null ? WEB_ROOT : webroot);
  }

  /**
   * The main run method. This handles the normal thread processing.
   */
  public void run()
  {
    interrupted = false;
    try
    {
      ServerSocket controlSocket = null;
      if (this.controlPort > 0)
      {
        controlSocket = new ServerSocket(this.controlPort);
        controlSocket.setSoTimeout(CONTROL_TIMEOUT);
      }

      Map params = new HashMap();
      params.put("[#controlPort]", (this.controlPort > 0 ? "" + this.controlPort : resources.getString("Launcher.ControlDisabled")));
      params.put("[#prefix]", this.webAppConfig.getPrefix());
      params.put("[#webroot]", this.webAppConfig.getWebroot());
      Logger.log(Logger.INFO, resources.getString("Launcher.StartupOK", params));

      // Enter the main loop
      while (!interrupted)
      {
        // Check max idle requestHandler count
        synchronized (this.requestHandlerSemaphore)
        {
          // If we have too many idle request handlers
          while (this.unusedRequestHandlerThreads.size() > MAX_IDLE_REQUEST_HANDLERS_IN_POOL)
          {
            RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
            rh.destroy();
            this.unusedRequestHandlerThreads.remove(rh);
          }
        }

        // Check for control request
        try
        {
          if (controlSocket != null)
          {
            Socket cs = controlSocket.accept();
            interrupted = true; //any connection on control port is interpreted as a shutdown
            cs.close();
          }
        }
        catch (InterruptedIOException err) {}
      }

      // Close server socket
      controlSocket.close();

      // Release all listeners/handlers/webapps
      for (Iterator i = this.listeners.iterator(); i.hasNext(); )
        ((Listener) i.next()).destroy();
      for (Iterator i = this.usedRequestHandlerThreads.iterator(); i.hasNext(); )
        releaseRequestHandler((RequestHandlerThread) i.next());
      for (Iterator i = this.unusedRequestHandlerThreads.iterator(); i.hasNext(); )
        ((RequestHandlerThread) i.next()).destroy();
      this.webAppConfig.destroy();

      Logger.log(Logger.INFO, resources.getString("Launcher.ShutdownOK"));
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, resources.getString("Launcher.ShutdownError"), err);}
  }

  public void shutdown() {this.interrupted = true;}

  /**
   * Once the socket request comes in, this method is called. It reserves a
   * request handler, then delegates the socket to that class. When it finishes,
   * the handler is released back into the pool.
   */
  public void handleRequest(Socket socket, Listener listener, HttpProtocol protocol)
    throws IOException, UnavailableException, InterruptedException
  {
    synchronized (this.requestHandlerSemaphore)
    {
      // If we have any spare, get it from the pool
      if (this.unusedRequestHandlerThreads.size() > 0)
      {
        RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Launcher.UsingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
        rh.commenceRequestHandling(socket, listener, protocol);
      }

      // If we are out (and not over our limit), allocate a new one
      else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL)
      {
        RequestHandlerThread rh = new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Launcher.NewRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
        rh.commenceRequestHandling(socket, listener, protocol);
      }

      // otherwise throw fail message - we've blown our limit
      else
      {
        // Possibly insert a second chance here ? Delay and one retry ?
        // Remember to release the lock first
        socket.close();
        //throw new UnavailableException("NoHandlersAvailable");
      }
    }
  }

  /**
   * Release the handler back into the pool
   */
  public void releaseRequestHandler(RequestHandlerThread rh)
  {
    synchronized (this.requestHandlerSemaphore)
    {
      if (this.usedRequestHandlerThreads.contains(rh))
      {
        this.usedRequestHandlerThreads.remove(rh);
        this.unusedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Launcher.ReleasingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
      }
      else
        Logger.log(Logger.WARNING, resources.getString("Launcher.UnknownRHPoolThread"));
    }
  }

  /**
   * An attempt at pooling request objects for reuse.
   */
  public WinstoneRequest getRequestFromPool() throws IOException
  {
    WinstoneRequest req = null;
    synchronized (this.requestPoolSemaphore)
    {
      // If we have any spare, get it from the pool
      if (this.unusedRequestPool.size() > 0)
      {
        req = (WinstoneRequest) this.unusedRequestPool.get(0);
        this.unusedRequestPool.remove(req);
        this.usedRequestPool.add(req);
        Logger.log(Logger.FULL_DEBUG, resources.getString("HttpListener.UsingRequestFromPool",
            "[#unused]", "" + this.unusedRequestPool.size()));
      }
      // If we are out, allocate a new one
      else if (this.usedRequestPool.size() < MAX_REQUESTS_IN_POOL)
      {
        req = new WinstoneRequest(this.resources);
        this.usedRequestPool.add(req);
        Logger.log(Logger.FULL_DEBUG, resources.getString("HttpListener.NewRequestForPool",
            "[#used]", "" + this.usedRequestPool.size()));
      }
      else
        throw new WinstoneException(this.resources.getString("HttpListener.PoolRequestLimitExceeded"));
    }
    return req;
  }

  public void releaseRequestToPool(WinstoneRequest req)
  {
    req.cleanUp();
    synchronized (this.requestPoolSemaphore)
    {
      this.usedRequestPool.remove(req);
      this.unusedRequestPool.add(req);
      Logger.log(Logger.FULL_DEBUG, resources.getString("HttpListener.RequestReleased",
          "[#unused]", "" + this.unusedRequestPool.size()));
    }
  }

  /**
   * An attempt at pooling request objects for reuse.
   */
  public WinstoneResponse getResponseFromPool() throws IOException
  {
    WinstoneResponse rsp = null;
    synchronized (this.responsePoolSemaphore)
    {
      // If we have any spare, get it from the pool
      if (this.unusedResponsePool.size() > 0)
      {
        rsp = (WinstoneResponse) this.unusedResponsePool.get(0);
        this.unusedResponsePool.remove(rsp);
        this.usedResponsePool.add(rsp);
        Logger.log(Logger.FULL_DEBUG, resources.getString("HttpListener.UsingResponseFromPool",
            "[#unused]", "" + this.unusedResponsePool.size()));
      }
      // If we are out, allocate a new one
      else if (this.usedResponsePool.size() < MAX_RESPONSES_IN_POOL)
      {
        rsp = new WinstoneResponse(this.resources);
        this.usedResponsePool.add(rsp);
        Logger.log(Logger.FULL_DEBUG, resources.getString("HttpListener.NewResponseForPool",
            "[#used]", "" + this.usedResponsePool.size()));
      }
      else
        throw new WinstoneException(this.resources.getString("HttpListener.PoolResponseLimitExceeded"));
    }
    return rsp;
  }

  public void releaseResponseToPool(WinstoneResponse rsp)
  {
    rsp.cleanUp();
    synchronized (this.responsePoolSemaphore)
    {
      this.usedResponsePool.remove(rsp);
      this.unusedResponsePool.add(rsp);
      Logger.log(Logger.FULL_DEBUG, resources.getString("HttpListener.ResponseReleased",
          "[#unused]", "" + this.unusedResponsePool.size()));
    }
  }

  /**
   * Get a parsed XML DOM from the given inputstream. Used to process the web.xml
   * application deployment descriptors.
   */
  protected Document parseStreamToXML(InputStream in)
  {
    try
    {
      // Use JAXP to create a document builder
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setExpandEntityReferences(false);
      factory.setValidating(true);
      factory.setNamespaceAware(false);
      factory.setIgnoringComments(true);
      factory.setCoalescing(true);
      factory.setIgnoringElementContentWhitespace(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(this);
      return builder.parse(in);
    }
    catch (Throwable errParser)
      {throw new WinstoneException(resources.getString("Launcher.WebXMLParseError"), errParser);}
  }

  /**
   * Implements the EntityResolver interface. This allows us to redirect any
   * requests by the parser for webapp DTDs to local copies. It's faster and
   * it means you can run winstone without being web-connected.
   */
  public InputSource resolveEntity(String publicName, String url)
    throws SAXException, IOException
  {
    if (publicName == null)
      return null;
    else if (publicName.equals(DTD_2_2_PUBLIC))
      return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(DTD_2_2_URL));
    else if (publicName.equals(DTD_2_3_PUBLIC))
      return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(DTD_2_3_URL));
    else
      return new InputSource(url);
  }

  public static WinstoneResourceBundle getResourceBundle()
  {
    return new WinstoneResourceBundle(RESOURCE_FILE);
  }

  /**
   * Main method. This basically just accepts a few args, then initialises the
   * listener thread. For now, just shut it down with a control-C.
   */
  public static void main(String argv[]) throws IOException
  {
    WinstoneResourceBundle resources = getResourceBundle();

    // Get command line args
    String configFilename = resources.getString("Launcher.DefaultPropertyFile");
    Map args = new HashMap();
    for (int n = 0; n < argv.length;  n++)
    {
      String option = argv[n];
      if (option.startsWith("--"))
      {
        int equalPos = option.indexOf('=');
        String paramName = option.substring(2, equalPos == -1 ? option.length() : equalPos);
        String paramValue = (equalPos == -1 ? "true" : option.substring(equalPos + 1));
        args.put(paramName, paramValue);
        if (paramName.equals("config"))
          configFilename = paramValue;
      }
    }

    // Load default props if available
    File configFile = new File(configFilename);
    if (configFile.exists() && configFile.isFile())
    {
      InputStream inConfig = new FileInputStream(configFile);
      Properties props = new Properties();
      props.load(inConfig);
      inConfig.close();
      props.putAll(args);
      args = props;

      // Reset the log level
      Logger.setCurrentDebugLevel(args.get("debug") == null ?
                                  Logger.INFO :
                                  Integer.parseInt((String) args.get("debug")));
      Logger.log(Logger.DEBUG, resources.getString("Launcher.UsingPropertyFile",
                "[#filename]", configFilename));

    }
    else
    {
      Logger.setCurrentDebugLevel(args.get("debug") == null ?
                                  Logger.INFO :
                                  Integer.parseInt((String) args.get("debug")));
      Logger.log(Logger.DEBUG, resources.getString("Launcher.NoPropertyFile",
                "[#filename]", configFilename));
    }

    if (!args.containsKey("webroot") && !args.containsKey("warfile"))
      printUsage(resources);
    else
    {
      if (args.containsKey("usage") || args.containsKey("help"))
        printUsage(resources);

      try
      {
        Launcher launcher = new Launcher(args, resources);
        Thread th = new Thread(launcher);
        th.setDaemon(false);
        th.start();
      }
      catch (WinstoneException err) {System.err.println(err.getMessage()); err.printStackTrace();}
    }
  }

  private static void printUsage(WinstoneResourceBundle resources)
  {
    System.out.println(resources.getString("Launcher.UsageInstructions", "[#version]", resources.getString("ServerVersion")));
  }
}

