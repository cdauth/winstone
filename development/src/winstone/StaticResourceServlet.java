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

import javax.servlet.http.*;
import javax.servlet.*;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Servlet to handle static resources. Simply finds and sends them, or
 * dispatches to the error servlet.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class StaticResourceServlet extends HttpServlet
{
  //final String JSP_FILE = "org.apache.catalina.jsp_file";
  final static String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
  final static String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
  
  final static String CACHED_RESOURCE_DATE_HEADER = "If-Modified-Since";
  final static String LAST_MODIFIED_DATE_HEADER   = "Last-Modified";
  final static String RANGE_HEADER   							= "Range";
  final static String ACCEPT_RANGES_HEADER 				= "Accept-Ranges";
  final static String CONTENT_RANGE_HEADER 				= "Content-Range";

  final static String RESOURCE_FILE    = "winstone.LocalStrings";

  private DateFormat sdfFileDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

  private String webRoot;
  private String prefix;
  private boolean directoryList;
  private WinstoneResourceBundle resources;
  
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    this.resources = new WinstoneResourceBundle(RESOURCE_FILE);
    this.webRoot = config.getInitParameter("webRoot");
    this.prefix = config.getInitParameter("prefix");
    String dirList = config.getInitParameter("directoryList");
    this.directoryList = (dirList == null) || dirList.equalsIgnoreCase("true")
                                           || dirList.equalsIgnoreCase("yes");
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {doGet(request, response);}
  
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    boolean isInclude = (request.getAttribute(INCLUDE_PATH_INFO) != null);
    boolean isForward = (request.getAttribute(FORWARD_PATH_INFO) != null);
    String path = null;
    
    if (isInclude)
      path = (String) request.getAttribute(INCLUDE_PATH_INFO);
    else if (isForward)
      path = (String) request.getAttribute(FORWARD_PATH_INFO);
    else if (request.getPathInfo() != null)
      path = request.getPathInfo();
    else
    	path = "";
    
    long cachedResDate = request.getDateHeader(CACHED_RESOURCE_DATE_HEADER);
    Logger.log(Logger.DEBUG, this.resources, "StaticResourceServlet.PathRequested",
              new String[] {getServletConfig().getServletName(), path});

    // Check for the resource
    File res = path.equals("") ? new File(this.webRoot) : new File(this.webRoot, path);

    // Send a 404 if not found
    if (!res.exists())
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
            this.resources.getString("StaticResourceServlet.PathNotFound", path));

    // Check we are below the webroot
    else if (!res.getCanonicalPath().startsWith(this.webRoot))
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
            this.resources.getString("StaticResourceServlet.PathInvalid", path));

    // Check we are not below the web-inf
    else if (!isInclude && res.getCanonicalPath().toUpperCase().startsWith(new File(this.webRoot, "WEB-INF").getPath().toUpperCase()))
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
            this.resources.getString("StaticResourceServlet.PathInvalid", path));

    // Check we are not below the meta-inf
    else if (!isInclude && res.getCanonicalPath().toUpperCase().startsWith(new File(this.webRoot, "META-INF").getPath().toUpperCase()))
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
            this.resources.getString("StaticResourceServlet.PathInvalid", path));

    // check for the directory case
    else if (res.isDirectory())
    {
      if (path.endsWith("/"))
      {
        // Try to match each of the welcome files
        //String matchedWelcome = matchWelcomeFiles(path, res);
        //if (matchedWelcome != null)
        //  response.sendRedirect(this.prefix + path + matchedWelcome);
        //else 
        if (this.directoryList)
          generateDirectoryList(request, response, path);
        else
          response.sendError(HttpServletResponse.SC_FORBIDDEN,
              this.resources.getString("StaticResourceServlet.AccessDenied"));
      }
      else
        response.sendRedirect(this.prefix + path + "/");
    }

    // Send a 304 if not modified
    else if ((cachedResDate != -1) &&
          (cachedResDate < System.currentTimeMillis()) &&
          (cachedResDate >= res.lastModified()))
    {
      String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
      if (mimeType != null)
        response.setContentType(mimeType);
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      response.setContentLength(0);
      response.getOutputStream().close();
    }

    // Write out the resource if not range
    else if (request.getHeader(RANGE_HEADER) == null)
    {
      String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
      if (mimeType != null)
        response.setContentType(mimeType);
      InputStream resStream = new FileInputStream(res);

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentLength((int) res.length());
      response.addHeader(ACCEPT_RANGES_HEADER, "bytes");
      response.addDateHeader(LAST_MODIFIED_DATE_HEADER, res.lastModified());
      OutputStream out = null;
      Writer outWriter = null;
      try 
        {out = response.getOutputStream();} 
      catch (IOException err) 
        {outWriter = response.getWriter();}
      byte buffer[] = new byte[4096];
      int read = resStream.read(buffer);
      while (read > 0)
      {
        if (out != null) 
          out.write(buffer, 0, read);
        else
          outWriter.write(new String(buffer, 0, read, response.getCharacterEncoding()));
        read = resStream.read(buffer);
      }
      resStream.close();
    }
    else if (request.getHeader(RANGE_HEADER).startsWith("bytes="))
    {
      String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
      if (mimeType != null)
        response.setContentType(mimeType);
      InputStream resStream = new FileInputStream(res);

      List ranges = new ArrayList();
      StringTokenizer st = new StringTokenizer(request.getHeader(RANGE_HEADER)
                                                .substring(6).trim(), ",", false);
      int totalSent = 0;
      String rangeText = "";
      while (st.hasMoreTokens())
      {
        String rangeBlock = st.nextToken();
        int start = 0;
        int end = (int) res.length();
        int delim = rangeBlock.indexOf('-');
        if (delim != 0)
          start = Integer.parseInt(rangeBlock.substring(0, delim).trim());
        if (delim != rangeBlock.length() - 1)
          end = Integer.parseInt(rangeBlock.substring(delim + 1).trim());
        totalSent += (end - start);
        rangeText += "," + start + "-" + end;
        ranges.add(start + "-" + end);
      }
      response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
      response.addHeader(CONTENT_RANGE_HEADER, "bytes " + rangeText.substring(1) + "/" + res.length());
      response.setContentLength(totalSent);
      
      response.addHeader(ACCEPT_RANGES_HEADER, "bytes");
      response.addDateHeader(LAST_MODIFIED_DATE_HEADER, res.lastModified());
      OutputStream out = response.getOutputStream();
      int bytesRead = 0;
      for (Iterator i = ranges.iterator(); i.hasNext(); )
      {
        String rangeBlock = (String) i.next();
        int delim = rangeBlock.indexOf('-');
        int start = Integer.parseInt(rangeBlock.substring(0, delim));
        int end = Integer.parseInt(rangeBlock.substring(delim + 1));
        int read = 0;
        while ((read != -1) && (bytesRead <= res.length()))
        {
          read = resStream.read();
          if ((bytesRead >= start) && (bytesRead < end))
            out.write(read);
          bytesRead++;
        }
      }
      resStream.close();
    }
    else
      response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
  }

  /**
   * Generate a list of the files in this directory
   */
  private void generateDirectoryList(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String path)
    throws ServletException, IOException
  {
    // Get the file list
    File dir = path.equals("") ? new File(this.webRoot) : new File(this.webRoot, path);
    File children[] = dir.listFiles();
    Arrays.sort(children);

    // Build row content
    StringWriter rowString = new StringWriter();
    String oddColour      = this.resources.getString("StaticResourceServlet.DirectoryList.OddColour");
    String evenColour     = this.resources.getString("StaticResourceServlet.DirectoryList.EvenColour");
    String rowTextColour  = this.resources.getString("StaticResourceServlet.DirectoryList.RowTextColour");

    String directoryLabel = this.resources.getString("StaticResourceServlet.DirectoryList.DirectoryLabel");
    String parentDirLabel = this.resources.getString("StaticResourceServlet.DirectoryList.ParentDirectoryLabel");
    String noDateLabel    = this.resources.getString("StaticResourceServlet.DirectoryList.NoDateLabel");

    int rowCount = 0;

    // Write the parent dir row
    if (!path.equals("") && !path.equals("/"))
    {
      rowString.write(this.resources.getString("StaticResourceServlet.DirectoryList.Row", new String[] 
        {rowTextColour, evenColour, parentDirLabel, "..", noDateLabel, directoryLabel}));
      rowCount++;
    }

    // Write the rows for each file
    for (int n = 0; n < children.length; n++)
      if (!children[n].getName().equalsIgnoreCase("web-inf"))
      {
        File file = children[n];
        rowString.write(this.resources.getString("StaticResourceServlet.DirectoryList.Row", new String[] {
            rowTextColour,
            rowCount % 2 == 0 ? evenColour : oddColour,
            file.getName() + (file.isDirectory() ? "/" : ""),
            "./" + file.getName() + (file.isDirectory() ? "/" : ""),
            file.isDirectory() ? noDateLabel : sdfFileDate.format(new Date(file.lastModified())),
            file.isDirectory() ? directoryLabel : "" + file.length()}));
        rowCount++;
      }

    // Build wrapper body
    String out = this.resources.getString("StaticResourceServlet.DirectoryList.Body", new String[] {
        this.resources.getString("StaticResourceServlet.DirectoryList.HeaderColour"),
        this.resources.getString("StaticResourceServlet.DirectoryList.HeaderTextColour"),
        this.resources.getString("StaticResourceServlet.DirectoryList.LabelColour"),
        this.resources.getString("StaticResourceServlet.DirectoryList.LabelTextColour"),
        new Date() + "",
        this.resources.getString("ServerVersion"),
        path.equals("") ? "/" : path,
        rowString.toString()});

    response.setContentLength(out.getBytes().length);
    response.setContentType("text/html");
    Writer w = response.getWriter();
    w.write(out);
    w.close();
  }
}

