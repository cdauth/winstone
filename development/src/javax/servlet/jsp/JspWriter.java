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
package javax.servlet.jsp;

import java.io.*;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class JspWriter extends Writer
{
  protected boolean autoFlush;
  protected int bufferSize;
  public static int DEFAULT_BUFFER = -1;
  public static int NO_BUFFER = 0;
  public static int UNBOUNDED_BUFFER = -2;
  
  protected JspWriter (int bufferSize, boolean autoFlush) 
  {
    super();
    this.autoFlush = autoFlush;
    this.bufferSize = bufferSize;
  }
  
  public abstract void clear() throws IOException;
  public abstract void clearBuffer() throws IOException;
  public abstract void close() throws IOException;
  public abstract void flush() throws IOException;
  public int getBufferSize() {return this.bufferSize;}
  public abstract int getRemaining();
  public boolean isAutoFlush() {return this.autoFlush;}
  public abstract void newLine() throws IOException;
  public abstract void print(boolean b) throws IOException;
  public abstract void print(char c) throws IOException;
  public abstract void print(char[] s) throws IOException;
  public abstract void print(double d) throws IOException;
  public abstract void print(float f) throws IOException;
  public abstract void print(int i) throws IOException;
  public abstract void print(long l) throws IOException;
  public abstract void print(Object obj) throws IOException;
  public abstract void print(String s) throws IOException;
  public abstract void println() throws IOException;
  public abstract void println(boolean x) throws IOException;
  public abstract void println(char x) throws IOException;
  public abstract void println(char[] x) throws IOException;
  public abstract void println(double x) throws IOException;
  public abstract void println(float x) throws IOException;
  public abstract void println(int x) throws IOException;
  public abstract void println(long x) throws IOException;
  public abstract void println(java.lang.Object x) throws IOException;
  public abstract void println(java.lang.String x) throws IOException;

}
