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

/**
 * An object addition or removal notification
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class HttpSessionBindingEvent extends HttpSessionEvent
{
  private String name;
  private Object value;

  public HttpSessionBindingEvent(HttpSession session, String name)
  {
    super(session);
    this.name = name;
  }

  public HttpSessionBindingEvent(HttpSession session, String name, Object value)
  {
    this(session, name);
    this.value = value;
  }

  public String getName()   {return this.name;}
  public Object getValue()  {return this.value;}

}

 