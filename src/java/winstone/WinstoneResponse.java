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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Response for servlet
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneResponse.java,v 1.28 2005/04/19 07:33:41 rickknowles
 *          Exp $
 */
public class WinstoneResponse implements HttpServletResponse {
    protected static final DateFormat df = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    static {
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    static final String CONTENT_LENGTH_HEADER = "Content-Length";
    static final String CONTENT_TYPE_HEADER = "Content-Type";

    // Response header constants
    static final String CONTENT_LANGUAGE_HEADER = "Content-Language";
    static final String KEEP_ALIVE_HEADER = "Connection";
    static final String ENCODING_HEADER = "Transfer-Encoding";
    static final String KEEP_ALIVE_OPEN = "Keep-Alive";
    static final String KEEP_ALIVE_CLOSE = "Close";
    static final String DATE_HEADER = "Date";
    static final String SERVER_HEADER = "Server";
    static final String LOCATION_HEADER = "Location";
    static final String OUT_COOKIE_HEADER1 = "Set-Cookie";
    static final String OUT_COOKIE_HEADER2 = "Set-Cookie2";
    static final String X_POWERED_BY_HEADER = "X-Powered-By";
    static final String POWERED_BY_WINSTONE = "Servlet/2.4 (Winstone/0.8)";

    private int statusCode;
    private WinstoneRequest req;
    private WebAppConfiguration webAppConfig;
    private WinstoneOutputStream outputStream;
    private Stack includeByteArrays;
    private Stack includeOutputStreams;
    
    private List headers;
    private String explicitlySetEncoding;
    private String currentEncoding;
    private List cookies;
    
    private Map outPrintWriters;
    private Locale locale;
    private String protocol;
    private String reqKeepAliveHeader;
    private Integer errorStatusCode;
    
    /**
     * Constructor
     */
    public WinstoneResponse() {
        
        this.headers = new ArrayList();
        this.cookies = new ArrayList();
        this.includeByteArrays = new Stack();
        this.includeOutputStreams = new Stack();
        this.outPrintWriters = new Hashtable();

        this.statusCode = SC_OK;
        this.locale = null; //Locale.getDefault();
        this.explicitlySetEncoding = null;
        this.protocol = null;
        this.reqKeepAliveHeader = null;
        
        this.headers.add(SERVER_HEADER + ": " + Launcher.RESOURCES.getString("ServerVersion"));
    }

    /**
     * Resets the request to be reused
     */
    public void cleanUp() {
        this.req = null;
        this.webAppConfig = null;
        this.outputStream = null;
        this.includeByteArrays.clear();
        this.includeOutputStreams.clear();
        this.outPrintWriters.clear();
        this.headers.clear();
        this.cookies.clear();
        this.protocol = null;
        this.reqKeepAliveHeader = null;

        this.statusCode = SC_OK;
        this.errorStatusCode = null;
        this.locale = null; //Locale.getDefault();
        this.explicitlySetEncoding = null;
        this.currentEncoding = null;
        
        this.headers.add(SERVER_HEADER + ": " + Launcher.RESOURCES.getString("ServerVersion"));
    }

    private String getEncodingFromLocale(Locale loc) {
        String localeString = loc.getLanguage() + "_" + loc.getCountry();
        Map encMap = this.webAppConfig.getLocaleEncodingMap();
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                "WinstoneResponse.LookForLocaleEncoding",
                new String[] {localeString, encMap + ""});

        String fullMatch = (String) encMap.get(localeString);
        if (fullMatch != null) {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                    "WinstoneResponse.FoundLocaleEncoding", fullMatch);
            return fullMatch;
        } else {
            localeString = loc.getLanguage();
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                    "WinstoneResponse.LookForLocaleEncoding",
                    new String[] {localeString, encMap + ""});
            String match = (String) encMap.get(localeString);
            if (match != null) {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                        "WinstoneResponse.FoundLocaleEncoding", match);
            }
            return match;
        }
    }

    public void setErrorStatusCode(int statusCode) {
        this.errorStatusCode = new Integer(statusCode);
        this.statusCode = statusCode;
    }
    
    public WinstoneOutputStream getWinstoneOutputStream() {
        return this.outputStream;
    }
    
    public void setOutputStream(WinstoneOutputStream outData) {
        this.outputStream = outData;
    }

    public void setWebAppConfig(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void extractRequestKeepAliveHeader(WinstoneRequest req) {
        this.reqKeepAliveHeader = req.getHeader(KEEP_ALIVE_HEADER);
    }

    public List getHeaders() {
        return this.headers;
    }

    public List getCookies() {
        return this.cookies;
    }

    public WinstoneRequest getRequest() {
        return this.req;
    }

    public void setRequest(WinstoneRequest req) {
        this.req = req;
    }
    
    public void startIncludeBuffer() {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        WinstoneOutputStream outStream = new WinstoneOutputStream(outBytes, true);
        outStream.setResponse(this);
            
        this.includeByteArrays.push(outBytes);
        this.includeOutputStreams.push(outStream);
    }
    
    public void finishIncludeBuffer() throws IOException {
        if (this.includeOutputStreams.isEmpty()) {
            return; // must have been forwarded, and the include buffers cleared. Ignore
        }
        ByteArrayOutputStream body = (ByteArrayOutputStream) this.includeByteArrays.pop();
        WinstoneOutputStream included = (WinstoneOutputStream) this.includeOutputStreams.pop();
        PrintWriter includedWriter = (PrintWriter) this.outPrintWriters.get(included);
        if (includedWriter != null) {
            includedWriter.flush();
        }
        included.commit();
        included.close();
        included.setResponse(null); // for garbage collection
        body.flush();
        String underlyingEncoding = getCharacterEncoding();
        String bodyBlock = (underlyingEncoding != null 
                ? new String(body.toByteArray(), underlyingEncoding)
                : new String(body.toByteArray()));

        // Try to write the body in
        WinstoneOutputStream including = getTopOutputStream();
        PrintWriter out = (PrintWriter) this.outPrintWriters.get(including);
        if (out == null) {
            out = new PrintWriter(new OutputStreamWriter(getOutputStream(), underlyingEncoding), false);
        }
        out.write(bodyBlock);
        out.flush();
    }
    
    public void clearIncludeStackForForward() throws IOException {
        for (Iterator i = this.includeOutputStreams.iterator(); i.hasNext(); ) {
            ((WinstoneOutputStream) i.next()).close();
        }
        this.includeOutputStreams.clear();
        this.includeByteArrays.clear();
        PrintWriter mainOutWriter = (PrintWriter) this.outPrintWriters.get(this.outputStream);
        this.outPrintWriters.clear();
        if (mainOutWriter != null) {
            this.outPrintWriters.put(this.outputStream, mainOutWriter);
        }
    }

    protected static String getCharsetFromContentTypeHeader(String type, StringBuffer remainder) {
        if (type == null) {
            type = "text/html";
        }
        // Parse type to set encoding if needed
        StringTokenizer st = new StringTokenizer(type, ";");
        String localEncoding = null;
        while (st.hasMoreTokens()) {
            String clause = st.nextToken().trim();
            if (clause.startsWith("charset="))
                localEncoding = clause.substring(8);
            else {
                if (remainder.length() > 0) {
                    remainder.append(";");
                }
                remainder.append(clause);
            }
        }
        if ((localEncoding == null) || 
                !localEncoding.startsWith("\"") || 
                !localEncoding.endsWith("\"")) {
            return localEncoding;
        } else {
            return localEncoding.substring(1, localEncoding.length() - 1);
        }
    } 

    /**
     * This ensures the bare minimum correct http headers are present
     */
    public void validateHeaders() {        
        // Need this block for WebDAV support. "Connection:close" header is ignored
        String lengthHeader = getHeader(CONTENT_LENGTH_HEADER);
//        String oldKeepAliveHeader = getHeader(KEEP_ALIVE_HEADER);
//        if ((lengthHeader == null) && 
//                (oldKeepAliveHeader != null) && 
//                oldKeepAliveHeader.equals(KEEP_ALIVE_OPEN)) {
        if ((lengthHeader == null) && (this.statusCode >= 300)) {
            int bodyBytes = this.outputStream.getOutputStreamLength();
            if (getBufferSize() > bodyBytes) {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                        "WinstoneResponse.ForcingContentLength", "" + bodyBytes);
                setContentLength(bodyBytes);
                lengthHeader = getHeader(CONTENT_LENGTH_HEADER);
            }
        }
        
        setHeader(KEEP_ALIVE_HEADER, !closeAfterRequest() ? KEEP_ALIVE_OPEN : KEEP_ALIVE_CLOSE);
        String contentType = getHeader(CONTENT_TYPE_HEADER);
        if ((contentType == null) && (this.statusCode != SC_MOVED_TEMPORARILY)) {
            // Bypass normal encoding
            setHeader(CONTENT_TYPE_HEADER, "text/html" + 
                    (this.currentEncoding == null ? "" : ";charset=" + this.currentEncoding));
        }
        if (getHeader(DATE_HEADER) == null)
            setDateHeader(DATE_HEADER, System.currentTimeMillis());
        if (getHeader(X_POWERED_BY_HEADER) == null)
            setHeader(X_POWERED_BY_HEADER, POWERED_BY_WINSTONE);
        if (this.locale != null) {
            setHeader(CONTENT_LANGUAGE_HEADER, this.locale.getLanguage() + 
                    (this.locale.getCountry() != null ? "-" + this.locale.getCountry() : ""));
        }
        
        // If we don't have a webappConfig, exit here, cause we definitely don't
        // have a session
        if (req.getWebAppConfig() == null)
            return;

        // Write out the new session cookie if it's present
        HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());
        for (Iterator i = req.getCurrentSessionIds().keySet().iterator(); i.hasNext(); ) {
            String prefix = (String) i.next();
            String sessionId = (String) req.getCurrentSessionIds().get(prefix);
            WebAppConfiguration ownerContext = hostConfig.getWebAppByURI(prefix);
            if (ownerContext != null) {
                WinstoneSession session = ownerContext.getSessionById(sessionId, true);
                if ((session != null) && session.isNew()) {
                    session.setIsNew(false);
                    Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, session.getId());
                    cookie.setMaxAge(-1);
                    cookie.setSecure(req.isSecure());
                    cookie.setVersion(0); //req.isSecure() ? 1 : 0);
                    cookie.setPath(req.getWebAppConfig().getPrefix().equals("") ? "/"
                                    : req.getWebAppConfig().getPrefix());
                    this.addCookie(cookie);
                }
            }
        }
        
        // Look for expired sessions: ie ones where the requested and current ids are different
        for (Iterator i = req.getRequestedSessionIds().keySet().iterator(); i.hasNext(); ) {
            String prefix = (String) i.next();
            String sessionId = (String) req.getRequestedSessionIds().get(prefix);
            if (!req.getCurrentSessionIds().containsKey(prefix)) {
                Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, sessionId);
                cookie.setMaxAge(0); // explicitly expire this cookie
                cookie.setSecure(req.isSecure());
                cookie.setVersion(0); //req.isSecure() ? 1 : 0);
                cookie.setPath(prefix.equals("") ? "/" : prefix);
                this.addCookie(cookie);
            }
        }
        
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneResponse.HeadersPreCommit",
                this.headers + "");
    }

    /**
     * Writes out the http header for a single cookie
     */
    public String writeCookie(Cookie cookie) throws IOException {
        
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneResponse.WritingCookie", cookie + "");
        StringBuffer out = new StringBuffer();

        // Set-Cookie or Set-Cookie2
        if (cookie.getVersion() >= 1)
            out.append(OUT_COOKIE_HEADER1).append(": "); // TCK doesn't like set-cookie2
        else
            out.append(OUT_COOKIE_HEADER1).append(": ");

        // name/value pair
        if (cookie.getVersion() == 0)
            out.append(cookie.getName()).append("=").append(cookie.getValue());
        else {
            out.append(cookie.getName()).append("=");
            quote(cookie.getValue(), out);
        }

        if (cookie.getVersion() >= 1) {
            out.append("; Version=1");
            if (cookie.getDomain() != null) {
                out.append("; Domain=");
                quote(cookie.getDomain(), out);
            }
            if (cookie.getSecure())
                out.append("; Secure");

            if (cookie.getMaxAge() >= 0)
                out.append("; Max-Age=").append(cookie.getMaxAge());
            else
                out.append("; Discard");
            if (cookie.getPath() != null) {
                out.append("; Path=");
                quote(cookie.getPath(), out);
            }
        } else {
            if (cookie.getDomain() != null) {
                out.append("; Domain=");
                out.append(cookie.getDomain());
            }
            if (cookie.getMaxAge() > 0) {
                long expiryMS = System.currentTimeMillis()
                        + (1000 * (long) cookie.getMaxAge());
                Date expiryDate = new Date(expiryMS);
                out.append("; Expires=").append(df.format(expiryDate));
            } else if (cookie.getMaxAge() == 0) {
                out.append("; Expires=").append(df.format(new Date(5000)));
            }
            if (cookie.getPath() != null)
                out.append("; Path=").append(cookie.getPath());
            if (cookie.getSecure())
                out.append("; Secure");
        }
        return out.toString();
    }

    /**
     * Quotes the necessary strings in a cookie header. The quoting is only
     * applied if the string contains special characters.
     */
    protected void quote(String value, StringBuffer out) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            out.append(value);
        } else {
            boolean containsSpecial = false;
            for (int n = 0; n < value.length(); n++) {
                char thisChar = value.charAt(n);
                if ((thisChar < 32) || (thisChar >= 127)
                        || (specialCharacters.indexOf(thisChar) != -1)) {
                    containsSpecial = true;
                    break;
                }
            }
            if (containsSpecial)
                out.append('"').append(value).append('"');
            else
                out.append(value);
        }
    }

    final String specialCharacters = "()<>@,;:\\\"/[]?={} \t";

    /**
     * Based on request/response headers and the protocol, determine whether or
     * not this connection should operate in keep-alive mode.
     */
    public boolean closeAfterRequest() {
        String inKeepAliveHeader = this.reqKeepAliveHeader;
        String outKeepAliveHeader = getHeader(KEEP_ALIVE_HEADER);
        boolean hasContentLength = (getHeader(CONTENT_LENGTH_HEADER) != null);
        if (this.protocol.startsWith("HTTP/0"))
            return true;
        else if ((inKeepAliveHeader == null) && (outKeepAliveHeader == null))
            return this.protocol.equals("HTTP/1.0") ? true : !hasContentLength;
        else if (outKeepAliveHeader != null)
            return outKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE) || !hasContentLength;
        else if (inKeepAliveHeader != null)
            return inKeepAliveHeader.equalsIgnoreCase(KEEP_ALIVE_CLOSE) || !hasContentLength;
        else
            return false;
    }

    private WinstoneOutputStream getTopOutputStream() {
        WinstoneOutputStream outStream = this.outputStream;
        if (!this.includeOutputStreams.isEmpty()) {
            outStream = (WinstoneOutputStream) this.includeOutputStreams.peek();
        }
        return outStream;
    }
    
    // ServletResponse interface methods
    public void flushBuffer() throws IOException {
        WinstoneOutputStream outStream = getTopOutputStream();
        PrintWriter outWriter = (PrintWriter) this.outPrintWriters.get(outStream);
        if (outWriter != null) {
            outWriter.flush();
        }
        outStream.flush();
    }

    public void setBufferSize(int size) {
        WinstoneOutputStream outStream = getTopOutputStream();
        outStream.setBufferSize(size);
    }

    public int getBufferSize() {
        WinstoneOutputStream outStream = getTopOutputStream();
        return outStream.getBufferSize();
    }

    public String getCharacterEncoding() {
        return (this.currentEncoding == null ? "ISO-8859-1" : this.currentEncoding);
    }

    public void setCharacterEncoding(String encoding) {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneResponse.SettingEncoding", encoding);
        StringBuffer remainderHeader = new StringBuffer();
        getCharsetFromContentTypeHeader(getContentType(), remainderHeader);
        setContentType(remainderHeader + ";charset=" + encoding);
    }

    public String getContentType() {
        return getHeader(CONTENT_TYPE_HEADER);
    }

    public void setContentType(String type) {
        setHeader(CONTENT_TYPE_HEADER, type);
    }

    public Locale getLocale() {
        return this.locale == null ? Locale.getDefault() : this.locale;
    }

    public void setLocale(Locale loc) {
        if (!this.includeOutputStreams.isEmpty()) {
            return;
        }
        
        if (isCommitted()) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES,
                    "WinstoneResponse.SetLocaleTooLate");
        } else if ((this.outPrintWriters.get(this.outputStream) == null)
                && (this.explicitlySetEncoding == null)) {
            String localeEncoding = getEncodingFromLocale(loc);
            if (localeEncoding != null) {
                this.currentEncoding = localeEncoding;
                String contentTypeHeader = getContentType();
                if (contentTypeHeader == null) {
                    this.headers.add(CONTENT_TYPE_HEADER + ": text/html;charset=" + localeEncoding);
                } else {
                    StringBuffer remainderHeader = new StringBuffer();
                    getCharsetFromContentTypeHeader(contentTypeHeader, remainderHeader);
                    String contentHeader = remainderHeader + ";charset=" + localeEncoding;
                    boolean found = false;
                    for (int n = 0; (n < this.headers.size()) && !found; n++) {
                        String header = (String) this.headers.get(n);
                        if (header.startsWith(CONTENT_TYPE_HEADER + ": ")) {
                            this.headers.set(n, CONTENT_TYPE_HEADER + ": " + contentHeader);
                            found = true;
                        }
                    }
                }
            }
        }
        
        if (!isCommitted()) {
            this.locale = loc;
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneResponse.GetOutputStream");
        return getTopOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneResponse.GetWriter");
        WinstoneOutputStream outStream = getTopOutputStream();
        PrintWriter outWriter = (PrintWriter) this.outPrintWriters.get(outStream);
        if (outWriter != null)
            return outWriter;
        else {
            outWriter = new WinstoneResponseWriter(outStream, this); 
            this.outPrintWriters.put(outStream, outWriter);
            return outWriter;
        }
    }

    public boolean isCommitted() {
        return this.outputStream.isCommitted();
    }

    public void reset() {
        if (this.includeOutputStreams.isEmpty()) {
            resetBuffer();
            this.statusCode = SC_OK;
            this.headers.clear();
            this.cookies.clear();
        }
    }

    public void resetBuffer() {
        if (this.includeOutputStreams.isEmpty()) {
            if (isCommitted())
                throw new IllegalStateException(Launcher.RESOURCES
                        .getString("WinstoneResponse.ResponseCommitted"));
            
            // Disregard any output temporarily while we flush
            this.outputStream.setDisregardMode(true);
            PrintWriter mainOutWriter = (PrintWriter) this.outPrintWriters.get(this.outputStream);
            this.outPrintWriters.clear();
            if (mainOutWriter != null) {
                mainOutWriter.flush();
                this.outPrintWriters.put(this.outputStream, mainOutWriter);
            }
            this.outputStream.setDisregardMode(false);
            this.outputStream.reset();
        }
    }

    public void setContentLength(int len) {
        setIntHeader(CONTENT_LENGTH_HEADER, len);
    }

    // HttpServletResponse interface methods
    public void addCookie(Cookie cookie) {
        if (this.includeOutputStreams.isEmpty()) {
            this.cookies.add(cookie);
        }
    }

    public boolean containsHeader(String name) {
        for (int n = 0; n < this.headers.size(); n++)
            if (((String) this.headers.get(n)).startsWith(name))
                return true;
        return false;
    }

    public void addDateHeader(String name, long date) {
        addHeader(name, df.format(new Date(date)));
    } // df.format(new Date(date)));}

    public void addIntHeader(String name, int value) {
        addHeader(name, "" + value);
    }

    public void addHeader(String name, String value) {
        if (!this.includeOutputStreams.isEmpty()) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneResponse.HeaderInInclude", 
                    new String[] {name, value});  
        } else if (isCommitted()) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneResponse.HeaderAfterCommitted", 
                    new String[] {name, value});  
        } else {
            if (name.equals(CONTENT_TYPE_HEADER)) {
                StringBuffer remainderHeader = new StringBuffer();
                String headerEncoding = getCharsetFromContentTypeHeader(
                        value, remainderHeader);
                if ((headerEncoding != null) && 
                        (this.outPrintWriters.get(getTopOutputStream()) == null)) {
                    value = remainderHeader.toString() + "; charset=" + headerEncoding;
                    this.explicitlySetEncoding = headerEncoding;
                    this.currentEncoding = headerEncoding;
                } else {
                    value = remainderHeader.toString() + 
                        (this.currentEncoding == null ? "" : 
                            ";charset=" + this.currentEncoding);
                }
            }
            this.headers.add(name + ": " + value);
        }
    }

    public void setDateHeader(String name, long date) {
        setHeader(name, df.format(new Date(date)));
    }

    public void setIntHeader(String name, int value) {
        setHeader(name, "" + value);
    }

    public void setHeader(String name, String value) {
        if (!this.includeOutputStreams.isEmpty()) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneResponse.HeaderInInclude", 
                    new String[] {name, value});  
        } else if (isCommitted()) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneResponse.HeaderAfterCommitted", 
                    new String[] {name, value});
        } else {
            boolean found = false;
            for (int n = 0; (n < this.headers.size()) && !found; n++) {
                String header = (String) this.headers.get(n);
                if (header.startsWith(name + ": ")) {
                    if (name.equals(CONTENT_TYPE_HEADER)) {
                        StringBuffer remainderHeader = new StringBuffer();
                        String headerEncoding = getCharsetFromContentTypeHeader(
                                value, remainderHeader);
                        if ((headerEncoding != null) && 
                                (this.outPrintWriters.get(getTopOutputStream()) == null)) {
                            value = remainderHeader.toString() + "; charset=" + headerEncoding;
                            this.explicitlySetEncoding = headerEncoding;
                            this.currentEncoding = headerEncoding;
                        } else {
                            value = remainderHeader.toString() + 
                                (this.currentEncoding == null ? "" : 
                                    "; charset=" + this.currentEncoding); 
                        }
                    }

                    this.headers.set(n, name + ": " + value);
                    found = true;
                }
            }
            if (!found)
                addHeader(name, value);
        }
    }

    public String getHeader(String name) {
        for (int n = 0; n < this.headers.size(); n++) {
            String header = (String) this.headers.get(n);
            if (header.startsWith(name + ": "))
                return header.substring(name.length() + 2);
        }
        return null;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeURL(String url) {
        return url;
    }

    public int getStatus() {
        return this.statusCode;
    }

    public Integer getErrorStatusCode() {
        return this.errorStatusCode;
    }

    public void setStatus(int sc) {
        if (this.includeOutputStreams.isEmpty() && (this.errorStatusCode == null)) {
            this.statusCode = sc;
        }
    }

    public void sendRedirect(String location) throws IOException {
        if (!this.includeOutputStreams.isEmpty()) {
            Logger.log(Logger.ERROR, Launcher.RESOURCES, "IncludeResponse.Redirect",
                    location);
            return;
        } else if (isCommitted()) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneOutputStream.AlreadyCommitted"));
        }
        resetBuffer();
        
        // Build location
        StringBuffer fullLocation = new StringBuffer();
        if (location.startsWith("http://") || location.startsWith("https://"))
            fullLocation.append(location);
        else {
            fullLocation.append(this.req.getScheme()).append("://");
            fullLocation.append(this.req.getServerName());
            if (!((this.req.getServerPort() == 80) && this.req.getScheme()
                    .equals("http"))
                    && !((this.req.getServerPort() == 443) && this.req
                            .getScheme().equals("https")))
                fullLocation.append(':').append(this.req.getServerPort());
            if (location.startsWith("/")) {
                fullLocation.append(location);
            } else {
                fullLocation.append(this.req.getRequestURI());
                if (fullLocation.toString().indexOf("?") != -1)
                    fullLocation.delete(fullLocation.toString().indexOf("?"),
                            fullLocation.length());
                fullLocation.delete(
                        fullLocation.toString().lastIndexOf("/") + 1,
                        fullLocation.length());
                fullLocation.append(location);
            }
        }
        if (this.req != null) {
            this.req.discardRequestBody();
        }
        this.statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
        setHeader(LOCATION_HEADER, fullLocation.toString());
        setContentLength(0);
        getWriter().flush();
    }

    public void sendError(int sc) throws IOException {
        sendError(sc, null);
    }

    public void sendError(int sc, String msg) throws IOException {
        if (!this.includeOutputStreams.isEmpty()) {
            Logger.log(Logger.ERROR, Launcher.RESOURCES, "IncludeResponse.Error",
                    new String[] { "" + sc, msg });
            return;
        }
        
        Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                "WinstoneResponse.SendingError", new String[] { "" + sc, msg });

        if ((this.webAppConfig != null) && (this.req != null)) {
//                && (this.webAppConfig.getErrorPagesByCode().get("" + sc) != null)) {
//            String errorPage = (String) this.webAppConfig.getErrorPagesByCode().get("" + sc);
            
            RequestDispatcher rd = this.webAppConfig
                    .getErrorDispatcherByCode(sc, msg, null);
            if (rd != null) {
                try {
                    rd.forward(this.req, this);
                    return;
                } catch (IllegalStateException err) {
                    throw err;
                } catch (IOException err) {
                    throw err;
                } catch (Throwable err) {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES,
                            "WinstoneResponse.ErrorInErrorPage", new String[] {
                                    rd.getName(), sc + "" }, err);
                    return;
                }
            }
        }
        // If we are here there was no webapp and/or no request object, so 
        // show the default error page
        if (this.errorStatusCode == null) {
            this.statusCode = sc;
        }
        String output = Launcher.RESOURCES.getString("WinstoneResponse.ErrorPage",
                new String[] { sc + "", (msg == null ? "" : msg), "",
                        Launcher.RESOURCES.getString("ServerVersion"),
                        "" + new Date() });
        setContentLength(output.getBytes(getCharacterEncoding()).length);
        Writer out = getWriter();
        out.write(output);
        out.flush();
    }

    /**
     * @deprecated
     */
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    /**
     * @deprecated
     */
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    /**
     * @deprecated
     */
    public void setStatus(int sc, String sm) {
        setStatus(sc);
    }
}
