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
package com.rickknowles.winstone.auth;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;

import com.rickknowles.winstone.*;

/**
 * Handles FORM based authentication configurations. Fairly simple ... it 
 * just redirects any unauthorized requests to the login page, and any
 * bad logins to the error page. The auth values are stored in the session
 * in a special slot.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class FormAuthenticationHandler extends AuthenticationHandler
{
  final String FORM_ACTION = "j_security_check";
  final String FORM_USER   = "j_username";
  final String FORM_PASS   = "j_password";
  
  private String loginPage;
  private String errorPage;
  
  /**
   * Constructor for the FORM authenticator
   * 
   * @param realm The realm against which we are authenticating
   * @param constraints The array of security constraints that might apply
   * @param resources The list of resource strings for messages
   * @param realmName The name of the realm this handler claims
   */
  public FormAuthenticationHandler(List roles, String realmName, String loginPage,
    String errorPage, WinstoneResourceBundle resources,
    SecurityConstraint[] constraints, AuthenticationRealm realm)
  {
    super(realm, constraints, resources, realmName);
    this.loginPage = loginPage;
    this.errorPage = errorPage;
    Logger.log(Logger.FULL_DEBUG, resources.getString("FormAuthenticationHandler.Initialised",
      "[#name]", realmName));
  }

  /**
   * Evaluates any authentication constraints, intercepting if auth is required.
   * The relevant authentication handler subclass's logic is used to actually
   * authenticate.
   * @return A boolean indicating whether to continue after this request
   */
  public boolean processAuthentication(WinstoneRequest request,
    WinstoneResponse response, String pathRequested)
    throws IOException, ServletException
  {
    if (pathRequested.equals(this.loginPage) || pathRequested.equals(this.errorPage))
      return true;
    else
      return super.processAuthentication(request, response, pathRequested);
  }

  /**
   * Call this once we know that we need to authenticate
   */
  protected void requestAuthentication(WinstoneRequest request,
  WinstoneResponse response, String pathRequested) throws ServletException, IOException
  {
    // Save the critical details of the request into the session map
    WinstoneSession session = (WinstoneSession) request.getSession(true);
    session.setCachedRequest(new CachedRequest(request, this.resources));
        
    // Forward on to the login page
    Logger.log(Logger.FULL_DEBUG, "Forwarding to the login page");
    javax.servlet.RequestDispatcher rd = request.getRequestDispatcher(this.loginPage);
    rd.forward(request, response);
  }

  /**
   * Check the response - is it a response to the login page ?
   * @return A boolean indicating whether to continue with the request or not
   */
  protected boolean validatePossibleAuthenticationResponse(WinstoneRequest request,
    WinstoneResponse response, String pathRequested) throws ServletException, IOException
  {
    // Check if this is a j_security_check uri
    if (pathRequested.endsWith(FORM_ACTION))
    {
      String username = request.getParameter(FORM_USER);
      String password = request.getParameter(FORM_PASS);

      // Send to error page if invalid
      AuthenticationPrincipal principal = this.realm.authenticateByUsernamePassword(username, password);
      if (principal == null)
      {
        javax.servlet.RequestDispatcher rd = request.getRequestDispatcher(this.errorPage);
        rd.forward(request, response);
      }
      
      // Send to stashed request
      else
      {
        // Get the stashed request
        WinstoneSession session = (WinstoneSession) request.getSession(true);
        principal.setAuthType(HttpServletRequest.FORM_AUTH);
        session.setAuthenticatedUser(principal);
        String previousLocation = this.loginPage;
        if (session.getCachedRequest() != null)
        {
          // Repopulate this request from the params we saved
          ((CachedRequest)session.getCachedRequest()).transferContent(request);
          previousLocation = request.getServletPath();
          session.setCachedRequest(null);
        }
        else
          Logger.log(Logger.DEBUG, this.resources.getString("FormAuthenticationHandler.NoCachedRequest"));
        javax.servlet.RequestDispatcher rd = request.getRequestDispatcher(previousLocation);
        rd.forward(request, response);
      }
      return false;
    }

    // If it's not a login, get the session, and look up the auth user variable
    else
    {
      WinstoneSession session = (WinstoneSession) request.getSession(false);
      if ((session != null) && (session.getAuthenticatedUser() != null))
      {
        request.setRemoteUser(session.getAuthenticatedUser());
        Logger.log(Logger.FULL_DEBUG, "Got authenticated user from session");
      }
      return true;
    }
  }
}
