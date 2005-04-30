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
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used to test the unavailable exception processing
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class UnavailableServlet extends HttpServlet {
    protected boolean errorAtInit;

    public void init() throws ServletException {
        String errorTime = getServletConfig().getInitParameter("errorTime");
        this.errorAtInit = ((errorTime == null) || errorTime.equals("init"));
        if (this.errorAtInit)
            throw new UnavailableException(
                    "Error thrown deliberately during init");
    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        if (!this.errorAtInit)
            throw new UnavailableException(
                    "Error thrown deliberately during get");

        Writer out = response.getWriter();
        out
                .write("This should not be shown, because we've thrown unavailable exceptions");
        out.close();
    }

}
