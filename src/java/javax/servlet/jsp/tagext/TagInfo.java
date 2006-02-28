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
public class TagInfo {
    public static final String BODY_CONTENT_EMPTY = "EMPTY";
    public static final String BODY_CONTENT_JSP = "JSP";
    public static final String BODY_CONTENT_SCRIPTLESS = "SCRIPTLESS";
    public static final String BODY_CONTENT_TAG_DEPENDENT = "TAGDEPENDENT";

    private String tagName;
    private String tagClassName;
    private String bodycontent;
    private String infoString;
    private TagLibraryInfo taglib;
    private TagExtraInfo tagExtraInfo;
    private TagAttributeInfo[] attributeInfo;
    private String displayName;
    private String smallIcon;
    private String largeIcon;
    private TagVariableInfo[] tvi;
    private boolean dynamicAttributes;

    public TagInfo(String tagName, String tagClassName, String bodycontent,
            String infoString, TagLibraryInfo taglib,
            TagExtraInfo tagExtraInfo, TagAttributeInfo[] attributeInfo) {
        this(tagName, tagClassName, bodycontent, infoString, taglib,
                tagExtraInfo, attributeInfo, null, null, null,
                (TagVariableInfo[]) null);
    }

    public TagInfo(String tagName, String tagClassName, String bodycontent,
            String infoString, TagLibraryInfo taglib,
            TagExtraInfo tagExtraInfo, TagAttributeInfo[] attributeInfo,
            String displayName, String smallIcon, String largeIcon,
            TagVariableInfo[] tvi) {
        this(tagName, tagClassName, bodycontent, infoString, taglib,
                tagExtraInfo, attributeInfo, displayName, smallIcon, largeIcon,
                tvi, false);
    }

    public TagInfo(String tagName, String tagClassName, String bodycontent,
            String infoString, TagLibraryInfo taglib,
            TagExtraInfo tagExtraInfo, TagAttributeInfo[] attributeInfo,
            String displayName, String smallIcon, String largeIcon,
            TagVariableInfo[] tvi, boolean dynamicAttributes) {
        this.tagName = tagName;
        this.tagClassName = tagClassName;
        this.bodycontent = bodycontent;
        this.infoString = infoString;
        this.taglib = taglib;
        this.attributeInfo = attributeInfo;
        this.displayName = displayName;
        this.smallIcon = smallIcon;
        this.largeIcon = largeIcon;
        this.tvi = tvi;
        this.dynamicAttributes = dynamicAttributes;

        this.tagExtraInfo = tagExtraInfo;
        if (tagExtraInfo != null)
            tagExtraInfo.setTagInfo(this);
    }

    public TagAttributeInfo[] getAttributes() {
        return this.attributeInfo;
    }

    public String getBodyContent() {
        return this.bodycontent;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getInfoString() {
        return this.infoString;
    }

    public String getLargeIcon() {
        return this.largeIcon;
    }

    public String getSmallIcon() {
        return this.smallIcon;
    }

    public String getTagClassName() {
        return this.tagClassName;
    }

    public TagExtraInfo getTagExtraInfo() {
        return this.tagExtraInfo;
    }

    public TagLibraryInfo getTagLibrary() {
        return this.taglib;
    }

    public String getTagName() {
        return this.tagName;
    }

    public TagVariableInfo[] getTagVariableInfos() {
        return this.tvi;
    }

    public boolean hasDynamicAttributes() {
        return this.dynamicAttributes;
    }

    public VariableInfo[] getVariableInfo(TagData data) {
        TagExtraInfo tei = getTagExtraInfo();
        return (tei == null ? null : tei.getVariableInfo(data));
    }

    public boolean isValid(TagData data) {
        TagExtraInfo tei = getTagExtraInfo();
        return (tei == null ? true : tei.isValid(data));
    }

    public void setTagExtraInfo(TagExtraInfo tei) {
        this.tagExtraInfo = tei;
    }

    public void setTagLibrary(TagLibraryInfo tl) {
        this.taglib = tl;
    }

    public ValidationMessage[] validate(TagData data) {
        TagExtraInfo tei = getTagExtraInfo();
        return (tei == null ? null : tei.validate(data));
    }
}
