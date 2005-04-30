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

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class ServletOutputStream extends OutputStream {
    final String CR_LF = "\r\n";

    protected ServletOutputStream() {
        super();
    }

    public void print(boolean b) throws IOException {
        print("" + b);
    }

    public void print(char c) throws IOException {
        print("" + c);
    }

    public void print(double d) throws IOException {
        print("" + d);
    }

    public void print(float f) throws IOException {
        print("" + f);
    }

    public void print(int i) throws IOException {
        print("" + i);
    }

    public void print(long l) throws IOException {
        print("" + l);
    }

    public void print(String s) throws IOException {
        write(s.getBytes());
    }

    public void println() throws IOException {
        println("");
    }

    public void println(boolean b) throws IOException {
        println("" + b);
    }

    public void println(char c) throws IOException {
        println("" + c);
    }

    public void println(double d) throws IOException {
        println("" + d);
    }

    public void println(float f) throws IOException {
        println("" + f);
    }

    public void println(int i) throws IOException {
        println("" + i);
    }

    public void println(long l) throws IOException {
        println("" + l);
    }

    public void println(String s) throws IOException {
        print(s + CR_LF);
    }
}
