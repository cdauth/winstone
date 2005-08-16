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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

/**
 * This class implements both the RequestDispatcher and FilterChain components. On 
 * the first call to include() or forward(), it starts the filter chain execution
 * if one exists. On the final doFilter() or if there is no chain, we call the include()
 * or forward() again, and the servlet is executed.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class RequestDispatcher implements javax.servlet.RequestDispatcher,
        javax.servlet.FilterChain {
    
    static final String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";
    static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
    static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
    static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
    static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

    static final String FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";
    static final String FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
    static final String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
    static final String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
    static final String FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";

    static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
    static final String ERROR_MESSAGE = "javax.servlet.error.message";
    static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
    static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
    static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
    
    private WebAppConfiguration webAppConfig;
    private ServletConfiguration servletConfig;
    private WinstoneResourceBundle resources;
    
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String requestURI;
    
    private Integer errorStatusCode;
    private Throwable errorException;
    private String errorSummaryMessage;
    
    private AuthenticationHandler authHandler;
    
    private Mapping forwardFilterPatterns[];
    private Mapping includeFilterPatterns[];
    private Mapping filterPatterns[];
    private int filterPatternsEvaluated;
    
    private Boolean doInclude;
    private boolean isErrorDispatch;
    private boolean useRequestAttributes;
    
    /**
     * Constructor. This initializes the filter chain and sets up the details
     * needed to handle a servlet excecution, such as security constraints,
     * filters, etc.
     */
    public RequestDispatcher(WebAppConfiguration webAppConfig, ServletConfiguration servletConfig, 
            WinstoneResourceBundle resources) {
        this.servletConfig = servletConfig;
        this.webAppConfig = webAppConfig;
        this.resources = resources;

        this.filterPatternsEvaluated = 0;
    }

    public void setForNamedDispatcher(Mapping forwardFilterPatterns[],
            Mapping includeFilterPatterns[]) {
        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        this.filterPatterns = null; // set after the call to forward or include
        this.useRequestAttributes = false;
        this.isErrorDispatch = false;
    }

    public void setForURLDispatcher(String servletPath, String pathInfo,
            String queryString, String requestURIInsideWebapp,
            Mapping forwardFilterPatterns[], Mapping includeFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = requestURIInsideWebapp;

        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        this.filterPatterns = null; // set after the call to forward or include
        this.useRequestAttributes = true;
        this.isErrorDispatch = false;
    }

    public void setForErrorDispatcher(String servletPath, String pathInfo,
            String queryString, int statusCode, String summaryMessage, 
            Throwable exception, String errorHandlerURI, 
            Mapping errorFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = errorHandlerURI;

        this.errorStatusCode = new Integer(statusCode);
        this.errorException = exception;
        this.errorSummaryMessage = summaryMessage;

        this.filterPatterns = errorFilterPatterns;
        this.useRequestAttributes = true;
        this.isErrorDispatch = true;
    }

    public void setForInitialDispatcher(String servletPath, String pathInfo, 
            String queryString, String requestURIInsideWebapp, Mapping requestFilterPatterns[],
            AuthenticationHandler authHandler) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = requestURIInsideWebapp;
        this.authHandler = authHandler;
        this.filterPatterns = requestFilterPatterns;
        this.useRequestAttributes = false;
        this.isErrorDispatch = false;
    }

    public String getName() {
        return this.servletConfig.getServletName();
    }

    /**
     * Includes the execution of a servlet into the current request
     * 
     * Note this method enters itself twice: once with the initial call, and once again 
     * when all the filters have completed.
     */
    public void include(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {

        // On the first call, log and initialise the filter chain
        if (this.doInclude == null) {
            Logger.log(Logger.DEBUG, resources,
                    "RequestDispatcher.IncludeMessage", new String[] {
                            getName(), this.requestURI });
            
            WinstoneRequest wr = getUnwrappedRequest(request);
            // Add the query string to the included query string stack
            wr.addIncludeQueryParameters(this.queryString);
            
            // Set request attributes
            if (useRequestAttributes) {
                wr.addIncludeAttributes(this.webAppConfig.getPrefix() + this.requestURI, 
                        this.webAppConfig.getPrefix(), this.servletPath, this.pathInfo, this.queryString);
            }
            // Add another include buffer to the response stack
            getUnwrappedResponse(response).startIncludeBuffer();

            this.doInclude = Boolean.TRUE;
        }
        if (this.filterPatterns == null)
            this.filterPatterns = this.includeFilterPatterns;

        try {
            // Make sure the filter chain is exhausted first
            if (this.filterPatternsEvaluated < this.filterPatterns.length) {
                doFilter(request, response);
                finishInclude(request, response);
            }
            else {

                try {
                    this.servletConfig.execute(request, response, 
                            this.webAppConfig.getPrefix() + this.requestURI);
                } finally {
                    if (this.filterPatterns.length == 0) {
                        finishInclude(request, response);
                    }
                }
            }
        } catch (Throwable err) {
            finishInclude(request, response);
            if (err instanceof ServletException) {
                throw (ServletException) err;
            } else if (err instanceof IOException) {
                throw (IOException) err;
            } else if (err instanceof Error) {
                throw (Error) err;
            } else {
                throw (RuntimeException) err;
            }
        }
    }

    private void finishInclude(ServletRequest request, ServletResponse response) 
            throws IOException {
        WinstoneRequest wr = getUnwrappedRequest(request);
        wr.removeIncludeQueryString();
        
        // Set request attributes
        if (useRequestAttributes) {
            wr.removeIncludeAttributes();
        }
        // Remove the include buffer from the response stack
        getUnwrappedResponse(response).finishIncludeBuffer();
    }
    
    /**
     * Forwards to another servlet, and when it's finished executing that other
     * servlet, cut off execution.
     * 
     * Note this method enters itself twice: once with the initial call, and once again 
     * when all the filters have completed.
     */
    public void forward(ServletRequest request, ServletResponse response) 
            throws ServletException, IOException {

        // Only on the first call to forward, we should set any forwarding attributes
        if (this.doInclude == null) {
            Logger.log(Logger.DEBUG, resources,
                    "RequestDispatcher.ForwardMessage", new String[] {
                    getName(), this.requestURI });
            if (response.isCommitted()) {
                throw new IllegalStateException(resources.getString(
                        "RequestDispatcher.ForwardCommitted"));
            }
            
            WinstoneRequest req = getUnwrappedRequest(request);
            WinstoneResponse rsp = getUnwrappedResponse(response);
            
            // Clear the include stack if one has been accumulated
            rsp.resetBuffer();
            req.clearIncludeStackForForward();
            rsp.clearIncludeStackForForward();
            
            // Set request attributes (because it's the first step in the filter chain of a forward or error)
            if (useRequestAttributes) {
                req.setAttribute(FORWARD_REQUEST_URI, req.getRequestURI());
                req.setAttribute(FORWARD_CONTEXT_PATH, req.getContextPath());
                req.setAttribute(FORWARD_SERVLET_PATH, req.getServletPath());
                req.setAttribute(FORWARD_PATH_INFO, req.getPathInfo());
                req.setAttribute(FORWARD_QUERY_STRING, req.getQueryString());
                
                if (this.isErrorDispatch) {
                    req.setAttribute(ERROR_REQUEST_URI, req.getRequestURI());
                    req.setAttribute(ERROR_STATUS_CODE, this.errorStatusCode);
                    req.setAttribute(ERROR_MESSAGE, errorSummaryMessage);
                    if (req.getServletConfig() != null) {
                        req.setAttribute(ERROR_SERVLET_NAME, req.getServletConfig().getServletName());
                    }
                    
                    if (this.errorException != null) {
                        req.setAttribute(ERROR_EXCEPTION_TYPE, errorException.getClass());
                        req.setAttribute(ERROR_EXCEPTION, errorException);
                    }
                    
                    // Revert back to the original request and response
                    rsp.setErrorStatusCode(this.errorStatusCode.intValue());
                    request = req;
                    response = rsp;
                }
            }

//            if (useRequestAttributes) {
//                // Strip back to bare request/response - setup servlet path etc

            req.setServletPath(this.servletPath);
            req.setPathInfo(this.pathInfo);
            req.setForwardQueryString(this.queryString);
            req.setRequestURI(this.webAppConfig.getPrefix() + this.requestURI);
            req.setQueryString(this.queryString);
            req.setWebAppConfig(this.webAppConfig);
            req.setServletConfig(this.servletConfig);
            req.setRequestAttributeListeners(this.webAppConfig.getRequestAttributeListeners());
            
            rsp.setWebAppConfig(this.webAppConfig);
//            }

            // Forwards haven't set up the filter pattern set yet
            if (this.filterPatterns == null) {
                this.filterPatterns = this.forwardFilterPatterns;
            }
            
            // Otherwise we are an initial or error dispatcher, so check security if initial -
            // if we should not continue, return
            else if (!this.isErrorDispatch && !continueAfterSecurityCheck(request, response)) {
                return;
            }
            
            this.doInclude = Boolean.FALSE;
        }

        // Make sure the filter chain is exhausted first
        if (this.filterPatternsEvaluated < this.filterPatterns.length)
            doFilter(request, response);
        else
            this.servletConfig.execute(request, response, this.webAppConfig.getPrefix() + this.requestURI);
    }

    private boolean continueAfterSecurityCheck(ServletRequest request,
            ServletResponse response) throws IOException, ServletException {
        // Evaluate security constraints
        if (this.authHandler != null) {
            return this.authHandler.processAuthentication(request, response,
                    this.servletPath + (this.pathInfo == null ? "" : this.pathInfo));
        } else {
            return true;
        }
    }

    /**
     * Handles the processing of the chain of filters, so that we process them
     * all, then pass on to the main servlet
     */
    public void doFilter(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        // Loop through the filter mappings until we hit the end
        while (this.filterPatternsEvaluated < this.filterPatterns.length) {
            // Get the pattern and eval it, bumping up the eval'd count
            Mapping filterPattern = this.filterPatterns[this.filterPatternsEvaluated++];
            String fullPath = this.servletPath + (this.pathInfo == null ? "" : this.pathInfo);

            // If the servlet name matches this name, execute it
            if ((filterPattern.getLinkName() != null)
                    && filterPattern.getLinkName().equals(getName())) {
                FilterConfiguration filter = (FilterConfiguration) this.webAppConfig.getFilters()
                        .get(filterPattern.getMappedTo());
                Logger.log(Logger.DEBUG, this.resources,
                        "RequestDispatcher.ExecutingFilter", filterPattern.getMappedTo());
                filter.execute(request, response, this);
                return;
            }
            // If the url path matches this filters mappings
            else if ((filterPattern.getLinkName() == null)
                    && (this.servletPath != null)
                    && filterPattern.match(fullPath, null, null)) {
                FilterConfiguration filter = (FilterConfiguration) this.webAppConfig.getFilters()
                        .get(filterPattern.getMappedTo());
                Logger.log(Logger.DEBUG, this.resources, 
                        "RequestDispatcher.ExecutingFilter", filterPattern.getMappedTo());
                filter.execute(request, response, this);
                return;
            } else {
                Logger.log(Logger.FULL_DEBUG, this.resources, "RequestDispatcher.BypassingFilter",
                        new String[] { getName(), filterPattern.toString(), fullPath });
            }
        }

        // Forward / include as requested in the beginning
        if (this.doInclude == null)
            return; // will never happen, because we can't call doFilter before forward/include
        else if (this.doInclude.booleanValue())
            include(request, response);
        else
            forward(request, response);
    }

    /**
     * Unwrap back to the original container allocated request object
     */
    protected WinstoneRequest getUnwrappedRequest(ServletRequest request) {
        ServletRequest workingRequest = request;
        while (workingRequest instanceof ServletRequestWrapper) {
            workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
        }
        return (WinstoneRequest) workingRequest;
    }

    /**
     * Unwrap back to the original container allocated response object
     */
    protected WinstoneResponse getUnwrappedResponse(ServletResponse response) {
        ServletResponse workingResponse = response;
        while (workingResponse instanceof ServletResponseWrapper) {
            workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
        }
        return (WinstoneResponse) workingResponse;
    }
}
