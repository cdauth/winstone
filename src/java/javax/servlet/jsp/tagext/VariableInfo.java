/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class VariableInfo {
    public static final int NESTED = 0;
    public static final int AT_BEGIN = 1;
    public static final int AT_END = 2;

    private String varName;
    private String className;
    private boolean declare;
    private int scope;

    public VariableInfo(String varName, String className, boolean declare,
            int scope) {
        this.varName = varName;
        this.className = className;
        this.declare = declare;
        this.scope = scope;
    }

    public String getClassName() {
        return this.className;
    }

    public boolean getDeclare() {
        return this.declare;
    }

    public int getScope() {
        return this.scope;
    }

    public String getVarName() {
        return this.varName;
    }
}
