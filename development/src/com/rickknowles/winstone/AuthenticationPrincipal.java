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

import java.util.Arrays;

/**
 * Implements the principal method - basically just a way of identifying
 * an authenticated user.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class AuthenticationPrincipal implements java.security.Principal
{
  private String userName;
  private String password;
  private String roles[];
  private String authenticationType;

  /**
   * Constructor
   */
  public AuthenticationPrincipal(String userName, String password, String roles[])
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
      return (Arrays.binarySearch(this.roles, role) >= 0);
  }
}

 