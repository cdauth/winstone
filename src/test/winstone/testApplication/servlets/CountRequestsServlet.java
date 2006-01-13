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
package winstone.testApplication.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple test servlet that counts the number of times it has been requested,
 * and returns that number in the response.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class CountRequestsServlet extends HttpServlet {
    private int numberOfGets;

    public void init() {
        String offset = getServletConfig().getInitParameter("offset");
        numberOfGets = offset == null ? 0 : Integer.parseInt(offset);
    }

    /**
     * Get implementation - increments and shows the access count
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        numberOfGets++;
        ServletOutputStream out = response.getOutputStream();
        out.println("<html><body>This servlet has been accessed via GET "
                + numberOfGets + " times</body></html>");
        out.flush();
    }
}
