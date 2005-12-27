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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * The threads to which incoming requests get allocated.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestHandlerThread implements Runnable {
    private Thread thread;
    private ObjectPool objectPool;
    private WinstoneInputStream inData;
    private WinstoneOutputStream outData;
    private WinstoneRequest req;
    private WinstoneResponse rsp;
    private Listener listener;
    private Socket socket;
    private String threadName;
    private long requestStartTime;
    private boolean simulateModUniqueId;
//    private Object processingMonitor = new Boolean(true);

    /**
     * Constructor - this is called by the handler pool, and just sets up for
     * when a real request comes along.
     */
    public RequestHandlerThread(ObjectPool objectPool, int threadIndex, 
            boolean simulateModUniqueId) {
        this.objectPool = objectPool;
        this.simulateModUniqueId = simulateModUniqueId;
        this.threadName = Launcher.RESOURCES.getString(
                "RequestHandlerThread.ThreadName", "" + threadIndex);

        // allocate a thread to run on this object
        this.thread = new Thread(this, threadName);
        this.thread.setDaemon(true);
    }

    /**
     * The main thread execution code.
     */
    public void run() {
        
        boolean interrupted = false;
        while (!interrupted) {
            // Start request processing
            InputStream inSocket = null;
            OutputStream outSocket = null;
            boolean iAmFirst = true;
            try {
                // Get input/output streams
                inSocket = socket.getInputStream();
                outSocket = socket.getOutputStream();

                // The keep alive loop - exiting from here means the connection has closed
                boolean continueFlag = true;
                while (continueFlag && !interrupted) {
                    try {
                        long requestId = System.currentTimeMillis();
                        this.listener.allocateRequestResponse(socket, inSocket,
                                outSocket, this, iAmFirst);
                        if (this.req == null) {
                            // Dead request - happens sometimes with ajp13 - discard
                            this.listener.deallocateRequestResponse(this, req,
                                    rsp, inData, outData);
                            continue;
                        }
                        String servletURI = this.listener.parseURI(this,
                                this.req, this.rsp, this.inData, this.socket,
                                iAmFirst);
                        if (servletURI == null) {
                            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                                    "RequestHandlerThread.KeepAliveTimedOut", this.threadName);
                            
                            // Keep alive timed out - deallocate and go into wait state
                            this.listener.deallocateRequestResponse(this, req,
                                    rsp, inData, outData);
                            continueFlag = false;
                            continue;
                        }
                        
                        if (this.simulateModUniqueId) {
                            req.setAttribute("UNIQUE_ID", "" + requestId);
                        }
                        long headerParseTime = getRequestProcessTime();
                        iAmFirst = false;

                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                                "RequestHandlerThread.StartRequest",
                                "" + requestId);

                        // Get the URI from the request, check for prefix, then
                        // match it to a requestDispatcher
                        WebAppConfiguration webAppConfig = req.getWebAppGroup().getWebAppByURI(servletURI);
                        if (webAppConfig == null) {
                            webAppConfig = req.getWebAppGroup().getWebAppByURI("/");    
                        }
                        if (webAppConfig == null) {
                            Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                    "RequestHandlerThread.UnknownWebapp",
                                    new String[] { servletURI });
                            rsp.sendError(WinstoneResponse.SC_NOT_FOUND, 
                                    Launcher.RESOURCES.getString("RequestHandlerThread.UnknownWebappPage", servletURI));
                            rsp.flushBuffer();
                            rsp.verifyContentLength();

                            // Process keep-alive
                            continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);
                            this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
                            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "RequestHandlerThread.FinishRequest",
                                    "" + requestId);
                            Logger.log(Logger.SPEED, Launcher.RESOURCES, "RequestHandlerThread.RequestTime",
                                    new String[] { servletURI, "" + headerParseTime, "" + getRequestProcessTime() });
                            continue;
                        }
                        req.setWebAppConfig(webAppConfig);

                        // Now we've verified it's in the right webapp, send
                        // request in scope notify
                        ServletRequestListener reqLsnrs[] = webAppConfig.getRequestListeners();
                        for (int n = 0; n < reqLsnrs.length; n++) {
                            reqLsnrs[n].requestInitialized(
                                    new ServletRequestEvent(webAppConfig, req));
                        }

                        // Lookup a dispatcher, then process with it
                        processRequest(webAppConfig, req, rsp, 
                                webAppConfig.getServletURIFromRequestURI(servletURI));
                        this.outData.finishResponse();
                        this.inData.finishRequest();

                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                                "RequestHandlerThread.FinishRequest",
                                "" + requestId);

                        // Process keep-alive
                        continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);

                        // Set last accessed time on session as start of this
                        // request
                        WinstoneSession session = (WinstoneSession) req.getSession(false);
                        if (session != null) {
                            session.setLastAccessedDate(this.requestStartTime);
                        }

                        // send request listener notifies
                        for (int n = 0; n < reqLsnrs.length; n++)
                            reqLsnrs[n].requestDestroyed(new ServletRequestEvent(
                                            webAppConfig, req));

                        req.setWebAppConfig(null);
                        rsp.setWebAppConfig(null);
                        req.setRequestAttributeListeners(null);

                        this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
                        Logger.log(Logger.SPEED, Launcher.RESOURCES, "RequestHandlerThread.RequestTime",
                                new String[] { servletURI, "" + headerParseTime, 
                                                "" + getRequestProcessTime() });
                    } catch (InterruptedIOException errIO) {
                        continueFlag = false;
                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                                "RequestHandlerThread.SocketTimeout", errIO);
                    } catch (SocketException errIO) {
                        continueFlag = false;
                    }
                }
                this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
                this.listener.releaseSocket(this.socket, inSocket, outSocket); // shut sockets
            } catch (Throwable err) {
                try {
                    this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
                } catch (Throwable errClose) {
                }
                try {
                    this.listener.releaseSocket(this.socket, inSocket,
                            outSocket); // shut sockets
                } catch (Throwable errClose) {
                }
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "RequestHandlerThread.RequestError", err);
            }

            this.objectPool.releaseRequestHandler(this);

            if (!interrupted) {
                // Suspend this thread until we get assigned and woken up
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "RequestHandlerThread.EnterWaitState");
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException err) {
                    interrupted = true;
                }
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "RequestHandlerThread.WakingUp");
            }
        }
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "RequestHandlerThread.ThreadExit");
    }

    /**
     * Actually process the request. This takes the request and response, and feeds
     * them to the desired servlet, which then processes them or throws them off to
     * another servlet.
     */
    private void processRequest(WebAppConfiguration webAppConfig, WinstoneRequest req, 
            WinstoneResponse rsp, String path) throws IOException, ServletException {
        RequestDispatcher rd = null;
        javax.servlet.RequestDispatcher rdError = null;
        try {
            rd = webAppConfig.getInitialDispatcher(path, req, rsp);

            // Null RD means an error or we have been redirected to a welcome page
            if (rd != null) {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "RequestHandlerThread.HandlingRD", rd.getName());
                rd.forward(req, rsp);
            }
            // if null returned, assume we were redirected
        } catch (Throwable err) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES,
                    "RequestHandlerThread.UntrappedError", err);
            rdError = webAppConfig.getErrorDispatcherByClass(err);
        }

        // If there was any kind of error, execute the error dispatcher here
        if (rdError != null) {
            try {
                if (rsp.isCommitted()) {
                    rdError.include(req, rsp);
                } else {
                    rsp.resetBuffer();
                    rdError.forward(req, rsp);
                }
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "RequestHandlerThread.ErrorInErrorServlet", err);
            }
//            rsp.sendUntrappedError(err, req, rd != null ? rd.getName() : null);
        }
        rsp.flushBuffer();
        rsp.verifyContentLength();
    }

    /**
     * Assign a socket to the handler
     */
    public void commenceRequestHandling(Socket socket, Listener listener) {
        this.listener = listener;
        this.socket = socket;
        if (this.thread.isAlive())
            synchronized (this) {
                this.notifyAll();
            }
        else
            this.thread.start();
    }

    public void setRequest(WinstoneRequest request) {
        this.req = request;
    }

    public void setResponse(WinstoneResponse response) {
        this.rsp = response;
    }

    public void setInStream(WinstoneInputStream inStream) {
        this.inData = inStream;
    }

    public void setOutStream(WinstoneOutputStream outStream) {
        this.outData = outStream;
    }

    public void setRequestStartTime() {
        this.requestStartTime = System.currentTimeMillis();
    }

    public long getRequestProcessTime() {
        return System.currentTimeMillis() - this.requestStartTime;
    }

    /**
     * Trigger the thread destruction for this handler
     */
    public void destroy() {
        if (this.thread.isAlive()) {
            this.thread.interrupt();
        }
    }
}
