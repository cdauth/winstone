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

import java.util.*;
import org.w3c.dom.Node;
import winstone.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Base class for managers of authentication within Winstone. This class also
 * acts as a factory, loading the appropriate subclass for the requested auth type.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class BaseAuthenticationHandler implements AuthenticationHandler
{
  static final String ELEM_REALM_NAME = "realm-name";
  static final String LOCAL_RESOURCES = "winstone.auth.LocalStrings";

  protected SecurityConstraint constraints[];
  protected AuthenticationRealm realm;
  protected String realmName;
  protected WinstoneResourceBundle resources;

  /**
   * Factory method - this parses the web.xml nodes and builds the correct
   * subclass for handling that auth type.
   */
  protected BaseAuthenticationHandler(Node loginConfigNode, List constraintNodes,
      Set rolesAllowed, WinstoneResourceBundle resources, AuthenticationRealm realm)
  {
    this.realm = realm;
    this.resources = new WinstoneResourceBundle(LOCAL_RESOURCES); //resources;

    for (int m = 0; m < loginConfigNode.getChildNodes().getLength(); m++)
    {
      Node loginElm = (Node) loginConfigNode.getChildNodes().item(m);
      if (loginElm.getNodeType() != Node.ELEMENT_NODE)
          continue;
      else if (loginElm.getNodeName().equals(ELEM_REALM_NAME))
        realmName = loginElm.getFirstChild().getNodeValue().trim();
    }

    // Build security constraints
    this.constraints = new SecurityConstraint[constraintNodes.size()]; 
    for (int n = 0; n < constraints.length; n++)
      this.constraints[n] = new SecurityConstraint((Node) constraintNodes.get(n), rolesAllowed, resources, this.resources, n);
  }

  /**
   * Evaluates any authentication constraints, intercepting if auth is required.
   * The relevant authentication handler subclass's logic is used to actually
   * authenticate.
   * @return A boolean indicating whether to continue after this request
   */
  public boolean processAuthentication(ServletRequest inRequest,
    ServletResponse inResponse, String pathRequested)
    throws IOException, ServletException
  {
    Logger.log(Logger.FULL_DEBUG, this.resources.getString("BaseAuthenticationHandler.StartAuthCheck"));
    
    HttpServletRequest request = (HttpServletRequest) inRequest;
    HttpServletResponse response = (HttpServletResponse) inResponse;
    
    // Give previous attempts a chance to be validated
    if (!validatePossibleAuthenticationResponse(request, response, pathRequested))
      return false;

    // Loop through constraints
    boolean foundApplicable = false;
    for (int n = 0; (n < this.constraints.length) && !foundApplicable; n++)
    {
      Logger.log(Logger.FULL_DEBUG, this.resources.getString("BaseAuthenticationHandler.EvalConstraint", 
                                    "[#name]", this.constraints[n].getName()));

      // Find one that applies, then
      if (this.constraints[n].isApplicable(pathRequested, request.getMethod()))
      {
        Logger.log(Logger.FULL_DEBUG, this.resources.getString("BaseAuthenticationHandler.ApplicableConstraint", 
                                      "[#name]", this.constraints[n].getName()));
        foundApplicable = true;

        if (this.constraints[n].needsSSL() && !request.isSecure())
        {
          String msg = this.resources.getString("BaseAuthenticationHandler.ConstraintNeedsSSL",
            "[#name]", this.constraints[n].getName());
          Logger.log(Logger.DEBUG, msg);
          response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
          return false;
        }

        else if (!this.constraints[n].isAllowed(request))
        {
          //Logger.log(Logger.FULL_DEBUG, "Not allowed - requesting auth");
          requestAuthentication(request, response, pathRequested);
          return false;
        }
        else
          // Logger.log(Logger.FULL_DEBUG, "Allowed - authorization accepted");
          // Ensure that secured resources are not cached
          setNoCache(response);
      }
    }
    // If we made it this far without a check being run, there must be none applicable
    Logger.log(Logger.FULL_DEBUG, this.resources.getString("BaseAuthenticationHandler.PassedAuthCheck"));
    return true;
  }

  protected void setNoCache(HttpServletResponse response)
  {
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "No-cache");
    response.setDateHeader("Expires", 1);
  }
  
  /**
   * The actual auth request implementation.
   */
  protected abstract void requestAuthentication(HttpServletRequest request,
    HttpServletResponse response, String pathRequested) 
      throws IOException, ServletException;

  /**
   * Handling the (possible) response
   */
  protected abstract boolean validatePossibleAuthenticationResponse(
    HttpServletRequest request, HttpServletResponse response, String pathRequested)
      throws ServletException, IOException;
}

