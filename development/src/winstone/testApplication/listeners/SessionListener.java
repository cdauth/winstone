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
package winstone.testApplication.listeners;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class SessionListener implements HttpSessionListener, HttpSessionAttributeListener,
      HttpSessionActivationListener
{
  public void sessionCreated(HttpSessionEvent se)
    {se.getSession().getServletContext().log("Session Created - id=" + 
                                              se.getSession().getId());}

  public void sessionDestroyed(HttpSessionEvent se)
    {se.getSession().getServletContext().log("Session Destroyed - id=" + 
                                              se.getSession().getId());}

  public void attributeAdded(HttpSessionBindingEvent se)
    {se.getSession().getServletContext().log("Session Attribute added (session id=" + 
                                              se.getSession().getId() + ") " + 
                                              se.getName() + "=" + se.getValue());}

  public void attributeRemoved(HttpSessionBindingEvent se)
    {se.getSession().getServletContext().log("Session Attribute removed (session id=" + 
                                              se.getSession().getId() + ") " + 
                                              se.getName() + "=" + se.getValue());}

  public void attributeReplaced(HttpSessionBindingEvent se)
    {se.getSession().getServletContext().log("Session Attribute replaced (session id=" + 
                                              se.getSession().getId() + ") " + 
                                              se.getName() + "=" + se.getValue());}

  public void sessionDidActivate(HttpSessionEvent se)
    {se.getSession().getServletContext().log("Session activated - id=" + 
                                              se.getSession().getId());}

  public void sessionWillPassivate(HttpSessionEvent se)
    {se.getSession().getServletContext().log("Session passivating - id=" + 
                                              se.getSession().getId());}
}
