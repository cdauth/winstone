
// Copyright (c) 2003 
package javax.servlet;

/**
 * Configuration for filter objects.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface FilterConfig
{
  public String getFilterName();
  public String getInitParameter(String name);
  public java.util.Enumeration getInitParameterNames();
  public ServletContext getServletContext();
}

 