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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

/**
 * A utility class for logging event and status messages. It maintains a
 * collection of streams for different types of messages, but any messages with
 * unknown or unspecified stream go to the default stream.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Logger {
    public final static String DEFAULT_STREAM = "Winstone";
    public static int MIN = 1;
    public static int ERROR = 2;
    public static int WARNING = 3;
    public static int INFO = 5;
    public static int SPEED = 6;
    public static int DEBUG = 7;
    public static int FULL_DEBUG = 8;
    public static int MAX = 9;

    protected static Boolean semaphore = new Boolean(true);
    protected static boolean initialised = false;
    protected static Map streams;
    protected static Collection nullStreams;
    protected static int currentDebugLevel;
    protected static DateFormat sdfLog = new SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss");
    protected static boolean showThrowingLineNo;

    /**
     * Initialises default streams
     */
    public static void init(int level) {
        init(level, System.out, false);
    }

    /**
     * Initialises default streams
     */
    public static void init(int level, OutputStream defaultStream, boolean showThrowingLineNoArg) {
        currentDebugLevel = level;
        streams = new Hashtable();
        nullStreams = new ArrayList();
        initialised = true;
        setStream(DEFAULT_STREAM, defaultStream);
        showThrowingLineNo = showThrowingLineNoArg;
    }

    /**
     * Allocates a stream for redirection to a file etc
     */
    public static void setStream(String name, OutputStream stream) {
        if (stream == null)
            nullStreams.add(name);
        else
            setStream(name, new PrintWriter(stream));
    }

    /**
     * Allocates a stream for redirection to a file etc
     */
    public static void setStream(String name, PrintWriter stream) {
        // synchronized (semaphore)
        {
            if (!initialised)
                init(INFO);
            if (stream == null)
                nullStreams.add(name);
            else
                streams.put(name, stream);
        }
    }

    /**
     * Forces a flush of the contents to file, display, etc
     */
    public static void flush(String name) {
        // synchronized (semaphore)
        {
            if (!initialised)
                init(INFO);
            PrintWriter p = (PrintWriter) streams.get(name);
            if (p != null)
                p.flush();
        }
    }

    public static void setCurrentDebugLevel(int level) {
        if (!initialised)
            init(level);
        else
            currentDebugLevel = level;
    }

    /**
     * Writes a log message to the default stream / public static void log(int
     * level, String message) { log(level, DEFAULT_STREAM, message); }
     * 
     * /** Writes a log message to the default stream / public static void
     * log(int level, String message, Throwable error) { log(level,
     * DEFAULT_STREAM, message, error); }
     * 
     * /** Writes a log message to the default stream / public static void
     * log(int level, String streamName, String message) { log(level,
     * streamName, message, null); }
     * 
     * /** Writes a log message to the requested stream, and immediately flushes
     * the contents of the stream.
     */
    private static void logInternal(String streamName, String message, Throwable error) {
        
        if (!initialised) {
            init(INFO);
        }
        
        String lineNoText = "";
        if (showThrowingLineNo) {
            Throwable dummyError = new RuntimeException();
            StackTraceElement[] elements = dummyError.getStackTrace();
            int elemNumber = Math.min(2, elements.length);
            String errorClass = elements[elemNumber].getClassName();
            if (errorClass.lastIndexOf('.') != -1) {
                errorClass = errorClass.substring(errorClass.lastIndexOf('.') + 1);
            }
            lineNoText = "[" + errorClass + ":" + elements[elemNumber].getLineNumber() + "] - "; 
        }

        PrintWriter stream = (PrintWriter) streams.get(streamName);
        boolean nullStream = nullStreams.contains(streamName);
        if ((stream == null) && !nullStream)
            stream = (PrintWriter) streams.get(DEFAULT_STREAM);

        if (stream != null) {
            StringBuffer fullMessage = new StringBuffer();
            fullMessage.append("[").append(streamName).append(" ").append(
                    sdfLog.format(new Date())).append("] - ").append(
                            lineNoText).append(message);
            if (error != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                error.printStackTrace(pw);
                pw.flush();
                fullMessage.append('\n').append(sw.toString());
            }
            stream.println(fullMessage.toString());
            stream.flush();
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(DEFAULT_STREAM, resources.getString(messageKey), null);
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, Throwable error) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(DEFAULT_STREAM, resources.getString(messageKey), error);
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String param) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, param), null);
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String params[]) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, params), null);
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String param, Throwable error) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, param), error);
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String params[], Throwable error) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, params), error);
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String streamName, String messageKey, String params[], Throwable error) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(streamName, resources.getString(messageKey, params), error);
    }

    public static void logDirectMessage(int level, String streamName, String message, 
            Throwable error) {
        if (currentDebugLevel < level)
            return;
        else
            logInternal(streamName, message, error);
    }
}