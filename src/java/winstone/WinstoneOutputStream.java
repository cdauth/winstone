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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;

import javax.servlet.http.Cookie;

/**
 * Matches the socket output stream to the servlet output.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream {
    final int DEFAULT_BUFFER_SIZE = 8192;
    protected OutputStream outStream;
    protected int bufferSize;
    protected int bufferPosition;
    protected int bytesCommitted;
    protected ByteArrayOutputStream buffer;
    protected boolean committed;
    protected boolean bodyOnly;
    protected WinstoneResponse owner;
    protected boolean disregardMode = false;
    
    /**
     * Constructor
     */
    public WinstoneOutputStream(OutputStream out, boolean bodyOnlyForInclude) {
        this.outStream = out;
        this.bodyOnly = bodyOnlyForInclude;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.committed = false;
        // this.headersWritten = false;
        this.buffer = new ByteArrayOutputStream();
    }

    public void setResponse(WinstoneResponse response) {
        this.owner = response;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        if (this.owner.isCommitted()) {
            throw new IllegalStateException(Launcher.RESOURCES.getString(
                    "WinstoneOutputStream.AlreadyCommitted"));
        }
        this.bufferSize = bufferSize;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public int getOutputStreamLength() {
        return this.bytesCommitted + this.bufferPosition;
    }

    public int getBytesCommitted() {
        return this.bytesCommitted;
    }
    
    public void setDisregardMode(boolean disregard) {
        this.disregardMode = disregard;
    }

    public void write(int oneChar) throws IOException {
        if (this.disregardMode) {
            return;
        }
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if ((contentLengthHeader != null) && 
                (this.bytesCommitted >= Integer.parseInt(contentLengthHeader))) {
            return;
        }
//        System.out.println("Out: " + this.bufferPosition + " char=" + (char)oneChar);
        this.buffer.write(oneChar);
        this.bufferPosition++;
        // if (this.headersWritten)
        if (this.bufferPosition >= this.bufferSize) {
            commit();
        } else if ((contentLengthHeader != null) && 
                ((this.bufferPosition + this.bytesCommitted) 
                        >= Integer.parseInt(contentLengthHeader))) {
            commit();
        }
    }

    public void commit() throws IOException {
        this.buffer.flush();

        // If we haven't written the headers yet, write them out
        if (!this.committed && !this.bodyOnly) {
            this.owner.validateHeaders();
            this.committed = true;

            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneOutputStream.CommittingOutputStream");
            
            PrintStream headerStream = new PrintStream(this.outStream, true);
            String statusLine = this.owner.getProtocol() + " "
                    + this.owner.getStatus();
            headerStream.println(statusLine);
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WinstoneOutputStream.ResponseStatus", statusLine);

            // Write headers and cookies
            for (Iterator i = this.owner.getHeaders().iterator(); i.hasNext();) {
                String header = (String) i.next();
                headerStream.println(header);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneOutputStream.Header", header);
            }

            if (!this.owner.getHeaders().isEmpty()) {
                for (Iterator i = this.owner.getCookies().iterator(); i.hasNext();) {
                    Cookie cookie = (Cookie) i.next();
                    String cookieText = this.owner.writeCookie(cookie);
                    headerStream.println(cookieText);
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneOutputStream.Header", cookieText);
                }
            }
            headerStream.println();
            headerStream.flush();
            // Logger.log(Logger.FULL_DEBUG,
            // Launcher.RESOURCES.getString("HttpProtocol.OutHeaders") + out.toString());
        }
        byte content[] = this.buffer.toByteArray();
//        winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
//        this.buffer.writeTo(this.outStream);
        int commitLength = content.length;
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if (contentLengthHeader != null) {
            commitLength = Math.min(Integer.parseInt(contentLengthHeader)
                    - this.bytesCommitted, content.length);
        }
        this.outStream.write(content, 0, commitLength);
        this.outStream.flush();

        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                "WinstoneOutputStream.CommittedBytes", 
                "" + (this.bytesCommitted + commitLength));

        this.bytesCommitted += commitLength;
        this.buffer.reset();
        this.bufferPosition = 0;
    }

    public void reset() {
        if (isCommitted())
            throw new IllegalStateException(Launcher.RESOURCES
                    .getString("WinstoneOutputStream.AlreadyCommitted"));
        else {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WinstoneOutputStream.ResetBuffer", this.bufferPosition
                            + "");
            this.buffer.reset();
            this.bufferPosition = 0;
            this.bytesCommitted = 0;
        }
    }

    public void finishResponse() throws IOException {
        this.outStream.flush();
        this.outStream = null;
    }

    public void flush() throws IOException {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneOutputStream.Flushing");
        if (this.disregardMode) {
            return;
        }
        super.flush();
        this.commit();
    }
}
