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
package com.rickknowles.winstone;

import java.lang.reflect.*;
import java.util.*;
import org.w3c.dom.Node;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Base class for managers of authentication within Winstone. This class also
 * acts as a factory, loading the appropriate subclass for the requested auth type.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class AuthenticationHandler
{
  static final String ELEM_SECURITY_CONSTRAINT  = "security-constraint";
  static final String ELEM_LOGIN_CONFIG         = "login-config";
  static final String ELEM_AUTH_METHOD          = "auth-method";
  static final String ELEM_REALM_NAME           = "realm-name";
  static final String ELEM_FORM_LOGIN_CONFIG    = "form-login-config";
  static final String ELEM_FORM_LOGIN_PAGE      = "form-login-page";
  static final String ELEM_FORM_ERROR_PAGE      = "form-error-page";

  static final String DEFAULT_REALM = "DefaultRealm";

  protected SecurityConstraint constraints[];
  protected AuthenticationRealm realm;
  protected String realmName;
  protected WinstoneResourceBundle resources;

  /**
   * Factory method - this parses the web.xml nodes and builds the correct
   * subclass for handling that auth type.
   */
  public static AuthenticationHandler getInstance(List securityRelatedNodes,
    WinstoneResourceBundle resources, AuthenticationRealm realm)
  {
    List localConstraints = new ArrayList();
    List roles = new ArrayList();
    String authMethod = null;
    String realmName = null;
    String loginPage = null;
    String errorPage = null;

    for (Iterator i = securityRelatedNodes.iterator(); i.hasNext(); )
    {
      Node child = (Node) i.next();
      if (child.getNodeName().equals(ELEM_LOGIN_CONFIG))
      {
        for (int m = 0; m < child.getChildNodes().getLength(); m++)
        {
          Node loginElm = (Node) child.getChildNodes().item(m);
          if (loginElm.getNodeType() != Node.ELEMENT_NODE)
            continue;
          else if (loginElm.getNodeName().equals(ELEM_AUTH_METHOD))
            authMethod = loginElm.getFirstChild().getNodeValue().trim();
          else if (loginElm.getNodeName().equals(ELEM_REALM_NAME))
            realmName = loginElm.getFirstChild().getNodeValue().trim();
          else if (loginElm.getNodeName().equals(ELEM_FORM_LOGIN_CONFIG))
          {
            for (int k = 0; k < loginElm.getChildNodes().getLength(); k++)
            {
              Node formElm = (Node) loginElm.getChildNodes().item(k);
              if (formElm.getNodeType() != Node.ELEMENT_NODE)
                continue;
              else if (formElm.getNodeName().equals(ELEM_FORM_LOGIN_PAGE))
                loginPage = formElm.getFirstChild().getNodeValue().trim();
              else if (formElm.getNodeName().equals(ELEM_FORM_ERROR_PAGE))
                errorPage = formElm.getFirstChild().getNodeValue().trim();
            }
          }
        }
      }
      else if (child.getNodeName().equals(ELEM_SECURITY_CONSTRAINT))
        localConstraints.add(new SecurityConstraint(child, resources, localConstraints.size()));
    }
    SecurityConstraint constraints[] = (SecurityConstraint []) localConstraints.toArray(
                                        new SecurityConstraint[localConstraints.size()]);

    // Defaults
    if (authMethod == null)
      authMethod = "BASIC";
    if (realm == null)
    {
      Logger.log(Logger.WARNING, resources.getString("AuthenticationHandler.NoRealmFound"));
      return null;
    }

    // Get the class matching the auth method
    try
    {
      if (authMethod.equalsIgnoreCase("BASIC"))
      {
        Class authClass = Class.forName("com.rickknowles.winstone.auth.BasicAuthenticationHandler");
        Constructor authConstr = authClass.getConstructor(new Class[]
            {List.class, String.class, WinstoneResourceBundle.class,
              constraints.getClass(), AuthenticationRealm.class});
        return (AuthenticationHandler) authConstr.newInstance(new Object[]
            {roles, realmName, resources, constraints, realm});
      }
      else if (authMethod.equalsIgnoreCase("FORM"))
      {
        Class authClass = Class.forName("com.rickknowles.winstone.auth.FormAuthenticationHandler");
        Constructor authConstr = authClass.getConstructor(new Class[]
            {List.class, String.class, String.class, String.class,
                WinstoneResourceBundle.class, constraints.getClass(), AuthenticationRealm.class});
        return (AuthenticationHandler) authConstr.newInstance(new Object[]
            {roles, realmName, loginPage, errorPage, resources, constraints, realm});
      }
      else
        Logger.log(Logger.WARNING, resources.getString("AuthenticationHandler.UnknownAuthMethod", "[#authMethod]", authMethod));
    }
    catch (Throwable err)
      {Logger.log(Logger.WARNING, resources.getString("AuthenticationHandler.ErrorLoadingManager",
          "[#authMethod]", authMethod), err);}
    return null;
  }

  /**
   * Base class constructor
   */
  protected AuthenticationHandler(AuthenticationRealm realm, SecurityConstraint constraints[],
    WinstoneResourceBundle resources, String realmName)
  {
    this.realm = realm;
    this.constraints = constraints;
    this.resources = resources;
    this.realmName = realmName;
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
    // Give previous attempts a chance to be validated
    if (!validatePossibleAuthenticationResponse(request, response, pathRequested))
      return false;

    // Loop through constraints
    boolean foundApplicable = false;
    for (int n = 0; (n < this.constraints.length) && !foundApplicable; n++)
    {
      // Find one that applies, then
      if (this.constraints[n].isApplicable(pathRequested, request.getProtocol()))
      {
        //Logger.log(Logger.FULL_DEBUG, "Found applicable constraint");
        foundApplicable = true;

        if (this.constraints[n].needsSSL() && !request.isSecure())
        {
          String msg = this.resources.getString("AuthenticationHandler.ConstraintNeedsSSL",
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
        {
          // Logger.log(Logger.FULL_DEBUG, "Allowed - authorization accepted");
          // Ensure that secured resources are not cached
          response.setHeader("Pragma", "No-cache");
          response.setHeader("Cache-Control", "No-cache");
          response.setDateHeader("Expires", 1);
        }
      }
    }
    // If we made it this far without a check being run, there must be none applicable
    //Logger.log(Logger.FULL_DEBUG, "Successfully passed through authentication phase");
    return true;
  }

  /**
   * The actual auth request implementation.
   */
  protected abstract void requestAuthentication(WinstoneRequest request,
  WinstoneResponse response, String pathRequested) 
      throws IOException, ServletException;

  /**
   * Handling the (possible) response
   */
  protected abstract boolean validatePossibleAuthenticationResponse(
    WinstoneRequest request, WinstoneResponse response, String pathRequested)
      throws ServletException, IOException;
      
  /**
   * Useful helper method ... base 64 decoding for strings
   */
  public String decodeBase64String(String input)
  {
    try
    {
      byte inBytes[] = input.getBytes("8859_1");
      byte outBytes[] = new byte[(int) Math.floor((inBytes.length - 1) * 0.75)];
      decodeBase64(inBytes, 0, input.indexOf('='), outBytes, 0);
      return new String(outBytes, "8859_1");
    }
    catch (Throwable err) {return null;}
  }

  /**
   * Decodes a byte array from base64
   */
  public static void decodeBase64(byte input[],  int inOffset,  int inLength,
                                  byte output[], int outOffset)
  {
    if (inLength == 0)
      return;

    // Decode four bytes
    int thisPassInBytes = Math.min(inLength, 4);
    int outBuffer = 0;
    int thisPassOutBytes = 0;
    if (thisPassInBytes == 2)
    {
      outBuffer = ((B64_DECODE_ARRAY[input[inOffset]]     & 0xFF) << 18)
                | ((B64_DECODE_ARRAY[input[inOffset + 1]] & 0xFF) << 12);
      output[outOffset] = (byte) ((outBuffer >> 16) & 0xFF);
      thisPassOutBytes = 1;
    }
    else if (thisPassInBytes == 3)
    {
      outBuffer = ((B64_DECODE_ARRAY[input[inOffset]]     & 0xFF) << 18)
                | ((B64_DECODE_ARRAY[input[inOffset + 1]] & 0xFF) << 12)
                | ((B64_DECODE_ARRAY[input[inOffset + 2]] & 0xFF) << 6);
      output[outOffset]     = (byte) ((outBuffer >> 16) & 0xFF);
      output[outOffset + 1] = (byte) ((outBuffer >> 8)  & 0xFF);
      thisPassOutBytes = 2;
    }
    else
    {
      outBuffer = ((B64_DECODE_ARRAY[input[inOffset]]     & 0xFF) << 18)
                | ((B64_DECODE_ARRAY[input[inOffset + 1]] & 0xFF) << 12)
                | ((B64_DECODE_ARRAY[input[inOffset + 2]] & 0xFF) << 6)
                | ((B64_DECODE_ARRAY[input[inOffset + 3]] & 0xFF));
      output[outOffset]     = (byte) ((outBuffer >> 16) & 0xFF);
      output[outOffset + 1] = (byte) ((outBuffer >> 8)  & 0xFF);
      output[outOffset + 2] = (byte)  (outBuffer  & 0xFF);
      thisPassOutBytes = 3;
    }
    // Recurse
    decodeBase64(input, inOffset + thisPassInBytes, inLength - thisPassInBytes,
                 output, outOffset + thisPassOutBytes);
  }

  private static byte B64_DECODE_ARRAY[] = new byte[]
    {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
     -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
     -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
     62, // Plus sign
     -1,-1,-1,
     63, // Slash
     52,53,54,55,56,57,58,59,60,61, // Numbers
     -1,-1,-1,-1,-1,-1,-1,
     0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25, // Large letters
     -1,-1,-1,-1,-1,-1,
     26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51, // Small letters
     -1,-1,-1,-1};
}

