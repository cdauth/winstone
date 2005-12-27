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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * A hacked print writer that allows us to trigger an automatic flush on 
 * println operations that go over the content length or buffer size.
 * 
 * This is only necessary because the spec authors seem intent of having 
 * the print writer's flushing behaviour be half auto-flush and half not.
 * Damned if I know why - seems unnecessary and confusing to me.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneResponseWriter extends PrintWriter {

    private WinstoneOutputStream outputStream;
    private WinstoneResponse response;
    private int bytesBuffered;
    
    public WinstoneResponseWriter(WinstoneOutputStream out, 
            WinstoneResponse response) throws UnsupportedEncodingException {
        super(new OutputStreamWriter(out, response.getCharacterEncoding()), false);
        this.outputStream = out;
        this.response = response;
        this.bytesBuffered = 0;
    }

    public void write(int c) {
        super.write(c);
        appendByteCount("" + ((char) c));
    }
    
    public void write(char[] buf, int off, int len) {
        super.write(buf, off, len);
        if (buf != null) {
            appendByteCount(new String(buf, off, len));
        }
    }

    public void write(String s, int off, int len) {
        super.write(s, off, len);
        if (s != null) {
            appendByteCount(s.substring(off, len));
        }
    }

    protected void appendByteCount(String input) {
        try {
            this.bytesBuffered += input.getBytes(response.getCharacterEncoding()).length;
        } catch (IOException err) {/* impossible */}

    }
    
    public void println() {
        super.println();
        simulateAutoFlush();
    }

    public void flush() {
        super.flush();
        this.bytesBuffered = 0;
    }

    protected void simulateAutoFlush() {
        String contentLengthHeader = response.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if ((contentLengthHeader != null) && 
                ((this.outputStream.getOutputStreamLength() + this.bytesBuffered) >= 
                        Integer.parseInt(contentLengthHeader))) {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneResponseWriter.AutoFlush",
                    new String[] {contentLengthHeader,
                    (this.outputStream.getOutputStreamLength() + this.bytesBuffered) + ""});
            flush();
        }
    }
}
