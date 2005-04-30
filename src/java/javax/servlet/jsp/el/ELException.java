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
package javax.servlet.jsp.el;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ELException extends Exception {
    private Throwable rootCause;

    public ELException() {
        super();
    }

    public ELException(String msg) {
        super(msg);
    }

    public ELException(String msg, Throwable rootCause) {
        this(msg);
        this.rootCause = rootCause;
    }

    public ELException(Throwable rootCause) {
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
