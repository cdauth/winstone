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
package winstone.tools;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import winstone.Launcher;
import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;

/**
 * Included so that we can control winstone from the command line a little more
 * easily.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WinstoneControl {
    private final static WinstoneResourceBundle TOOLS_RESOURCES = new WinstoneResourceBundle("winstone.tools.LocalStrings");
    
    final static String OPERATION_SHUTDOWN = "shutdown";
    final static String OPERATION_RELOAD = "reload:";
    static int TIMEOUT = 10000;

    /**
     * Parses command line parameters, and calls the appropriate method for
     * executing the winstone operation required.
     */
    public static void main(String args[]) throws Exception {

        // Loop for args
        Map options = new HashMap();
        String operation = "";
        for (int n = 0; n < args.length; n++) {
            String option = args[n];
            if (option.startsWith("--")) {
                int equalPos = option.indexOf('=');
                String paramName = option.substring(2, equalPos == -1 ? option
                        .length() : equalPos);
                String paramValue = (equalPos == -1 ? "true" : option
                        .substring(equalPos + 1));
                options.put(paramName, paramValue);
            } else
                operation = option;
        }

        if (operation.equals("")) {
            printUsage();
            return;
        }

        Logger.setCurrentDebugLevel(Integer.parseInt(WebAppConfiguration
                .stringArg(options, "debug", "5")));

        String host = WebAppConfiguration.stringArg(options, "host",
                "localhost");
        String port = WebAppConfiguration.stringArg(options, "port", "8081");

        Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.UsingHostPort",
                new String[] { host, port });

        // Check for shutdown
        if (operation.equalsIgnoreCase(OPERATION_SHUTDOWN)) {
            Socket socket = new Socket(host, Integer.parseInt(port));
            socket.setSoTimeout(TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(Launcher.SHUTDOWN_TYPE);
            out.close();
            Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.ShutdownOK",
                    new String[] { host, port });
        }

        // check for reload
        else if (operation.toLowerCase().startsWith(OPERATION_RELOAD.toLowerCase())) {
            String webappName = operation.substring(OPERATION_RELOAD.length());
            Socket socket = new Socket(host, Integer.parseInt(port));
            socket.setSoTimeout(TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(Launcher.RELOAD_TYPE);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeUTF(host);
            objOut.writeUTF(webappName);
            objOut.close();
            out.close();
            Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.ReloadOK",
                    new String[] { host, port });
        }
        else {
            printUsage();
        }
    }

    /**
     * Displays the usage message
     */
    private static void printUsage() throws IOException {
        System.out.println(TOOLS_RESOURCES.getString("WinstoneControl.Usage"));
    }
}
