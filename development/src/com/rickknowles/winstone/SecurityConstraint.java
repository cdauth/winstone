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

import org.w3c.dom.Node;
import java.util.*;

/**
 * Models a restriction on a particular set of resources in the webapp.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class SecurityConstraint
{
  final String ELEM_DISPLAY_NAME          = "display-name";
  final String ELEM_WEB_RESOURCES         = "web-resource-collection";
  final String ELEM_WEB_RESOURCE_NAME     = "web-resource-name";
  final String ELEM_URL_PATTERN           = "url-pattern";
  final String ELEM_HTTP_METHOD           = "http-method";
  final String ELEM_AUTH_CONSTRAINT       = "auth-constraint";
  final String ELEM_ROLE_NAME             = "role-name";
  final String ELEM_USER_DATA_CONSTRAINT  = "user-data-constraint";
  final String ELEM_TRANSPORT_GUARANTEE   = "transport-guarantee";

  final String GUARANTEE_NONE = "NONE";

  private String displayName;
  private String methodSets[];
  private String urlPatterns[];
  private String rolesAllowed[];
  private boolean needsSSL;

  private WinstoneResourceBundle resources;

  /**
   * Constructor
   */
  public SecurityConstraint(Node elm, WinstoneResourceBundle resources, int counter)
  {
    this.resources = resources;
    this.needsSSL = false;
    List localUrlPatternList = new ArrayList();
    List localMethodSetList = new ArrayList();
    List localRolesAllowed = new ArrayList();

    for (int i = 0; i < elm.getChildNodes().getLength(); i++)
    {
      Node child = elm.getChildNodes().item(i);
      if (child.getNodeType() != Node.ELEMENT_NODE)
        continue;
      else if (child.getNodeName().equals(ELEM_DISPLAY_NAME))
        this.displayName = child.getFirstChild().getNodeValue().trim();
      else if (child.getNodeName().equals(ELEM_WEB_RESOURCES))
      {
        String methodSet = "";
        // Parse the element and extract
        for (int k = 0; k < child.getChildNodes().getLength(); k++)
        {
          Node resourceChild = child.getChildNodes().item(k);
          if (resourceChild.getNodeType() != Node.ELEMENT_NODE)
            continue;
          String resourceChildNodeName = resourceChild.getNodeName();
          if (resourceChildNodeName.equals(ELEM_URL_PATTERN))
            localUrlPatternList.add(resourceChild.getFirstChild().getNodeValue().trim());
          else if (resourceChildNodeName.equals(ELEM_HTTP_METHOD))
            methodSet = (methodSet == null ? "." : methodSet) +
                        resourceChild.getFirstChild().getNodeValue().trim() + ".";
        }
        localMethodSetList.add(methodSet == null ? ".ALL." : methodSet);
      }
      else if (child.getNodeName().equals(ELEM_AUTH_CONSTRAINT))
      {
        // Parse the element and extract
        for (int k = 0; k < child.getChildNodes().getLength(); k++)
        {
          Node roleChild = child.getChildNodes().item(k);
          if ((roleChild.getNodeType() == Node.ELEMENT_NODE) &&
               roleChild.getNodeName().equals(ELEM_ROLE_NAME))
            localRolesAllowed.add(roleChild.getFirstChild().getNodeValue().trim());
        }
      }
      else if (child.getNodeName().equals(ELEM_USER_DATA_CONSTRAINT))
      {
        // Parse the element and extract
        for (int k = 0; k < child.getChildNodes().getLength(); k++)
        {
          Node roleChild = child.getChildNodes().item(k);
          if ((roleChild.getNodeType() == Node.ELEMENT_NODE) &&
               roleChild.getNodeName().equals(ELEM_TRANSPORT_GUARANTEE))
            this.needsSSL = !roleChild.getFirstChild().getNodeValue().trim().equalsIgnoreCase(GUARANTEE_NONE);
        }
      }
    }
    this.urlPatterns  = (String []) localUrlPatternList.toArray(new String[localUrlPatternList.size()]);
    this.methodSets   = (String []) localMethodSetList.toArray(new String[localMethodSetList.size()]);
    this.rolesAllowed = (String []) localRolesAllowed.toArray(new String[localRolesAllowed.size()]);

    if (this.displayName == null)
      this.displayName = this.resources.getString("SecurityConstraint.DefaultName", "[#counter]", "" + counter);
  }

  /**
   * Call this to evaluate the security constraint - is this operation allowed ?
   */
  public boolean isAllowed(WinstoneRequest request)
  {
    for (int n = 0; n < this.rolesAllowed.length; n++)
    {
      if (request.isUserInRole(this.rolesAllowed[n]))
      {
        Logger.log(Logger.FULL_DEBUG, "Passed security constraint: " +
                    this.displayName + " role: " + this.rolesAllowed[n]);
        return true;
      }
    }
    Logger.log(Logger.FULL_DEBUG, "Failed security constraint: " + this.displayName);
    return false;
  }

  /**
   * Call this to evaluate the security constraint - is this constraint applicable to this url ?
   */
  public boolean isApplicable(String url, String protocol)
  {
    for (int n = 0; n < this.urlPatterns.length; n++)
      if (WebAppConfiguration.wildcardMatch(this.urlPatterns[n], url) &&
          (this.methodSets[n].equals(".ALL.") || 
          (this.methodSets[n].indexOf("." + protocol.toUpperCase() + ".") != -1)))
        return true;

    return false;
  }

  public boolean needsSSL() {return this.needsSSL;}
  public String getName() {return this.displayName;}
}

