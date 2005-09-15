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
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Node;

import winstone.AuthenticationPrincipal;
import winstone.AuthenticationRealm;
import winstone.Logger;
import winstone.WinstoneRequest;
import winstone.WinstoneResourceBundle;
import winstone.WinstoneSession;

/**
 * Handles FORM based authentication configurations. Fairly simple ... it just
 * redirects any unauthorized requests to the login page, and any bad logins to
 * the error page. The auth values are stored in the session in a special slot.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class FormAuthenticationHandler extends BaseAuthenticationHandler {
    static final String ELEM_FORM_LOGIN_CONFIG = "form-login-config";
    static final String ELEM_FORM_LOGIN_PAGE = "form-login-page";
    static final String ELEM_FORM_ERROR_PAGE = "form-error-page";
    static final String FORM_ACTION = "j_security_check";
    static final String FORM_USER = "j_username";
    static final String FORM_PASS = "j_password";
    private String loginPage;
    private String errorPage;

    /**
     * Constructor for the FORM authenticator
     * 
     * @param realm
     *            The realm against which we are authenticating
     * @param constraints
     *            The array of security constraints that might apply
     * @param resources
     *            The list of resource strings for messages
     * @param realmName
     *            The name of the realm this handler claims
     */
    public FormAuthenticationHandler(Node loginConfigNode,
            List constraintNodes, Set rolesAllowed,
            WinstoneResourceBundle resources, AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, resources, realm);

        for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++) {
            Node loginElm = loginConfigNode.getChildNodes().item(n);
            if (loginElm.getNodeName().equals(ELEM_FORM_LOGIN_CONFIG)) {
                for (int k = 0; k < loginElm.getChildNodes().getLength(); k++) {
                    Node formElm = loginElm.getChildNodes().item(k);
                    if (formElm.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    else if (formElm.getNodeName().equals(ELEM_FORM_LOGIN_PAGE))
                        loginPage = formElm.getFirstChild().getNodeValue()
                                .trim();
                    else if (formElm.getNodeName().equals(ELEM_FORM_ERROR_PAGE))
                        errorPage = formElm.getFirstChild().getNodeValue()
                                .trim();
                }
            }
        }
        Logger.log(Logger.DEBUG, this.resources,
                "FormAuthenticationHandler.Initialised", realmName);
    }

    /**
     * Evaluates any authentication constraints, intercepting if auth is
     * required. The relevant authentication handler subclass's logic is used to
     * actually authenticate.
     * 
     * @return A boolean indicating whether to continue after this request
     */
    public boolean processAuthentication(ServletRequest request,
            ServletResponse response, String pathRequested) throws IOException,
            ServletException {
        if (pathRequested.equals(this.loginPage)
                || pathRequested.equals(this.errorPage))
            return true;
        else
            return super
                    .processAuthentication(request, response, pathRequested);
    }

    /**
     * Call this once we know that we need to authenticate
     */
    protected void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response, String pathRequested)
            throws ServletException, IOException {
        // Save the critical details of the request into the session map
        WinstoneRequest actualRequest = null;
        if (request instanceof WinstoneRequest)
            actualRequest = (WinstoneRequest) request;
        else if (request instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
            if (wrapper.getRequest() instanceof WinstoneRequest)
                actualRequest = (WinstoneRequest) wrapper.getRequest();
            else
                Logger.log(Logger.WARNING, this.resources,
                        "FormAuthenticationHandler.CantSetUser", wrapper
                                .getRequest().getClass().getName());
        } else
            Logger.log(Logger.WARNING, this.resources,
                    "FormAuthenticationHandler.CantSetUser", request.getClass()
                            .getName());

        WinstoneSession session = (WinstoneSession) actualRequest
                .getSession(true);
        session.setCachedRequest(new CachedRequest(actualRequest,
                this.resources));

        // Forward on to the login page
        Logger.log(Logger.FULL_DEBUG, resources,
                "FormAuthenticationHandler.GoToLoginPage");
        javax.servlet.RequestDispatcher rd = request
                .getRequestDispatcher(this.loginPage);
        setNoCache(response);
        rd.forward(request, response);
    }

    /**
     * Check the response - is it a response to the login page ?
     * 
     * @return A boolean indicating whether to continue with the request or not
     */
    protected boolean validatePossibleAuthenticationResponse(
            HttpServletRequest request, HttpServletResponse response,
            String pathRequested) throws ServletException, IOException {
        // Check if this is a j_security_check uri
        if (pathRequested.endsWith(FORM_ACTION)) {
            String username = request.getParameter(FORM_USER);
            String password = request.getParameter(FORM_PASS);

            // Send to error page if invalid
            AuthenticationPrincipal principal = this.realm
                    .authenticateByUsernamePassword(username, password);
            if (principal == null) {
                javax.servlet.RequestDispatcher rd = request
                        .getRequestDispatcher(this.errorPage);
                rd.forward(request, response);
            }

            // Send to stashed request
            else {
                // Iterate back as far as we can
                ServletRequest wrapperCheck = request;
                while (wrapperCheck instanceof HttpServletRequestWrapper)
                    wrapperCheck = ((HttpServletRequestWrapper) wrapperCheck)
                            .getRequest();

                // Get the stashed request
                WinstoneRequest actualRequest = null;
                if (wrapperCheck instanceof WinstoneRequest) {
                    actualRequest = (WinstoneRequest) wrapperCheck;
                    actualRequest.setRemoteUser(principal);
                } else
                    Logger.log(Logger.WARNING, this.resources,
                            "FormAuthenticationHandler.CantSetUser",
                            wrapperCheck.getClass().getName());

                WinstoneSession session = (WinstoneSession) request.getSession(true);
                String previousLocation = this.loginPage;
                if ((session.getCachedRequest() != null)
                        && (actualRequest != null)) {
                    // Repopulate this request from the params we saved
                    ((CachedRequest) session.getCachedRequest())
                            .transferContent(actualRequest);
                    previousLocation = request.getServletPath();
                    // session.setCachedRequest(null); - commented out so that
                    // refreshes will work
                } else
                    Logger.log(Logger.DEBUG, this.resources,
                            "FormAuthenticationHandler.NoCachedRequest");
                
                // do role check, since we don't know that this user has permission
                if (doRoleCheck(request, response, previousLocation)) {
                    principal.setAuthType(HttpServletRequest.FORM_AUTH);
                    session.setAuthenticatedUser(principal);
                    javax.servlet.RequestDispatcher rd = request
                            .getRequestDispatcher(previousLocation);
                    rd.forward(request, response);
                } else {
                    javax.servlet.RequestDispatcher rd = request
                            .getRequestDispatcher(this.errorPage);
                    rd.forward(request, response);
                }
            }
            return false;
        }

        // If it's not a login, get the session, and look up the auth user
        // variable
        else {
            WinstoneRequest actualRequest = null;
            if (request instanceof WinstoneRequest)
                actualRequest = (WinstoneRequest) request;
            else if (request instanceof HttpServletRequestWrapper) {
                HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                if (wrapper.getRequest() instanceof WinstoneRequest)
                    actualRequest = (WinstoneRequest) wrapper.getRequest();
                else
                    Logger.log(Logger.WARNING, this.resources,
                            "FormAuthenticationHandler.CantSetUser", wrapper
                                    .getRequest().getClass().getName());
            } else
                Logger.log(Logger.WARNING, this.resources,
                        "FormAuthenticationHandler.CantSetUser", request
                                .getClass().getName());

            WinstoneSession session = (WinstoneSession) actualRequest
                    .getSession(false);
            if ((session != null) && (session.getAuthenticatedUser() != null)) {
                actualRequest.setRemoteUser(session.getAuthenticatedUser());
                Logger.log(Logger.FULL_DEBUG, this.resources,
                        "FormAuthenticationHandler.GotUserFromSession");
            }
            return true;
        }
    }
}
