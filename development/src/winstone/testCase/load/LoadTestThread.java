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
  private boolean keepAlive;
  private int expectedTotal;
  private int successCount;
  private boolean interrupted;
  private Thread thread;
  
  public LoadTestThread(String url, boolean keepAlive, int expectedTotal,
      WinstoneResourceBundle resources)
  {
    this.resources = resources;
    this.url = url;
    this.keepAlive = keepAlive;
    this.expectedTotal = expectedTotal;
    this.interrupted = false;
    this.thread = new Thread(this);
    this.thread.setDaemon(true);
    this.thread.start();
  }
  
  public void run() 
  {
    WebConversation wc = null;
    for (int n = 0; (n < this.expectedTotal) && !this.interrupted; n++)
    {
      long startTime = System.currentTimeMillis();
      
      try
      {
        if (!this.keepAlive || (wc == null))
          wc = new WebConversation();
        
        // Access the URL
        WebRequest wreq = new GetMethodWebRequest(this.url);
        WebResponse wresp = wc.getResponse(wreq);
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
          this.successCount++;
        else
          throw new IOException("Only downloaded " + position + " of " + contentLength + " bytes");
      }
      catch (IOException err) {Logger.log(Logger.DEBUG, resources, "LoadTestThread.Error", err);}
      catch (SAXException err) {Logger.log(Logger.DEBUG, resources, "LoadTestThread.Error", err);}

      try
      {
        // Make it sleep the rest of the second
        if (System.currentTimeMillis() - startTime < 1000)
          Thread.sleep(1000 - (System.currentTimeMillis() - startTime));
      }
      catch (InterruptedException err) {this.interrupted = true;}
    }
  }
  
  public void destroy() 
  {
    this.interrupted = true;
    this.thread.interrupt();
  }
  
  public int getSuccessCount() {return this.successCount;}
  public int getErrorCount() {return this.expectedTotal - this.successCount;}
}
