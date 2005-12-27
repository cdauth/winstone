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
package winstone.jndi;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import winstone.Logger;

/**
 * Implements a simple web.xml + command line arguments style jndi manager
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebAppJNDIManager extends ContainerJNDIManager {
    final static String ELEM_ENV_ENTRY = "env-entry";
    final static String ELEM_ENV_ENTRY_NAME = "env-entry-name";
    final static String ELEM_ENV_ENTRY_TYPE = "env-entry-type";
    final static String ELEM_ENV_ENTRY_VALUE = "env-entry-value";

    /**
     * Gets the relevant list of objects from the args, validating against the
     * web.xml nodes supplied. All node addresses are assumed to be relative to
     * the java:/comp/env context
     */
    public WebAppJNDIManager(Map args, List webXMLNodes, ClassLoader loader) {
        super(args, webXMLNodes, loader);

        // If the webXML nodes are not null, validate that all the entries we
        // wanted have been created
        if (webXMLNodes != null)
            for (Iterator i = webXMLNodes.iterator(); i.hasNext();) {
                Node node = (Node) i.next();

                // Extract the env-entry nodes and create the objects
                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                else if (node.getNodeName().equals(ELEM_ENV_ENTRY)) {
                    String name = null;
                    String type = null;
                    String value = null;
                    for (int m = 0; m < node.getChildNodes().getLength(); m++) {
                        Node envNode = node.getChildNodes().item(m);
                        if (envNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (envNode.getNodeName().equals(
                                ELEM_ENV_ENTRY_NAME))
                            name = envNode.getFirstChild().getNodeValue()
                                    .trim();
                        else if (envNode.getNodeName().equals(
                                ELEM_ENV_ENTRY_TYPE))
                            type = envNode.getFirstChild().getNodeValue()
                                    .trim();
                        else if (envNode.getNodeName().equals(
                                ELEM_ENV_ENTRY_VALUE))
                            value = envNode.getFirstChild().getNodeValue()
                                    .trim();
                    }
                    if ((name != null) && (type != null) && (value != null)) {
                        Logger.log(Logger.FULL_DEBUG, JNDI_RESOURCES,
                                "WebAppJNDIManager.CreatingResourceWebXML",
                                name);
                        Object obj = createObject(name, type, value, args, loader);
                        if (obj != null)
                            this.objectsToCreate.put(name, obj);
                    }
                }
            }
    }

}
