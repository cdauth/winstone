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
import java.security.Principal;
import java.util.*;

/**
 * Base class for authentication realms. Subclasses provide the source of
 * authentication roles, usernames, passwords, etc, and when asked for
 * validation respond with a role if valid, or null otherwise.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class AuthenticationRealm
{
  static final String DEFAULT_REALM_CLASS = "com.rickknowles.winstone.realm.ArgumentsRealm";

  protected WinstoneResourceBundle resources;

  /**
   * Factory method for retrieving concrete subclasses of this abstract class.
   * Basically we dynamically load the realm manager based on startup arguments.
   */
  public static AuthenticationRealm getInstance(WinstoneResourceBundle resources,
    Map args)
  {
    // Get the realm class name from the argset
    String realmClassName = args.get("realmClass") == null
                    ? DEFAULT_REALM_CLASS : (String) args.get("realmClass");
    try
    {
      Class realmClass = Class.forName(realmClassName);
      Constructor realmConstr = realmClass.getConstructor(new Class[] {WinstoneResourceBundle.class, Map.class});
      return (AuthenticationRealm) realmConstr.newInstance(new Object[] {resources, args});
    }
    catch (Throwable err)
      {Logger.log(Logger.WARNING, resources.getString("AuthenticationRealm.Error"), err);}
    return null;
  }

  /**
   * Base class constructor. Sets the resources and realm name.
   */
  protected AuthenticationRealm(WinstoneResourceBundle resources, Map args)
  {
    this.resources = resources;
  }

  /**
   * Authenticate the user - do we know them ? Return a distinct id once we know them
   */
  public abstract AuthenticationPrincipal authenticateByUsernamePassword(String userName, String password);
}

