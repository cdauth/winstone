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
package winstone.jndi.resourceFactories;

import java.io.*;
import java.sql.*;
import javax.sql.DataSource;
import java.util.*;

import winstone.*;

/**
 * Implements the Winstone connection pooling data source
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneDataSource implements DataSource, Runnable
{
  final static int SLEEP_PERIOD = 2000;

  private static final String LOCAL_RESOURCE_FILE = "winstone.jndi.resourceFactories.LocalStrings";
  
  private Thread thread;
  private String name;
  private List usedWrappers;
  private List usedConnections;
  private List unusedConnections;
  private Object semaphore = new Boolean(true);
  private boolean interrupted;
  private int loginTimeout;
  private PrintWriter logWriter;
  
  private String url;
  private Driver driver;
  private Properties connectProps;
  
  private WinstoneResourceBundle resources;
     
  private int MAX_CONNECTIONS = 20;
  private int MAX_IDLE_CONNECTIONS = 10;
  private int START_CONNECTIONS = 2;
  
  /**
   * Build a fresh instance
   */
  public WinstoneDataSource(String name, Map args, ClassLoader loader,
    WinstoneResourceBundle resources) throws SQLException, ClassNotFoundException
  {
    this.name = name;
    this.resources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    this.interrupted = false;
    this.loginTimeout = 0;
    this.usedWrappers = new ArrayList();
    this.usedConnections = new ArrayList();
    this.unusedConnections = new ArrayList();
    this.connectProps = new Properties();

    // Extract the connection properties from the map
    this.url = (String) args.get("url");
    String driverClassName = (String) args.get("driverClassName");
    if (args.get("username") != null)
      this.connectProps.put("user", args.get("username"));
    if (args.get("password") != null)
      this.connectProps.put("password", args.get("password"));
    
    if (args.get("maxConnections") != null)
      MAX_CONNECTIONS = Integer.parseInt((String) args.get("maxConnections"));
    if (args.get("maxIdle") != null)
      MAX_IDLE_CONNECTIONS = Integer.parseInt((String) args.get("maxIdle"));
    if (args.get("startConnections") != null)
      START_CONNECTIONS = Integer.parseInt((String) args.get("startConnections"));

    if (this.url == null)
      throw new SQLException(this.resources.getString("WinstoneDataSource.NoUrlSupplied"));
    else if (driverClassName == null)
      throw new SQLException(this.resources.getString("WinstoneDataSource.NoDriverSupplied"));
    else try
    {
      Class driverClass = Class.forName(driverClassName, true, loader);
      this.driver = (Driver) driverClass.newInstance();
      
      // Get a test connection, and exit if it fails
      Connection realConnection = this.driver.connect(this.url, this.connectProps);
      this.unusedConnections.add(realConnection);

      // Add missing idle connections
      while ((this.unusedConnections.size() < START_CONNECTIONS) &&
             (this.usedConnections.size() + this.unusedConnections.size() < MAX_CONNECTIONS))
      {
        Connection conn = this.driver.connect(this.url, this.connectProps);
        if (conn == null)
          throw new WinstoneException(this.resources.getString("WinstoneDataSource.DriverConnectNull"));
        this.unusedConnections.add(conn);
        Logger.log(Logger.FULL_DEBUG, this.resources, "WinstoneDataSource.AddingPooledConnection",
          new String[] {"" + this.usedConnections.size(),
                        "" + this.unusedConnections.size()});
      }
    
      thread = new Thread(this, this.resources.getString("WinstoneDataSource.ThreadName", name));
      thread.setDaemon(true);
      thread.setContextClassLoader(loader);
      thread.start();
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, this.resources, "WinstoneDataSource.ErrorLoadingDriver", driverClassName, err);}    
  }
  
  public void destroy() 
  {
    this.interrupted = true;
    if (this.thread != null)
      this.thread.interrupt();
  }
  
  /**
   * Pool management thread
   */
  public void run()
  {
    Logger.log(Logger.FULL_DEBUG, this.resources, "WinstoneDataSource.MaintenanceStarted");

    while (!interrupted)
    {
      try
      {
        synchronized (this.semaphore)
        {        
          // Trim excessive idle connections
          while (this.unusedConnections.size() > MAX_IDLE_CONNECTIONS)
          {
            Connection closeMe = (Connection) this.unusedConnections.get(0);
            this.unusedConnections.remove(closeMe);
            closeMe.close();
            Logger.log(Logger.FULL_DEBUG, resources, "WinstoneDataSource.ClosingPooledConnection",
              new String[] {"" + this.usedConnections.size(),
                            "" + this.unusedConnections.size()});
          }
          
          // Iterate through the list of used wrappers, and release any that
          // have been held for too long ? Maybe later
        }
        
        Thread.sleep(SLEEP_PERIOD);
      }
      catch (InterruptedException err) 
        {Logger.log(Logger.DEBUG, this.resources, "WinstoneDataSource.MaintenanceThread");}
      catch (Throwable err)
        {Logger.log(Logger.ERROR, this.resources, "WinstoneDataSource.MaintenanceError", err);}
    }
    Logger.log(Logger.FULL_DEBUG, this.resources, "WinstoneDataSource.MaintenanceFinished");
  }
  
  /**
   * Releases a wrapper from the used pool, and adds the real connection back to the
   * unused pool.
   * 
   * @param wrapper Connection wrapper we are finished with
   * @param realConnection JDBC Connection that we want to return to the pool
   */
  void releaseConnection(WinstoneConnection wrapper, Connection realConnection)
    throws SQLException
  {
    synchronized (this.semaphore)
    {
      // Remove the wrapper from the used list
      if (wrapper != null)
        this.usedWrappers.remove(wrapper);
        // Log destroying wrapper
      
      // Put the real connection back in the pool 
      if (realConnection != null)
      {
        if (this.usedConnections.contains(realConnection))
        {
          this.unusedConnections.add(realConnection);
          this.usedConnections.remove(realConnection);
          Logger.log(Logger.FULL_DEBUG, resources, "WinstoneDataSource.ReleasingPooledConnection",
            new String[] {"" + this.usedConnections.size(),
                          "" + this.unusedConnections.size()});
        }
        else
        {  
          realConnection.close(); 
          Logger.log(Logger.FULL_DEBUG, resources, "WinstoneDataSource.ClosingUnpooledConnection");
        }
      }
    }
  }
  
  public int getLoginTimeout() {return this.loginTimeout;}
  public PrintWriter getLogWriter() {return this.logWriter;}

  public void setLoginTimeout(int timeout) {this.loginTimeout = timeout;}    
  public void setLogWriter(PrintWriter writer) {this.logWriter = writer;}
  
  /**
   * Gets a connection from the pool
   */
  public Connection getConnection() throws SQLException
  {
    WinstoneConnection wrapper = null;
    synchronized (this.semaphore)
    {
      Connection realConnection = null;
      // If we have any spare, get it from the pool
      if (this.unusedConnections.size() > 0)
      {
        realConnection = (Connection) this.unusedConnections.get(0);
        this.unusedConnections.remove(realConnection);
        this.usedConnections.add(realConnection);
        Logger.log(Logger.FULL_DEBUG, resources, "WinstoneDataSource.UsingPooledConnection",
          new String[] {"" + this.usedConnections.size(),
                        "" + this.unusedConnections.size()});
        // Log using pooled connection
      }

      // If we are out (and not over our limit), allocate a new one
      else if (this.usedConnections.size() < MAX_CONNECTIONS)
      {
        realConnection = this.driver.connect(this.url, this.connectProps);
        this.usedConnections.add(realConnection);
        Logger.log(Logger.FULL_DEBUG, resources, "WinstoneDataSource.AddingPooledConnection",
          new String[] {"" + this.usedConnections.size(),
                        "" + this.unusedConnections.size()});
        // Log using new connection
      }

      // otherwise throw fail message - we've blown our limit
      else
        throw new SQLException(this.resources.getString("WinstoneDataSource.PoolLimitExceeded", "" + MAX_CONNECTIONS));

      wrapper = new WinstoneConnection(realConnection, this, this.resources);
      this.usedWrappers.add(wrapper);
    }
    return wrapper;    
  }
  
  /**
   * Gets a connection with a specific username/password. These are not pooled.
   */
  public Connection getConnection(String username, String password) throws SQLException
  {
    Properties newProps = new Properties();
    newProps.put("user", username);
    newProps.put("password", password);
    Connection conn = this.driver.connect(this.url, newProps);
    WinstoneConnection wrapper = null;
    synchronized (this.semaphore)
    {
      wrapper = new WinstoneConnection(conn, this, this.resources);
      this.usedWrappers.add(wrapper);
    }    
    return wrapper;
  }
}
