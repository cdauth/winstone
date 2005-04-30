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

import java.util.Hashtable;
import javax.servlet.ServletInputStream;
import java.util.StringTokenizer;

/**
 * Generic utility functions for use in the servlet container.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class HttpUtils {
    /**
     * @deprecated Reconstructs the URL the client used to make the request,
     *             using information in the HttpServletRequest object.
     */
    public static StringBuffer getRequestURL(HttpServletRequest req) {
        return req.getRequestURL();
    }

    /**
     * @deprecated Parses data from an HTML form that the client sends to the
     *             server using the HTTP POST method and the
     *             application/x-www-form-urlencoded MIME type.
     */
    public static Hashtable parsePostData(int len, ServletInputStream in) {
        try {
            byte body[] = new byte[len];
            int startPos = 0;
            int readChars = in.read(body, startPos, len - startPos);
            while ((readChars != -1) && (len != startPos)) {
                startPos += readChars;
                readChars = in.read(body, startPos, len - startPos);
            }
            if (len != startPos)
                throw new java.io.IOException("Stream ended early");
            else {
                String post = new String(body, 0, len, "8859_1");
                return parseQueryString(post);
            }
        } catch (java.io.IOException err) {
            throw new IllegalArgumentException("Error parsing request body - "
                    + err.getMessage());
        }
    }

    /**
     * @deprecated Parses a query string passed from the client to the server
     *             and builds a HashTable object with key-value pairs.
     */
    public static Hashtable parseQueryString(String urlEncodedParams) {
        Hashtable params = new Hashtable();
        StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int equalPos = token.indexOf('=');
            if (equalPos == -1)
                continue;
            String name = token.substring(0, equalPos);
            String value = token.substring(equalPos + 1);
            String decodedName = decodeURLToken(name);
            String decodedValue = decodeURLToken(value);

            Object already = params.get(decodedName);
            if (already == null)
                params.put(decodedName, new String[] { decodedValue });
            else if (already instanceof String[]) {
                String alreadyArray[] = (String[]) already;
                String oneMore[] = new String[alreadyArray.length + 1];
                System.arraycopy(alreadyArray, 0, oneMore, 0,
                        alreadyArray.length);
                oneMore[oneMore.length - 1] = decodedValue;
                params.put(decodedName, oneMore);
            }
        }
        return params;
    }

    private static String decodeURLToken(String in) {
        StringBuffer workspace = new StringBuffer();
        for (int n = 0; n < in.length(); n++) {
            char thisChar = in.charAt(n);
            if (thisChar == '+')
                workspace.append(' ');
            else if (thisChar == '%') {
                int decoded = Integer.parseInt(in.substring(n + 1, n + 3), 16);
                workspace.append((char) decoded);
                n += 2;
            } else
                workspace.append(thisChar);
        }
        return workspace.toString();
    }
}
