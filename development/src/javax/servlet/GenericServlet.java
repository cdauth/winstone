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

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;

/**
 * The base class from which all servlets extend.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class GenericServlet implements Servlet, ServletConfig, Serializable
{
  private ServletConfig config;

  public GenericServlet() {}

  public String getInitParameter(String name) {return config.getInitParameter(name);}
  public Enumeration getInitParameterNames()  {return config.getInitParameterNames();}
  public ServletConfig getServletConfig()     {return this.config;}

  public void init(ServletConfig config) throws ServletException
  {
    this.config = config;
    init();
  }
  public void init() throws ServletException {}
  public void destroy() {}

  public ServletContext getServletContext() {return config.getServletContext();}
  public String getServletInfo()            {return "";}
  public String getServletName()            {return config.getServletName();}

  public void log(String msg) {config.getServletContext().log(msg);}
  public void log(String message, Throwable t) {config.getServletContext().log(message, t);}

  public abstract void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException;
}

 