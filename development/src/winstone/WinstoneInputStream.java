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


/**
 * The request stream management class.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneInputStream extends javax.servlet.ServletInputStream
{
  final int BUFFER_SIZE = 4096;

  private InputStream inData;
  private Integer contentLength;
  private int readSoFar;
  private WinstoneResourceBundle resources;
  private ByteArrayOutputStream dump;

  /**
   * Constructor
   */
  public WinstoneInputStream(InputStream inData, WinstoneResourceBundle resources)
  {
    super();
    this.inData = inData;
    this.resources = resources;
    this.dump = new ByteArrayOutputStream();
  }

  public WinstoneInputStream(byte inData[], WinstoneResourceBundle resources)
  {
    this(new ByteArrayInputStream(inData), resources);
  }

  public InputStream getRawInputStream() {return this.inData;}

  public void setContentLength(int length)
  {
    this.contentLength = new Integer(length);
    this.readSoFar = 0;
  }

  public int read() throws IOException
  {
    if (this.contentLength == null)
    {
      int data = this.inData.read();
      this.dump.write(data);
      return data;
    }
    else if (this.contentLength.intValue() > this.readSoFar)
    {
      this.readSoFar++;
      int data = this.inData.read();
      this.dump.write(data);
      return data;
    }
    else
      return -1;
  }

  public void finishRequest()
  {
    //this.inData = null;
    //byte content[] = this.dump.toByteArray();
    //com.rickknowles.winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
  }

  /*{
    int charRead = this.inData.read();
    System.out.println("Char: " + (char) charRead);
    return charRead;
  }*/

  public int available() throws IOException  {return this.inData.available();}

  /**
   * Wrapper for the servletInputStream's readline method
   */
  public byte[] readLine() throws IOException
  {
    //System.out.println("ReadLine()");
    byte buffer[] = new byte[BUFFER_SIZE];
    int charsRead = super.readLine(buffer, 0, BUFFER_SIZE);
    if (charsRead == -1)
    {
      Logger.log(Logger.DEBUG, resources, "WinstoneInputStream.EndOfStream");
      return new byte[0];
    }
    byte outBuf[] = new byte[charsRead];
    System.arraycopy(buffer, 0, outBuf, 0, charsRead);
    return outBuf;
  }

}

 