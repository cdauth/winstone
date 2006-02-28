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
public interface BodyTag extends IterationTag {
    public static final int EVAL_BODY_BUFFERED = 2;

    public void doInitBody() throws JspException;

    public void setBodyContent(BodyContent b);

    /**
     * @deprecated
     */
    public static final int EVAL_BODY_TAG = 2;
}
