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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;


/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneConnection implements Connection
{
  private Connection realConnection;
  private WinstoneDataSource datasource;
  
  /**
   * Constructor - this sets the real connection and the link back to the pool 
   */
  public WinstoneConnection(Connection connection, WinstoneDataSource datasource)
  {
    this.realConnection = connection;
    this.datasource = datasource;
  }

  public void close() throws SQLException
  {
    Connection realConnectionHolder = null;
    if (this.realConnection != null)
    {
      if (!this.realConnection.getAutoCommit())
        this.realConnection.rollback();
      realConnectionHolder = this.realConnection;
      this.realConnection = null;
    }
    if (this.datasource != null)
      this.datasource.releaseConnection(this, realConnectionHolder);
  }
  public boolean isClosed() throws SQLException 
    {return (this.realConnection == null);}

  public void commit() throws SQLException
    {this.realConnection.commit();}
  public void rollback() throws SQLException
    {this.realConnection.rollback();}
  public void rollback(Savepoint sp) throws SQLException
    {this.realConnection.rollback(sp);}
  public boolean getAutoCommit() throws SQLException
    {return this.realConnection.getAutoCommit();}
  public void setAutoCommit(boolean autoCommit) throws SQLException
    {this.realConnection.setAutoCommit(autoCommit);}
  
  public int getHoldability() throws SQLException 
    {return this.realConnection.getHoldability();}
  public void setHoldability(int hold) throws SQLException
    {this.realConnection.setHoldability(hold);}

  public int getTransactionIsolation() throws SQLException
    {return this.realConnection.getTransactionIsolation();}
  public void setTransactionIsolation(int level) throws SQLException
    {this.realConnection.setTransactionIsolation(level);}
    
  public void clearWarnings() throws SQLException
    {this.realConnection.clearWarnings();}
  public SQLWarning getWarnings() throws SQLException
    {return this.realConnection.getWarnings();}

  public boolean isReadOnly() throws SQLException
    {return this.realConnection.isReadOnly();}
  public void setReadOnly(boolean ro) throws SQLException
    {this.realConnection.setReadOnly(ro);}

  public String getCatalog() throws SQLException
    {return this.realConnection.getCatalog();}
  public void setCatalog(String catalog) throws SQLException
    {this.realConnection.setCatalog(catalog);}
  public DatabaseMetaData getMetaData() throws SQLException
    {return this.realConnection.getMetaData();}

  public Savepoint setSavepoint() throws SQLException
    {return this.realConnection.setSavepoint();}
  public Savepoint setSavepoint(String name) throws SQLException
    {return this.realConnection.setSavepoint(name);}
  public void releaseSavepoint(Savepoint sp) throws SQLException
    {this.realConnection.releaseSavepoint(sp);}
    
  public Map getTypeMap() throws SQLException
    {return this.realConnection.getTypeMap();}
  public void setTypeMap(Map map) throws SQLException
    {this.realConnection.setTypeMap(map);}

  public String nativeSQL(String sql) throws SQLException
    {return this.realConnection.nativeSQL(sql);}
    
  public CallableStatement prepareCall(String sql) throws SQLException
    {return this.realConnection.prepareCall(sql);}
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
    {return this.realConnection.prepareCall(sql, resultSetType, resultSetConcurrency);}
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
    throws SQLException
    {return this.realConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);}

  public Statement createStatement() throws SQLException
    {return this.realConnection.createStatement();}
  public Statement createStatement(int resultSetType, int resultSetConcurrency) 
    throws SQLException
    {return this.realConnection.createStatement(resultSetType, resultSetConcurrency);}
  public Statement createStatement(int resultSetType, int resultSetConcurrency, 
    int resultSetHoldability) throws SQLException
    {return this.realConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);}

  public PreparedStatement prepareStatement(String sql) throws SQLException
    {return this.realConnection.prepareStatement(sql);}
  public PreparedStatement prepareStatement(String sql, int autogeneratedKeys) 
    throws SQLException
    {return this.realConnection.prepareStatement(sql, autogeneratedKeys);}
  public PreparedStatement prepareStatement(String sql, int resultSetType, 
    int resultSetConcurrency) throws SQLException
    {return this.realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);}
  public PreparedStatement prepareStatement(String sql, int resultSetType, 
    int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {return this.realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);}
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) 
    throws SQLException
    {return this.realConnection.prepareStatement(sql, columnIndexes);}
  public PreparedStatement prepareStatement(String sql, String[] columnNames) 
    throws SQLException
    {return this.realConnection.prepareStatement(sql, columnNames);}
}