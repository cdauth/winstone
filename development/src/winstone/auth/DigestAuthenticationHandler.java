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

import org.w3c.dom.Node;

import winstone.*;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implements the MD5 digest version of authentication
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class DigestAuthenticationHandler extends BaseAuthenticationHandler
{
  private MessageDigest md5Digester;

  public DigestAuthenticationHandler(Node loginConfigNode, List constraintNodes, 
    WinstoneResourceBundle resources, AuthenticationRealm realm)
      throws NoSuchAlgorithmException
  {
    super(loginConfigNode, constraintNodes, resources, realm);
    this.md5Digester = MessageDigest.getInstance("MD5");
    Logger.log(Logger.DEBUG, this.resources.getString("DigestAuthenticationHandler.Initialised",
      "[#name]", realmName));
  }

  /**
   * Call this once we know that we need to authenticate
   */
  protected void requestAuthentication(HttpServletRequest request,
      HttpServletResponse response, String pathRequested) throws IOException
  {
    // Generate the one time token
    String oneTimeToken = "WinstoneToken:" + (new Random().nextDouble() * System.currentTimeMillis());
    
    // Need to write the www-authenticate header 
    String authHeader = "Digest realm=\"" + this.realmName + "\", qop=\"auth\", " + 
        "nonce=\"" + oneTimeToken + "\", opaque=\"" + md5Encode(oneTimeToken) + "\"";
    response.setHeader("WWW-Authenticate", authHeader);
    
    // Return unauthorized
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
      this.resources.getString("DigestAuthenticationHandler.UnauthorizedMessage"));
  }

  /**
   * Handling the (possible) response
   * @return True if the request should continue, or false if we have intercepted it
   */
  protected boolean validatePossibleAuthenticationResponse(HttpServletRequest request,
    HttpServletResponse response, String pathRequested) throws IOException
  {
    String authorization = request.getHeader("Authorization");
    if (authorization == null)
      return true;

    //Logger.log(Logger.FULL_DEBUG, "Authorization: " + authorization);
    if (!authorization.startsWith("Digest"))
      return true;

    // Extract tokens from auth string
    String userName = null;
    String realm = null;
    String qop = null;
    String algorithm = null;
    String uri = null;
    String nOnce = null;
    String nc = null;
    String cnOnce = null;
    String clientResponseDigest = null;
    
    StringTokenizer st = new StringTokenizer(authorization.substring(6).trim(), ",");
    while (st.hasMoreTokens())
    {
      String token = st.nextToken().trim();
      int equalPos = token.indexOf('=');
      String paramName = token.substring(0, equalPos);
      if (paramName.equals("username"))
        userName = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("realm"))
        realm = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("qop"))
        qop = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("algorithm"))
        algorithm = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("uri"))
        uri = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("nonce"))
        nOnce = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("nc"))
        nc = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("cnonce"))
        cnOnce = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
      else if (paramName.equals("response"))
        clientResponseDigest = WinstoneResourceBundle.globalReplace(token.substring(equalPos + 1).trim(), "\"", "");
    }
    
    // Throw out bad attempts
    if ((userName == null) || (realm == null) || (qop == null) || 
        (uri == null) || (nOnce == null) || (nc == null) || (cnOnce == null) || 
        (clientResponseDigest == null))
      return true;
    else if ((algorithm != null) && !algorithm.equals("MD5"))
      return true;
    
    // Get a user matching the username
    AuthenticationPrincipal principal = this.realm.retrieveUser(userName);
    if (principal == null)
      return true;
        
    // Compute the 2 digests and compare
    String userRealmPasswordDigest = md5Encode(userName + ":" + realm + ":" + principal.getPassword());
    String methodURIDigest = md5Encode(request.getMethod() + ":" + uri);
    String serverResponseDigest = md5Encode(userRealmPasswordDigest + ":" + nOnce + ":" +
                                            nc + ":" + cnOnce + ":" + qop + ":" +
                                            methodURIDigest);
    if (serverResponseDigest.equals(clientResponseDigest))
    {
      principal.setAuthType(HttpServletRequest.DIGEST_AUTH);
      if (request instanceof WinstoneRequest)
        ((WinstoneRequest) request).setRemoteUser(principal);
      else if (request instanceof HttpServletRequestWrapper)
      {
        HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
        if (wrapper.getRequest() instanceof WinstoneRequest)
          ((WinstoneRequest) wrapper.getRequest()).setRemoteUser(principal);
        else
          Logger.log(Logger.WARNING, this.resources.getString("DigestAuthenticationHandler.CantSetUser", "[#class]", wrapper.getRequest().getClass().getName()));
      }
      else
        Logger.log(Logger.WARNING, this.resources.getString("DigestAuthenticationHandler.CantSetUser", "[#class]", request.getClass().getName()));
    }
    return true;
  }
  
  /**
   * Returns a hex encoded MD5 digested version of the input string
   * @param input The string to encode
   * @return MD5 digested, hex encoded version of the input
   */
  public String md5Encode(String input) throws UnsupportedEncodingException
  {
    // Digest 
    byte digestBytes[] = this.md5Digester.digest(input.getBytes("8859_1"));
    
    // Write out in hex format
    char outArray[] = new char[32];
    for (int n = 0; n < digestBytes.length; n++)
    {
      int hiNibble = (digestBytes[n] & 0xFF) >> 4;
      int loNibble = (digestBytes[n] & 0xF);
      outArray[2*n]     = (hiNibble > 9 ? (char) (hiNibble + 87) : (char) (hiNibble + 48));
      outArray[2*n + 1] = (loNibble > 9 ? (char) (loNibble + 87) : (char) (loNibble + 48));
    }
    return new String(outArray);
  }
}
