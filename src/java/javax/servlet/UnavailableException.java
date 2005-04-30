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
package javax.servlet;

/**
 * Thrown if a servlet is permanently or temporarily unavailable
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class UnavailableException extends ServletException {
    private int seconds;

    private Servlet servlet;

    /**
     * @deprecated As of Java Servlet API 2.2, use UnavailableException(String,
     *             int) instead.
     */
    public UnavailableException(int seconds, Servlet servlet, String msg) {
        this(servlet, msg);
        this.seconds = (seconds <= 0 ? 0 : seconds);
    }

    /**
     * @deprecated As of Java Servlet API 2.2, use UnavailableException(String)
     *             instead.
     */
    public UnavailableException(Servlet servlet, String msg) {
        this(msg);
        this.servlet = servlet;
    }

    /**
     * Constructs a new exception with a descriptive message indicating that the
     * servlet is permanently unavailable.
     */
    public UnavailableException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception with a descriptive message indicating that the
     * servlet is temporarily unavailable and giving an estimate of how long it
     * will be unavailable.
     */
    public UnavailableException(String msg, int seconds) {
        this(msg);
        this.seconds = (seconds <= 0 ? 0 : seconds);
    }

    /**
     * @deprecated As of Java Servlet API 2.2, with no replacement. Returns the
     *             servlet that is reporting its unavailability.
     */
    public Servlet getServlet() {
        return this.servlet;
    }

    /**
     * Returns the number of seconds the servlet expects to be temporarily
     * unavailable.
     */
    public int getUnavailableSeconds() {
        return this.seconds;
    }

    /**
     * Returns a boolean indicating whether the servlet is permanently
     * unavailable.
     */
    public boolean isPermanent() {
        return this.seconds <= 0;
    }

}
