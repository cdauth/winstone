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
public class TagFileInfo {
    private String name;

    private String path;

    private TagInfo tagInfo;

    public TagFileInfo(String name, String path, TagInfo tagInfo) {
        this.name = name;
        this.path = path;
        this.tagInfo = tagInfo;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public TagInfo getTagInfo() {
        return this.tagInfo;
    }

}
