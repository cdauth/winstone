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

import com.rickknowles.winstone.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles HTTP basic authentication.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class BasicAuthenticationHandler extends AuthenticationHandler
{
  public BasicAuthenticationHandler(List roles, String realmName,
    WinstoneResourceBundle resources, SecurityConstraint constraints[],
    AuthenticationRealm realm)
  {
    super(realm, constraints, resources, realmName);
    Logger.log(Logger.FULL_DEBUG, resources.getString("BasicAuthenticationHandler.Initialised",
      "[#name]", realmName));
  }

  /**
   * Call this once we know that we need to authenticate
   */
  protected void requestAuthentication(HttpServletRequest request,
    HttpServletResponse response, String pathRequested) throws IOException
  {
    // Return unauthorized, and set the realm name
    response.setHeader("WWW-Authenticate", "Basic Realm=\"" + this.realmName + "\"");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
      this.resources.getString("BasicAuthenticationHandler.UnauthorizedMessage"));
  }

  /**
   * Handling the (possible) response
   */
  protected void validatePossibleAuthenticationResponse(WinstoneRequest request,
    WinstoneResponse response, String pathRequested) throws IOException
  {
    String authorization = request.getHeader("Authorization");
    if (authorization == null)
      return;

    // Check it's basic
    if (authorization.toLowerCase().startsWith("basic"))
    {
      String decoded = decodeBase64String(authorization.substring(5).trim());
      int delimPos = decoded.indexOf(':');
      if (delimPos != -1)
      {
        AuthenticationPrincipal principal = this.realm.authenticateByUsernamePassword(
            decoded.substring(0, delimPos).trim(), decoded.substring(delimPos + 1).trim());
        if (principal != null)
        {
          principal.setAuthType(HttpServletRequest.BASIC_AUTH);
          request.setRemoteUser(principal);
        }
      }
    }
  }
}

