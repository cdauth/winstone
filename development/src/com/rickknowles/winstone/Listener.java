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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implements the main listener daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Listener implements Runnable, EntityResolver
{
  final String DTD_2_2_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
  final String DTD_2_2_URL    = "javax/servlet/resources/web-app_2_2.dtd";

  final String DTD_2_3_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
  final String DTD_2_3_URL    = "javax/servlet/resources/web-app_2_3.dtd";

  static final String RESOURCE_FILE    = "com.rickknowles.winstone.LocalStrings";

  private int LISTENER_TIMEOUT = 500; // every 500ms reset the listener socket
  private int CONTROL_TIMEOUT = 1; // wait 10ms for control connection
  private int DEFAULT_PORT = 8080;
  private int DEFAULT_CONTROL_PORT = -1;
  private boolean DEFAULT_HNL = true;
  private String DEFAULT_INVOKER_PREFIX = "/servlet/";

  private int MIN_IDLE_REQUEST_HANDLERS_IN_POOL = 2;
  private int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 10;
  private int MAX_REQUEST_HANDLERS_IN_POOL = 100;

  private String WEB_ROOT = "webroot";
  private String WEB_INF  = "WEB-INF";
  private String WEB_XML  = "web.xml";

  private WinstoneResourceBundle resources;
  private Map arguments;
  private int listenPort;
  private int controlPort;
  private List unusedRequestHandlerThreads;
  private List usedRequestHandlerThreads;

  private WebAppConfiguration webAppConfig;
  private HttpConnector connector;

  private Object requestHandlerSemaphore = new Boolean(true);

  /**
   * Constructor
   */
  public Listener(Map args, WinstoneResourceBundle resources) throws IOException
  {
    // Load resources
    this.resources = resources;

    this.arguments = args;
    this.listenPort = (args.get("port") == null ?
                       DEFAULT_PORT :
                       Integer.parseInt((String) args.get("port")));
    this.controlPort = (args.get("controlPort") == null ?
                       DEFAULT_CONTROL_PORT :
                       Integer.parseInt((String) args.get("controlPort")));

    File webRoot = new File(args.get("webroot") == null ? WEB_ROOT
                                                        : (String) args.get("webroot"));
    if (!webRoot.exists())
      throw new WinstoneException(resources.getString("Listener.NoWebRoot") + webRoot);

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

    // Get options
    String dirLists = (String) args.get("directoryListings");
    String useJasper = (String) args.get("useJasper");
    String useWCL = (String) args.get("useWinstoneClassLoader");
    String useInvoker = (String) args.get("useInvoker");
    String invokerPrefix = (String) (args.get("invokerPrefix") == null
                                                ? DEFAULT_INVOKER_PREFIX
                                                : args.get("invokerPrefix"));
    String hnl = (String) args.get("doHostnameLookups");

    // Build switch values
    boolean switchOnDirLists = (dirLists == null)   || (dirLists.equalsIgnoreCase("true")   || dirLists.equalsIgnoreCase("yes"));
    boolean switchOnJasper   = (useJasper != null)  && (useJasper.equalsIgnoreCase("true")  || useJasper.equalsIgnoreCase("yes"));
    boolean switchOnWCL      = (useWCL == null)     || (useWCL.equalsIgnoreCase("true")     || useWCL.equalsIgnoreCase("yes"));
    boolean switchOnInvoker  = (useInvoker == null) || (useInvoker.equalsIgnoreCase("true") || useInvoker.equalsIgnoreCase("yes"));
    boolean switchOnHNL      = (hnl == null ? DEFAULT_HNL : (hnl.equalsIgnoreCase("yes") || hnl.equalsIgnoreCase("true")));

    this.connector = new HttpConnector(this.resources, switchOnHNL);
    this.webAppConfig = new WebAppConfiguration(webRoot.getCanonicalPath(),
                                                (String) args.get("prefix"),
                                                switchOnDirLists,
                                                switchOnJasper,
                                                switchOnWCL,
                                                switchOnInvoker ? invokerPrefix : null,
                                                webXMLParentNode,
                                                this.resources);

    this.unusedRequestHandlerThreads = new Vector();
    this.usedRequestHandlerThreads = new Vector();
  }

  /**
   * The main run method. This handles the normal thread processing.
   */
  public void run()
  {
    boolean interrupted = false;
    try
    {
      ServerSocket ss = new ServerSocket(this.listenPort);
      ss.setSoTimeout(LISTENER_TIMEOUT);
      ServerSocket controlSocket = null;
      if (this.controlPort > 0)
      {
        controlSocket = new ServerSocket(this.controlPort);
        controlSocket.setSoTimeout(CONTROL_TIMEOUT);
      }

      Map params = new HashMap();
      params.put("[#port]", this.listenPort + "");
      params.put("[#controlPort]", (this.controlPort > 0 ? "" + this.controlPort : resources.getString("Listener.ControlDisabled")));
      params.put("[#prefix]", this.webAppConfig.getPrefix());
      params.put("[#webroot]", this.webAppConfig.getWebroot());
      Logger.log(Logger.INFO, resources.getString("Listener.StartupOK", params));

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

        // Check min idle requestHandler count
        synchronized (this.requestHandlerSemaphore)
        {
          // If we're short an idle request handler
          while ((this.unusedRequestHandlerThreads.size() < MIN_IDLE_REQUEST_HANDLERS_IN_POOL) &&
                 (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL - MIN_IDLE_REQUEST_HANDLERS_IN_POOL))
            this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this.webAppConfig, this, this.connector, this.resources));
        }

        // Get the listener
        Socket s = null;
        try
          {s = ss.accept();}
        catch (InterruptedIOException err) {s = null;}

        // if we actually got a socket, process it. Otherwise go around again
        if (s != null)
          handleRequest(s);

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
        catch (InterruptedIOException err2) {}
        Thread.currentThread().yield();
      }

      // Close server socket
      ss.close();
      controlSocket.close();

      // Release all handlers
      for (Iterator i = this.usedRequestHandlerThreads.iterator(); i.hasNext(); )
        releaseRequestHandler((RequestHandlerThread) i.next());
      this.webAppConfig.destroy();
      for (Iterator i = this.unusedRequestHandlerThreads.iterator(); i.hasNext(); )
        ((RequestHandlerThread) i.next()).destroy();
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, resources.getString("Listener.ShutdownError"), err);}

    Logger.log(Logger.INFO, resources.getString("Listener.ShutdownOK"));
    System.exit(0);
  }

  /**
   * Once the socket request comes in, this method is called. It reserves a
   * request handler, then delegates the socket to that class. When it finishes,
   * the handler is released back into the pool.
   */
  private void handleRequest(Socket s) throws IOException
  {
    // If we have any spare, get it from the pool
    if (this.unusedRequestHandlerThreads.size() > 0)
    {
      RequestHandlerThread rh = null;
      synchronized (this.requestHandlerSemaphore)
      {
        rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Listener.UsingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
      }
      rh.commenceRequestHandling(s);
    }

    // If we are out (and not over our limit), allocate a new one
    else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL)
    {
      RequestHandlerThread rh = null;
      synchronized (this.requestHandlerSemaphore)
      {
        rh = new RequestHandlerThread(this.webAppConfig, this, this.connector, this.resources);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, resources.getString("Listener.NewRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
      }
      rh.commenceRequestHandling(s);
    }

    // otherwise throw fail message - we've blown our limit
    else
    {
      s.close();
      Logger.log(Logger.ERROR, resources.getString("Listener.NoRHPoolThreads"));
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
        Logger.log(Logger.FULL_DEBUG, resources.getString("Listener.ReleasingRHPoolThread",
            "[#used]", "" + this.usedRequestHandlerThreads.size(),
            "[#unused]", "" + this.unusedRequestHandlerThreads.size()));
      }
      else
        Logger.log(Logger.WARNING, resources.getString("Listener.UnknownRHPoolThread"));
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
    catch (ParserConfigurationException errParser)
      {throw new WinstoneException(resources.getString("Listener.WebXMLParseError"), errParser);}
    catch (SAXException errSax)
      {throw new WinstoneException(resources.getString("Listener.WebXMLParseError"), errSax);}
    catch (IOException errIO)
      {throw new WinstoneException(resources.getString("Listener.WebXMLParseError"), errIO);}
  }

  public InputSource resolveEntity(String publicName, String url)
    throws SAXException, IOException
  {
    if (publicName == null)
      return null;
    else if (publicName.equals(DTD_2_2_PUBLIC))
      return new InputSource(this.getClass().getClassLoader().getResourceAsStream(DTD_2_2_URL));
    else if (publicName.equals(DTD_2_3_PUBLIC))
      return new InputSource(this.getClass().getClassLoader().getResourceAsStream(DTD_2_3_URL));
    else
      return new InputSource(url);
  }

  /**
   * Main method. This basically just accepts a few args, then initialises the
   * listener thread. For now, just shut it down with a control-C.
   */
  public static void main(String argv[]) throws IOException
  {
    Map args = new HashMap();

    // Get command line args
    for (int n = 0; n < argv.length;  n++)
    {
      String option = argv[n];
      if (option.equals("-help") || option.equals("-usage"))
        args.put(option.substring(1), "true");
      else if (option.startsWith("-"))
        args.put(option.substring(1), (n + 1 >= argv.length ? "" : argv[n+1]));
    }

    WinstoneResourceBundle resources = new WinstoneResourceBundle(RESOURCE_FILE);
    if (!args.containsKey("webroot"))
      printUsage(resources);
    else
    {
      if (args.containsKey("usage") || args.containsKey("help"))
        printUsage(resources);

      Logger.setCurrentDebugLevel(args.get("debug") == null ?
                                  Logger.DEBUG :
                                  Integer.parseInt((String) args.get("debug")));
      try
      {
        Listener listener = new Listener(args, resources);
        Thread th = new Thread(listener);
        th.start();
      }
      catch (WinstoneException err) {System.err.println(err.getMessage()); err.printStackTrace();}
    }
  }

  private static void printUsage(WinstoneResourceBundle resources)
  {
    System.out.println(resources.getString("Listener.UsageInstructions", "[#version]", resources.getString("ServerVersion")));
  }
}

