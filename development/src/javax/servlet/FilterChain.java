
// Copyright (c) 2003 
package javax.servlet;

/**
 * Interface def for chains of filters before invoking the resource.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface FilterChain
{
  public void doFilter(ServletRequest request, ServletResponse response)
              throws java.io.IOException, ServletException;
}

 