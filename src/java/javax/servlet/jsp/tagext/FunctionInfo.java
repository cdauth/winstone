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
public class FunctionInfo {
    private String name;
    private String className;
    private String signature;

    public FunctionInfo(String name, String className, String signature) {
        this.name = name;
        this.className = className;
        this.signature = signature;
    }

    public String getFunctionClass() {
        return this.className;
    }

    public String getFunctionSignature() {
        return this.signature;
    }

    public String getName() {
        return this.name;
    }
}
