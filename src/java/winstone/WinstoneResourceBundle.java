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

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A ResourceBundle that includes the ability to do string replacement on the
 * resources it retrieves.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class WinstoneResourceBundle {
    private ResourceBundle resources;

    /**
     * Constructor
     */
    public WinstoneResourceBundle(String baseName) {
        this.resources = ResourceBundle.getBundle(baseName);
    }

    public WinstoneResourceBundle(String baseName, Locale loc) {
        this.resources = ResourceBundle.getBundle(baseName, loc);
    }

    public WinstoneResourceBundle(String baseName, Locale loc, ClassLoader cl) {
        this.resources = ResourceBundle.getBundle(baseName, loc, cl);
    }

    /**
     * Default getString method
     */
    public String getString(String key) {
        return this.resources.getString(key);
    }

    /**
     * Perform a string replace for a single from/to pair.
     */
    public String getString(String key, String parameter) {
        return getString(key, new String[] { parameter });
    }

    /**
     * Perform a string replace for a double from/to pair.
     */
    public String getString(String key, String[] parameters) {
        String myCopy = this.resources.getString(key);
        if (parameters != null)
            for (int n = 0; n < parameters.length; n++)
                myCopy = globalReplace(myCopy, "[#" + n + "]", parameters[n]);
        return myCopy;
    }

    /**
     * Just does a string swap, replacing occurrences of from with to.
     */
    public static String globalReplace(String input, String fromMarker,
            String toValue) {
        if (input == null) {
            return null;
        } else if (fromMarker == null) {
            return input;
        }

        StringBuffer out = new StringBuffer();
        int index = 0;
        int foundAt = input.indexOf(fromMarker, index);
        while (foundAt != -1) {
            out.append(input.substring(index, foundAt));
            out.append(toValue);
            index = foundAt + fromMarker.length();
            foundAt = input.indexOf(fromMarker, index);
        }
        out.append(input.substring(index));
        return out.toString();
    }
    
    public static String globalReplace(String input, String parameters[][]) {
        if (parameters != null)
            for (int n = 0; n < parameters.length; n++)
                input = globalReplace(input, parameters[n][0], parameters[n][1]);
        return input;
    }
}
