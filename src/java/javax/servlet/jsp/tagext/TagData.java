/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class TagData implements Cloneable {
    public static final Object REQUEST_TIME_VALUE = new Object();

    private Map attributes;

    public TagData(Object[][] atts) {
        this.attributes = new Hashtable();
        if (atts != null)
            for (int n = 0; n < atts.length; n++)
                if (atts[n].length >= 2)
                    this.attributes.put(atts[n][0], atts[n][1]);
    }

    public TagData(Hashtable atts) {
        this.attributes = new Hashtable();

        if (atts != null)
            this.attributes.putAll(atts);
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Enumeration getAttributes() {
        return Collections.enumeration(this.attributes.keySet());
    }

    public String getId() {
        return getAttributeString(TagAttributeInfo.ID);
    }

    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public String getAttributeString(String name) {
        return (String) this.attributes.get(name);
    }
}
