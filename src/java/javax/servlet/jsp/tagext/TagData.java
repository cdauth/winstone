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
