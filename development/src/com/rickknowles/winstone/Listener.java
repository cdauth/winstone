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

  private int LISTENER_TIMEOUT = 500; // every 500ms reset the listener socket
  private int CONTROL_TIMEOUT = 1; // wait 10ms for control connection
  private int DEFAULT_PORT = 8080;
  private int DEFAULT_CONTROL_PORT = -1;
  private boolean DEFAULT_HNL = true;

  private int MIN_IDLE_REQUEST_HANDLERS_IN_POOL = 2;
  private int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 10;
  private int MAX_REQUEST_HANDLERS_IN_POOL = 100;

  private String WEB_ROOT = "webapp";
  private String WEB_INF  = "WEB-INF";
  private String WEB_XML  = "web.xml";

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
  public Listener(Map args) throws IOException
  {
    this.arguments = args;
    this.listenPort = (args.get("port") == null ?
                       DEFAULT_PORT :
                       Integer.parseInt((String) args.get("port")));
    this.controlPort = (args.get("controlPort") == null ?
                       DEFAULT_CONTROL_PORT :
                       Integer.parseInt((String) args.get("controlPort")));

    File webRoot = new File(args.get("webroot") == null ? WEB_ROOT : (String) args.get("webroot"));
    if (!webRoot.exists())
      throw new WinstoneException("Web root not found - " + webRoot.getCanonicalPath());

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
    String useJasper = (String) args.get("jasper");
    String hnl = (String) args.get("doHostnameLookups");

    this.connector = new HttpConnector(hnl == null ? DEFAULT_HNL :
                                       (hnl.equalsIgnoreCase("yes") || hnl.equalsIgnoreCase("true")));
    this.webAppConfig = new WebAppConfiguration(webRoot.getCanonicalPath(),
                                                (String) args.get("prefix"),
                                                (dirLists == null) || dirLists.equalsIgnoreCase("true") || dirLists.equalsIgnoreCase("yes"),
                                                (useJasper != null) && (useJasper.equalsIgnoreCase("true") || useJasper.equalsIgnoreCase("yes")),
                                                webXMLParentNode);

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

      Logger.log(Logger.INFO, "Winstone running: port=" + this.listenPort +
                              " controlPort=" + (this.controlPort > 0 ? "" + this.controlPort : "disabled")+
                              " prefix=" + this.webAppConfig.getPrefix() +
                              " webroot=" + this.webAppConfig.getWebroot());

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
            this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this.webAppConfig, this, this.connector));
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
      {Logger.log(Logger.ERROR, "Error during listener init or shutdown", err);}

    Logger.log(Logger.INFO, "Winstone shutdown successfully - " + Thread.activeCount() +
          " active threads remaining");
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
        Logger.log(Logger.FULL_DEBUG, "RHPool: Using pooled handler thread - used: " +
            this.usedRequestHandlerThreads.size() + " unused: " +
            this.unusedRequestHandlerThreads.size());
      }
      rh.commenceRequestHandling(s);
    }

    // If we are out (and not over our limit), allocate a new one
    else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL)
    {
      RequestHandlerThread rh = null;
      synchronized (this.requestHandlerSemaphore)
      {
        rh = new RequestHandlerThread(this.webAppConfig, this, this.connector);
        this.unusedRequestHandlerThreads.remove(rh);
        this.usedRequestHandlerThreads.add(rh);
        Logger.log(Logger.FULL_DEBUG, "RHPool: Using new handler thread - used: " +
            this.usedRequestHandlerThreads.size() + " unused: " +
            this.unusedRequestHandlerThreads.size());
      }
      rh.commenceRequestHandling(s);
    }

    // otherwise throw fail message - we've blown our limit
    else
    {
      s.close();
      Logger.log(Logger.ERROR, "ERROR: Request ignored because there were no " +
          "more request handlers available in the pool");
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
        Logger.log(Logger.FULL_DEBUG, "RHPool: Releasing request handler - used: " +
          this.usedRequestHandlerThreads.size() + " unused: " +
          this.unusedRequestHandlerThreads.size());
      }
      else
        Logger.log(Logger.WARNING, "RHPool: Releasing unknown handler. Ignoring");
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
      {throw new WinstoneException("Error parsing XML files", errParser);}
    catch (SAXException errSax)
      {throw new WinstoneException("Error parsing XML files", errSax);}
    catch (IOException errIO)
      {throw new WinstoneException("Error parsing XML files", errIO);}
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
      if (option.startsWith("-"))
        args.put(option.substring(1), argv[n+1]);
    }

    if (args.containsKey("usage"))
      printUsage();

    Logger.setCurrentDebugLevel(args.get("debug") == null ?
                                Logger.DEBUG :
                                Integer.parseInt((String) args.get("debug")));
    Listener listener = new Listener(args);
    Thread th = new Thread(listener);
    th.start();
  }

  private static void printUsage()
  {
    PrintWriter pw = new PrintWriter(System.out, true);
    pw.println("Winstone Server v0.1, (c) 2003 Rick Knowles");
    pw.println("Usage: java com.rickknowles.winstone.Listener [-option value] [-option value] [etc]");
    pw.println("");
    pw.println("Options:");
    pw.println("   -prefix            = add this prefix to all URLs (eg http://localhost:8080/prefix/resource).");
    pw.println("                        Default is none");
    pw.println("   -webroot           = set document root folder. Default <currentdir>/webroot");
    pw.println("   -debug             = set the level of debug msgs (1-9). Default is 5 (INFO only)");
    pw.println("");
    pw.println("   -port              = set the listening port. Default is 8080");
    pw.println("   -controlPort       = set the listening port. Default is 8081, -1 to disable");
    pw.println("");
    pw.println("   -directoryListings = enable directory lists (true/false). Default is true");
    pw.println("   -jasper            = enable jasper JSP handling (true/false). Default is false");
    pw.println("   -doHostnameLookups = enable host name lookups on incoming connections (true/false).");
    pw.println("                        Default is true");
    pw.println("   -usage             = show this message");
    pw.println("");
    pw.println("This program is free software; you can redistribute it and/or");
    pw.println("modify it under the terms of the GNU General Public License");
    pw.println("Version 2 as published by the Free Software Foundation.");
    pw.println("");
    pw.println("This program is distributed in the hope that it will be useful,");
    pw.println("but WITHOUT ANY WARRANTY; without even the implied warranty of");
    pw.println("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
    pw.println("GNU General Public License Version 2 for more details.");
    pw.println("");
    pw.println("You should have received a copy of the GNU General Public License");
    pw.println("Version 2 along with this program; if not, write to the Free Software");
    pw.println("Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.");
    pw.println("");
    pw.println("");
  }
}
