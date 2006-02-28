/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

/**
 * Interface for authentication realms.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public interface AuthenticationRealm {
    /**
     * Authenticate the user - do we know them ? Return a distinct id once we
     * know them
     */
    public AuthenticationPrincipal authenticateByUsernamePassword(
            String userName, String password);

    /**
     * Retrieve an authenticated user
     */
    public AuthenticationPrincipal retrieveUser(String userName);
}
