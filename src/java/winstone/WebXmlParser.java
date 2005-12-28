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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * The web.xml parsing logic. This is used by more than one launcher, so it's shared from here. 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class WebXmlParser implements EntityResolver, ErrorHandler {
    
    static final String DTD_2_2_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    static final String DTD_2_3_PUBLIC = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    static final String XSD_2_4_URL = "http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd";
    static final String XSD_XML_URL = "http://www.w3.org/2001/xml.xsd";
    static final String XSD_DTD_PUBLIC = "-//W3C//DTD XMLSCHEMA 200102//EN";
    static final String DATATYPES_URL = "http://www.w3.org/2001/datatypes.dtd";
    static final String WS_CLIENT_URL = "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd";
    
    static final String DTD_2_2_LOCAL = "javax/servlet/resources/web-app_2_2.dtd";
    static final String DTD_2_3_LOCAL = "javax/servlet/resources/web-app_2_3.dtd";
    static final String XSD_2_4_LOCAL = "javax/servlet/resources/web-app_2_4.xsd";
    static final String XSD_XML_LOCAL = "javax/servlet/resources/xml.xsd";
    static final String XSD_DTD_LOCAL = "javax/servlet/resources/XMLSchema.dtd";
    static final String DATATYPES_LOCAL = "javax/servlet/resources/datatypes.dtd";
    static final String WS_CLIENT_LOCAL = "javax/servlet/resources/j2ee_web_services_client_1_1.xsd";

    private ClassLoader commonLoader;

    public WebXmlParser(ClassLoader commonCL) {
        this.commonLoader = commonCL;
    }
    
    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the
     * web.xml application deployment descriptors.
     */
    protected Document parseStreamToXML(File webXmlFile) {
        try {
            
            // Use JAXP to create a document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setCoalescing(true);
            factory.setIgnoringElementContentWhitespace(true);

            // Test parse as a dtd-only version
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(this);
            builder.setErrorHandler(this);
            Document doc = null;
            String webXmlVersion = null;
            try {
                doc = builder.parse(webXmlFile);
                webXmlVersion = doc.getDocumentElement().getAttribute("version");
            } catch (SAXParseException err) {
                webXmlVersion = "2.4";
            }
            
            // Check for the version="2.4" attribute and reparse with the schema stuff set if found
            if ((webXmlVersion != null) && !webXmlVersion.trim().equals("") && 
                    !webXmlVersion.startsWith("2.2") && !webXmlVersion.startsWith("2.3")) {
                // If we have (and can parse) the 2.4 xsd, set to redirect locally to use it
                try {
                    factory.setAttribute(
                                    "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                                    "http://www.w3.org/2001/XMLSchema");
                    if (this.commonLoader.getResource(XSD_2_4_LOCAL) != null) {
                        factory.setAttribute(
                                        "http://java.sun.com/xml/jaxp/properties/schemaSource",
                                        this.commonLoader.getResource(
                                                XSD_2_4_LOCAL).toString());
                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WebXmlParser.Local24XSDEnabled");
                    } else {
                        Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebXmlParser.24XSDNotFound");
                    }
                } catch (Throwable err) {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES,
                            "WebXmlParser.NonXSDParser");
                }
                builder = factory.newDocumentBuilder();
                builder.setEntityResolver(this);
                builder.setErrorHandler(this);
                doc = builder.parse(webXmlFile);
            }
            return doc;
        } catch (Throwable errParser) {
            throw new WinstoneException(Launcher.RESOURCES
                    .getString("WebXmlParser.WebXMLParseError"), errParser);
        }
    }

    /**
     * Implements the EntityResolver interface. This allows us to redirect any
     * requests by the parser for webapp DTDs to local copies. It's faster and
     * it means you can run winstone without being web-connected.
     */
    public InputSource resolveEntity(String publicName, String url)
            throws SAXException, IOException {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WebXmlParser.ResolvingEntity",
                new String[] { publicName, url });
        if ((publicName != null) && publicName.equals(DTD_2_2_PUBLIC))
            return getLocalResource(url, DTD_2_2_LOCAL);
        else if ((publicName != null) && publicName.equals(DTD_2_3_PUBLIC))
            return getLocalResource(url, DTD_2_3_LOCAL);
        else if ((url != null) && url.equals(XSD_2_4_URL) &&
                (this.commonLoader.getResource(XSD_2_4_LOCAL) != null))
            return getLocalResource(url, XSD_2_4_LOCAL);
        else if ((url != null) && url.equals(XSD_XML_URL) &&
                (this.commonLoader.getResource(XSD_XML_LOCAL) != null))
            return getLocalResource(url, XSD_XML_LOCAL);
        else if ((publicName != null) && publicName.equals(XSD_DTD_PUBLIC) &&
                (this.commonLoader.getResource(XSD_DTD_LOCAL) != null))
            return getLocalResource(url, XSD_DTD_LOCAL);
        else if ((url != null) && url.equals(DATATYPES_URL) &&
                (this.commonLoader.getResource(DATATYPES_LOCAL) != null))
            return getLocalResource(url, DATATYPES_LOCAL);
        else if ((url != null) && url.equals(WS_CLIENT_URL) &&
                (this.commonLoader.getResource(WS_CLIENT_LOCAL) != null))
            return getLocalResource(url, WS_CLIENT_LOCAL);
        else if ((url != null) && url.startsWith("jar:"))
            return getLocalResource(url, url.substring(url.indexOf("!/") + 2));
        else {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WebXmlParser.NoLocalResource", url);
            return new InputSource(url);
        }
    }

    private InputSource getLocalResource(String url, String local) {
        if (this.commonLoader.getResource(local) == null)
            return new InputSource(url);
        InputSource is = new InputSource(this.commonLoader.getResourceAsStream(local));
        is.setSystemId(url);
        return is;
    }

    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        error(exception);
    }

    public void warning(SAXParseException exception) throws SAXException {
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WebXmlParser.XMLParseError",
                new String[] { exception.getLineNumber() + "",
                        exception.getMessage() });
    }

}
