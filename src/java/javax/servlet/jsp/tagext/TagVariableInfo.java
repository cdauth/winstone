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
public class TagVariableInfo {
    private String nameGiven;
    private String nameFromAttribute;
    private String className;
    private boolean declare;
    private int scope;

    public TagVariableInfo(String nameGiven, String nameFromAttribute,
            String className, boolean declare, int scope) {
        this.nameGiven = nameGiven;
        this.nameFromAttribute = nameFromAttribute;
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

    public String getNameGiven() {
        return this.nameGiven;
    }

    public String getNameFromAttribute() {
        return this.nameFromAttribute;
    }
}
