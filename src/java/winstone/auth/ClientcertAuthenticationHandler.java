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
package winstone.auth;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Node;

import winstone.AuthenticationPrincipal;
import winstone.AuthenticationRealm;
import winstone.Logger;
import winstone.WinstoneRequest;
import winstone.WinstoneResourceBundle;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ClientcertAuthenticationHandler extends BaseAuthenticationHandler {
    public ClientcertAuthenticationHandler(Node loginConfigNode,
            List constraintNodes, Set rolesAllowed,
            WinstoneResourceBundle resources, AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, resources, realm);
        Logger.log(Logger.DEBUG, this.resources,
                "ClientcertAuthenticationHandler.Initialised", realmName);
    }

    /**
     * Call this once we know that we need to authenticate
     */
    protected void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response, String pathRequested)
            throws IOException {
        // Return unauthorized, and set the realm name
        response
                .sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        this.resources
                                .getString("ClientcertAuthenticationHandler.UnauthorizedMessage"));
    }

    /**
     * Handling the (possible) response
     */
    protected boolean validatePossibleAuthenticationResponse(
            HttpServletRequest request, HttpServletResponse response,
            String pathRequested) throws IOException {
        // Check for certificates in the request attributes
        X509Certificate certificateArray[] = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");
        if ((certificateArray != null) && (certificateArray.length > 0)) {
            boolean failed = false;
            for (int n = 0; n < certificateArray.length; n++)
                try {
                    certificateArray[n].checkValidity();
                } catch (Throwable err) {
                    failed = true;
                }
            if (!failed) {
                AuthenticationPrincipal principal = this.realm
                        .retrieveUser(certificateArray[0].getSubjectDN()
                                .getName());
                if (principal != null) {
                    principal.setAuthType(HttpServletRequest.CLIENT_CERT_AUTH);
                    if (request instanceof WinstoneRequest)
                        ((WinstoneRequest) request).setRemoteUser(principal);
                    else if (request instanceof HttpServletRequestWrapper) {
                        HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                        if (wrapper.getRequest() instanceof WinstoneRequest)
                            ((WinstoneRequest) wrapper.getRequest())
                                    .setRemoteUser(principal);
                        else
                            Logger
                                    .log(
                                            Logger.WARNING,
                                            this.resources,
                                            "ClientCertAuthenticationHandler.CantSetUser",
                                            wrapper.getRequest().getClass()
                                                    .getName());
                    } else
                        Logger.log(Logger.WARNING, this.resources,
                                "ClientCertAuthenticationHandler.CantSetUser",
                                request.getClass().getName());
                }
            }
        }
        return true;
    }
}
