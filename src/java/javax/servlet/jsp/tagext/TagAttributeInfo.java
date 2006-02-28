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
public class TagAttributeInfo {
    public static final String ID = "id";
    private String name;
    private String type;
    private boolean required;
    private boolean reqTime;
    private boolean fragment;

    public TagAttributeInfo(String name, boolean required, String type,
            boolean reqTime) {
        this(name, required, type, reqTime, false);
    }

    public TagAttributeInfo(String name, boolean required, String type,
            boolean reqTime, boolean fragment) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.reqTime = reqTime;
        this.fragment = fragment;
    }

    public static TagAttributeInfo getIdAttribute(TagAttributeInfo[] a) {
        for (int n = 0; n < a.length; n++)
            if ((a[n].getName() != null) && a[n].getName().equals(ID))
                return a[n];
        return null;
    }

    public String getName() {
        return this.name;
    }

    public String getTypeName() {
        return this.type;
    }

    public boolean canBeRequestTime() {
        return this.reqTime;
    }

    public boolean isFragment() {
        return this.fragment;
    }

    public boolean isRequired() {
        return this.required;
    }

    public String toString() {
        return this.getClass().getName() + " [name=" + name + "]";
    }
}
