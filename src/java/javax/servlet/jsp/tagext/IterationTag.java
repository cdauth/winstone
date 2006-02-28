/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspException;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public interface IterationTag extends Tag {
    public static final int EVAL_BODY_AGAIN = 2;

    public int doAfterBody() throws JspException;
}
