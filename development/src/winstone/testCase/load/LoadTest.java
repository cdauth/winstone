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
import java.util.*;
import winstone.*;

/**
 * This class is an attempt to benchmark performance under load for winstone.
 * It works by hitting a supplied URL with parallel threads (with keep-alives or
 * without) at an escalating rate, and counting the no of failures.
 * 
 * It uses HttpUnit's WebConversation class for the connection.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class LoadTest
{
  private String url;
  private boolean useKeepAlives;
  private int startThreads;
  private int endThreads;
  private int stepSize;
  private long stepPeriod;
  private WinstoneResourceBundle resources; 
  private static String LOCAL_RESOURCE_FILE = "winstone.testCase.load.LocalStrings";
  
  public LoadTest(WinstoneResourceBundle resources, String url, boolean useKeepAlives,
      int startThreads, int endThreads, int stepSize, long stepPeriod)
  {
    this.resources = resources;
    this.url = url;
    this.useKeepAlives = useKeepAlives;
    this.startThreads = startThreads;
    this.endThreads = endThreads;
    this.stepSize = stepSize;
    this.stepPeriod = stepPeriod;
    
    Logger.log(Logger.INFO, resources, "LoadTest.Config", new String[] {this.url, 
        this.useKeepAlives + "", this.startThreads + "", this.endThreads + "",
        this.stepSize + "", this.stepPeriod + ""});
  }
  
  public void test() throws InterruptedException
  {
    // Loop through in steps
    for (int n = this.startThreads; n < this.endThreads; n += this.stepSize)
    {
      // Spawn the threads
      List threads = new ArrayList();
      for (int m = 0; m < n; m++) 
        threads.add(new LoadTestThread(this.url, this.useKeepAlives, 
            (int) this.stepPeriod / 1000, this.resources));
      
      // Sleep for step period
      Thread.sleep(this.stepPeriod + 2000);
      
      // Count the errors
      int errorCount = 0;
      int successCount = 0;
      for (Iterator i = threads.iterator(); i.hasNext(); )
      {
        LoadTestThread ltt = (LoadTestThread) i.next();
        errorCount += ltt.getErrorCount();
        successCount += ltt.getSuccessCount();
      }
      
      // Write out results
      Logger.log(Logger.INFO, resources, "LoadTest.LineResult", new String[] {n + "",
          successCount + "", errorCount + ""});
      
      // Close threads
      for (Iterator i = threads.iterator(); i.hasNext(); )
        ((LoadTestThread) i.next()).destroy();
    }
  }
  
  public static void main(String args[]) throws Exception
  {
    WinstoneResourceBundle resources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    
    // Loop for args
    Map options = new HashMap();
    String operation = "";
    for (int n = 0; n < args.length;  n++)
    {
      String option = args[n];
      if (option.startsWith("--"))
      {
        int equalPos = option.indexOf('=');
        String paramName = option.substring(2, equalPos == -1 ? option.length() : equalPos);
        String paramValue = (equalPos == -1 ? "true" : option.substring(equalPos + 1));
        options.put(paramName, paramValue);
      }
    }
    
    if (options.size() == 0)
    {
      printUsage(resources);
      return;
    }
    Logger.setCurrentDebugLevel(Integer.parseInt(WebAppConfiguration.stringArg(options, "debug", "5")));

    String url = WebAppConfiguration.stringArg(options, "url", "http://localhost:8080/");
    boolean keepAlive = WebAppConfiguration.booleanArg(options, "keepAlive", true);
    String startThreads = WebAppConfiguration.stringArg(options, "startThreads", "20");
    String endThreads = WebAppConfiguration.stringArg(options, "endThreads", "1000");
    String stepSize = WebAppConfiguration.stringArg(options, "stepSize", "20");
    String stepPeriod = WebAppConfiguration.stringArg(options, "stepPeriod", "5000");

    LoadTest lt = new LoadTest(resources, url, keepAlive, Integer.parseInt(startThreads),
        Integer.parseInt(endThreads), Integer.parseInt(stepSize), Integer.parseInt(stepPeriod));
    
    lt.test();
  }
  
  /**
   * Displays the usage message
   */
  private static void printUsage(WinstoneResourceBundle resources) throws IOException
  {
    System.out.println(resources.getString("LoadTest.Usage"));
  }

}
