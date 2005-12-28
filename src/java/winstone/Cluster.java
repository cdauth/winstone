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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Represents a cluster implementation, which is basically the communication
 * mechanism between a group of winstone containers.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public interface Cluster {
    /**
     * Destroy the maintenance thread if there is one. Prepare for shutdown
     */
    public void destroy();

    /**
     * Check if the other nodes in this cluster have a session for this
     * sessionId.
     * 
     * @param sessionId The id of the session to check for
     * @param webAppConfig The web app that owns the session we want
     * @return A valid session instance
     */
    public WinstoneSession askClusterForSession(String sessionId,
            WebAppConfiguration webAppConfig);

    /**
     * Accept a control socket request related to the cluster functions and
     * process the request.
     * 
     * @param requestType A byte indicating the request type
     * @param in Socket input stream
     * @param outSocket output stream
     * @param hostConfig The collection of all local webapps
     * @throws IOException
     */
    public void clusterRequest(byte requestType, InputStream in,
            OutputStream out, Socket socket, HostGroup hostGroup)
            throws IOException;
}
