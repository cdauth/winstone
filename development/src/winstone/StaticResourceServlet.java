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
  final String JSP_FILE = "org.apache.catalina.jsp_file";
  final String CACHED_RESOURCE_DATE_HEADER = "If-Modified-Since";
  final String LAST_MODIFIED_DATE_HEADER   = "Last-Modified";

  final String RESOURCE_FILE    = "winstone.LocalStrings";

  private DateFormat sdfFileDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

  private String webRoot;
  private String prefix;
  private boolean directoryList;
  private String welcomeFiles[];
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

    int welcomeFileCount = Integer.parseInt(config.getInitParameter("welcomeFileCount"));
    this.welcomeFiles = new String[welcomeFileCount];
    for (int n = 0; n < welcomeFileCount; n++)
      this.welcomeFiles[n] = config.getInitParameter("welcomeFile_" + n);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {doGet(request, response);}
  
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    // Trim the host name if supplied
    String path = (String) request.getAttribute(JSP_FILE);
    String isIncludeStr = (String) request.getAttribute("winstone.requestDispatcher.include");
    boolean isInclude = ((isIncludeStr != null) && isIncludeStr.equals("true"));
    
    long cachedResDate = request.getDateHeader(CACHED_RESOURCE_DATE_HEADER);
    Logger.log(Logger.DEBUG, this.resources.getString("StaticResourceServlet.PathRequested",
              "[#name]", getServletConfig().getServletName(), "[#path]", path));

    // Check for the resource
    File res = path.equals("") ? new File(this.webRoot) : new File(this.webRoot, path);

    // Send a 404 if not found
    if (!res.exists())
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
            this.resources.getString("StaticResourceServlet.PathNotFound",
              "[#path]", path));

    // Check we are below the webroot
    else if (!res.getCanonicalPath().startsWith(this.webRoot))
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
            this.resources.getString("StaticResourceServlet.PathInvalid",
              "[#path]", path));

    // Check we are not below the web-inf
    else if (!isInclude && res.getCanonicalPath().startsWith(new File(this.webRoot, "WEB-INF").getPath()))
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
            this.resources.getString("StaticResourceServlet.PathInvalid",
              "[#path]", path));

    // check for the directory case
    else if (res.isDirectory())
    {
      if (path.endsWith("/"))
      {
        // Try to match each of the welcome files
        String matchedWelcome = matchWelcomeFiles(path, res);
        if (matchedWelcome != null)
          response.sendRedirect(this.prefix + path + matchedWelcome);
        else if (this.directoryList)
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

    // Write out the resource
    else
    {
      String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
      if (mimeType != null)
        response.setContentType(mimeType);
      InputStream resStream = new FileInputStream(res);
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentLength((int)res.length());
      response.addDateHeader(LAST_MODIFIED_DATE_HEADER, res.lastModified());
      OutputStream out = response.getOutputStream();
      byte buffer[] = new byte[1024];
      while (resStream.available() > 0)
      {
        int read = resStream.read(buffer);
        out.write(buffer, 0, read);
      }
      out.close();
    }
  }

  private String matchWelcomeFiles(String path, File res)
  {
    for (int n = 0; n < this.welcomeFiles.length; n++)
    {
      String welcomeFile = this.welcomeFiles[n];
      Set subfiles = getServletConfig().getServletContext().getResourcePaths(path);
      Logger.log(Logger.DEBUG,
            this.resources.getString("StaticResourceServlet.TestingWelcomeFile",
              "[#welcomeFile]", this.prefix + path + welcomeFile));
      if (subfiles.contains(this.prefix + path + welcomeFile))
        return welcomeFile;
    }
    return null;
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

    Map rowKeys = new HashMap();
    rowKeys.put("[#rowTextColour]", rowTextColour);
    int rowCount = 0;

    // Write the parent dir row
    if (!path.equals("") && !path.equals("/"))
    {
      rowKeys.put("[#rowColour]", evenColour);
      rowKeys.put("[#fileLabel]", parentDirLabel);
      rowKeys.put("[#fileHref]", "..");
      rowKeys.put("[#fileDate]", noDateLabel);
      rowKeys.put("[#fileLength]", directoryLabel);
      rowString.write(this.resources.getString("StaticResourceServlet.DirectoryList.Row", rowKeys));
      rowCount++;
    }

    // Write the rows for each file
    for (int n = 0; n < children.length; n++)
      if (!children[n].getName().equalsIgnoreCase("web-inf"))
      {
        File file = children[n];
        rowKeys.put("[#rowColour]", rowCount % 2 == 0 ? evenColour : oddColour);
        rowKeys.put("[#fileLabel]", file.getName() + (file.isDirectory() ? "/" : ""));
        rowKeys.put("[#fileHref]", "./" + file.getName() + (file.isDirectory() ? "/" : ""));
        rowKeys.put("[#fileDate]", file.isDirectory() ? noDateLabel : sdfFileDate.format(new Date(file.lastModified())));
        rowKeys.put("[#fileLength]", file.isDirectory() ? directoryLabel : "" + file.length());
        rowString.write(this.resources.getString("StaticResourceServlet.DirectoryList.Row", rowKeys));
        rowCount++;
      }

    // Build wrapper body
    Map bodyKeys = new HashMap();
    bodyKeys.put("[#headerColour]", this.resources.getString("StaticResourceServlet.DirectoryList.HeaderColour"));
    bodyKeys.put("[#headerTextColour]", this.resources.getString("StaticResourceServlet.DirectoryList.HeaderTextColour"));
    bodyKeys.put("[#labelColour]", this.resources.getString("StaticResourceServlet.DirectoryList.LabelColour"));
    bodyKeys.put("[#labelTextColour]", this.resources.getString("StaticResourceServlet.DirectoryList.LabelTextColour"));
    bodyKeys.put("[#date]", new Date() + "");
    bodyKeys.put("[#serverVersion]", this.resources.getString("ServerVersion"));
    bodyKeys.put("[#path]", path.equals("") ? "/" : path);
    bodyKeys.put("[#rows]", rowString.toString());
    String out = this.resources.getString("StaticResourceServlet.DirectoryList.Body", bodyKeys);

    response.setContentLength(out.getBytes().length);
    response.setContentType("text/html");
    Writer w = response.getWriter();
    w.write(out);
    w.close();
  }
}

