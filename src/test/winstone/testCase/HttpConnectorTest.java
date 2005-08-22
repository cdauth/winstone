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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.SAXException;

import winstone.Launcher;
import winstone.Logger;
import winstone.WinstoneResourceBundle;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebImage;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Test case for the Http Connector to Winstone. Simulates a simple connect and
 * retrieve case, then a keep-alive connection case.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpConnectorTest extends TestCase {
    public static Test suite() {
        return (new TestSuite(HttpConnectorTest.class));
    }

    /**
     * Constructor
     */
    public HttpConnectorTest(String name) {
        super(name);
    }

    /**
     * Test the simple case of connecting, retrieving and disconnecting
     */
    public void testSimpleConnection() throws IOException, SAXException,
            InterruptedException {
        // Initialise container
        Map args = new HashMap();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10003");
        args.put("ajp13Port", "-1");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Logger.FULL_DEBUG, System.out, true, true);
        WinstoneResourceBundle resources = Launcher.getResourceBundle();
        Launcher winstone = new Launcher(args, resources);

        // Check for a simple connection
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(
                "http://localhost:10003/examples/CountRequestsServlet");
        WebResponse wresp = wc.getResponse(wreq);
        InputStream content = wresp.getInputStream();
        assertTrue("Loading CountRequestsServlet", content.available() > 0);
        content.close();
        winstone.shutdown();
        Thread.sleep(500);
    }

    /**
     * Test the keep alive case
     */
    public void testKeepAliveConnection() throws IOException,
            InterruptedException, SAXException {
        // Initialise container
        Map args = new HashMap();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10004");
        args.put("ajp13Port", "-1");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Logger.FULL_DEBUG, System.out, true, true);
        WinstoneResourceBundle resources = Launcher.getResourceBundle();
        Launcher winstone = new Launcher(args, resources);

        // Check for a simple connection
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(
                "http://localhost:10004/examples/CountRequestsServlet");
        WebResponse wresp1 = wc.getResponse(wreq);
        WebImage img[] = wresp1.getImages();
        for (int n = 0; n < img.length; n++)
            wc.getResponse(img[n].getRequest());
        // Thread.sleep(2000);
        // WebResponse wresp2 = wc.getResponse(wreq);
        // Thread.sleep(2000);
        //WebResponse wresp3 = wc.getResponse(wreq);
        InputStream content = wresp1.getInputStream();
        assertTrue("Loading CountRequestsServlet + child images", content
                .available() > 0);
        content.close();
        winstone.shutdown();
        Thread.sleep(500);
    }
}
