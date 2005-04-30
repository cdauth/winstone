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
import java.util.Hashtable;
import java.util.Vector;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

import winstone.WinstoneResourceBundle;

/**
 * Enumeration over the set of bindings for this context.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneBindingEnumeration implements NamingEnumeration {
    private Enumeration nameEnumeration;
    private Hashtable bindings;
    private Hashtable contextEnvironment;
    private Context context;
    private WinstoneResourceBundle resources;

    /**
     * Constructor - sets up the enumeration ready for retrieving bindings
     * instead of NameClassPairs.
     * 
     * @param bindings
     *            The source binding set
     */
    public WinstoneBindingEnumeration(Hashtable bindings,
            Hashtable environment, Context context,
            WinstoneResourceBundle resources) {
        this.resources = resources;
        Object keys[] = bindings.keySet().toArray();
        Arrays.sort(keys);
        Vector nameList = new Vector(Arrays.asList(keys));
        this.nameEnumeration = nameList.elements();
        this.bindings = (Hashtable) bindings.clone();
        this.context = context;
        this.contextEnvironment = environment;
    }

    public Object next() throws NamingException {
        if (this.nameEnumeration == null)
            throw new NamingException(this.resources
                    .getString("WinstoneBindingEnumeration.AlreadyClosed"));

        String name = (String) this.nameEnumeration.nextElement();
        Object value = this.bindings.get(name);
        try {
            value = NamingManager.getObjectInstance(value, new CompositeName()
                    .add(name), this.context, this.contextEnvironment);
        } catch (Throwable err) {
            NamingException errNaming = new NamingException(
                    this.resources
                            .getString("WinstoneBindingEnumeration.FailedToGetInstance"));
            errNaming.setRootCause(err);
            throw errNaming;
        }
        return new Binding(name, value);
    }

    public boolean hasMore() throws NamingException {
        if (this.nameEnumeration == null)
            throw new NamingException(this.resources
                    .getString("WinstoneBindingEnumeration.AlreadyClosed"));
        else
            return this.nameEnumeration.hasMoreElements();
    }

    public void close() throws NamingException {
        this.nameEnumeration = null;
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
