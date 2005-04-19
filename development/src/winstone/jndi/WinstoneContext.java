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
package winstone.jndi;

import java.util.*;
import javax.naming.*;
import javax.naming.spi.*;

import winstone.*;

/**
 * The main jndi context implementation class.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneContext implements Context
{
  static final String PREFIX = "java:";
  static final String FIRST_CHILD = "comp";
  static final String BODGED_PREFIX = "java:comp";

  private static final String LOCAL_RESOURCE_FILE = "winstone.jndi.LocalStrings";

  private Hashtable environment;
  private Hashtable bindings;
  private final static NameParser nameParser = new WinstoneNameParser();
  private WinstoneContext parent;
  private String myAbsoluteName;
  private Object contextLock;
  private WinstoneResourceBundle resources;
    
  /**
   * Constructor - sets up environment 
   */
  public WinstoneContext(Map sourceEnvironment, WinstoneContext parent, String absoluteName,
    Object contextLock) 
    throws NamingException
  {
    this.resources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    this.environment = new Hashtable();
    List sourceKeys = new ArrayList(sourceEnvironment.keySet());
    for (Iterator i = sourceKeys.iterator(); i.hasNext(); )
    {
      String key = (String) i.next();
      addToEnvironment(key, sourceEnvironment.get(key));
    }
    this.parent = parent;
    this.myAbsoluteName = absoluteName;
    this.contextLock = contextLock;
    this.bindings = new Hashtable();
    Logger.log(Logger.FULL_DEBUG, this.resources, "WinstoneContext.Initialised", this.myAbsoluteName);
  }

  /**
   * Constructor - sets up environment and copies the bindings across
   */
  protected WinstoneContext(Map sourceEnvironment, WinstoneContext parent, String absoluteName,
    Object contextLock, Hashtable bindings, WinstoneResourceBundle resources) throws NamingException
  {
    this.environment = new Hashtable();
    List sourceKeys = new ArrayList(sourceEnvironment.keySet());
    for (Iterator i = sourceKeys.iterator(); i.hasNext(); )
    {
      String key = (String) i.next();
      addToEnvironment(key, sourceEnvironment.get(key));
    }
    this.parent = parent;
    this.myAbsoluteName = absoluteName;
    this.contextLock = contextLock;
    this.bindings = bindings;
    this.resources = resources;
    Logger.log(Logger.FULL_DEBUG, this.resources, "WinstoneContext.Copied", this.myAbsoluteName);
  }
  
  public void close() throws NamingException {}
  
  public Hashtable getEnvironment() throws NamingException
    {return new Hashtable(this.environment);}
  public Object removeFromEnvironment(String property) throws NamingException
    {return this.environment.remove(property);}
  public Object addToEnvironment(String property, Object value) throws NamingException
    {return this.environment.put(property, value);}

  /**
   * Handles the processing of relative and absolute names. If a relative name is 
   * detected, it is processed by the name parser. If an absolute name is detected,
   * it determines first if the absolute name refers to this context. If not, it then
   * determines whether the request can be passed back to the parent or not, and
   * returns null if it can, and throws an exception otherwise.
   */
  protected Name validateName(Name name) throws NamingException
  {
    // Check for absolute urls and redirect or correct
    if (name.isEmpty())
      return name;
    else if (name.get(0).equals(BODGED_PREFIX))
    {
      Name newName = name.getSuffix(1).add(0, FIRST_CHILD).add(0, PREFIX);
      return validateName(newName);
    }
    else if (name.get(0).equals(PREFIX))
    {
      String stringName = name.toString();
      if (stringName.equals(this.myAbsoluteName))
        return nameParser.parse("");
      else if (stringName.startsWith(this.myAbsoluteName))
        return nameParser.parse(stringName.substring(this.myAbsoluteName.length() + 1));
      else if (this.parent != null)
        return null;
      else
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", name.toString()));      
    }
    else if (name instanceof CompositeName)
      return nameParser.parse(name.toString());
    else
      return name;
  }

  /**
   * Lookup an object in the context. Returns a copy of this context if the
   * name is empty, or the specified resource (if we have it). If the name is
   * unknown, throws a NameNotFoundException.
   */
  public Object lookup(Name name) throws NamingException 
  {
    Name searchName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (searchName == null)
      return this.parent.lookup(name);
    // If empty name, return a copy of this Context
    else if (searchName.isEmpty())
      return new WinstoneContext(this.environment, this.parent, this.myAbsoluteName, 
                                this.contextLock, this.bindings, this.resources);

    String thisName = searchName.get(0);
    synchronized (this.contextLock)
    {
      Object thisValue = bindings.get(thisName);

      // If the name points to something in this level, try to find it, and give 
      // an error if not available
      if (searchName.size() == 1)    
      {
        if (thisValue == null)
          throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", name.toString()));

        try
          {return NamingManager.getObjectInstance(thisValue, 
                                                  new CompositeName().add(thisName), 
                                                  this, this.environment);}
        catch (Exception e)
        {
          NamingException ne = new NamingException(this.resources.getString("WinstoneContext.FailedToGetInstance"));
          ne.setRootCause(e);
          throw ne;
        }
      } 
    
      else if (thisValue == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", thisName.toString()));
        
      // If it's not in this level and what we found is not a context, complain
      else if (!(thisValue instanceof Context)) 
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {thisName.toString(), thisValue.getClass().getName()}));
      
      // Open the context, perform a lookup, then close the context we opened
      else
        try
          {return ((Context) thisValue).lookup(searchName.getSuffix(1));}
        finally 
          {((Context) thisValue).close();}
    }
  }
  public Object lookup(String name) throws NamingException 
    {return lookup(new CompositeName(name));}

  public Object lookupLink(Name name) throws NamingException 
  {
    Logger.log(Logger.WARNING, this.resources, "WinstoneContext.LinkRefUnsupported");
    return lookup(name);
  }
  public Object lookupLink(String name) throws NamingException 
    {return lookupLink(new CompositeName(name));}

  /**
   * Returns a list of objects bound to the context
   */
  public NamingEnumeration list(Name name) throws NamingException 
  {
    Name searchName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (searchName == null)
      return this.parent.list(name);
    // If empty name, return a copy of this Context
    else if (searchName.isEmpty())
    {
      NamingEnumeration e = null;
      synchronized (this.contextLock)
        {e = new WinstoneNameEnumeration(this.bindings, this.resources);}
      return e;
    }
    
    // Lookup the object - if it's not a context, throw an error
    else 
    {
      Object ctx = this.lookup(searchName);
      if (ctx instanceof Context)
        try
          {return ((Context) ctx).list(new CompositeName(""));}
        finally
          {((Context) ctx).close();}
      else if (ctx == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", searchName.toString()));
      else
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {searchName.toString(), ctx.getClass().getName()}));
    }
  }
  public NamingEnumeration list(String name) throws NamingException 
    {return list(new CompositeName(name));}

  public NamingEnumeration listBindings(Name name) throws NamingException
  {
    Name searchName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (searchName == null)
      return this.parent.list(name);
    // If empty name, return a copy of this Context
    else if (searchName.isEmpty())
    {
      NamingEnumeration e = null;
      synchronized (this.contextLock)
        {e = new WinstoneBindingEnumeration(this.bindings, this.environment, this, this.resources);}
      return e;
    }
    
    // Lookup the object - if it's not a context, throw an error
    else 
    {
      Object ctx = this.lookup(searchName);
      if (ctx instanceof Context)
        try
          {return ((Context) ctx).listBindings(new CompositeName(""));}
        finally
          {((Context) ctx).close();}
      else if (ctx == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", searchName.toString()));
      else
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {searchName.toString(), ctx.getClass().getName()}));
    }
  }
  public NamingEnumeration listBindings(String name) throws NamingException
    {return listBindings(new CompositeName(name));}

  public NameParser getNameParser(Name name) throws NamingException  
  {
    Object obj = lookup(name);
    if (obj instanceof Context) {
        ((Context)obj).close();
    }
    return nameParser;
  }
  public NameParser getNameParser(String name) throws NamingException 
    {return getNameParser(new CompositeName(name));}

  public String getNameInNamespace() throws NamingException 
    {return this.myAbsoluteName;}

  /**************************************************************
   * Below here is for read-write contexts ...                  *
   **************************************************************/

  public void bind(String name, Object value) throws NamingException 
    {bind(new CompositeName(name), value);}
  public void bind(Name name, Object value) throws NamingException
    {bind(name, value, false);}
  protected void bind(Name name, Object value, boolean allowOverwrites) throws NamingException
  {
    Name bindName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (bindName == null)
      this.parent.bind(name, value, allowOverwrites);
    // If empty name, complain - we should have a child name here
    else if (bindName.isEmpty())
      throw new NamingException(this.resources.getString("WinstoneContext.AlreadyExists", name.toString()));
    else if (bindName.size() > 1)
    {
      Object ctx = lookup(bindName.get(0));
      if (!(ctx instanceof Context))
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {bindName.get(0), ctx.getClass().getName()}));
      else if (ctx == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", bindName.get(0)));
      else try
      {
        if (allowOverwrites)
          ((Context) ctx).rebind(bindName.getSuffix(1), value);
        else
          ((Context) ctx).bind(bindName.getSuffix(1), value);
      }
      finally
        {((Context) ctx).close();}
    }
    else if ((!allowOverwrites) && this.bindings.get(name.get(0)) != null)
      throw new NamingException(this.resources.getString("WinstoneContext.AlreadyExists", name.toString()));
    else
    {
      value = NamingManager.getStateToBind(value, new CompositeName().add(bindName.get(0)), this, this.environment);
      synchronized (this.contextLock)
        {this.bindings.put(bindName.get(0), value);}
    }
  }

  public void rebind(String name, Object value) throws NamingException
    {rebind(new CompositeName(name), value);}
  public void rebind(Name name, Object value) throws NamingException 
    {bind(name, value, true);}

  public void unbind(String name) throws NamingException {unbind(new CompositeName(name));}
  public void unbind(Name name) throws NamingException 
  {
    Name unbindName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (unbindName == null)
      this.parent.unbind(name);
    // If empty name, complain - we should have a child name here
    else if (unbindName.isEmpty())
      throw new NamingException(this.resources.getString("WinstoneContext.CantUnbindEmptyName"));
    else if (unbindName.size() > 1)
    {
      Object ctx = lookup(unbindName.get(0));
      if (!(ctx instanceof Context))
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {unbindName.get(0), ctx.getClass().getName()}));
      else if (ctx == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", unbindName.get(0)));
      else try
        {((Context) ctx).unbind(unbindName.getSuffix(1));}
      finally
        {((Context) ctx).close();}
    }
    else if (this.bindings.get(name.get(0)) == null)
      throw new NamingException(this.resources.getString("WinstoneContext.NameNotFound", name.toString()));
    else
    {
      synchronized (this.contextLock)
      {
        //Object removing = this.bindings.get(unbindName.get(0));
        this.bindings.remove(unbindName.get(0));
      }
    }
  }

  public void rename(Name oldName, Name newName) throws NamingException 
    {throw new OperationNotSupportedException("rename not supported in Winstone java:/ context");}
  public void rename(String oldName, String newName) throws NamingException 
    {rename(new CompositeName(oldName), new CompositeName(newName));}

  public Context createSubcontext(String name) throws NamingException 
    {return createSubcontext(new CompositeName(name));}
  public Context createSubcontext(Name name) throws NamingException 
  {
    Name childName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (childName == null)
      return this.parent.createSubcontext(name);
    // If empty name, complain - we should have a child name here
    else if (childName.isEmpty())
      throw new NamingException(this.resources.getString("WinstoneContext.AlreadyExists", name.toString()));
    else if (childName.size() > 1)
    {
      Object ctx = lookup(childName.get(0));
      if (!(ctx instanceof Context))
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {childName.get(0), ctx.getClass().getName()}));
      else if (ctx == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", childName.get(0)));
      else try
        {((Context) ctx).createSubcontext(childName.getSuffix(1));}
      finally
        {((Context) ctx).close();}
    }

    Context childContext = null;
    synchronized (this.contextLock)
    { 
      if (this.bindings.get(childName.get(0)) != null)
        throw new NamingException(this.resources.getString("WinstoneContext.AlreadyExists", childName.get(0)));
      else
      {
        childContext = new WinstoneContext(this.environment, this, 
                          this.myAbsoluteName + "/" + childName.get(0), new Boolean(true));
        this.bindings.put(childName.get(0), childContext);
      }
    }
    return childContext;
  }

  public void destroySubcontext(String name) throws NamingException
    {destroySubcontext(new CompositeName(name));}
  public void destroySubcontext(Name name) throws NamingException 
  {
    Name childName = validateName(name);

    // If null, it means we don't know how to handle this -> throw to the parent
    if (childName == null)
      this.parent.destroySubcontext(name);
    // If absolutely referring to this context, tell the parent to delete this context
    else if (childName.isEmpty())
    {
      if(!name.isEmpty())
        this.parent.destroySubcontext(name.getSuffix(name.size() - 2));
      else
        throw new NamingException(this.resources.getString("WinstoneContext.CantDestroyEmptyName"));
    }
    else if (childName.size() > 1)
    {
      Object ctx = lookup(childName.get(0));
      if (!(ctx instanceof Context))
        throw new NotContextException(this.resources.getString("WinstoneContext.NotContext", 
            new String[] {childName.get(0), ctx.getClass().getName()}));
      else if (ctx == null)
        throw new NameNotFoundException(this.resources.getString("WinstoneContext.NameNotFound", childName.get(0)));
      else try
        {((Context) ctx).destroySubcontext(childName.getSuffix(1));}
      finally
        {((Context) ctx).close();}
    }
    else
      synchronized (this.contextLock)    
      {
        Context childContext = (Context) lookup(childName.get(0));
        childContext.close();
        this.bindings.remove(childName.get(0));
      }
  }

  public String composeName(String name1, String name2) throws NamingException 
  {
    Name name = composeName(new CompositeName(name1), new CompositeName(name2));
    return name == null ? null : name.toString();
  }
  public Name composeName(Name name1, Name name2) throws NamingException 
    {throw new OperationNotSupportedException("composeName not supported in Winstone java:/ namespace");}
}
