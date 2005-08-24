/*
 * Generator Runtime Servlet Framework
 * Copyright (C) 2004 Rick Knowles
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package winstone.filters.gzip;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A filter that checks if the request will accept a gzip encoded response, and
 * if so wraps the response in a gzip encoding response wrapper.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class GzipFilter implements Filter {

    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    
    private Integer minimumSizeForCompression;
    private ServletContext context;
    
    public void init(FilterConfig config) throws ServletException {
        String minimumSizeForCompressionStr = config.getInitParameter("minimumSizeForCompression");
        if ((minimumSizeForCompressionStr != null) && 
            (minimumSizeForCompressionStr.equals(""))) {
            this.minimumSizeForCompression = new Integer(minimumSizeForCompressionStr);
        }
        this.context = config.getServletContext();
    }

    public void destroy() {
        this.minimumSizeForCompression = null;
        this.context = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        Enumeration headers = ((HttpServletRequest) request).getHeaders(ACCEPT_ENCODING);
        boolean acceptsGzipEncoding = false;
        while (headers.hasMoreElements() && !acceptsGzipEncoding) {
            acceptsGzipEncoding = (((String) headers.nextElement()).indexOf("gzip") != -1);
        }
        if (acceptsGzipEncoding) {
            GzipResponseWrapper encodedResponse = new GzipResponseWrapper(
                    (HttpServletResponse) response, this.context,
                    this.minimumSizeForCompression); 
            chain.doFilter(request, encodedResponse);
            encodedResponse.close();
        } else {
            chain.doFilter(request, response);
        }
    }
}
