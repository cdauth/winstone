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
package winstone;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Wraps request for include/forward processing. Sets all the necessary 
 * attributes for dispatching under 2.4 spec conditions
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class DispatchRequestWrapper extends HttpServletRequestWrapper
{
  static final String INCLUDE_REQUEST_URI  = "javax.servlet.include.request_uri";
  static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
  static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
  static final String INCLUDE_PATH_INFO    = "javax.servlet.include.path_info";
  static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

  static final String FORWARD_REQUEST_URI  = "javax.servlet.forward.request_uri";
  static final String FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
  static final String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
  static final String FORWARD_PATH_INFO    = "javax.servlet.forward.path_info";
  static final String FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";

  static final String ERROR_STATUS_CODE    = "javax.servlet.error.status_code";
  static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
  static final String ERROR_MESSAGE        = "javax.servlet.error.message";
  static final String ERROR_EXCEPTION      = "javax.servlet.error.exception";
  static final String ERROR_REQUEST_URI    = "javax.servlet.error.request_uri";
  static final String ERROR_SERVLET_NAME   = "javax.servlet.error.servlet_name";

  private Map dispatchAttributes;
  private String dispatchQueryString;
  private Map dispatchParameters;
  private WinstoneResourceBundle resources;
  
  public DispatchRequestWrapper(ServletRequest request, 
      WinstoneResourceBundle resources, boolean isInclude,
      String requestURI, String contextPath, String servletPath, 
      String pathInfo, String queryString) 
  {
    super(null);
    this.resources = resources;
    super.setRequest(request);
    this.dispatchAttributes = new Hashtable();
    setAtt(isInclude ? INCLUDE_REQUEST_URI : FORWARD_REQUEST_URI, requestURI);
    setAtt(isInclude ? INCLUDE_CONTEXT_PATH : FORWARD_CONTEXT_PATH, contextPath);
    setAtt(isInclude ? INCLUDE_SERVLET_PATH : FORWARD_SERVLET_PATH, servletPath);
    setAtt(isInclude ? INCLUDE_PATH_INFO : FORWARD_PATH_INFO, pathInfo);
    setAtt(isInclude ? INCLUDE_QUERY_STRING : FORWARD_QUERY_STRING, queryString);

    this.dispatchQueryString = queryString;
  }
  
  public DispatchRequestWrapper(ServletRequest request, 
      WinstoneResourceBundle resources, String errorPageURI, 
      String contextPath, String servletPath, String pathInfo, 
      String queryString, Integer statusCode, 
      Throwable originalException, String originalErrorURI, 
      String servletName) 
  {
    super(null);
    this.resources = resources;
    super.setRequest(request);
    this.dispatchAttributes = new Hashtable();
    
    // Forward atts
    setAtt(FORWARD_REQUEST_URI, errorPageURI);
    setAtt(FORWARD_CONTEXT_PATH, contextPath);
    setAtt(FORWARD_SERVLET_PATH, servletPath);
    setAtt(FORWARD_PATH_INFO, pathInfo);
    setAtt(FORWARD_QUERY_STRING, queryString);

    setAtt(ERROR_REQUEST_URI, originalErrorURI);
    setAtt(ERROR_SERVLET_NAME, servletName);
    setAtt(ERROR_STATUS_CODE, statusCode);
    if (originalException != null)
    {
      setAtt(ERROR_EXCEPTION_TYPE, originalException.getClass());
      setAtt(ERROR_MESSAGE, originalException.getMessage());
      setAtt(ERROR_EXCEPTION, originalException);
    }
    
    // Empty param set
    this.dispatchParameters = new Hashtable();
  }
  
  private void setAtt(String name, Object value)
  {
    if (value != null)
      this.dispatchAttributes.put(name, value);
  }
  
  public Object getAttribute(String name)         
  {
    Object att = this.dispatchAttributes.get(name);
    return (att != null ? att : super.getAttribute(name));
  }
  public Enumeration getAttributeNames()          
  {
    Set s1 = this.dispatchAttributes.keySet();
    List s2 = Collections.list(super.getAttributeNames());
    s2.addAll(s1);
    return Collections.enumeration(s2);
  }

  public String getParameter(String name)           
  {
    initialiseDispatchParams();
    Object dispParam = this.dispatchParameters.get(name);
    if (dispParam == null)
      return super.getParameter(name);
    else if (dispParam instanceof String)
      return (String) dispParam;
    else if (dispParam instanceof String[])
      return ((String []) dispParam)[0];
    else
      return dispParam.toString();
  }
  public Map getParameterMap()                      
  {
    initialiseDispatchParams();
    Map params = super.getParameterMap();
    params.putAll(this.dispatchParameters);
    return params;
  }
  public Enumeration getParameterNames()            
  	{return Collections.enumeration(getParameterMap().keySet());}
  public String[] getParameterValues(String name)   
  {
    initialiseDispatchParams();
    Object dispParam = this.dispatchParameters.get(name);
    if (dispParam == null)
      return super.getParameterValues(name);
    else if (dispParam instanceof String[])
      return (String []) dispParam;
    else if (dispParam instanceof String)
    {
      String arr[] = new String[1];
      arr[0] = (String) dispParam;
      return arr;
    }
    else
      throw new WinstoneException(resources.getString("WinstoneRequest.UnknownParamType")
          + name + " - " + dispParam.getClass());
  }
  
  private void initialiseDispatchParams()
  {
    if (this.dispatchParameters != null)
      return;
    initialiseDispatchParams();
    this.dispatchParameters = new Hashtable();
    this.dispatchParameters.putAll(WinstoneRequest.extractParameters(
            this.dispatchQueryString, getCharacterEncoding(), resources));
    
  }

}
