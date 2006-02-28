/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp;

import java.io.Writer;
import java.util.Enumeration;

import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

/**
 * Base context class. Mainly useful for the push and pop body methods
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class JspContext {
    public JspContext() {
    }

    public abstract Object findAttribute(String name);

    public abstract Object getAttribute(String name);

    public abstract Object getAttribute(String name, int scope);

    public abstract Enumeration getAttributeNamesInScope(int scope);

    public abstract int getAttributesScope(String name);

    public abstract ExpressionEvaluator getExpressionEvaluator();

    public abstract JspWriter getOut();

    public abstract VariableResolver getVariableResolver();

    public JspWriter popBody() {
        return null;
    }

    public JspWriter pushBody(Writer writer) {
        return null;
    }

    public abstract void removeAttribute(java.lang.String name);

    public abstract void removeAttribute(java.lang.String name, int scope);

    public abstract void setAttribute(String name, Object value);

    public abstract void setAttribute(String name, Object value, int scope);

}
