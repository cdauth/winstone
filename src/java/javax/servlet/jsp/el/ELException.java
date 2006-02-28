/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
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
