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
import java.util.*;
import javax.servlet.http.Cookie;

/**
 * Matches the socket output stream to the servlet output.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream
{
  final int DEFAULT_BUFFER_SIZE = 8192;

  protected OutputStream outStream;
  protected int bufferSize;
  protected int bufferPosition;
  protected int bytesWritten;
  protected ByteArrayOutputStream buffer;
  protected boolean committed;
  protected boolean bodyOnly;
  //private boolean headersWritten;
  protected WinstoneResourceBundle resources;
  protected WinstoneResponse owner;

  /**
   * Constructor
   */
  public WinstoneOutputStream(OutputStream out,
                              boolean bodyOnly,
                              WinstoneResourceBundle resources)
  {
    this.resources = resources;
    this.outStream = out;
    this.bodyOnly = bodyOnly;
    setBufferSize(DEFAULT_BUFFER_SIZE);
    this.committed = false;
    //this.headersWritten = false;
    this.buffer = new ByteArrayOutputStream();
  }

  public void setResponse(WinstoneResponse response) {this.owner = response;}
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
    this.buffer.flush();

    // If we haven't written the headers yet, write them out
    if (!this.committed && !this.bodyOnly)
    {
      this.owner.validateHeaders();
      this.owner.verifyContentLength();

      PrintStream headerStream = new PrintStream(this.outStream, true);
      String statusLine = this.owner.getProtocol() + " " + this.owner.getStatus();
      headerStream.println(statusLine);
      Logger.log(Logger.FULL_DEBUG, "Response: " + statusLine);

      // Write headers and cookies
      for (Iterator i = this.owner.getHeaders().iterator(); i.hasNext(); )
      {
        String header = (String) i.next();
        headerStream.println(header);
        Logger.log(Logger.FULL_DEBUG, "Header: " + header);
      }

      if (!this.owner.getHeaders().isEmpty())
      {
        for (Iterator i = this.owner.getCookies().iterator(); i.hasNext(); )
        {
          Cookie cookie = (Cookie) i.next();
          String cookieText = this.owner.writeCookie(cookie);
          headerStream.println(cookieText);
          Logger.log(Logger.FULL_DEBUG, "Header: " + cookieText);
        }
      }
      headerStream.println();
      headerStream.flush();
      //Logger.log(Logger.FULL_DEBUG, resources.getString("HttpProtocol.OutHeaders") + out.toString());
    }
    //byte content[] = this.buffer.toByteArray();
    //com.rickknowles.winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
    this.buffer.writeTo(this.outStream);
    this.outStream.flush();
    
    Logger.log(Logger.FULL_DEBUG, resources.getString("WinstoneOutputStream.CommittedBytes", "[#postHeaderBytes]", "" + this.bytesWritten));

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

  public void finishResponse() throws IOException
  {
    this.outStream.flush();
    this.outStream = null;
  }
  
}

