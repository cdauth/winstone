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
import javax.servlet.http.*;
import java.io.IOException;
import org.w3c.dom.*;

import winstone.*;

/**
 * Handles HTTP basic authentication.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class BasicAuthenticationHandler extends BaseAuthenticationHandler
{
  public BasicAuthenticationHandler(Node loginConfigNode, List constraintNodes, 
    Set rolesAllowed, WinstoneResourceBundle resources, AuthenticationRealm realm)
  {
    super(loginConfigNode, constraintNodes, rolesAllowed, resources, realm);
    Logger.log(Logger.DEBUG, this.resources.getString("BasicAuthenticationHandler.Initialised",
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
  protected boolean validatePossibleAuthenticationResponse(HttpServletRequest request,
    HttpServletResponse response, String pathRequested) throws IOException
  {
    String authorization = request.getHeader("Authorization");
    if ((authorization != null) && authorization.toLowerCase().startsWith("basic"))
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
          if (request instanceof WinstoneRequest)
            ((WinstoneRequest) request).setRemoteUser(principal);
          else if (request instanceof HttpServletRequestWrapper)
          {
            HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
            if (wrapper.getRequest() instanceof WinstoneRequest)
              ((WinstoneRequest) wrapper.getRequest()).setRemoteUser(principal);
            else
              Logger.log(Logger.WARNING, this.resources.getString("BasicAuthenticationHandler.CantSetUser", "[#class]", wrapper.getRequest().getClass().getName()));
          }
          else
            Logger.log(Logger.WARNING, this.resources.getString("BasicAuthenticationHandler.CantSetUser", "[#class]", request.getClass().getName()));
        }
      }
    }
    return true;
  }
      
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

