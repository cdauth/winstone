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
package winstone.testCase;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.meterware.httpunit.*;

import java.io.*;
import java.util.*;
import org.xml.sax.SAXException;

import winstone.*;

/**
 * Test case for the Http Connector to Winstone. Simulates a simple connect
 * and retrieve case, then a keep-alive connection case.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpConnectorTest extends TestCase
{
  public static Test suite() {return (new TestSuite(HttpConnectorTest.class));}

  /**
   * Constructor
   */
  public HttpConnectorTest(String name) {super(name);}

  /**
   * Test the simple case of connecting, retrieving and disconnecting
   */
  public void testSimpleConnection() throws IOException, SAXException, InterruptedException
  {
    // Initialise container
    Map args = new HashMap();
    args.put("webroot", "c:/java/tomcat/webapps/examples");
    args.put("prefix", "/examples");
    args.put("httpPort", "10003");
    args.put("ajp13Port", "-1");
    args.put("controlPort", "-1");
    args.put("debug", "7");
    Logger.setStream(Logger.DEFAULT_STREAM, System.err);
    Logger.setCurrentDebugLevel(Logger.DEBUG);
    WinstoneResourceBundle resources = Launcher.getResourceBundle();
    Launcher winstone = new Launcher(args, resources);

    // Check for a simple connection
    WebConversation wc = new WebConversation();
    WebRequest wreq = new GetMethodWebRequest("http://localhost:10003/examples/images/execute.gif");
    WebResponse wresp = wc.getResponse(wreq);
    assertTrue("Loading execute.gif", wresp.getContentLength() > 0);
    winstone.shutdown();
    Thread.sleep(500);
  }

  /**
   * Test the keep alive case
   */
  public void testKeepAliveConnection() throws IOException, InterruptedException, SAXException
  {
    // Initialise container
    Map args = new HashMap();
    args.put("webroot", "c:/java/tomcat/webapps/examples");
    args.put("prefix", "/examples");
    args.put("httpPort", "10004");
    args.put("ajp13Port", "-1");
    args.put("controlPort", "-1");
    args.put("debug", "7");
    Logger.setStream(Logger.DEFAULT_STREAM, System.err);
    Logger.setCurrentDebugLevel(Logger.DEBUG);
    WinstoneResourceBundle resources = Launcher.getResourceBundle();
    Launcher winstone = new Launcher(args, resources);

    // Check for a simple connection
    WebConversation wc = new WebConversation();
    WebRequest wreq = new GetMethodWebRequest("http://localhost:10004/examples/servlets/index.html");
    WebResponse wresp1 = wc.getResponse(wreq);
    WebImage img[] = wresp1.getImages();
    for (int n = 0; n < img.length; n++)
      wc.getResponse(img[n].getRequest());
    //Thread.sleep(2000);
    //WebResponse wresp2 = wc.getResponse(wreq);
    //Thread.sleep(2000);
    //WebResponse wresp3 = wc.getResponse(wreq);
    assertTrue("Loading login page", wresp1.getContentLength() > 0);
    winstone.shutdown();
    Thread.sleep(500);
  }
}

