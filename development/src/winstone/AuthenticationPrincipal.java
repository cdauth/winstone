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
package winstone;

import java.util.List;
import java.security.Principal;
import java.io.Serializable;

/**
 * Implements the principal method - basically just a way of identifying
 * an authenticated user.
 *
 * @author  <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class AuthenticationPrincipal implements Principal, Serializable
{
  private String userName;
  private String password;
  private List roles;
  private String authenticationType;

  /**
   * Constructor
   */
  public AuthenticationPrincipal(String userName, String password, List roles)
  {
    this.userName = userName;
    this.password = password;
    this.roles = roles;
  }

  public String getName()     {return this.userName;}
  public String getPassword() {return this.password;}
  public String getAuthType() {return this.authenticationType;}

  public void setAuthType(String authType) {this.authenticationType = authType;}

  /**
   * Searches for the requested role in this user's roleset.
   */
  public boolean isUserIsInRole(String role)
  {
    if (this.roles == null)
      return false;
    else if (role == null)
      return false;
    else
      return this.roles.contains(role);
  }
}

 