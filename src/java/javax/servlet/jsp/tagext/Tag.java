/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public interface Tag extends JspTag {
    public static final int EVAL_BODY_INCLUDE = 1;
    public static final int EVAL_PAGE = 6;
    public static final int SKIP_BODY = 0;
    public static final int SKIP_PAGE = 5;

    public int doEndTag() throws JspException;

    public int doStartTag() throws JspException;

    public Tag getParent();

    public void release();

    public void setPageContext(PageContext pc);

    public void setParent(Tag t);

}
