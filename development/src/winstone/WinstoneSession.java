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
package winstone;

import java.io.*;
import java.util.*;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.ServletContext;

/**
 * Http session implementation for Winstone.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneSession implements HttpSession, Serializable
{
  public static final String SESSION_COOKIE_NAME = "JSESSIONID";

  private String sessionId;
  private WebAppConfiguration webAppConfig;
  private Map sessionData;
  private long createTime;
  private long lastAccessedTime;
  private int maxInactivePeriod;
  private boolean isNew;
  private HttpSessionAttributeListener sessionAttributeListeners[];
  private HttpSessionListener sessionListeners[];
  private HttpSessionActivationListener sessionActivationListeners[];
  private boolean distributable;
  private WinstoneResourceBundle resources;
  private AuthenticationPrincipal authenticatedUser;
  private WinstoneRequest cachedRequest;
  
  private Object sessionMonitor = new Boolean(true);
    
  /**
   * Constructor
   */
  public WinstoneSession(String sessionId,
                         WebAppConfiguration webAppConfig,
                         boolean distributable)
  {
    this.sessionId = sessionId;
    this.webAppConfig = webAppConfig;
    this.sessionData = new Hashtable();
    this.createTime = System.currentTimeMillis();
    this.isNew = true;
    this.distributable = distributable;
  }

  public void sendCreatedNotifies()
  {
    // Notify session listeners of new session
    for (int n = 0; n < this.sessionListeners.length; n++)
      this.sessionListeners[n].sessionCreated(new HttpSessionEvent(this));
  }

  public void setSessionActivationListeners(HttpSessionActivationListener listeners[]) {this.sessionActivationListeners = listeners;}
  public void setSessionAttributeListeners(HttpSessionAttributeListener listeners[]) {this.sessionAttributeListeners = listeners;}
  public void setSessionListeners(HttpSessionListener listeners[]) {this.sessionListeners = listeners;}
  public void setResources(WinstoneResourceBundle resources) {this.resources = resources;}
  
  public void setLastAccessedDate(long time) {this.lastAccessedTime = time;}
  public void setIsNew(boolean isNew)        {this.isNew = isNew;}

  // Implementation methods
  public Object getAttribute(String name) 
  {
    Object att = null;
    synchronized (this.sessionMonitor)
      {att = this.sessionData.get(name);}
    return att;
  }
  
  public Enumeration getAttributeNames()  
  {
    Enumeration names = null;
    synchronized (this.sessionMonitor)
      {names = Collections.enumeration(this.sessionData.keySet());}
    return names;
  }
  
  public void setAttribute(String name, Object value)
  {
    // Check for serializability if distributable
    if (this.distributable &&
        (value != null) &&
        !(value instanceof java.io.Serializable))
      throw new IllegalArgumentException(this.resources.getString("WinstoneSession.AttributeNotSerializable",
          new String[] {name, value.getClass().getName()}));

    // valueBound must be before binding
    if (value instanceof HttpSessionBindingListener)
    {
      HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
      hsbl.valueBound(new HttpSessionBindingEvent(this, name, value));
    }

    Object oldValue = null;
    synchronized (this.sessionMonitor)
    {
      oldValue = this.sessionData.get(name);
      this.sessionData.put(name, value);
    }

    // valueUnbound must be after unbinding
    if (oldValue instanceof HttpSessionBindingListener)
    {
      HttpSessionBindingListener hsbl = (HttpSessionBindingListener) oldValue;
      hsbl.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
    }
    
    // Notify other listeners
    if (oldValue != null)
      for (int n = 0; n < this.sessionAttributeListeners.length; n++)
        this.sessionAttributeListeners[n].attributeReplaced(new HttpSessionBindingEvent(this, name, oldValue));
    else
      for (int n = 0; n < this.sessionAttributeListeners.length; n++)
        this.sessionAttributeListeners[n].attributeAdded(new HttpSessionBindingEvent(this, name, value));
  }
  
  public void removeAttribute(String name)
  {
    Object value = null;
    synchronized (this.sessionMonitor)
    { 
      value = this.sessionData.get(name);
      this.sessionData.remove(name);
    }
    
    // Notify listeners
    if (value instanceof HttpSessionBindingListener)
    {
      HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
      hsbl.valueUnbound(new HttpSessionBindingEvent(this, name));
    }
    if (value != null)
      for (int n = 0; n < this.sessionAttributeListeners.length; n++)
        this.sessionAttributeListeners[n].attributeRemoved(new HttpSessionBindingEvent(this, name, value));
  }

  public long getCreationTime()     {return this.createTime;}
  public long getLastAccessedTime() {return this.lastAccessedTime;}
  public String getId()             {return this.sessionId;}

  public WinstoneRequest getCachedRequest() {return this.cachedRequest;}
  public void setCachedRequest(WinstoneRequest req) {this.cachedRequest = req;}
  public AuthenticationPrincipal getAuthenticatedUser() {return this.authenticatedUser;}
  public void setAuthenticatedUser(AuthenticationPrincipal user) {this.authenticatedUser = user;}

  public int  getMaxInactiveInterval()                {return this.maxInactivePeriod;}
  public void setMaxInactiveInterval(int interval)    {this.maxInactivePeriod = interval;}
  public boolean isNew()                              {return this.isNew;}
  public ServletContext getServletContext()           {return this.webAppConfig;}

  public void invalidate()
  {
    // Notify session listeners of invalidated session -- backwards
    for (int n = this.sessionListeners.length - 1; n >= 0; n--)
      this.sessionListeners[n].sessionDestroyed(new HttpSessionEvent(this));

    List keys = new ArrayList(this.sessionData.keySet());
    for (Iterator i = keys.iterator(); i.hasNext(); )
      removeAttribute((String) i.next());
    synchronized (this.sessionMonitor)
      {this.sessionData.clear();}
    this.webAppConfig.removeSessionById(this.sessionId);
  }

  /**
   * Called after the session has been serialized to another server.
   */
  public void passivate()
  {
    // Notify session listeners of invalidated session
    for (int n = 0; n < this.sessionActivationListeners.length; n++)
      this.sessionActivationListeners[n].sessionWillPassivate(new HttpSessionEvent(this));

    // Question: Is passivation equivalent to invalidation ? Should all entries be removed ?
    //List keys = new ArrayList(this.sessionData.keySet());
    //for (Iterator i = keys.iterator(); i.hasNext(); )
    //  removeAttribute((String) i.next());
    synchronized (this.sessionMonitor)
      {this.sessionData.clear();}
    this.webAppConfig.removeSessionById(this.sessionId);
  }

  /**
   * Called after the session has been deserialized from another server.
   */
  public void activate(WebAppConfiguration webAppConfig)
  {
    this.webAppConfig = webAppConfig;
    this.cachedRequest = null;
    webAppConfig.setSessionListeners(this);
    
    // Notify session listeners of invalidated session
    for (int n = 0; n < this.sessionActivationListeners.length; n++)
      this.sessionActivationListeners[n].sessionDidActivate(new HttpSessionEvent(this));
  }

  /**
   * Serialization implementation. This makes sure to only serialize the parts we want to 
   * send to another server.
   * @param out The stream to write the contents to
   * @throws IOException
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException
  {
    out.writeUTF(sessionId);
    out.writeLong(createTime);
    out.writeLong(lastAccessedTime);
    out.writeInt(maxInactivePeriod);
    out.writeBoolean(isNew);
    out.writeBoolean(distributable);
    out.writeObject(authenticatedUser);
    
    // Write the map, but first remove non-serializables
    Map copy = new HashMap(sessionData);
    for (Iterator i = copy.keySet().iterator(); i.hasNext(); )
    {
    	String key = (String) i.next();
    	if (!(copy.get(key) instanceof Serializable))
    		Logger.log(Logger.WARNING, resources, "WinstoneSession.SkippingNonSerializable",
    					new String[] {key, copy.get(key).getClass().getName()});
    		copy.remove(key);
    }
    out.writeInt(copy.size());
    for (Iterator i = copy.keySet().iterator(); i.hasNext(); )
    {
      String key = (String) i.next();
      out.writeUTF(key);
      out.writeObject(copy.get(key));
    }
  }

  /**
   * Deserialization implementation
   * @param in The source of stream data
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    this.sessionId = in.readUTF();
    this.createTime = in.readLong();
    this.lastAccessedTime = in.readLong();
    this.maxInactivePeriod = in.readInt();
    this.isNew = in.readBoolean();
    this.distributable = in.readBoolean();
    this.authenticatedUser = (AuthenticationPrincipal) in.readObject();
    
    // Read the map
    this.sessionData = new Hashtable();
    int entryCount = in.readInt();
    for (int n = 0; n < entryCount; n++)
    {
      String key = in.readUTF();
      Object variable = in.readObject();
      this.sessionData.put(key, variable);
    }
    this.sessionMonitor = new Boolean(true);
  }
  
  /**
   * @deprecated
   */
  public Object getValue(String name)             {return getAttribute(name);}
  /**
   * @deprecated
   */
  public void putValue(String name, Object value) {setAttribute(name, value);}
  /**
   * @deprecated
   */
  public void removeValue(String name)            {removeAttribute(name);}
  /**
   * @deprecated
   */
  public String[] getValueNames()
    {return (String []) this.sessionData.keySet().toArray(new String[this.sessionData.size()]);}
  /**
   * @deprecated
   */
  public javax.servlet.http.HttpSessionContext getSessionContext()       {return null;} // deprecated
}

