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

  static final String RESOURCE_FILE    = "com.rickknowles.winstone.LocalStrings";

  private int CONTROL_TIMEOUT = 1; // wait 10ms for control connection
  private int DEFAULT_CONTROL_PORT = -1;
  private String DEFAULT_INVOKER_PREFIX = "/servlet/";

  private int MIN_IDLE_REQUEST_HANDLERS_IN_POOL = 2;
  private int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 10;
  private int MAX_REQUEST_HANDLERS_IN_POOL = 100;

  private String WEB_ROOT = "webroot";
  private String WEB_INF  = "WEB-INF";
  private String WEB_XML  = "web.xml";

  private WinstoneResourceBundle resources;
  private int controlPort;
  private List unusedRequestHandlerThreads;
  private List usedRequestHandlerThreads;
  private WebAppConfiguration webAppConfig;

  private Object requestHandlerSemaphore = new Boolean(true);
  private int threadIndex = 0;

  private HttpListener httpListener;
  
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
    File webRoot = getWebRoot((String) args.get("webroot"), (String) args.get("warfile"));
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

    // If we're short an idle request handler
    while ((this.unusedRequestHandlerThreads.size() < MIN_IDLE_REQUEST_HANDLERS_IN_POOL) &&
           (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL - MIN_IDLE_REQUEST_HANDLERS_IN_POOL))
      this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++));

    // Create connector
    this.httpListener = new HttpListener(args, resources, this);
  }

  /**
   * Setup the webroot. If a warfile is supplied, extract any files that the
   * war file is newer than. If none is supplied, use the default.
   */
  private File getWebRoot(String webroot, String warfile) throws IOException
  {
    if (warfile != null)
    {
      Logger.log(Logger.INFO, this.resources.getString("Launcher.BeginningWarExtraction"));

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
        byte buffer[] = new byte[1000];

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
    boolean interrupted = false;
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
          // If we're short an idle request handler
          while (this.unusedRequestHandlerThreads.size() > MAX_IDLE_REQUEST_HANDLERS_IN_POOL)
            this.unusedRequestHandlerThreads.remove(0);
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
        Thread.currentThread().yield();
      }

      // Close server socket
      controlSocket.close();

      // Release all listeners/handlers/webapps
      if (this.httpListener != null) this.httpListener.destroy();
      for (Iterator i = this.usedRequestHandlerThreads.iterator(); i.hasNext(); )
        releaseRequestHandler((RequestHandlerThread) i.next());
      for (Iterator i = this.unusedRequestHandlerThreads.iterator(); i.hasNext(); )
        ((RequestHandlerThread) i.next()).destroy();
      this.webAppConfig.destroy();
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, resources.getString("Launcher.ShutdownError"), err);}

    Logger.log(Logger.INFO, resources.getString("Launcher.ShutdownOK"));
  }

  /**
   * Once the socket request comes in, this method is called. It reserves a
   * request handler, then delegates the socket to that class. When it finishes,
   * the handler is released back into the pool.
   */
  public void handleRequest(Socket s, HttpProtocol protocol) throws IOException
  {
    // If we have any spare, get it from the pool
    synchronized (this.requestHandlerSemaphore)
    {
      // If we're short an idle request handler
      while ((this.unusedRequestHandlerThreads.size() < MIN_IDLE_REQUEST_HANDLERS_IN_POOL) &&
             (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL - MIN_IDLE_REQUEST_HANDLERS_IN_POOL))
        this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++));

      if (this.unusedRequestHandlerThreads.size() > 0)
      {
        RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Launcher.UsingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
        rh.commenceRequestHandling(s, protocol);
      }

      // If we are out (and not over our limit), allocate a new one
      else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL)
      {
        RequestHandlerThread rh = new RequestHandlerThread(this.webAppConfig, this, this.resources, this.threadIndex++);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Launcher.NewRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
        rh.commenceRequestHandling(s, protocol);
      }

      // otherwise throw fail message - we've blown our limit
      else
      {
        s.close();
        Logger.log(Logger.ERROR, resources.getString("Launcher.NoRHPoolThreads"));
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

  public Document parseStreamToXML(InputStream in)
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

  /**
   * Main method. This basically just accepts a few args, then initialises the
   * listener thread. For now, just shut it down with a control-C.
   */
  public static void main(String argv[]) throws IOException
  {
    WinstoneResourceBundle resources = new WinstoneResourceBundle(RESOURCE_FILE);

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
                                  Logger.DEBUG :
                                  Integer.parseInt((String) args.get("debug")));
      Logger.log(Logger.DEBUG, resources.getString("Launcher.UsingPropertyFile",
                "[#filename]", configFilename));

    }
    else
    {
      Logger.setCurrentDebugLevel(args.get("debug") == null ?
                                  Logger.DEBUG :
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

