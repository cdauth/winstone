
// Copyright (c) 2003 
package javax.servlet;

/**
 * Inteface definition for filter objects
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface Filter
{
  public void destroy();
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException;
  public void init(FilterConfig filterConfig) throws ServletException;
}

 