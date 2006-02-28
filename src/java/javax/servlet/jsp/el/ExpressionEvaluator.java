/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.el;

/**
 * Used by EL library
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class ExpressionEvaluator {
    public abstract Expression parseExpression(String expression,
            Class expectedType, FunctionMapper fMapper) throws ELException;

    public abstract Object evaluate(String expression, Class expectedType,
            VariableResolver vResolver, FunctionMapper fMapper)
            throws ELException;
}
