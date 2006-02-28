/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.jsp.tagext;

import java.util.*;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public abstract class TagLibraryValidator {
    private Map initParameters;

    public TagLibraryValidator() {
        this.initParameters = new Hashtable();
    }

    public Map getInitParameters() {
        return Collections.unmodifiableMap(this.initParameters);
    }

    public void release() {
        this.initParameters.clear();
    }

    public void setInitParameters(Map map) {
        if (map != null) {
            this.initParameters.clear();
            this.initParameters.putAll(map);
        }
    }

    public ValidationMessage[] validate(String prefix, String uri, PageData page) {
        return null;
    }
}
