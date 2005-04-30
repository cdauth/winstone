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

import java.io.PrintWriter;
import java.io.PrintStream;

/**
 * Generic servlet exception
 * 
 * @author Rick Knowles
 */
public class ServletException extends java.lang.Exception {
    private Throwable rootCause;

    public ServletException() {
        super();
    }

    public ServletException(String message) {
        super(message);
    }

    public ServletException(String message, Throwable rootCause) {
        this(message);
        this.rootCause = rootCause;
    }

    public ServletException(Throwable rootCause) {
        this();
        this.rootCause = rootCause;
    }

    public Throwable getRootCause() {
        return this.rootCause;
    }

    public void printStackTrace(PrintWriter p) {
        if (this.rootCause != null)
            this.rootCause.printStackTrace(p);
        p.write("\n");
        super.printStackTrace(p);
    }

    public void printStackTrace(PrintStream p) {
        if (this.rootCause != null)
            this.rootCause.printStackTrace(p);
        p.println("\n");
        super.printStackTrace(p);
    }

    public void printStackTrace() {
        if (this.rootCause != null)
            this.rootCause.printStackTrace();
        super.printStackTrace();
    }
}
