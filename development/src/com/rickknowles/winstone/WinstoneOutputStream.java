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

import java.io.*;
import java.util.List;

/**
 * Matches the socket output stream to the servlet output.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream
{
  final int DEFAULT_BUFFER_SIZE = 8192;

  private OutputStream socketOut;
  private int bufferSize;
  private int bufferPosition;
  private int bytesWritten;
  private ByteArrayOutputStream buffer;
  private boolean committed;
  //private boolean headersWritten;
  private WinstoneResourceBundle resources;
  private WinstoneResponse owner;

  /**
   * Constructor
   */
  public WinstoneOutputStream(OutputStream out,
                              WinstoneResourceBundle resources,
                              WinstoneResponse owner)
  {
    this.resources = resources;
    this.socketOut = out;
    setBufferSize(DEFAULT_BUFFER_SIZE);
    this.committed = false;
    //this.headersWritten = false;
    this.owner = owner;
    this.buffer = new ByteArrayOutputStream();
  }

  public int getBufferSize()  {return this.bufferSize;}
  public void setBufferSize(int bufferSize) {this.bufferSize = bufferSize;}

  public boolean isCommitted() {return this.committed;}

  //public boolean areHeadersWritten() {return this.headersWritten;}
  //public void setHeadersWritten(boolean headersWritten) {this.headersWritten = headersWritten;}
  public int getBytesWritten() {return this.bytesWritten;}

  public void write(int oneChar) throws IOException
  {
    this.buffer.write(oneChar);
    this.bufferPosition++;
    //if (this.headersWritten)
    this.bytesWritten++;
    if (this.bufferPosition >= this.bufferSize)
      commit();
  }

  public void commit() throws IOException
  {
    Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneOutputStream.CommittedBytes", "[#postHeaderBytes]", "" + this.bytesWritten));
    this.buffer.flush();

    // If we haven't written the headers yet, write them out
    if (!this.committed)
      this.owner.writeHeaders(new PrintStream(this.socketOut, true));
    this.buffer.writeTo(this.socketOut);

    this.committed = true;
    this.buffer.reset();
    this.bufferPosition = 0;
  }

  public void reset()
  {
    if (isCommitted())
      throw new IllegalStateException(resources.getString("WinstoneOutputStream.AlreadyCommitted"));
    else
    {
      Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneOutputStream.ResetBuffer", "[#discardBytes]", this.bufferPosition + ""));
      this.buffer.reset();
      this.bufferPosition = 0;
      this.bytesWritten = 0;
    }
  }
}

