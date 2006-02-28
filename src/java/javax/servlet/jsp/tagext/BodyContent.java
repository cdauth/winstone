/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class BodyContent extends JspWriter {
    private JspWriter enclosingWriter;

    protected BodyContent(JspWriter e) {
        super(e.getBufferSize(), e.isAutoFlush());
        this.enclosingWriter = e;
    }

    public void clearBody() {
        try {
            clear();
        } catch (IOException err) {
            err.printStackTrace();
            throw new RuntimeException("Error in clearBody");
        }
    }

    public void flush() throws IOException {
        throw new IOException("Flush is illegal");
    }

    public JspWriter getEnclosingWriter() {
        return this.enclosingWriter;
    }

    public abstract java.io.Reader getReader();

    public abstract java.lang.String getString();

    public abstract void writeOut(java.io.Writer out) throws IOException;
}
