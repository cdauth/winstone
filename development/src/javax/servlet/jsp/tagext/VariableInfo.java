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
public class VariableInfo
{
  public static final int NESTED = 0;
  public static final int AT_BEGIN = 1;
  public static final int AT_END = 2;

  private String varName;
  private String className;
  private boolean declare;
  private int scope;
  
  public VariableInfo(String varName, String className, boolean declare, int scope)
  {
    this.varName = varName;
    this.className = className;
    this.declare = declare;
    this.scope = scope;
  }
  public String getClassName() {return this.className;}
  public boolean getDeclare() {return this.declare;}
  public int getScope() {return this.scope;} 
  public String getVarName() {return this.varName;}  
}
