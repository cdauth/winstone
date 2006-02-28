/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class BodyTagSupport extends TagSupport implements BodyTag {
    protected BodyContent bodyContent;

    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {
        return super.doEndTag();
    }

    public void doInitBody() throws JspException {
    }

    public int doAfterBody() throws JspException {
        return super.doAfterBody();
    }

    public BodyContent getBodyContent() {
        return this.bodyContent;
    }

    public JspWriter getPreviousOut() {
        return this.bodyContent.getEnclosingWriter();
    }

    public void release() {
        super.release();
    }

    public void setBodyContent(BodyContent b) {
        this.bodyContent = b;
    }
}
