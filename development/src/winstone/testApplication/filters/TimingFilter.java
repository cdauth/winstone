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
package winstone.testApplication.filters;

import java.io.IOException;
import java.util.*;

import javax.servlet.*;

/**
 * Simple timing and request dumping test filter
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class TimingFilter implements Filter
{
  private boolean dumpRequestParams;
  private ServletContext context;
  
  public void init(FilterConfig config)
  {
    String dumpRequestParams = config.getInitParameter("dumpRequestParameters");
    this.dumpRequestParams = ((dumpRequestParams != null) && dumpRequestParams.equalsIgnoreCase("true"));
    this.context = config.getServletContext();
  }
  
  public void destroy() {this.context = null;}
  
  /**
   * Times the execution of the rest of the filter chain, optionally dumping
   * the request parameters to the servlet context log
   */
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    if (this.dumpRequestParams)
      for (Enumeration paramNames = request.getParameterNames(); paramNames.hasMoreElements(); )
      {
        String name = (String) paramNames.nextElement();
        this.context.log("Request parameter: " + name + "=" + request.getParameter(name));
      }
    
    long startTime = System.currentTimeMillis();
    chain.doFilter(request, response);
    this.context.log("Filter chain executed in " + (System.currentTimeMillis() - startTime) + "ms");
  }
}
