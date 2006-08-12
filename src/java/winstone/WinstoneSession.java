/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Http session implementation for Winstone.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneSession implements HttpSession, Serializable {
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private String sessionId;
    private WebAppConfiguration webAppConfig;
    private Map sessionData;
    private long createTime;
    private long lastAccessedTime;
    private int maxInactivePeriod;
    private boolean isNew;
    private boolean isInvalidated;
    private HttpSessionAttributeListener sessionAttributeListeners[];
    private HttpSessionListener sessionListeners[];
    private HttpSessionActivationListener sessionActivationListeners[];
    private boolean distributable;
    private AuthenticationPrincipal authenticatedUser;
    private WinstoneRequest cachedRequest;
    private Object sessionMonitor = new Boolean(true);
    private Set requestsUsingMe;

    /**
     * Constructor
     */
    public WinstoneSession(String sessionId, WebAppConfiguration webAppConfig,
            boolean distributable) {
        this.sessionId = sessionId;
        this.webAppConfig = webAppConfig;
        this.sessionData = new HashMap();
        this.requestsUsingMe = new HashSet();
        this.createTime = System.currentTimeMillis();
        this.isNew = true;
        this.isInvalidated = false;
        this.distributable = distributable;
    }

    public void sendCreatedNotifies() {
        // Notify session listeners of new session
        for (int n = 0; n < this.sessionListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionListeners[n].sessionCreated(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void setSessionActivationListeners(
            HttpSessionActivationListener listeners[]) {
        this.sessionActivationListeners = listeners;
    }

    public void setSessionAttributeListeners(
            HttpSessionAttributeListener listeners[]) {
        this.sessionAttributeListeners = listeners;
    }

    public void setSessionListeners(HttpSessionListener listeners[]) {
        this.sessionListeners = listeners;
    }

    public void setLastAccessedDate(long time) {
        this.lastAccessedTime = time;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }
    
    public void addUsed(WinstoneRequest request) {
        this.requestsUsingMe.add(request);
    }
    
    public void removeUsed(WinstoneRequest request) {
        this.requestsUsingMe.remove(request);
    }
    
    public boolean isUnsedByRequests() {
        return this.requestsUsingMe.isEmpty();
    }
    
    public boolean isExpired() {
        // check if it's expired yet
        long nowDate = System.currentTimeMillis();
        long maxInactive = getMaxInactiveInterval() * 1000;
        return ((maxInactive > 0) && (nowDate - this.lastAccessedTime > maxInactive ));
    }

    // Implementation methods
    public Object getAttribute(String name) {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        Object att = null;
        synchronized (this.sessionMonitor) {
            att = this.sessionData.get(name);
        }
        return att;
    }

    public Enumeration getAttributeNames() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        Enumeration names = null;
        synchronized (this.sessionMonitor) {
            names = Collections.enumeration(this.sessionData.keySet());
        }
        return names;
    }

    public void setAttribute(String name, Object value) {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        // Check for serializability if distributable
        if (this.distributable && (value != null)
                && !(value instanceof java.io.Serializable))
            throw new IllegalArgumentException(Launcher.RESOURCES.getString(
                    "WinstoneSession.AttributeNotSerializable", new String[] {
                            name, value.getClass().getName() }));

        // valueBound must be before binding
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueBound(new HttpSessionBindingEvent(this, name, value));
            Thread.currentThread().setContextClassLoader(cl);
        }

        Object oldValue = null;
        synchronized (this.sessionMonitor) {
            oldValue = this.sessionData.get(name);
            if (value == null) {
                this.sessionData.remove(name);
            } else {
                this.sessionData.put(name, value);
            }
        }

        // valueUnbound must be after unbinding
        if (oldValue instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) oldValue;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Notify other listeners
        if (oldValue != null)
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeReplaced(
                        new HttpSessionBindingEvent(this, name, oldValue));
                Thread.currentThread().setContextClassLoader(cl);
            }
                
        else
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeAdded(
                        new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
                
            }
    }

    public void removeAttribute(String name) {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        Object value = null;
        synchronized (this.sessionMonitor) {
            value = this.sessionData.get(name);
            this.sessionData.remove(name);
        }

        // Notify listeners
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name));
            Thread.currentThread().setContextClassLoader(cl);
        }
        if (value != null)
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeRemoved(
                        new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
    }

    public long getCreationTime() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        return this.createTime;
    }

    public long getLastAccessedTime() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        return this.lastAccessedTime;
    }

    public String getId() {
        return this.sessionId;
    }

    public WinstoneRequest getCachedRequest() {
        return this.cachedRequest;
    }

    public void setCachedRequest(WinstoneRequest req) {
        this.cachedRequest = req;
    }

    public AuthenticationPrincipal getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    public void setAuthenticatedUser(AuthenticationPrincipal user) {
        this.authenticatedUser = user;
    }

    public int getMaxInactiveInterval() {
        return this.maxInactivePeriod;
    }

    public void setMaxInactiveInterval(int interval) {
        this.maxInactivePeriod = interval;
    }

    public boolean isNew() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        return this.isNew;
    }

    public ServletContext getServletContext() {
        return this.webAppConfig;
    }

    public void invalidate() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        // Notify session listeners of invalidated session -- backwards
        for (int n = this.sessionListeners.length - 1; n >= 0; n--) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionListeners[n].sessionDestroyed(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        List keys = new ArrayList(this.sessionData.keySet());
        for (Iterator i = keys.iterator(); i.hasNext();)
            removeAttribute((String) i.next());
        synchronized (this.sessionMonitor) {
            this.sessionData.clear();
        }
        this.isInvalidated = true;
        this.webAppConfig.removeSessionById(this.sessionId);
    }

    /**
     * Called after the session has been serialized to another server.
     */
    public void passivate() {
        // Notify session listeners of invalidated session
        for (int n = 0; n < this.sessionActivationListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionActivationListeners[n].sessionWillPassivate(
                    new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Question: Is passivation equivalent to invalidation ? Should all
        // entries be removed ?
        // List keys = new ArrayList(this.sessionData.keySet());
        // for (Iterator i = keys.iterator(); i.hasNext(); )
        // removeAttribute((String) i.next());
        synchronized (this.sessionMonitor) {
            this.sessionData.clear();
        }
        this.webAppConfig.removeSessionById(this.sessionId);
    }

    /**
     * Called after the session has been deserialized from another server.
     */
    public void activate(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        this.cachedRequest = null;
        webAppConfig.setSessionListeners(this);

        // Notify session listeners of invalidated session
        for (int n = 0; n < this.sessionActivationListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionActivationListeners[n].sessionDidActivate(
                    new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Serialization implementation. This makes sure to only serialize the parts
     * we want to send to another server.
     * 
     * @param out
     *            The stream to write the contents to
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(sessionId);
        out.writeLong(createTime);
        out.writeLong(lastAccessedTime);
        out.writeInt(maxInactivePeriod);
        out.writeBoolean(isNew);
        out.writeBoolean(distributable);
        out.writeObject(authenticatedUser);

        // Write the map, but first remove non-serializables
        Map copy = new HashMap(sessionData);
        for (Iterator i = copy.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (!(copy.get(key) instanceof Serializable))
                Logger
                        .log(Logger.WARNING, Launcher.RESOURCES,
                                "WinstoneSession.SkippingNonSerializable",
                                new String[] { key,
                                        copy.get(key).getClass().getName() });
            copy.remove(key);
        }
        out.writeInt(copy.size());
        for (Iterator i = copy.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            out.writeUTF(key);
            out.writeObject(copy.get(key));
        }
    }

    /**
     * Deserialization implementation
     * 
     * @param in
     *            The source of stream data
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
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
        for (int n = 0; n < entryCount; n++) {
            String key = in.readUTF();
            Object variable = in.readObject();
            this.sessionData.put(key, variable);
        }
        this.sessionMonitor = new Boolean(true);
    }

    /**
     * @deprecated
     */
    public Object getValue(String name) {
        return getAttribute(name);
    }

    /**
     * @deprecated
     */
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    /**
     * @deprecated
     */
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /**
     * @deprecated
     */
    public String[] getValueNames() {
        return (String[]) this.sessionData.keySet().toArray(new String[0]);
    }

    /**
     * @deprecated
     */
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    } // deprecated
}
