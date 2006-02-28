/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import javax.servlet.jsp.*;
import java.io.*;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class SimpleTagSupport implements SimpleTag {
    private JspTag parent;
    private JspContext context;
    private JspFragment body;

    public void doTag() throws JspException, IOException {
    }

    public JspTag getParent() {
        return this.parent;
    }

    public void setParent(JspTag parent) {
        this.parent = parent;
    }

    protected JspContext getJspContext() {
        return this.context;
    }

    public void setJspContext(JspContext pc) {
        this.context = pc;
    }

    protected JspFragment getJspBody() {
        return this.body;
    }

    public void setJspBody(JspFragment jspBody) {
        this.body = jspBody;
    }

    public static final JspTag findAncestorWithClass(JspTag from,
            Class searchClass) {
        if ((from == null) || (searchClass == null))
            return null;

        // Check if className is a subclass of Tag
        else if (JspTag.class.isAssignableFrom(searchClass))
            return iterateParentTags(from, searchClass, false);
        else if (searchClass.isInterface())
            return iterateParentTags(from, searchClass, true);
        else
            return null;
    }

    private static final JspTag iterateParentTags(JspTag from,
            Class searchClass, boolean isSearchClassAnInterface) {
        JspTag parentTag = from;

        while (parentTag != null) {
            parentTag = (parentTag instanceof SimpleTag ? ((SimpleTag) parentTag)
                    .getParent()
                    : (parentTag instanceof Tag ? ((Tag) parentTag).getParent()
                            : null));
            if (parentTag instanceof TagAdapter)
                parentTag = ((TagAdapter) parentTag).getAdaptee();

            if (isSearchClassAnInterface && searchClass.isInstance(parentTag))
                return parentTag;
            else if (searchClass.isAssignableFrom(parentTag.getClass()))
                return parentTag;
        }
        return null;
    }
}
