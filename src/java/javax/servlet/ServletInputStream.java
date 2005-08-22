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
package javax.servlet;

/**
 * Provides the base class for servlet request streams.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class ServletInputStream extends java.io.InputStream {
    protected ServletInputStream() {
        super();
    }

    public int readLine(byte[] b, int off, int len) throws java.io.IOException {
        if (b == null)
            throw new IllegalArgumentException("null buffer");
        else if (len + off > b.length)
            throw new IllegalArgumentException(
                    "offset + length is greater than buffer length");

        int positionCounter = 0;
        int charRead = read();
        while (charRead != -1) {
            b[off + positionCounter++] = (byte) charRead;
            if ((charRead == '\n') || (off + positionCounter == len)) {
                return positionCounter;
            } else {
                charRead = read();
            }
        }
        return -1;
    }

}
