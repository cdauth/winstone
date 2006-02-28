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
public class TagAdapter implements Tag {
    private SimpleTag adaptee;
    private Tag parent;

    public TagAdapter(SimpleTag adaptee) {
        if (adaptee == null)
            throw new IllegalArgumentException("Null adaptee tag");
        this.adaptee = adaptee;
    }

    public int doEndTag() throws JspException {
        throw new UnsupportedOperationException();
    }

    public int doStartTag() throws JspException {
        throw new UnsupportedOperationException();
    }

    public JspTag getAdaptee() {
        return this.adaptee;
    }

    public Tag getParent() {
        if (this.parent == null) {
            JspTag adapteesParent = adaptee.getParent();
            if (adapteesParent instanceof Tag)
                this.parent = (Tag) adapteesParent;
            else if (adapteesParent != null)
                this.parent = new TagAdapter((SimpleTag) adapteesParent);
        }
        return this.parent;
    }

    public void release() {
        throw new UnsupportedOperationException();
    }

    public void setPageContext(PageContext pc) {
        throw new UnsupportedOperationException();
    }

    public void setParent(Tag parentTag) {
        throw new UnsupportedOperationException();
    }

}
