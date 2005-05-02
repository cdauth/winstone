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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import winstone.StaticResourceServlet;

/**
 * Automated tests for the url security check inside the static resource servlet
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class StaticResourceServletTest extends TestCase {
    
    /**
     * Constructor
     */
    public StaticResourceServletTest(String name) {
        super(name);
    }
    
    public void testIsDescendant() throws IOException {
        File webroot = new File("src/testwebapp");
        File webinf = new File(webroot, "WEB-INF");
        assertTrue("Direct subfolder", StaticResourceServlet.isDescendant(webroot, webinf, webroot));
        assertTrue("Self is a descendent of itself", StaticResourceServlet.isDescendant(webinf, webinf, webroot));
        assertTrue("Direct subfile", StaticResourceServlet.isDescendant(webinf, new File(webinf, "web.xml"), webroot));
        assertTrue("Indirect subfile", StaticResourceServlet.isDescendant(webroot, new File(webinf, "web.xml"), webroot));
        assertTrue("Backwards iterations", !StaticResourceServlet.isDescendant(webinf, new File(webinf, ".."), webroot));
    }
    
    public void testCanonicalVersion() throws IOException {
        File webroot = new File("src/testwebapp");
        File webinf = new File(webroot, "WEB-INF");
        File webxml = new File(webinf, "web.xml");
        assertTrue("Simplest case", 
                StaticResourceServlet.constructOurCanonicalVersion(
                        webxml, webroot).equals("/WEB-INF/web.xml"));
        assertTrue("One back step", 
                StaticResourceServlet.constructOurCanonicalVersion(
                        new File(webroot, "/test/../WEB-INF/web.xml"), webroot)
                .equals("/WEB-INF/web.xml"));
    }
}
