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
package com.rickknowles.winstone.classloader;

import com.rickknowles.winstone.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * This subclass of WinstoneClassLoader is the reloading version. It runs a 
 * monitoring thread in the background that checks for updates to any files in
 * the class path.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ReloadingClassLoader extends WinstoneClassLoader implements Runnable
{
  final int RELOAD_SEARCH_SLEEP = 50;
  private static final String LOCAL_RESOURCE_FILE = "com.rickknowles.winstone.classloader.LocalStrings";

  private boolean interrupted;
  private boolean reloadable;
  private Set loadedClasses;
  private WinstoneResourceBundle localResources;

  public ReloadingClassLoader(WebAppConfiguration webAppConfig, ClassLoader parent,
    WinstoneResourceBundle resources)
  {
    super(webAppConfig, parent, resources);
    this.localResources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    
    // Start the file date changed monitoring thread
    this.interrupted = false;
    this.loadedClasses = new HashSet();
    Thread thread = new Thread(this, localResources.getString("ReloadingClassLoader.ThreadName"));
    thread.setDaemon(true);
    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }

  public void destroy() {this.interrupted = true;}

  /**
   * The maintenance thread. This makes sure that any changes in the files in
   * the classpath trigger a classLoader self destruct and recreate.
   */
  public void run()
  {
    Logger.log(Logger.FULL_DEBUG, localResources.getString("ReloadingClassLoader.MaintenanceThreadStarted"));

    Map classDateTable = new HashMap();
    Map classLocationTable = new HashMap();
    while (!interrupted)
    {
      try
      {
        Thread.sleep(RELOAD_SEARCH_SLEEP);
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
            Logger.log(Logger.WARNING, localResources.getString("ReloadingClassLoader.ClassLost", "[#className]", className));
            continue;
          }

          // Stash date of loaded files, and compare with last iteration
          Long oldClassDate = (Long) classDateTable.get(className);
          if (oldClassDate == null)
            classDateTable.put(className, classDate);
          // Trigger reset of webAppConfig
          else if (oldClassDate.compareTo(classDate) != 0)
          {
            Logger.log(Logger.INFO, localResources.getString("ReloadingClassLoader.ReloadRequired",
              "[#className]", className, "[#date]", "" + new Date(classDate.longValue()),
              "[#oldDate]",  "" + new Date(oldClassDate.longValue())));
            this.webAppConfig.resetClassLoader();
          }
          Thread.sleep(RELOAD_SEARCH_SLEEP);
        }
      }
      catch (Throwable err)
        {Logger.log(Logger.ERROR, localResources.getString("ReloadingClassLoader.MaintenanceThreadError"), err);}
    }
    Logger.log(Logger.FULL_DEBUG, localResources.getString(
          "ReloadingClassLoader.MaintenanceThreadFinished"));
  }

  protected Class findClass(String name) throws ClassNotFoundException
  {
    if (this.reloadable)
      this.loadedClasses.add("Class:" + name);
    return super.findClass(name);
  }

  public URL findResource(String name)
  {
    if (this.reloadable)
      this.loadedClasses.add(name);
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

