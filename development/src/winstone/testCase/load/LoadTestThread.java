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
package winstone.testCase.load;

import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.SAXException;
import winstone.*;
import com.meterware.httpunit.*;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class LoadTestThread implements Runnable
{
  private WinstoneResourceBundle resources;
  private String url;
  private long delayBeforeStarting;
  private LoadTest loadTest;
  private WebConversation webConv;
  private Thread thread;
  private LoadTestThread next;
  
  public LoadTestThread(String url, LoadTest loadTest,
      WinstoneResourceBundle resources, WebConversation webConv, int delayedThreads)
  {
    this.resources = resources;
    this.url = url;
    this.loadTest = loadTest;
    this.webConv = webConv;
    this.delayBeforeStarting = 1000 * delayedThreads;
    this.thread = new Thread(this);
    this.thread.setDaemon(true);
    this.thread.start();
    
    // Launch the next second's getter
    if (delayedThreads > 0)
      this.next = new LoadTestThread(url, loadTest, resources, webConv, delayedThreads - 1);
  }
  
  public void run() 
  {
    if (this.delayBeforeStarting > 0)
      try {Thread.sleep(this.delayBeforeStarting);}
      catch (InterruptedException err) {}
    
    long startTime = System.currentTimeMillis();
    
    try
    {
      if (this.webConv == null)
        this.webConv = new WebConversation();
      
      // Access the URL
      WebRequest wreq = new GetMethodWebRequest(this.url);
      WebResponse wresp = this.webConv.getResponse(wreq);
      int responseCode = wresp.getResponseCode();
      if (responseCode >= 400)
        throw new IOException("Failed with status " + responseCode);
      InputStream inContent = wresp.getInputStream();
      int contentLength = wresp.getContentLength() == -1 ? inContent.available() : wresp.getContentLength();
      byte content[] = new byte[contentLength];
      int position = 0;
      while (position < contentLength)
        position += inContent.read(content, position, contentLength - position);
      inContent.close();

      // Confirm the result is the same size the content-length said it was
      if (position == contentLength)
      {
        this.loadTest.incTimeTotal(System.currentTimeMillis() - startTime);
        this.loadTest.incSuccessCount();
      }
      else
        throw new IOException("Only downloaded " + position + " of " + contentLength + " bytes");
    }
    catch (IOException err) {Logger.log(Logger.DEBUG, resources, "LoadTestThread.Error", err);}
    catch (SAXException err) {Logger.log(Logger.DEBUG, resources, "LoadTestThread.Error", err);}
  }
  
  public void destroy() 
  {
    this.thread.interrupt();
    if (this.next != null)
      this.next.destroy();
  }
}
