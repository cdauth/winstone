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
package winstone.jndi;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import winstone.WinstoneResourceBundle;

/**
 * Enumeration across the names/classes of the bindings in a particular context.
 * Used by the list() method.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneNameEnumeration implements NamingEnumeration {
    private Enumeration nameEnumeration;
    private WinstoneResourceBundle resources;
    
    /**
     * Constructor
     */
    public WinstoneNameEnumeration(Map bindings,
            WinstoneResourceBundle resources) {
        this.resources = resources;
        Object keys[] = bindings.keySet().toArray();
        Arrays.sort(keys);
        Vector nameClassPairs = new Vector();
        for (int n = 0; n < keys.length; n++)
            nameClassPairs.add(new NameClassPair((String) keys[n], bindings
                    .get(keys[n]).getClass().getName()));
        this.nameEnumeration = nameClassPairs.elements();
    }

    public void close() throws NamingException {
        this.nameEnumeration = null;
    }

    public boolean hasMore() throws NamingException {
        if (this.nameEnumeration == null)
            throw new NamingException(this.resources
                    .getString("WinstoneNameEnumeration.AlreadyClosed"));
        else
            return this.nameEnumeration.hasMoreElements();
    }

    public Object next() throws NamingException {
        if (hasMore())
            return this.nameEnumeration.nextElement();
        else
            return null;
    }

    public boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (NamingException err) {
            return false;
        }
    }

    public Object nextElement() {
        try {
            return next();
        } catch (NamingException err) {
            return null;
        }
    }

}
