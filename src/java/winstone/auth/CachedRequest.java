/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import winstone.WinstoneInputStream;
import winstone.WinstoneRequest;

/**
 * This class has been included so that we can handle caching a request object
 * without taking it out of circulation. This class just caches and replays the
 * crucial data from a request, making sure the original request is ok to be
 * returned to the pool.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class CachedRequest extends WinstoneRequest {
    /**
     * Constructor - dumps the input request contents here, so that they can be
     * retrieved by transferContent() later.
     * 
     * @param resources
     *            Resource bundle for error messages
     * @param request
     *            The source request to copy from
     * @throws IOException
     *             If there are any problems with reading from the stream
     */
    public CachedRequest(WinstoneRequest request) throws IOException {
        super();

        // Stash the relevant pieces of info
        this.attributesStack.addAll(request.getAttributesStack());
        this.parametersStack.addAll(request.getParametersStack());
        this.attributes.putAll(request.getAttributes());
        this.parameters.putAll(request.getParameters());
        this.forwardedParameters.putAll(request.getForwardedParameters());
        this.locales = request.getListLocales();
        this.method = request.getMethod();
        this.scheme = request.getScheme();
        this.serverName = request.getServerName();
        this.requestURI = request.getRequestURI();
        this.servletPath = request.getServletPath();
        this.pathInfo = request.getPathInfo();
        this.queryString = request.getQueryString();
        this.protocol = request.getProtocol();
        this.contentLength = request.getContentLength();
        this.contentType = request.getContentType();
        this.serverPort = request.getServerPort();
        this.requestedSessionIds.putAll(request.getRequestedSessionIds());
        this.currentSessionIds.putAll(request.getCurrentSessionIds());
        this.deadRequestedSessionId = request.getDeadRequestedSessionId();
        this.servletConfig = request.getServletConfig();
        this.webappConfig = request.getWebAppConfig();
        this.hostGroup = request.getHostGroup();
        this.encoding = request.getEncoding();
        this.parsedParameters = request.getParsedParameters();
        InputStream in = request.getInputStream();
        ByteArrayOutputStream inBackup = new ByteArrayOutputStream();
        if (this.method.equals("POST")) {
            byte buffer[] = new byte[8192];
            int inputCounter = 0;
            int readBytes = in.read(buffer);
            while ((readBytes != -1)
                    && ((inputCounter < this.contentLength) || (this.contentLength == -1))) {
                inputCounter += readBytes;
                inBackup.write(buffer, 0, readBytes);
                readBytes = in.read(buffer);
            }
        }
        this.inputData = new WinstoneInputStream(inBackup.toByteArray());
    }

    /**
     * Copies the contents we stashed earlier into a new request
     * @param request The request to write to
     */
    public void transferContent(WinstoneRequest request) {
        request.getAttributesStack().clear();
        request.getAttributesStack().addAll(this.attributesStack);
        request.getParametersStack().clear();
        request.getParametersStack().addAll(this.parametersStack);
        request.getParameters().clear();
        request.getParameters().putAll(this.parameters);
        request.getForwardedParameters().clear();
        request.getForwardedParameters().putAll(this.forwardedParameters);
        request.getAttributes().clear();
        request.getAttributes().putAll(this.attributes);
        request.setLocales(this.locales);
        request.setMethod(this.method);
        request.setScheme(this.scheme);
        request.setServerName(this.serverName);
        request.setRequestURI(this.requestURI);
        request.setServletPath(this.servletPath);
        request.setPathInfo(this.pathInfo);
        request.setQueryString(this.queryString);
        request.setProtocol(this.protocol);
        request.setContentLength(this.contentLength);
        request.setContentType(this.contentType);
        request.setServerPort(this.serverPort);
        request.getRequestedSessionIds().clear();
        request.getRequestedSessionIds().putAll(this.requestedSessionIds);
        request.getCurrentSessionIds().clear();
        request.getCurrentSessionIds().putAll(this.currentSessionIds);
        request.setDeadRequestedSessionId(this.deadRequestedSessionId);
        request.setEncoding(this.encoding);
        request.setParsedParameters(this.parsedParameters);
        request.setInputStream(this.inputData);
        request.setServletConfig(this.servletConfig);
        request.setWebAppConfig(this.webappConfig);
        request.setHostGroup(this.hostGroup);
    }
}
