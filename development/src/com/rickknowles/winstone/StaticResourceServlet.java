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

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Servlet to handle static resources. Simply finds an sends them, or
 * dispatches to the error servlet.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class StaticResourceServlet extends HttpServlet
{
  final String CACHED_RESOURCE_DATE_HEADER = "If-Modified-Since";
  final String LAST_MODIFIED_DATE_HEADER   = "Last-Modified";

  private DateFormat sdfFileDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

  private String webRoot;
  private String prefix;
  private boolean directoryList;
  private String welcomeFiles[];
  private Map mimeTypes;

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    this.webRoot = config.getInitParameter("webRoot");
    this.prefix = config.getInitParameter("prefix");
    String dirList = config.getInitParameter("directoryList");
    this.directoryList = (dirList == null) || dirList.equalsIgnoreCase("true")
                                           || dirList.equalsIgnoreCase("yes");

    int welcomeFileCount = Integer.parseInt(config.getInitParameter("welcomeFileCount"));
    this.welcomeFiles = new String[welcomeFileCount];
    for (int n = 0; n < welcomeFileCount; n++)
      this.welcomeFiles[n] = config.getInitParameter("welcomeFile_" + n);

    this.mimeTypes = new Hashtable();
    this.mimeTypes.put("jpg", "image/jpeg");
    this.mimeTypes.put("jpeg", "image/jpeg");
    this.mimeTypes.put("gif", "image/gif");
    this.mimeTypes.put("css", "text/css");
    this.mimeTypes.put("js", "text/javascript");
  }

  private void setMimeType(String fileName, HttpServletResponse response)
  {
    int dotPos = fileName.lastIndexOf('.');
    if ((dotPos != -1) && (dotPos != fileName.length() - 1))
    {
      String extension = fileName.substring(dotPos + 1).toLowerCase();
      String mimeType = (String) this.mimeTypes.get(extension);
      if (mimeType != null)
        response.setContentType(mimeType);
    }
  }

  private String trimHostName(String input)
  {
    if (input == null)
      return null;
    else if (input.startsWith("/"))
      return input;

    int hostStart = input.indexOf("://");
    if (hostStart == -1)
      return input;
    String hostName = input.substring(hostStart + 3);
    int pathStart = hostName.indexOf('/');
    if (pathStart == -1)
      return "/";
    else
      return hostName.substring(pathStart);
  }

  private String trimQueryString(String input)
  {
    if (input == null)
      return null;

    int questionPos = input.indexOf('?');
    if (questionPos == -1)
      return input;
    else
      return input.substring(0, questionPos);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    // Trim the host name if supplied
    String requestURI = trimQueryString(trimHostName(request.getRequestURI()));

    // Get the URI from the servlet, check for prefix
    String path = null;
    if (this.prefix == null)
      path = requestURI;
    else if (requestURI.startsWith(this.prefix))
      path = requestURI.substring(this.prefix.length());
    else
      path = requestURI;

    long cachedResDate = request.getDateHeader(CACHED_RESOURCE_DATE_HEADER);
    Logger.log(Logger.DEBUG, "SRP: path=" + path);

    // Check for the resource
    File res = path.equals("") ? new File(this.webRoot) : new File(this.webRoot, path);

    // Check we are below the webroot
    if (!res.getCanonicalPath().startsWith(this.webRoot))
      response.sendError(response.SC_FORBIDDEN, "Illegal path error - " + path);

    // Check we are below the webroot
    else if (path.toUpperCase().startsWith("/WEB-INF"))
      response.sendError(response.SC_FORBIDDEN, "Illegal path error - " + path);

    // Send a 404 if not found
    else if (!res.exists())
      response.sendError(response.SC_NOT_FOUND, "File " + path + " not found");

    // check for the directory case
    else if (res.isDirectory())
    {
      if (path.endsWith("/"))
      {
        // Try to match each of the welcome files
        String matchedWelcome = matchWelcomeFiles(path, res);
        if (matchedWelcome != null)
          response.sendRedirect(requestURI + matchedWelcome);
        else if (this.directoryList)
          generateDirectoryList(request, response, path);
        else
          response.sendError(response.SC_FORBIDDEN, "Access to this resource is denied");
      }
      else
        response.sendRedirect(requestURI + "/");
    }

    // Send a 304 if not modified
    else if ((cachedResDate != -1) &&
          (cachedResDate < System.currentTimeMillis()) &&
          (cachedResDate >= res.lastModified()))
    {
      setMimeType(res.getName().toLowerCase(), response);
      response.setStatus(response.SC_NOT_MODIFIED);
      response.setContentLength(0);
      response.getOutputStream().close();
    }

    // Write out the resource
    else
    {
      setMimeType(res.getName().toLowerCase(), response);
      InputStream resStream = new FileInputStream(res);
      response.setStatus(response.SC_OK);
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
    //WebAppConfiguration wac = (WebAppConfiguration) getServletConfig().getServletContext();
    for (int n = 0; n < this.welcomeFiles.length; n++)
    {
      String welcomeFile = this.welcomeFiles[n];
      Set subfiles = getServletConfig().getServletContext().getResourcePaths(path);
      Logger.log(Logger.DEBUG, "Testing welcome file: " + this.prefix + path + welcomeFile);
      if (subfiles.contains(this.prefix + path + welcomeFile))
        return welcomeFile;
      // check for servlets
      //if (wac.getRequestDispatcher(path + welcomeFile, false) != null)
      //  return welcomeFile;
      //File wf = new File(res, welcomeFile);
      //if (wf.exists())
      //  return welcomeFile;
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

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println("<html>");
    pw.println("<head><title>Winstone directory listing - " +
                  (path.equals("") ? "/" : path) + "</title></head>");
    pw.println("<body bgcolor=\"#ffffff\">");

    // Heading
    pw.println("<table border=\"0\" width=\"#90%\">");
    pw.println("<tr><td bgcolor=\"#ffffff\"><h1><font color=\"#000033\">Directory - " +
                  (path.equals("") ? "/" : path) + "</font></h1></td></tr>");
    pw.println("</table>");
    pw.println("<hr width=\"90%\" size=\"1\">");

    // Files section
    pw.println("<center>");
    pw.println("<table border=\"0\" width=\"90%\">");
    int offset = 0;
    if (!path.equals("") && !path.equals("/"))
    {
      writeLabels(pw);
      writeDirectoryRow("Parent directory",  "..", dir.getParentFile(), pw, 0, "");
      offset++;
    }
    else
      writeLabels(pw);

    for (int n = 0; n < children.length; n++)
      if (!children[n].getName().equalsIgnoreCase("web-inf"))
        writeDirectoryRow(children[n].getName(), children[n].getName(), children[n],
                          pw, n + offset, (path.startsWith("/") ? path.substring(1) : path) + "/");
    pw.println("</table>");
    pw.println("</center>");

    // Footer
    pw.println("<hr width=\"90%\" size=\"1\">");
    pw.println("<i>Directory list generated by " +
              getServletConfig().getServletContext().getServerInfo() +
              " at " + new Date());
    pw.println("</body>");
    pw.println("</html>");
    pw.flush();

    String out = sw.toString();
    response.setContentLength(out.getBytes().length);
    response.setContentType("text/html");
    Writer w = response.getWriter();
    w.write(out);
    w.close();
  }

  private void writeLabels(PrintWriter pw)
    throws IOException
  {
    pw.println("<tr bgcolor=\"#aeaeae\" color=\"#000033\">");
    pw.println("<td align=\"center\"><font color=\"white\"><b>Name</b></font></td>");
    pw.println("<td align=\"center\" width=\"100\"><font color=\"white\"><b>Size</b></font></td>");
    pw.println("<td align=\"center\" width=\"150\"><font color=\"white\"><b>Date</b></font></td>");
    pw.println("</tr>");
  }

  private void writeDirectoryRow(String label, String path, File file,
                                 PrintWriter pw, int rowCount, String parentPath)
    throws IOException
  {
    String rowColor = (rowCount % 2 == 0 ? "#cbcbcb" : "#dddddd");
    pw.println("<tr bgcolor=\"" + rowColor + "\" color=\"#000033\">");
    pw.println("<td><a href=\"./" + path + (file.isDirectory() ? "/" : "") + "\">" +
                label + (file.isDirectory() ? "/" : "") + "</a></td>");
    pw.println("<td align=\"right\">" +
              (file.isDirectory() ? "(directory)" : "" + file.length()) +
              "</td>");
    pw.println("<td align=\"right\">" +
              (file.isDirectory() ? "-" :
                     this.sdfFileDate.format(new Date(file.lastModified()))) +
              "</td>");
    pw.println("</tr>");
  }
}

