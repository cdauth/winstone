/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.classLoader;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Implements the servlet spec model (v2.3 section 9.7.2) for classloading, which
 * is different to the standard JDK model in that it delegates *after* checking
 * local repositories. This has the effect of isolating copies of classes that exist
 * in 2 webapps from each other. 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(URL[] urls) {
        super(urls);
    }

    public WebappClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    public WebappClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name);
        
        // If an allowed class, load it locally first
        try {
            if (c == null) {
                // If still not found, then invoke findClass in order to find the class.
                c = findClass(name);
            }
        } catch (ClassNotFoundException e) {
            c = null;
        }
        if (c == null) {
            ClassLoader parent = getParent();
            if (parent != null) {
                c = parent.loadClass(name);
            } else {
                c = getSystemClassLoader().loadClass(name);                    
            }
        }
        if (resolve && (c != null)) {
            resolveClass(c);
        }
        return c;
    }
}
