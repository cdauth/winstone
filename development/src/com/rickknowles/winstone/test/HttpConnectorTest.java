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
package com.rickknowles.winstone.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.meterware.httpunit.*;

import java.net.*;
import java.io.*;
import java.util.*;
import org.xml.sax.SAXException;

import com.rickknowles.winstone.*;

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

  private Launcher winstone;

  /**
   * Constructor
   */
  public HttpConnectorTest(String name) {super(name);}

  public void setUp() throws IOException, InterruptedException
  {/*
    Map args = new HashMap();
    args.put("webroot", "c:/java/tomcat/webapps/examples");
    args.put("prefix", "/examples");
    args.put("httpPort", "9080");
    args.put("debug", "8");
    WinstoneResourceBundle resources = Launcher.getResourceBundle();
    winstone = new Launcher(args, resources);
    Thread.currentThread().sleep(1000);
  */}

  public void tearDown() throws InterruptedException
  {/*
    winstone.shutdown();
    Thread.currentThread().sleep(2000);
  */}

  /**
   * Test the simple case of connecting, retrieving and disconnecting
   */
  public void testSimpleConnection() throws IOException, SAXException
  {
/*    // Check for a simple connection
    WebConversation wc = new WebConversation();
    WebRequest wreq = new GetMethodWebRequest("http://localhost:9080/examples/images/execute.gif");
    WebResponse wresp = wc.getResponse(wreq);
    this.assertTrue("Loading execute.gif", wresp.getContentLength() > 0);
*/  }

  /**
   * Test the keep alive case
   */
  public void testKeepAliveConnection() throws IOException, InterruptedException, SAXException
  {
    // Check for a simple connection
    WebConversation wc = new WebConversation();
    WebRequest wreq = new GetMethodWebRequest("http://localhost:9080/training/cgi/Router");
    WebResponse wresp1 = wc.getResponse(wreq);
    WebImage img[] = wresp1.getImages();
    for (int n = 0; n < img.length; n++)
      wc.getResponse(img[n].getRequest());
    //Thread.sleep(2000);
    //WebResponse wresp2 = wc.getResponse(wreq);
    //Thread.sleep(2000);
    //WebResponse wresp3 = wc.getResponse(wreq);
    this.assertTrue("Loading login page", wresp1.getContentLength() > 0);
  }
}

