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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

/**
 * A simple servlet that writes out the body of the error 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class ErrorServlet extends HttpServlet {
    
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        
        Integer sc = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String msg = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Throwable err = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (err != null) {
            err.printStackTrace(pw);
        } else {
            pw.println("(none)");
        }
        pw.flush();
         
        // If we are here there was no error servlet, so show the default error page
        String output = Launcher.RESOURCES.getString("WinstoneResponse.ErrorPage",
                new String[] { sc + "", (msg == null ? "" : msg), sw.toString(),
                Launcher.RESOURCES.getString("ServerVersion"),
                        "" + new Date() });
        response.setContentLength(output.getBytes(response.getCharacterEncoding()).length);
        Writer out = response.getWriter();
        out.write(output);
        out.flush();
    }
}
