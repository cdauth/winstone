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
package com.rickknowles.winstone.jndi;

import com.rickknowles.winstone.*;
import com.rickknowles.winstone.jndi.resourceFactories.*;
import java.util.*;
import javax.naming.*;
import java.lang.reflect.*;
import org.w3c.dom.Node;

/**
 * Implements a simple web.xml + command line arguments style jndi manager
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebAppJNDIManager implements JNDIManager
{
  final static String ELEM_ENV_ENTRY       = "env-entry";
  final static String ELEM_ENV_ENTRY_NAME  = "env-entry-name";
  final static String ELEM_ENV_ENTRY_TYPE  = "env-entry-type";
  final static String ELEM_ENV_ENTRY_VALUE = "env-entry-value";

  private static final String LOCAL_RESOURCE_FILE = "com.rickknowles.winstone.jndi.LocalStrings";
  
  private Map objectsToCreate;
  private ClassLoader loader;
  private WinstoneResourceBundle resources;
  
  /**
   * Gets the relevant list of objects from the args, validating against
   * the web.xml nodes supplied. All node addresses are assumed to be
   * relative to the java:/comp/env context
   */
  public WebAppJNDIManager(Map args, List webXMLNodes, ClassLoader loader)
  {
    // Build all the objects we wanted
    this.resources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    this.objectsToCreate = new HashMap();
    this.loader = loader;
    
    Collection keys = new ArrayList(args.keySet());
    for (Iterator i = keys.iterator(); i.hasNext(); )
    {
      String key = (String) i.next();
      
      if (key.startsWith("jndi.resource."))
      {
        String resName = key.substring(14);
        String className = (String) args.get(key);
        String value = (String) args.get("jndi." + resName + ".value");
        Object obj = createObject(resName.trim(), className.trim(), value, args);
        if (obj != null)
        {
          this.objectsToCreate.put(resName, obj);
          Logger.log(Logger.FULL_DEBUG, this.resources.getString(
            "WebAppJNDIManager.CreatedResourceArgs", "[#name]", resName));
        }
      }
    }
    
    // If the webXML nodes are not null, validate that all the entries we wanted 
    // have been created
    if (webXMLNodes != null)
      for (Iterator i = webXMLNodes.iterator(); i.hasNext(); )
      {
        Node node = (Node) i.next();
        
        // Extract the env-entry nodes and create the objects
        if (node.getNodeType() != Node.ELEMENT_NODE)
          continue;
        else if (node.getNodeName().equals(ELEM_ENV_ENTRY))
        {
          String name = null;
          String type = null;
          String value = null;
          for (int m = 0; m < node.getChildNodes().getLength(); m++)
          {
            Node envNode = (Node) node.getChildNodes().item(m);
            if (envNode.getNodeType() != Node.ELEMENT_NODE)
              continue;
            else if (envNode.getNodeName().equals(ELEM_ENV_ENTRY_NAME))
              name = envNode.getFirstChild().getNodeValue().trim();
            else if (envNode.getNodeName().equals(ELEM_ENV_ENTRY_TYPE))
              type = envNode.getFirstChild().getNodeValue().trim();
            else if (envNode.getNodeName().equals(ELEM_ENV_ENTRY_VALUE))
              value = envNode.getFirstChild().getNodeValue().trim();
          }
          if ((name != null) && (type != null) && (value != null))
          {
            Object obj = createObject(name, type, value, args);
            if (obj != null)
            {
              this.objectsToCreate.put(name, obj);
              Logger.log(Logger.FULL_DEBUG, this.resources.getString(
                "WebAppJNDIManager.CreatedResourceWebXML", "[#name]", name));
            }
          }
        }
      }
  }

  /**
   * Add the objects passed to the constructor to the JNDI Context addresses
   * specified
   */
  public void setup()
  {
    try
    {
      InitialContext ic = new InitialContext();
      for (Iterator i = this.objectsToCreate.keySet().iterator(); i.hasNext(); )
      {
        String name = (String) i.next();
        try
        {
          Name fullName = new CompositeName(name);
          Context currentContext = ic;
          while (fullName.size() > 1)
          {
            // Make contexts that are not already present
            try
              {currentContext = currentContext.createSubcontext(fullName.get(0));}
            catch (NamingException err) 
              {currentContext = (Context) currentContext.lookup(fullName.get(0));}
            fullName = fullName.getSuffix(1);
          }
          ic.bind(name, this.objectsToCreate.get(name));
          Logger.log(Logger.FULL_DEBUG, this.resources.getString(
            "WebAppJNDIManager.BoundResource", "[#name]", name));
        }
        catch (NamingException err)
          {Logger.log(Logger.ERROR, this.resources.getString(
            "WebAppJNDIManager.ErrorBindingResource", "[#name]", name), err);}
      }
    }
    catch (NamingException err)
      {Logger.log(Logger.ERROR, this.resources.getString(
            "WebAppJNDIManager.ErrorGettingInitialContext"), err);}  
  }
  
  /**
   * Remove the objects under administration from the JNDI Context, and 
   * then destroy the objects
   */
  public void tearDown()
  {
    try
    {
      InitialContext ic = new InitialContext();
      for (Iterator i = this.objectsToCreate.keySet().iterator(); i.hasNext(); )
      {
        String name = (String) i.next();
        try
          {ic.unbind(name);}
        catch (NamingException err)
          {Logger.log(Logger.ERROR, this.resources.getString(
            "WebAppJNDIManager.ErrorUnbindingResource", "[#name]", name), err);}
        Object unboundObject = this.objectsToCreate.get(name);
        if (unboundObject instanceof WinstoneDataSource)
          ((WinstoneDataSource) unboundObject).destroy();
        Logger.log(Logger.FULL_DEBUG, this.resources.getString(
          "WebAppJNDIManager.BUnboundResource", "[#name]", name));
      }
    }
    catch (NamingException err)
      {Logger.log(Logger.ERROR, this.resources.getString(
          "WebAppJNDIManager.ErrorGettingInitialContext"), err);}  
  }
  
  /**
   * Build an object to insert into the jndi space 
   */
  private Object createObject(String name, String className, String value, Map args)
  {
    if ((className == null) || (name == null))
      return null;

    // If we are working with a datasource    
    if (className.equals("javax.sql.DataSource"))
    {
      Map relevantArgs = new HashMap();
      for (Iterator i = args.keySet().iterator(); i.hasNext(); )
      {
        String key = (String) i.next();
        if (key.startsWith("jndi." + name + "."))
          relevantArgs.put(key.substring(6 + name.length()), args.get(key));
      }
      try
        {return new WinstoneDataSource(name, relevantArgs, this.loader, this.resources);}
      catch (Throwable err)
        {Logger.log(Logger.ERROR, this.resources.getString(
          "WebAppJNDIManager.ErrorBuildingDatasource", "[#name]", name), err);}
    }
    
    // If unknown type, try to instantiate with the string constructor
    else if (value != null)
    try
    {
      Class objClass = Class.forName(className, true, this.loader);
      Constructor objConstr = objClass.getConstructor(new Class[] {String.class});
      return objConstr.newInstance(new Object[] {value});
    }
    catch (Throwable err)
      {Logger.log(Logger.ERROR, this.resources.getString(
        "WebAppJNDIManager.ErrorBuildingObject", "[#name]", name, "[#class]", className), err);}
    return null;
  }
}
