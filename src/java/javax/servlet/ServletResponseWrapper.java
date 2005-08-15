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

import java.util.Locale;
import java.io.IOException;

/**
 * Wraps a servlet response object using the decorator pattern
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class ServletResponseWrapper implements ServletResponse {
    private ServletResponse response;

    public ServletResponseWrapper(ServletResponse response) {
        setResponse(response);
    }

    public ServletResponse getResponse() {
        return this.response;
    }

    public void setResponse(ServletResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Response was null");
        }
        this.response = response;
    }

    public Locale getLocale() {
        return this.response.getLocale();
    }

    public void setLocale(Locale loc) {
        this.response.setLocale(loc);
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return this.response.getOutputStream();
    }

    public java.io.PrintWriter getWriter() throws IOException {
        return this.response.getWriter();
    }

    public boolean isCommitted() {
        return this.response.isCommitted();
    }

    public int getBufferSize() {
        return this.response.getBufferSize();
    }

    public void setBufferSize(int size) {
        this.response.setBufferSize(size);
    }

    public void reset() {
        this.response.reset();
    }

    public void resetBuffer() {
        this.response.resetBuffer();
    }

    public void flushBuffer() throws IOException {
        this.response.flushBuffer();
    }

    public void setContentLength(int len) {
        this.response.setContentLength(len);
    }

    public void setContentType(String type) {
        this.response.setContentType(type);
    }

    public String getContentType() {
        return this.response.getContentType();
    }

    public String getCharacterEncoding() {
        return this.response.getCharacterEncoding();
    }

    public void setCharacterEncoding(String charset) {
        this.response.setCharacterEncoding(charset);
    }
}
