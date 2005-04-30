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
package javax.servlet.http;

/**
 * Cookie model value object
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class Cookie implements Cloneable {
    private String name;
    private String value;
    private String comment;
    private String domain;
    private String path;
    private boolean secure;
    private int maxAge;
    private int version;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Object clone() {
        Cookie clone = new Cookie(this.name, this.value);
        clone.setComment(this.comment);
        clone.setDomain(this.domain);
        clone.setMaxAge(this.maxAge);
        clone.setSecure(this.secure);
        clone.setVersion(this.version);
        return clone;
    }

    public String getComment() {
        return this.comment;
    }

    public String getDomain() {
        return this.domain;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public boolean getSecure() {
        return this.secure;
    }

    public String getValue() {
        return this.value;
    }

    public int getVersion() {
        return this.version;
    }

    public void setComment(String purpose) {
        this.comment = purpose;
    }

    public void setDomain(String pattern) {
        this.domain = pattern;
    }

    public void setMaxAge(int expiry) {
        this.maxAge = expiry;
    }

    public void setPath(String uri) {
        this.path = uri;
    }

    public void setSecure(boolean flag) {
        this.secure = flag;
    }

    public void setValue(String newValue) {
        this.value = newValue;
    }

    public void setVersion(int v) {
        this.version = v;
    }
}
