/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.util.EventListener;

/**
 * Listener for requests going in and out of scope
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public interface ServletRequestListener extends EventListener {
    public void requestDestroyed(ServletRequestEvent sre);

    public void requestInitialized(ServletRequestEvent sre);
}
