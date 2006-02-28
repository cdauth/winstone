/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class TagSupport implements IterationTag, Serializable {
    protected String id;
    protected PageContext pageContext;
    private Map attributes;
    private Tag parentTag;

    /**
     * Tricky one. This checks for the parent/ancestor tag that closest matches
     * the class name specified
     */
    public static final Tag findAncestorWithClass(Tag from, Class searchClass) {
        if ((from == null) || (searchClass == null))
            return null;

        // Check if className is a subclass of Tag
        else if (Tag.class.isAssignableFrom(searchClass))
            return iterateParentTags(from, searchClass, false);
        else if (searchClass.isInterface())
            return iterateParentTags(from, searchClass, true);
        else
            return null;
    }

    private static final Tag iterateParentTags(Tag from, Class searchClass,
            boolean isSearchClassAnInterface) {
        Tag parentTag = from.getParent();
        while (parentTag != null) {
            if (isSearchClassAnInterface && searchClass.isInstance(parentTag))
                return parentTag;
            else if (searchClass.isAssignableFrom(parentTag.getClass()))
                return parentTag;
            else
                parentTag = parentTag.getParent();
        }
        return null;
    }

    public TagSupport() {
        this.attributes = new Hashtable();
    }

    public int doAfterBody() throws JspException {
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    public int doStartTag() throws JspException {
        return SKIP_BODY;
    }

    public Object getValue(String name) {
        return this.attributes.get(name);
    }

    public void removeValue(String name) {
        this.attributes.remove(name);
    }

    public void setValue(String name, Object value) {
        this.attributes.put(name, value);
    }

    public Enumeration getValues() {
        return Collections.enumeration(this.attributes.keySet());
    }

    public void release() {
        this.attributes.clear();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setPageContext(PageContext pageContext) {
        this.pageContext = pageContext;
    }

    public void setParent(Tag parentTag) {
        this.parentTag = parentTag;
    }

    public Tag getParent() {
        return this.parentTag;
    }

}
