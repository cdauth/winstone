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
import java.util.jar.*;

/**
 * The class loader for the application. This is the one that actually loads
 * servlet classes from the filesystem.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneClassLoader extends URLClassLoader implements Runnable
{
  final String WEB_INF = "WEB-INF";
  final String CLASSES = "classes/";
  final String LIB = "lib";
  
  final int RELOAD_SEARCH_SLEEP = 50;

  private List classPaths;
  private WinstoneResourceBundle resources;
  private WebAppConfiguration webAppConfig;
  private boolean interrupted;
  private boolean reloadable;
  private Set loadedClasses;

  public WinstoneClassLoader(WebAppConfiguration webAppConfig, ClassLoader cl,
    WinstoneResourceBundle resources, boolean reloadable)
  {
    super(new URL[0], cl);
    this.resources = resources;
    this.classPaths = new ArrayList();
    this.webAppConfig = webAppConfig;
    try
    {
      // Web-inf folder
      File webInfFolder = new File(webAppConfig.getWebroot(), WEB_INF);

      // Classes folder
      File classesFolder = new File(webInfFolder, CLASSES);
      if (classesFolder.exists())
      {
        addURL(new URL("file", null, classesFolder.getCanonicalPath() + "/"));
        classPaths.add(classesFolder);
        Logger.log(Logger.DEBUG, resources.getString("WinstoneClassLoader.WebAppClasses"));
      }
      else
        Logger.log(Logger.WARNING, resources.getString("WinstoneClassLoader.NoWebAppClasses") + " - " + classesFolder.toString());

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
            classPaths.add(jars[n]);
            Logger.log(Logger.DEBUG, resources.getString("WinstoneClassLoader.WebAppLib",
              "[#name]", jars[n].getName()));
          }
        }
      }
      else
        Logger.log(Logger.WARNING, resources.getString("WinstoneClassLoader.NoWebAppLib")
                  + " - " + libFolder.toString());
    }
    catch (MalformedURLException err)
      {throw new WinstoneException(resources.getString("WinstoneClassLoader.BadURL"), err);}
    catch (IOException err)
      {throw new WinstoneException(resources.getString("WinstoneClassLoader.IOException"), err);}

    // Start the file date changed monitoring thread
    this.reloadable = reloadable;
    if (reloadable)
    {
      this.interrupted = false;
      this.loadedClasses = new HashSet();
      Thread thread = new Thread(this, this.resources.getString("WinstoneClassLoader.ThreadName"));
      thread.setDaemon(true);
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  /**
   * Build a classpath string based on the elements in this classloader. This
   * is so that we can pass the classpath to Jasper.
   */
  public String getClasspath()
  {
    try
    {
      StringBuffer cp = new StringBuffer();
      for (Iterator i = this.classPaths.iterator(); i.hasNext(); )
        cp.append(((File) i.next()).getCanonicalPath()).append(';');
      return (cp.length() > 0 ? cp.substring(0, cp.length() - 1) : "");
    }
    catch (IOException err)
    {
      throw new WinstoneException(this.resources.getString("WinstoneClassLoader.ErrorBuildingClassPath"));
    }
  }

  public void destroy() {this.interrupted = true;}

  /**
   * The maintenance thread. This makes sure that any changes in the files in
   * the classpath trigger a classLoader self destruct and recreate.
   */
  public void run()
  {
    Logger.log(Logger.FULL_DEBUG, this.resources.getString("WinstoneClassLoader.MaintenanceThreadStarted"));

    Map classDateTable = new HashMap();
    Map classLocationTable = new HashMap();
    while (!interrupted)
    {
      try
      {
        Thread.currentThread().sleep(RELOAD_SEARCH_SLEEP);
        Set loadedClassesCopy = new HashSet(this.loadedClasses);

        for (Iterator i = loadedClassesCopy.iterator(); i.hasNext() && !interrupted; )
        {
          String className = transformToFileFormat((String) i.next());
          File location = (File) classLocationTable.get(className);
          Long classDate = null;
          if ((location == null) || !location.exists())
          {
            for (Iterator j = this.classPaths.iterator(); j.hasNext() && (classDate == null); )
            {
              File path = (File) j.next();
              if (!path.exists())
                continue;
              else if (path.isDirectory())
              {
                File classLocation = new File(path, className);
                if (classLocation.exists())
                {
                  classDate = new Long(classLocation.lastModified());
                  classLocationTable.put(className, classLocation);
                }
              }
              else if (path.isFile())
              {
                classDate = searchJarPath(className, path);
                if (classDate != null)
                  classLocationTable.put(className, path);
              }
            }
          }
          else if (location.exists())
            classDate = new Long(location.lastModified());

          // Has class vanished ? Leave a note and skip over it
          if (classDate == null)
          {
            Logger.log(Logger.WARNING, this.resources.getString("WinstoneClassLoader.ClassLost", "[#className]", className));
            continue;
          }

          // Stash date of loaded files, and compare with last iteration
          Long oldClassDate = (Long) classDateTable.get(className);
          if (oldClassDate == null)
            classDateTable.put(className, classDate);
          // Trigger reset of webAppConfig
          else if (oldClassDate.compareTo(classDate) != 0)
          {
            Logger.log(Logger.INFO, this.resources.getString("WinstoneClassLoader.ReloadRequired",
              "[#className]", className, "[#date]", "" + new Date(classDate.longValue()),
              "[#oldDate]",  "" + new Date(oldClassDate.longValue())));
            this.webAppConfig.resetClassLoader();
          }
          Thread.currentThread().sleep(RELOAD_SEARCH_SLEEP);
        }
      }
      catch (Throwable err)
        {Logger.log(Logger.ERROR, this.resources.getString("WinstoneClassLoader.MaintenanceThreadError"), err);}
    }
    Logger.log(Logger.FULL_DEBUG, this.resources.getString(
          "WinstoneClassLoader.MaintenanceThreadFinished"));
  }

  protected Class findClass(String name) throws ClassNotFoundException
  {
    if (this.reloadable)
      this.loadedClasses.add("Class:" + name);
    //Logger.log(Logger.FULL_DEBUG, "Loading class " + name);
    return super.findClass(name);
  }

  public URL findResource(String name)
  {
    if (this.reloadable)
      this.loadedClasses.add(name);
    //Logger.log(Logger.FULL_DEBUG, "Loading resource " + name);
    return super.findResource(name);
  }

  /**
   * Iterates through a jar file searching for a class. If found, it returns that classes date
   */
  private Long searchJarPath(String classResourceName, File path) throws IOException, InterruptedException
  {
    JarFile jar = new JarFile(path);
    for (Enumeration enum = jar.entries(); enum.hasMoreElements() && !interrupted; )
    {
      JarEntry entry = (JarEntry) enum.nextElement();
      if (entry.getName().equals(classResourceName))
        return new Long(path.lastModified());
    }
    return null;
  }

  private String transformToFileFormat(String name)
  {
    if (!name.startsWith("Class:"))
      return name;
    else
      return WinstoneResourceBundle.globalReplace(name.substring(6), ".", "/") + ".class";
  }
}

