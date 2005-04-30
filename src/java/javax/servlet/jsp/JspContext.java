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
