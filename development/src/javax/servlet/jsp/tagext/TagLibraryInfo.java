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
public abstract class TagLibraryInfo
{
  protected FunctionInfo[] functions;
  protected String info;
  protected String jspversion;
  protected String prefix;
  protected String shortname;
  protected TagFileInfo[] tagFiles;
  protected TagInfo[] tags;
  protected String tlibversion;
  protected String uri;
  protected String urn;

  protected TagLibraryInfo(String prefix, String uri)
  {
    this.prefix = prefix;
    this.uri = uri;
  }
  
  public FunctionInfo getFunction(String name) 
  {
    FunctionInfo[] functions = getFunctions();
    if (functions != null)
      for (int n = 0; n < functions.length; n++)
        if ((functions[n].getName() != null) &&
            functions[n].getName().equals(name))
          return functions[n];
    return null;
  }
  public FunctionInfo[] getFunctions() {return this.functions;}
  public String getInfoString()  {return this.info;}
  public String getPrefixString() {return this.prefix;}
  public String getReliableURN()  {return this.urn;}
  public String getRequiredVersion() {return this.jspversion;}
  public String getShortName() {return this.shortname;}
  public TagInfo getTag(String shortname) 
  {
    TagInfo[] tags = getTags();
    if (tags != null)
      for (int n = 0; n < tags.length; n++)
        if ((tags[n].getTagName() != null) &&
            tags[n].getTagName().equals(shortname))
          return tags[n];
    return null;
  }
  public TagFileInfo getTagFile(String shortname) 
  {
    TagFileInfo[] tagFiles = getTagFiles();
    if (tagFiles != null)
      for (int n = 0; n < tagFiles.length; n++)
        if ((tagFiles[n].getName() != null) &&
            tagFiles[n].getName().equals(shortname))
          return tagFiles[n];
    return null;
  }
  public TagFileInfo[] getTagFiles() {return this.tagFiles;}
  public TagInfo[] getTags() {return this.tags;}
  public String getURI() {return this.uri;}

}
