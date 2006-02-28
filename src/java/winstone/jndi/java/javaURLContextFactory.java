/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.jndi.java;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import winstone.jndi.WinstoneContext;

/**
 * Creates the initial instance of the Winstone JNDI context (corresponds to
 * java:/ urls)
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class javaURLContextFactory implements InitialContextFactory,
        ObjectFactory {
//    private static final WinstoneResourceBundle resources = new WinstoneResourceBundle("winstone.jndi.LocalStrings");
    
//    private static Map rootContexts;
//    static {
//        rootContexts = new Hashtable();
//    }
//
//    /**
//     * Gets a context using the thread context class loader as a key. This allows us
//     * to return different jndi spaces for each webapp, since the context class loader
//     * is unique for each
//     */
//    public Context getInitialContext(Hashtable env) throws NamingException {
//        
//        synchronized (rootContexts) {
//            // Check for a context matching this thread context class loader, and
//            // recurse back to the root CL until a match is found
//            ClassLoader cl = Thread.currentThread().getContextClassLoader();
//            ClassLoader loopIndex = cl;
//            while (loopIndex != null) {
//                Logger.log(Logger.FULL_DEBUG, resources, "javaURLContextFactory.TryingForClassLoader",
//                        loopIndex.toString());
//                Context rootContext = (Context) rootContexts.get(loopIndex);
//                if (rootContext != null) {
//                    return (Context) rootContext.lookup("");
//                } else {
//                    loopIndex = loopIndex.getParent();
//                }
//            }
//            
//            // If no match, create a new context
//            Logger.log(Logger.FULL_DEBUG, resources, "javaURLContextFactory.NewContext",
//                    cl.toString());
//            Context rootContext = new WinstoneContext(env, null, "java:/comp/env",
//                        new Boolean(true));
//            rootContexts.put(cl, rootContext);
//            return (Context) rootContext.lookup("");
//            
//        }
//    }

    private static Context rootContext;
    private Object lock = new Boolean(true);

    public Context getInitialContext(Hashtable env) throws NamingException {
        synchronized (lock) {
            if (rootContext == null)
                rootContext = new WinstoneContext(env, null, "java:/comp/env",
                        new Boolean(true));
        }
        return (Context) rootContext.lookup("");
    }
    
    public Object getObjectInstance(Object object, Name name, Context context,
            Hashtable env) {
        return null;
    }
}
