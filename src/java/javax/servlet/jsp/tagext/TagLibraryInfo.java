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
public abstract class TagLibraryInfo {
    protected FunctionInfo[] functions;
    protected String info;
    protected String jspversion;
    protected String prefix;
    protected String shortname;
    protected TagFileInfo[] tagFiles;
    protected TagInfo[] tags;
    protected String tlibversion;
    protected String uri;
    protected String urn;

    protected TagLibraryInfo(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
    }

    public FunctionInfo getFunction(String name) {
        FunctionInfo[] functions = getFunctions();
        if (functions != null)
            for (int n = 0; n < functions.length; n++)
                if ((functions[n].getName() != null)
                        && functions[n].getName().equals(name))
                    return functions[n];
        return null;
    }

    public FunctionInfo[] getFunctions() {
        return this.functions;
    }

    public String getInfoString() {
        return this.info;
    }

    public String getPrefixString() {
        return this.prefix;
    }

    public String getReliableURN() {
        return this.urn;
    }

    public String getRequiredVersion() {
        return this.jspversion;
    }

    public String getShortName() {
        return this.shortname;
    }

    public TagInfo getTag(String shortname) {
        TagInfo[] tags = getTags();
        if (tags != null)
            for (int n = 0; n < tags.length; n++)
                if ((tags[n].getTagName() != null)
                        && tags[n].getTagName().equals(shortname))
                    return tags[n];
        return null;
    }

    public TagFileInfo getTagFile(String shortname) {
        TagFileInfo[] tagFiles = getTagFiles();
        if (tagFiles != null)
            for (int n = 0; n < tagFiles.length; n++)
                if ((tagFiles[n].getName() != null)
                        && tagFiles[n].getName().equals(shortname))
                    return tagFiles[n];
        return null;
    }

    public TagFileInfo[] getTagFiles() {
        return this.tagFiles;
    }

    public TagInfo[] getTags() {
        return this.tags;
    }

    public String getURI() {
        return this.uri;
    }

}
