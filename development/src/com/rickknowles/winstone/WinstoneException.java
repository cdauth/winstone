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
package com.rickknowles.winstone;

import java.io.*;

/**
 * Master exception within the servlet container. This is thrown whenever a
 * non-recoverable error occurs that we want to throw to the top of the application.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneException extends RuntimeException
{
  private Throwable nestedError = null;

  /**
   * Create an exception with a useful message for the
   * system administrator.
   * @param pMsg Error message for to be used for administrative troubleshooting   */
  public WinstoneException(String pMsg)
  {
    super(pMsg);
  }

  /**
   * Create an exception with a useful message for the
   * system administrator and a nested throwable object.
   * @param pMsg Error message for administrative troubleshooting
   * @param pError The actual exception that occurred
   */
  public WinstoneException(String pMsg, Throwable pError)
  {
    super (pMsg);
    this.setNestedError(pError);
  }

  /**
   * Get the nested error or exception
   * @return The nested error or exception
   */
  public Throwable getNestedError(){return this.nestedError;}

  /**
   * Set the nested error or exception
   * @param pError The nested error or exception
   */
  private void setNestedError(Throwable pError){this.nestedError = pError;}

  public void printStackTrace(PrintWriter p)
  {
    if (this.nestedError != null)
      this.nestedError.printStackTrace(p);
    p.write("\n");
    super.printStackTrace(p);
  }

  public void printStackTrace(PrintStream p)
  {
    if (this.nestedError != null)
      this.nestedError.printStackTrace(p);
    p.println("\n");
    super.printStackTrace(p);
  }

  public void printStackTrace()
  {
    if (this.nestedError != null)
      this.nestedError.printStackTrace();
    super.printStackTrace();
  }
}

 