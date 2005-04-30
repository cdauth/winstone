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
