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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implements the main launcher daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Launcher implements EntityResolver, Runnable
{
  final String DTD_2_2_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
  final String DTD_2_2_URL    = "javax/servlet/resources/web-app_2_2.dtd";

  final String DTD_2_3_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
  final String DTD_2_3_URL    = "javax/servlet/resources/web-app_2_3.dtd";

  final String XSD_2_4_PUBLIC = "-//Sun Microsystems, Inc.//XSD Web Application 2.4//EN";
  final String XSD_2_4_URL    = "javax/servlet/resources/web-app_2_4.xsd";

  final String HTTP_LISTENER_CLASS = "winstone.HttpListener";
  final String AJP_LISTENER_CLASS  = "winstone.ajp13.Ajp13Listener";
  final String CLUSTER_CLASS       = "winstone.cluster.SimpleCluster";

  static final String RESOURCE_FILE    = "winstone.LocalStrings";
  static final String WEB_ROOT = "webroot";
  static final String WEB_INF  = "WEB-INF";
  static final String WEB_XML  = "web.xml";
  
  public static final byte SHUTDOWN_TYPE = (byte) '0';
  public static final byte RELOAD_TYPE = (byte) '4';

  private int CONTROL_TIMEOUT = 10; // wait 5s for control connection
  private int DEFAULT_CONTROL_PORT = -1;

  private WinstoneResourceBundle resources;
  private int controlPort;
  private WebAppConfiguration webAppConfig;
  private ObjectPool objectPool;

  private List listeners;
  private boolean interrupted;
  private Map args;
  private File webRoot;
  private ClassLoader commonLibCL;
  
  private Cluster cluster;

  /**
   * Constructor - initialises the web app, object pools, control port and the
   * available protocol listeners.
   */
  public Launcher(Map args, WinstoneResourceBundle resources) throws IOException
  {
    // Load resources
    this.resources = resources;
    this.args = args;
    this.controlPort = (args.get("controlPort") == null ?
                       DEFAULT_CONTROL_PORT :
                       Integer.parseInt((String) args.get("controlPort")));

    // Get the parsed webapp xml deployment descriptor
    this.webRoot = getWebRoot((String) args.get("webroot"), (String) args.get("warfile"), resources);
    if (!webRoot.exists())
      throw new WinstoneException(resources.getString("Launcher.NoWebRoot") + webRoot);
    else
      initWebApp((String) args.get("prefix"), webRoot);

    this.objectPool = new ObjectPool(args, resources, this.webAppConfig);
    
    // Set up common lib class loader
    String commonLibCLFolder = WebAppConfiguration.stringArg(args, "commonLibFolder", "lib");
    File libFolder = new File(commonLibCLFolder);
    if (libFolder.exists() && libFolder.isDirectory())
    {
      File children[] = libFolder.listFiles();
      List jars = new ArrayList();
      for (int n = 0; n < children.length; n++)
        if (children[n].getName().endsWith(".jar") ||
            children[n].getName().endsWith(".zip"))
          jars.add(children[n].toURL());
      this.commonLibCL = new URLClassLoader((URL []) jars.toArray(new URL[jars.size()]));
    }
    
    String useCluster = (String) args.get("useCluster");
    boolean switchOnCluster  = (useCluster != null) && (useCluster.equalsIgnoreCase("true") || useCluster.equalsIgnoreCase("yes"));
    if (switchOnCluster)
    {
      if (this.controlPort < 0)
        Logger.log(Logger.DEBUG, this.resources.getString("Launcher.ClusterOffNoControlPort"));
      else if (!this.webAppConfig.isDistributable())
        Logger.log(Logger.DEBUG, this.resources.getString("Launcher.ClusterOffNotDistributable"));
      else try
      {
        Class clusterClass = Class.forName(CLUSTER_CLASS);
        Constructor clusterConstructor = clusterClass.getConstructor(new Class[] {Map.class, WinstoneResourceBundle.class, Integer.class});
        this.cluster = (Cluster) clusterConstructor.newInstance(new Object[] {args, resources, new Integer(this.controlPort)});
      }
      catch (Throwable err)
        {Logger.log(Logger.DEBUG, this.resources.getString("Launcher.ClusterNotFound"));}
    }

    // Create connectors (http and ajp)
    this.listeners = new ArrayList();
    try
    {
      Class httpClass = Class.forName(HTTP_LISTENER_CLASS);
      Constructor httpConstructor = httpClass.getConstructor(new Class[] {Map.class, WinstoneResourceBundle.class, ObjectPool.class});
      Listener httpListener = (Listener) httpConstructor.newInstance(new Object[] {args, resources, this.objectPool});
      this.listeners.add(httpListener);
    }
    catch (Throwable err)
      {Logger.log(Logger.DEBUG, this.resources.getString("Launcher.HTTPNotFound"));}
    try
    {
      Class ajpClass = Class.forName(AJP_LISTENER_CLASS);
      Constructor ajpConstructor = ajpClass.getConstructor(new Class[] {Map.class, WinstoneResourceBundle.class, ObjectPool.class});
      Listener ajpListener = (Listener) ajpConstructor.newInstance(new Object[] {args, resources, this.objectPool});
      this.listeners.add(ajpListener);
    }
    catch (Throwable err)
      {Logger.log(Logger.DEBUG, this.resources.getString("Launcher.AJPNotFound"));}
  }

  public WebAppConfiguration getWebAppConfig() {return this.webAppConfig;}
  public Cluster getCluster() {return this.cluster;}
  
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
      params.put("[#serverName]", resources.getString("ServerVersion"));
      params.put("[#controlPort]", (this.controlPort > 0 ? "" + this.controlPort : resources.getString("Launcher.ControlDisabled")));
      params.put("[#prefix]", this.webAppConfig.getPrefix());
      params.put("[#webroot]", this.webAppConfig.getWebroot());
      Logger.log(Logger.INFO, resources.getString("Launcher.StartupOK", params));

      // Enter the main loop
      while (!interrupted)
      {
        this.objectPool.removeUnusedRequestHandlers();
        
        // Check for control request
        try 
        {
          if (controlSocket != null)
          {
            Socket csAccepted = controlSocket.accept();
         
            if (csAccepted == null)
              continue;         
            InputStream inSocket = csAccepted.getInputStream();
            int reqType = inSocket.read();
            if ((byte) reqType == SHUTDOWN_TYPE)
              interrupted = true; 
            else if ((byte) reqType == RELOAD_TYPE)
            {
              destroyWebApp(this.webAppConfig);
              initWebApp((String) args.get("prefix"), this.webRoot);
            }
            else if (this.cluster != null)
            {
              OutputStream outSocket = csAccepted.getOutputStream();
              this.cluster.clusterRequest((byte) reqType, inSocket, outSocket, csAccepted, this.webAppConfig);
              outSocket.close();
            }
            inSocket.close();
            csAccepted.close();
          }
          else
            Thread.sleep(CONTROL_TIMEOUT);
        } 
        catch (InterruptedIOException err) {}
        catch (Throwable err)
          {Logger.log(Logger.ERROR, resources.getString("Launcher.ShutdownError"), err);}
      }

      // Close server socket
      controlSocket.close();

      // Release all listeners/handlers/webapps
      for (Iterator i = this.listeners.iterator(); i.hasNext(); )
        ((Listener) i.next()).destroy();
      this.objectPool.destroy();
      if (this.cluster != null) this.cluster.destroy();
      destroyWebApp(this.webAppConfig);

      Logger.log(Logger.INFO, resources.getString("Launcher.ShutdownOK"));
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, resources.getString("Launcher.ShutdownError"), err);}
  }

  /**
   * Destroy this webapp instance. Kills the webapps, plus any servlets, 
   * attributes, etc
   * 
   * @param webApp The webapp to destroy
   */
  public void destroyWebApp(WebAppConfiguration webApp)
  {
    webApp.destroy();
    this.webAppConfig = null; // since we only hold one webapp right now
  }

  public void initWebApp(String prefix, File webRoot) throws IOException
  {
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

    // Instantiate the webAppConfig
    this.webAppConfig = new WebAppConfiguration(this, webRoot.getCanonicalPath(),
        prefix, this.objectPool, args, webXMLParentNode, this.resources,
        this.commonLibCL == null ? this.getClass().getClassLoader() : this.commonLibCL);
  }

  public void shutdown() {this.interrupted = true;}

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
    Logger.log(Logger.FULL_DEBUG, "Resolving entity - public=" + publicName + ", url=" + url);
    if (publicName == null)
      return null;
    else if (publicName.equals(DTD_2_2_PUBLIC))
      return new InputSource(getClass().getClassLoader().getResourceAsStream(DTD_2_2_URL));
    else if (publicName.equals(DTD_2_3_PUBLIC))
      return new InputSource(getClass().getClassLoader().getResourceAsStream(DTD_2_3_URL));
    //else if (publicName.equals(XSD_2_4_PUBLIC) &&
    //    (getClass().getClassLoader().getResourceAsStream(XSD_2_4_URL) != null))
    //  return new InputSource(getClass().getClassLoader().getResourceAsStream(XSD_2_4_URL));
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
      int logLevel = args.get("debug") == null 
                          ? Logger.INFO 
                          : Integer.parseInt((String) args.get("debug"));
      OutputStream logStream = args.get("logfile") == null 
                          ? (OutputStream) System.out 
                          : new FileOutputStream((String) args.get("logfile"));
      Logger.init(logLevel, logStream);
      Logger.log(Logger.DEBUG, resources.getString("Launcher.UsingPropertyFile",
                "[#filename]", configFilename));

    }
    else
    {
      int logLevel = args.get("debug") == null 
                          ? Logger.INFO 
                          : Integer.parseInt((String) args.get("debug"));
      OutputStream logStream = args.get("logfile") == null 
                          ? (OutputStream) System.out 
                          : new FileOutputStream((String) args.get("logfile"));
      Logger.init(logLevel, logStream);
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

