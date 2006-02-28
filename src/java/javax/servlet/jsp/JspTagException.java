/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp;

/**
 * Exception specific to jsp tag failures
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class JspTagException extends JspException {
    public JspTagException() {
        super();
    }

    public JspTagException(String msg) {
        super(msg);
    }

    public JspTagException(String msg, Throwable rootCause) {
        super(msg, rootCause);
    }

    public JspTagException(Throwable rootCause) {
        super(rootCause);
    }
}
