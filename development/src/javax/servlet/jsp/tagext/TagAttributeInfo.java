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
package javax.servlet.jsp.tagext;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class TagAttributeInfo
{
  public static final String ID = "id";
  
  private String name;
  private String type;
  private boolean required;
  private boolean reqTime;
  private boolean fragment;
  
  public TagAttributeInfo(String name, boolean required, String type, boolean reqTime) 
    {this(name, required, type, reqTime, false);}
  public TagAttributeInfo(String name, boolean required, String type, boolean reqTime, boolean fragment) 
  {
    this.name = name;
    this.type = type;
    this.required = required;
    this.reqTime = reqTime;
    this.fragment = fragment;
  }

  public static TagAttributeInfo getIdAttribute(TagAttributeInfo[] a)
  {
    for (int n = 0; n < a.length; n++)
      if ((a[n].getName() != null) && a[n].getName().equals(ID))
        return a[n];
    return null;
  } 
  public String getName() {return this.name;} 
  public String getTypeName() {return this.type;}
  public boolean canBeRequestTime() {return this.reqTime;}
  public boolean isFragment() {return this.fragment;} 
  public boolean isRequired() {return this.required;}
  public String toString() 
    {return this.getClass().getName() + " [name=" + name + "]";}
}
