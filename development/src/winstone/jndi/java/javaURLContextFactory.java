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
package winstone.jndi.java;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import winstone.jndi.WinstoneContext;


/**
 * Creates the initial instance of the Winstone JNDI context 
 * (corresponds to java:/ urls)
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class javaURLContextFactory implements InitialContextFactory, ObjectFactory
{
  private static Context rootContext;
  
  public Context getInitialContext(Hashtable env) throws NamingException
  {
    if (rootContext == null)
      rootContext = new WinstoneContext(env, null, "java:/comp/env", new Boolean(true));
    return (Context) rootContext.lookup("");
  }
    
  public Object getObjectInstance(Object object, Name name, Context context, Hashtable env)
    {return null;}
}
