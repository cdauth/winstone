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

import java.util.*;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.ServletContext;

/**
 * Http session implementation for Winstone.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneSession implements javax.servlet.http.HttpSession
{
  private String sessionId;
  private WebAppConfiguration webAppConfig;
  private Map sessionData;
  private long createTime;
  private long lastAccessedTime;
  private int maxInactivePeriod;
  private boolean isNew;
  private List sessionAttributeListeners;
  private List sessionListeners;
  private boolean distributable;
  private WinstoneResourceBundle resources;
  private AuthenticationPrincipal authenticatedUser;
  private WinstoneRequest cachedRequest; 
    
  /**
   * Constructor
   */
  public WinstoneSession(String sessionId,
                         WebAppConfiguration webAppConfig,
                         List sessionListeners,
                         List sessionAttributeListeners,
                         boolean distributable,
                         WinstoneResourceBundle resources)
  {
    this.sessionId = sessionId;
    this.webAppConfig = webAppConfig;
    this.sessionData = new Hashtable();
    this.createTime = System.currentTimeMillis();
    this.isNew = true;
    this.sessionAttributeListeners = sessionAttributeListeners;
    this.sessionListeners = sessionListeners;
    this.distributable = distributable;
    this.resources = resources;

    // Notify session listeners of new session
    for (Iterator i = this.sessionListeners.iterator(); i.hasNext(); )
      ((HttpSessionListener) i.next()).sessionCreated(new HttpSessionEvent(this));
  }

  public void setLastAccessedDate(long time) {this.lastAccessedTime = time;}
  public void setIsNew(boolean isNew)        {this.isNew = isNew;}

  // Implementation methods
  public Object getAttribute(String name)             {return this.sessionData.get(name);}
  public Enumeration getAttributeNames()              {return Collections.enumeration(this.sessionData.keySet());}
  public void setAttribute(String name, Object value)
  {
    // Check for serializability if distributable
    if (this.distributable &&
        (value != null) &&
        !(value instanceof java.io.Serializable))
      throw new WinstoneException(this.resources.getString("WinstoneSession.AttributeNotSerializable",
          "[#name]", name, "[#class]", value.getClass().getName()));

    Object oldValue = this.sessionData.get(name);
    this.sessionData.put(name, value);
    if (oldValue instanceof HttpSessionBindingListener)
    {
      HttpSessionBindingListener hsbl = (HttpSessionBindingListener) oldValue;
      hsbl.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
    }
    if (value instanceof HttpSessionBindingListener)
    {
      HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
      hsbl.valueBound(new HttpSessionBindingEvent(this, name, value));
    }
    if (oldValue != null)
      for (Iterator i = this.sessionAttributeListeners.iterator(); i.hasNext(); )
        ((HttpSessionAttributeListener) i.next()).attributeReplaced(new HttpSessionBindingEvent(this, name, oldValue));
    else
      for (Iterator i = this.sessionAttributeListeners.iterator(); i.hasNext(); )
        ((HttpSessionAttributeListener) i.next()).attributeAdded(new HttpSessionBindingEvent(this, name, value));
  }
  public void removeAttribute(String name)
  {
    Object value = this.sessionData.get(name);
    this.sessionData.remove(name);
    if (value instanceof HttpSessionBindingListener)
    {
      HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
      hsbl.valueUnbound(new HttpSessionBindingEvent(this, name));
    }
    if (value != null)
      for (Iterator i = this.sessionAttributeListeners.iterator(); i.hasNext(); )
        ((HttpSessionAttributeListener) i.next()).attributeRemoved(new HttpSessionBindingEvent(this, name, value));
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
    for (Iterator i = this.sessionData.keySet().iterator(); i.hasNext(); )
      removeAttribute((String) i.next());
    this.sessionData.clear();
    this.webAppConfig.getSessions().remove(this.sessionId);

    // Notify session listeners of invalidated session
    for (Iterator i = this.sessionListeners.iterator(); i.hasNext(); )
      ((HttpSessionListener) i.next()).sessionDestroyed(new HttpSessionEvent(this));
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
  public HttpSessionContext getSessionContext()       {return null;} // deprecated
}

