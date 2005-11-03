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
package winstone.classLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;

/**
 * This subclass of WinstoneClassLoader is the reloading version. It runs a
 * monitoring thread in the background that checks for updates to any files in
 * the class path.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ReloadingClassLoader extends URLClassLoader implements ServletContextListener, Runnable {
    final int RELOAD_SEARCH_SLEEP = 50;
    private static final String LOCAL_RESOURCE_FILE = "winstone.classLoader.LocalStrings";
    private boolean interrupted;
    private WebAppConfiguration webAppConfig;
    private Set loadedClasses;
    private WinstoneResourceBundle resources;
    private File classPaths[];
    
    public ReloadingClassLoader(URL urls[], ClassLoader parent) {
        super(urls, parent);
        this.classPaths = new File[urls.length];
        for (int n = 0; n < urls.length; n++) {
            this.classPaths[n] = new File(urls[n].getFile());
        }
        this.resources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);

        // Start the file date changed monitoring thread
        this.loadedClasses = new HashSet();
    }
    
    public void contextInitialized(ServletContextEvent sce) {
        this.webAppConfig = (WebAppConfiguration) sce.getServletContext();
        this.interrupted = false;
        synchronized (this) {
            this.loadedClasses.clear();
        }
        Thread thread = new Thread(this, resources
                .getString("ReloadingClassLoader.ThreadName"));
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        this.interrupted = true;
        this.webAppConfig = null;
        synchronized (this) {
            this.loadedClasses.clear();
        }
    }

    /**
     * The maintenance thread. This makes sure that any changes in the files in
     * the classpath trigger a classLoader self destruct and recreate.
     */
    public void run() {
        Logger.log(Logger.FULL_DEBUG, resources,
                "ReloadingClassLoader.MaintenanceThreadStarted");

        Map classDateTable = new HashMap();
        Map classLocationTable = new HashMap();
        Set lostClasses = new HashSet();
        while (!interrupted) {
            try {
                Thread.sleep(RELOAD_SEARCH_SLEEP);
                String loadedClassesCopy[] = null;
                synchronized (this) {
                    loadedClassesCopy = (String []) this.loadedClasses.toArray(new String[0]);
                }

                for (int n = 0; (n < loadedClassesCopy.length) && !interrupted; n++) {
                    String className = transformToFileFormat(loadedClassesCopy[n]);
                    File location = (File) classLocationTable.get(className);
                    Long classDate = null;
                    if ((location == null) || !location.exists()) {
                        for (int j = 0; (j < this.classPaths.length) && (classDate == null); j++) {
                            File path = this.classPaths[j];
                            if (!path.exists()) {
                                continue;
                            } else if (path.isDirectory()) {
                                File classLocation = new File(path, className);
                                if (classLocation.exists()) {
                                    classDate = new Long(classLocation.lastModified());
                                    classLocationTable.put(className, classLocation);
                                }
                            } else if (path.isFile()) {
                                classDate = searchJarPath(className, path);
                                if (classDate != null)
                                    classLocationTable.put(className, path);
                            }
                        }
                    } else if (location.exists())
                        classDate = new Long(location.lastModified());

                    // Has class vanished ? Leave a note and skip over it
                    if (classDate == null) {
                        if (!lostClasses.contains(className)) {
                            lostClasses.add(className);
                            Logger.log(Logger.DEBUG, resources,
                                    "ReloadingClassLoader.ClassLost", className);
                        }
                        continue;
                    }
                    if ((classDate != null) && lostClasses.contains(className)) {
                        lostClasses.remove(className);
                    }

                    // Stash date of loaded files, and compare with last
                    // iteration
                    Long oldClassDate = (Long) classDateTable.get(className);
                    if (oldClassDate == null) {
                        classDateTable.put(className, classDate);
                    } else if (oldClassDate.compareTo(classDate) != 0) {
                        // Trigger reset of webAppConfig
                        Logger.log(Logger.INFO, resources, 
                                "ReloadingClassLoader.ReloadRequired",
                                new String[] {className, 
                                        "" + new Date(classDate.longValue()),
                                        "" + new Date(oldClassDate.longValue()) });
                        this.webAppConfig.resetClassLoader();
                    }
                }
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, resources,
                        "ReloadingClassLoader.MaintenanceThreadError", err);
            }
        }
        Logger.log(Logger.FULL_DEBUG, resources,
                "ReloadingClassLoader.MaintenanceThreadFinished");
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        synchronized (this) {
            this.loadedClasses.add("Class:" + name);
        }
        return super.findClass(name);
    }

    public URL findResource(String name) {
        synchronized (this) {
            this.loadedClasses.add(name);
        }
        return super.findResource(name);
    }

    /**
     * Iterates through a jar file searching for a class. If found, it returns that classes date
     */
    private Long searchJarPath(String classResourceName, File path)
            throws IOException, InterruptedException {
        JarFile jar = new JarFile(path);
        for (Enumeration e = jar.entries(); e.hasMoreElements() && !interrupted;) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (entry.getName().equals(classResourceName))
                return new Long(path.lastModified());
        }
        return null;
    }

    private static String transformToFileFormat(String name) {
        if (!name.startsWith("Class:"))
            return name;
        else
            return WinstoneResourceBundle.globalReplace(name.substring(6), ".", "/") + ".class";
    }
}
