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

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * The class loader for the application. This is the one that actually loads
 * servlet classes from the filesystem.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneClassLoader extends URLClassLoader
{
  final static String WEB_INF = "WEB-INF";
  final static String CLASSES = "classes/";
  final static String LIB = "lib";

  private List classPaths;

  public WinstoneClassLoader(String webRoot, ClassLoader cl)
  {
    super(new URL[0], cl);
    this.classPaths = new ArrayList();
    try
    {
      // Web-inf folder
      File webInfFolder = new File(webRoot, WEB_INF);

      // Classes folder
      File classesFolder = new File(webInfFolder, CLASSES);
      if (classesFolder.exists())
      {
        addURL(new URL("file", null, classesFolder.getCanonicalPath() + "/"));
        classPaths.add(classesFolder.getCanonicalPath());
        Logger.log(Logger.DEBUG, "Adding webapp classes folder to classpath");
      }
      else
        Logger.log(Logger.WARNING, "No classes folder found - " + classesFolder.toString());

      // Lib folder's jar files
      File libFolder = new File(webInfFolder, LIB);
      if (libFolder.exists())
      {
        File jars[] = libFolder.listFiles();
        for (int n = 0; n < jars.length; n++)
        {
          String jarName = jars[n].getCanonicalPath().toLowerCase();
          if (jarName.endsWith(".jar") || jarName.endsWith(".zip"))
          {
            addURL(jars[n].toURL());
            classPaths.add(jars[n].getCanonicalPath());
            Logger.log(Logger.DEBUG, "Adding jar file (" + jars[n].getName() + ") to classpath");
          }
        }
      }
      else
        Logger.log(Logger.WARNING, "No lib folder found - " + libFolder.toString());
    }
    catch (MalformedURLException err)
      {throw new WinstoneException("Bad URL in WinstoneClassLoader", err);}
    catch (IOException err)
      {throw new WinstoneException("IOException in WinstoneClassLoader", err);}
  }

  public String getClasspath()
  {
    StringBuffer cp = new StringBuffer();
    for (Iterator i = this.classPaths.iterator(); i.hasNext(); )
    {
      cp.append((String) i.next()).append(';');
    }
    return (cp.length() > 0 ? cp.substring(0, cp.length() - 1) : "");
  }

//  protected Class findClass(String name) throws ClassNotFoundException
//  {
//    Logger.log(Logger.DEBUG, "Loading class " + name);
//    return super.findClass(name);
//  }
}

