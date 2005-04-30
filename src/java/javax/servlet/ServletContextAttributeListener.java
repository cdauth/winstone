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
 * Listens for changes to the context attributes.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletContextAttributeListener extends
        java.util.EventListener {
    public void attributeAdded(ServletContextAttributeEvent scab);

    public void attributeRemoved(ServletContextAttributeEvent scab);

    public void attributeReplaced(ServletContextAttributeEvent scab);
}
