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
package javax.servlet.jsp.tagext;

import javax.servlet.jsp.*;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public interface Tag extends JspTag
{
  public static final int EVAL_BODY_INCLUDE = 1;
  public static final int EVAL_PAGE = 6;
  public static final int SKIP_BODY = 0;
  public static final int SKIP_PAGE = 5;
  
  public int doEndTag() throws JspException;
  public int doStartTag() throws JspException;
  public Tag getParent();
  public void release();
  public void setPageContext(PageContext pc);
  public void setParent(Tag t);

}
