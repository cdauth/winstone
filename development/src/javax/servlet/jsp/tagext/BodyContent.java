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

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class BodyContent extends JspWriter
{
  private JspWriter enclosingWriter;
  protected BodyContent(JspWriter e) 
  {
    super(e.getBufferSize(), e.isAutoFlush());
    this.enclosingWriter = e;
  }
  public void clearBody() {}
  public void flush() throws IOException 
    {throw new IOException("Flush is illegal");} 
  public JspWriter getEnclosingWriter() {return this.enclosingWriter;}
  public abstract java.io.Reader getReader();
  public abstract  java.lang.String getString();
  public abstract  void writeOut(java.io.Writer out);
}
