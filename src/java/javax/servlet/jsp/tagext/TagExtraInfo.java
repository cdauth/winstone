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
public abstract class TagExtraInfo {
    private TagInfo tagInfo;

    public final TagInfo getTagInfo() {
        return this.tagInfo;
    }

    public VariableInfo[] getVariableInfo(TagData data) {
        return new VariableInfo[] {};
    }

    public boolean isValid(TagData data) {
        return true;
    }

    public final void setTagInfo(TagInfo tagInfo) {
        this.tagInfo = tagInfo;
    }

    public ValidationMessage[] validate(TagData data) {
        return (isValid(data) ? null
                : new ValidationMessage[] { new ValidationMessage(data.getId(),
                        "failed isValid") });
    }

}
