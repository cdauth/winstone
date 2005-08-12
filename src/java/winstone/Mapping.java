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
package winstone;

/**
 * Encapsulates the parsing of URL patterns, as well as the mapping of a 
 * url pattern to a servlet instance
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Mapping implements java.util.Comparator {
    public static final int EXACT_PATTERN = 1;
    public static final int FOLDER_PATTERN = 2;
    public static final int EXTENSION_PATTERN = 3;
    public static final int DEFAULT_SERVLET = 4;

    public static final String STAR = "*";
    public static final String SLASH = "/";

    private String urlPattern;
    private String linkName; // used to map filters to a specific servlet by
                             // name
    private String mappedTo;
    private int patternType;
    private boolean isPatternFirst; // ie is this a blah* pattern, not *blah
                                    // (extensions only)

    private WinstoneResourceBundle resources;

    protected Mapping(String mappedTo, WinstoneResourceBundle resources) {
        this.mappedTo = mappedTo;
        this.resources = resources;
    }

    /**
     * Factory constructor method - this parses the url pattern into pieces we can use to match
     * against incoming URLs.
     */
    public static Mapping createFromURL(String mappedTo, String pattern,
            WinstoneResourceBundle resources) {
        if ((pattern == null) || (mappedTo == null))
            throw new WinstoneException(resources.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        Mapping me = new Mapping(mappedTo, resources);

        int firstStarPos = pattern.indexOf(STAR);
        int lastStarPos = pattern.lastIndexOf(STAR);
        int patternLength = pattern.length();

        if (firstStarPos == -1) {
            me.urlPattern = pattern;
            me.patternType = EXACT_PATTERN;
        }

        // > 1 star = error
        else if (firstStarPos != lastStarPos)
            throw new WinstoneException(resources.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        // check for default servlet, ie mapping = exactly /*
        else if (pattern.equals(SLASH + STAR) || pattern.equals(SLASH)) {
            me.urlPattern = "";
            me.patternType = DEFAULT_SERVLET;
        }

        // check for folder style mapping (ends in /*)
        else if (pattern.indexOf(SLASH + STAR) == (patternLength - (SLASH + STAR).length())) {
            me.urlPattern = pattern.substring(0, pattern.length()
                    - (SLASH + STAR).length());
            me.patternType = FOLDER_PATTERN;
        }
        
        // check for non-extension match
        else if (pattern.indexOf(SLASH) != -1)
            throw new WinstoneException(resources.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        // check for extension match at the beginning (eg *blah)
        else if (firstStarPos == 0) {
            me.urlPattern = pattern.substring(STAR.length());
            me.patternType = EXTENSION_PATTERN;
            me.isPatternFirst = false;
        }
        // check for extension match at the end (eg blah*)
        else if (firstStarPos == (patternLength - STAR.length())) {
            me.urlPattern = pattern.substring(0, patternLength - STAR.length());
            me.patternType = EXTENSION_PATTERN;
            me.isPatternFirst = true;
        } else
            throw new WinstoneException(resources.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        Logger.log(Logger.FULL_DEBUG, resources, "Mapping.MappedPattern",
                new String[] { mappedTo, pattern });
        return me;
    }

    /**
     * Factory constructor method - this turns a servlet name into a mapping element
     */
    public static Mapping createFromLink(String mappedTo, String linkName,
            WinstoneResourceBundle resources) {
        if ((linkName == null) || (mappedTo == null))
            throw new WinstoneException(resources.getString(
                    "Mapping.InvalidLink", new String[] { mappedTo, linkName }));

        Mapping me = new Mapping(mappedTo, resources);
        me.linkName = linkName;
        return me;
    }

    public int getPatternType() {
        return this.patternType;
    }

    public String getUrlPattern() {
        return this.urlPattern;
    }

    public String getMappedTo() {
        return this.mappedTo;
    }

    public String getLinkName() {
        return this.linkName;
    }

    /**
     * Try to match this pattern against the incoming url
     * 
     * @param inputPattern The URL we want to check for a match
     * @param servletPath An empty stringbuffer for the servletPath of a successful match
     * @param pathInfo An empty stringbuffer for the pathInfo of a successful match
     * @return true if the match is successful
     */
    public boolean match(String inputPattern, StringBuffer servletPath,
            StringBuffer pathInfo) {
        // Logger.log(Logger.FULL_DEBUG, "Matching input=" + inputPattern + "
        // me=" + toString());
        switch (this.patternType) {
        case FOLDER_PATTERN:
            if (inputPattern.startsWith(this.urlPattern)) {
                if (servletPath != null)
                    servletPath.append(this.urlPattern);
                if (pathInfo != null)
                    pathInfo.append(inputPattern.substring(this.urlPattern
                            .length()));
                return true;
            } else
                return false;

        case EXTENSION_PATTERN:
            // Strip down to the last item in the path
            int slashPos = inputPattern.lastIndexOf(SLASH);
            if ((slashPos == -1) || (slashPos == inputPattern.length() - 1))
                return false;
            String fileName = inputPattern.substring(slashPos + 1);
            if ((this.isPatternFirst && fileName.startsWith(this.urlPattern))
                    || (!this.isPatternFirst && fileName
                            .endsWith(this.urlPattern))) {
                if (servletPath != null)
                    servletPath.append(inputPattern);
                return true;
            } else
                return false;

        case EXACT_PATTERN:
            if (inputPattern.equals(this.urlPattern)) {
                if (servletPath != null)
                    servletPath.append(inputPattern);
                return true;
            } else
                return false;

        case DEFAULT_SERVLET:
            if (pathInfo != null)
                pathInfo.append(inputPattern);
            return true;

        default:
            return false;
        }
    }

    /**
     * Used to compare two url patterns. Always sorts so that lowest pattern
     * type then longest path come first.
     */
    public int compare(Object objOne, Object objTwo) {
        Mapping one = (Mapping) objOne;
        Mapping two = (Mapping) objTwo;

        Integer intOne = new Integer(one.getPatternType());
        Integer intTwo = new Integer(two.getPatternType());
        int order = -1 * intOne.compareTo(intTwo);
        if (order != 0) {
            return order;
        }
        if (one.getLinkName() != null) {
            // servlet name mapping - just alphabetical sort
            return one.getLinkName().compareTo(two.getLinkName());
        } else {
            return -1 * one.getUrlPattern().compareTo(two.getUrlPattern());
        }
    }

    public String toString() {
        return this.linkName != null ? "Link:" + this.linkName
                : "URLPattern:type=" + this.patternType + ",pattern="
                        + this.urlPattern;
    }
}
