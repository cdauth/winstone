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
import java.util.Map;

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
    
    private ServletConfiguration config;
    private String name;
    private String prefix;
//    private ClassLoader loader;
//    private Object semaphore;
    private WinstoneResourceBundle resources;
    
    private Map filters;
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String requestURI;
    private String jspFile;
    
    private Integer errorStatusCode;
    private Throwable errorException;
    private String errorServletName;
    private String errorURI;
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
    public RequestDispatcher(ServletConfiguration config, String name, 
            ClassLoader loader, String prefix,
            String jspFile, Map filters, WinstoneResourceBundle resources) {
        this.config = config;
        this.resources = resources;
        this.name = name;
//        this.loader = loader;
        this.prefix = prefix;
        this.jspFile = jspFile;
        this.filters = filters;

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
            String queryString, String requestURI,
            Mapping forwardFilterPatterns[], Mapping includeFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = requestURI;

        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        this.filterPatterns = null; // set after the call to forward or include
        this.useRequestAttributes = true;
        this.isErrorDispatch = false;
    }

    public void setForErrorDispatcher(String servletPath, String pathInfo,
            String queryString, String requestURI, int statusCode,
            String summaryMessage, Throwable exception, String originalErrorURI,
            String errorServletName, Mapping errorFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = requestURI;

        this.errorStatusCode = new Integer(statusCode);
        this.errorException = exception;
        this.errorURI = originalErrorURI;
        this.errorServletName = errorServletName;
        this.errorSummaryMessage = summaryMessage;

        this.filterPatterns = errorFilterPatterns;
        this.useRequestAttributes = true;
        this.isErrorDispatch = true;
    }

    public void setForInitialDispatcher(String servletPath, String pathInfo,
            String requestURI, Mapping requestFilterPatterns[],
            AuthenticationHandler authHandler) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.requestURI = requestURI;
        this.authHandler = authHandler;
        this.filterPatterns = requestFilterPatterns;
        this.useRequestAttributes = false;
        this.isErrorDispatch = false;
    }

    public String getName() {
        return this.name;
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
                            this.name, this.requestURI });
            
            ServletRequest workingRequest = request;
            while (workingRequest instanceof ServletRequestWrapper) {
                workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
            }

            WinstoneRequest wr = (WinstoneRequest) workingRequest;
            // Add the query string to the included query string stack
            wr.addIncludeQueryParameters(this.queryString);
            
            // Set request attributes
            if (useRequestAttributes) {
                wr.addIncludeAttributes(this.prefix + this.requestURI, this.prefix,
                        this.servletPath, this.pathInfo, this.queryString);
            }

            ServletResponse workingResponse = response;
            while (workingResponse instanceof ServletResponseWrapper) {
                workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
            }
            // Add another include buffer to the response stack
            ((WinstoneResponse) workingResponse).startIncludeBuffer();

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
                    this.config.execute(request, response, this.prefix + this.requestURI);
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
        ServletRequest workingRequest = request;
        while (workingRequest instanceof ServletRequestWrapper) {
            workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
        }
        
        WinstoneRequest wr = (WinstoneRequest) workingRequest;
        wr.removeIncludeQueryString();
        
        // Set request attributes
        if (useRequestAttributes) {
            wr.removeIncludeAttributes();
        }
        
        ServletResponse workingResponse = response;
        while (workingResponse instanceof ServletResponseWrapper) {
            workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
        }
        // Remove the include buffer from the response stack
        ((WinstoneResponse) workingResponse).finishIncludeBuffer();
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
                            this.name, this.requestURI });
            if (response.isCommitted()) {
                throw new IllegalStateException(resources.getString(
                        "RequestDispatcher.ForwardCommitted"));
            }
            response.resetBuffer();

            ServletRequest workingRequest = request;
            while (workingRequest instanceof ServletRequestWrapper) {
                workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
            }
            ((WinstoneRequest) workingRequest).clearIncludeStackForForward();
            
            if (useRequestAttributes) {
                // Strip back to bare request/response - setup servlet path etc

                if (workingRequest instanceof WinstoneRequest) {
                    WinstoneRequest req = (WinstoneRequest) workingRequest;
                    req.setServletPath(this.servletPath);
                    req.setPathInfo(this.pathInfo);
                    req.setRequestURI(this.prefix + this.requestURI);
                    req.setQueryString(this.queryString);
                    req.setSecurityRoleRefsMap(this.config.getSecurityRoleRefs());
                }
            }
            
            ServletResponse workingResponse = response;
            while (workingResponse instanceof ServletResponseWrapper) {
                workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
            }
            // Clear the include stack if one has been accumulated
            ((WinstoneResponse) workingResponse).clearIncludeStackForForward();

            // Forwards haven't set up the filter pattern set yet
            if (this.filterPatterns == null) {
                this.filterPatterns = this.forwardFilterPatterns;
            }
            
            // Otherwise we are an initial or error dispatcher, so check security if initial -
            // if we should not continue, return
            else if (!this.isErrorDispatch && !continueAfterSecurityCheck(request, response)) {
                return;
            }

            // Set request attributes (because it's the first time)
            if (useRequestAttributes) {
                if (this.isErrorDispatch) {
                    request.setAttribute(FORWARD_REQUEST_URI, this.prefix + this.requestURI);
                    request.setAttribute(FORWARD_CONTEXT_PATH, this.prefix);
                    request.setAttribute(FORWARD_SERVLET_PATH, servletPath);
                    request.setAttribute(FORWARD_PATH_INFO, pathInfo);
                    request.setAttribute(FORWARD_QUERY_STRING, queryString);

                    request.setAttribute(ERROR_REQUEST_URI, this.errorURI);
                    request.setAttribute(ERROR_SERVLET_NAME, this.errorServletName);
                    request.setAttribute(ERROR_STATUS_CODE, this.errorStatusCode);
                    request.setAttribute(ERROR_MESSAGE, errorSummaryMessage);
                    if (this.errorException != null) {
                        request.setAttribute(ERROR_EXCEPTION_TYPE, errorException.getClass());
                        request.setAttribute(ERROR_EXCEPTION, errorException);
                    }
                    
                    response = new ErrorResponseWrapper(response, 
                            this.errorStatusCode.intValue());
                } else {
                    request.setAttribute(FORWARD_REQUEST_URI, this.prefix + this.requestURI);
                    request.setAttribute(FORWARD_CONTEXT_PATH, this.prefix);
                    request.setAttribute(FORWARD_SERVLET_PATH, this.servletPath);
                    request.setAttribute(FORWARD_PATH_INFO, this.pathInfo);
                    request.setAttribute(FORWARD_QUERY_STRING, this.queryString);
                }
            }
            this.doInclude = Boolean.FALSE;
        }

        // Make sure the filter chain is exhausted first
        if (this.filterPatternsEvaluated < this.filterPatterns.length)
            doFilter(request, response);
        else
            this.config.execute(request, response, this.prefix + this.requestURI);
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
                    && filterPattern.getLinkName().equals(this.name)) {
                FilterConfiguration filter = (FilterConfiguration) this.filters
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
                FilterConfiguration filter = (FilterConfiguration) this.filters
                        .get(filterPattern.getMappedTo());
                Logger.log(Logger.DEBUG, this.resources, 
                        "RequestDispatcher.ExecutingFilter", filterPattern.getMappedTo());
                filter.execute(request, response, this);
                return;
            } else {
                Logger.log(Logger.FULL_DEBUG, this.resources, "RequestDispatcher.BypassingFilter",
                        new String[] { this.name, filterPattern.toString(), fullPath });
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

}
