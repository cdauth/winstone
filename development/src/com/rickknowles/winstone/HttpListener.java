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
 * Implements the main listener daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpListener implements Runnable
{
  private int LISTENER_TIMEOUT = 500; // every 500ms reset the listener socket
  private int DEFAULT_PORT = 8080;
  private boolean DEFAULT_HNL = true;

  private WinstoneResourceBundle resources;
  private Launcher launcher;
  private int listenPort;
  private HttpProtocol protocol;
  private boolean interrupted;

  /**
   * Constructor
   */
  public HttpListener(Map args, WinstoneResourceBundle resources, Launcher launcher)
    throws IOException
  {
    // Load resources
    this.resources = resources;
    this.launcher = launcher;

    //this.arguments = args;
    this.listenPort = (args.get("httpPort") == null ? DEFAULT_PORT
                          : Integer.parseInt((String) args.get("httpPort")));
    String hnl = (String) args.get("httpDoHostnameLookups");
    boolean switchOnHNL = (hnl == null ? DEFAULT_HNL : (hnl.equalsIgnoreCase("yes") || hnl.equalsIgnoreCase("true")));
    this.protocol = new HttpProtocol(this.resources, switchOnHNL);
    this.interrupted = false;

    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * The main run method. This handles the normal thread processing.
   */
  public void run()
  {
    try
    {
      ServerSocket ss = new ServerSocket(this.listenPort);
      ss.setSoTimeout(LISTENER_TIMEOUT);
      Logger.log(Logger.INFO, resources.getString("HttpListener.StartupOK",
                              "[#port]", this.listenPort + ""));

      // Enter the main loop
      while (!interrupted)
      {
        // Get the listener
        Socket s = null;
        try
          {s = ss.accept();}
        catch (InterruptedIOException err) {s = null;}

        // if we actually got a socket, process it. Otherwise go around again
        if (s != null)
          this.launcher.handleRequest(s, this.protocol);
      }

      // Close server socket
      ss.close();
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, resources.getString("HttpListener.ShutdownError"), err);}

    Logger.log(Logger.INFO, resources.getString("HttpListener.ShutdownOK"));
  }

  public void destroy() {this.interrupted = true;}
}

