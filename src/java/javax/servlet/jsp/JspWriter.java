/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp;

import java.io.IOException;
import java.io.Writer;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class JspWriter extends Writer {
    protected boolean autoFlush;
    protected int bufferSize;

    public static final int DEFAULT_BUFFER = -1;
    public static final int NO_BUFFER = 0;
    public static final int UNBOUNDED_BUFFER = -2;

    protected JspWriter(int bufferSize, boolean autoFlush) {
        super();
        this.autoFlush = autoFlush;
        this.bufferSize = bufferSize;
    }

    public abstract void clear() throws IOException;

    public abstract void clearBuffer() throws IOException;

    public abstract void close() throws IOException;

    public abstract void flush() throws IOException;

    public int getBufferSize() {
        return this.bufferSize;
    }

    public abstract int getRemaining();

    public boolean isAutoFlush() {
        return this.autoFlush;
    }

    public abstract void newLine() throws IOException;

    public abstract void print(boolean b) throws IOException;

    public abstract void print(char c) throws IOException;

    public abstract void print(char[] s) throws IOException;

    public abstract void print(double d) throws IOException;

    public abstract void print(float f) throws IOException;

    public abstract void print(int i) throws IOException;

    public abstract void print(long l) throws IOException;

    public abstract void print(Object obj) throws IOException;

    public abstract void print(String s) throws IOException;

    public abstract void println() throws IOException;

    public abstract void println(boolean x) throws IOException;

    public abstract void println(char x) throws IOException;

    public abstract void println(char[] x) throws IOException;

    public abstract void println(double x) throws IOException;

    public abstract void println(float x) throws IOException;

    public abstract void println(int x) throws IOException;

    public abstract void println(long x) throws IOException;

    public abstract void println(java.lang.Object x) throws IOException;

    public abstract void println(java.lang.String x) throws IOException;

}
