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
package winstone.realm;

import java.util.*;

import winstone.*;

/**
 * Base class for authentication realms. Subclasses provide the source of
 * authentication roles, usernames, passwords, etc, and when asked for
 * validation respond with a role if valid, or null otherwise.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ArgumentsRealm implements AuthenticationRealm
{
  static final String PASSWORD_PREFIX = "argumentsRealm.passwd.";
  static final String ROLES_PREFIX    = "argumentsRealm.roles.";

  static final String LOCAL_RESOURCES = "winstone.realm.LocalStrings";

  private Map passwords;
  private Map roles;
  private WinstoneResourceBundle resources;

  /**
   * Constructor - this sets up an authentication realm, using the arguments supplied
   * on the command line as a source of userNames/passwords/roles.
   */
  public ArgumentsRealm(WinstoneResourceBundle resources, Map args)
  {
    this.resources = new WinstoneResourceBundle(LOCAL_RESOURCES); //resources;
    this.passwords = new Hashtable();
    this.roles = new Hashtable();
    
    for (Iterator i = args.keySet().iterator(); i.hasNext(); )
    {
      String key = (String) i.next();
      if (key.startsWith(PASSWORD_PREFIX))
      {
        String userName = key.substring(PASSWORD_PREFIX.length());
        String password = (String) args.get(key);
        this.passwords.put(userName, password);

        String roleList = (String) args.get(ROLES_PREFIX + userName);
        StringTokenizer st = new StringTokenizer(roleList, ",");
        String roleArray[] = new String[st.countTokens()];
        for (int n = 0; n < roleArray.length; n++)
          roleArray[n] = st.nextToken();
        Arrays.sort(roleArray);
        this.roles.put(userName, Arrays.asList(roleArray));
      }
    }

    Logger.log(Logger.DEBUG, this.resources.getString("ArgumentsRealm.Initialised",
      "[#userCount]", "" + this.passwords.size()));
  }

  /**
   * Authenticate the user - do we know them ? Return a principal once we know them
   */
  public AuthenticationPrincipal authenticateByUsernamePassword(String userName, String password)
  {
    if ((userName == null) || (password == null))
      return null;
      
    String realPassword = (String) this.passwords.get(userName);
    if (realPassword == null)
      return null;
    else if (!realPassword.equals(password))
      return null;
    else
      return new AuthenticationPrincipal(userName, password,
                                          (List) this.roles.get(userName));
  }

  /**
   * Retrieve an authenticated user
   */
  public AuthenticationPrincipal retrieveUser(String userName)
  {
    if (userName == null)
      return null;
    else
      return new AuthenticationPrincipal(userName, (String) this.passwords.get(userName), (List) this.roles.get(userName));
  }
}