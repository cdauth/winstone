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
package com.rickknowles.winstone.realm;

import com.rickknowles.winstone.*;
import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

/**
 * @author rickk
 * @version $Id$
 */
public class FileRealm extends AuthenticationRealm 
{
  final String FILE_NAME_ARGUMENT = "fileRealm.configFile";
  final String DEFAULT_FILE_NAME  = "users.xml";

  final String ELEM_USER = "user";
  
  final String ATT_USERNAME = "username";
  final String ATT_PASSWORD = "password";
  final String ATT_ROLELIST = "roles";

  private Map passwords;
  private Map roles;

  /**
   * Constructor - this sets up an authentication realm, using the file supplied
   * on the command line as a source of userNames/passwords/roles.
   */
  public FileRealm(WinstoneResourceBundle resources, Map args)
  {
    super(resources, args);
    this.passwords = new Hashtable();
    this.roles = new Hashtable();

    // Get the filename and parse the xml doc
    String realmFileName = args.get(FILE_NAME_ARGUMENT) == null 
                ? DEFAULT_FILE_NAME 
                : (String) args.get(FILE_NAME_ARGUMENT);
    File realmFile = new File(realmFileName);
    if (!realmFile.exists())
      throw new WinstoneException(this.resources.getString("FileRealm.FileNotFound", 
            "[#name]", realmFile.getPath()));
    try
    {
      InputStream inFile = new FileInputStream(realmFile);
      Document doc = this.parseStreamToXML(inFile);
      inFile.close();
      Node rootElm = doc.getDocumentElement();
      for (int n = 0; n < rootElm.getChildNodes().getLength(); n++)
      {
        Node child = (Node) rootElm.getChildNodes().item(n);
        if ((child.getNodeType() == Node.ELEMENT_NODE) &&
            (child.getNodeName().equals(ELEM_USER)))
        {
          String userName = null;
          String password = null;
          String roleList = null;
          // Loop through for attributes
          for (int j = 0; j < child.getAttributes().getLength(); j++)
          {
            Node thisAtt = child.getAttributes().item(n);
            if (thisAtt.getNodeName().equals(ATT_USERNAME)) 
              userName = thisAtt.getFirstChild().getNodeValue();
            else if (thisAtt.getNodeName().equals(ATT_PASSWORD)) 
              password = thisAtt.getFirstChild().getNodeValue();
            else if (thisAtt.getNodeName().equals(ATT_ROLELIST)) 
              roleList = thisAtt.getFirstChild().getNodeValue();
          }

          if ((userName == null) || (password == null) || (roleList == null))
            Logger.log(Logger.FULL_DEBUG, 
              this.resources.getString("FileRealm.SkippingUser", "[#name]", userName));
          else
          {
            this.passwords.put(userName, password);
          
            // Parse the role list into an array and sort it
            StringTokenizer st = new StringTokenizer(roleList, ",");
            String roleArray[] = new String[st.countTokens()];
            for (int k = 0; k < roleArray.length; k++)
              roleArray[k] = st.nextToken();
            Arrays.sort(roleArray);
            this.roles.put(userName, roleArray);
          }
        }
      }
      Logger.log(Logger.FULL_DEBUG, resources.getString("FileRealm.Initialised",
          "[#userCount]", "" + this.passwords.size()));
    }
    catch (IOException err) 
      {throw new WinstoneException(this.resources.getString("FileRealm.ErrorLoading"), err);
    }
  }

  /**
   * Get a parsed XML DOM from the given inputstream. Used to process the web.xml
   * application deployment descriptors.
   */
  private Document parseStreamToXML(InputStream in)
  {
    try
    {
      // Use JAXP to create a document builder
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setExpandEntityReferences(false);
      factory.setValidating(true);
      factory.setNamespaceAware(false);
      factory.setIgnoringComments(true);
      factory.setCoalescing(true);
      factory.setIgnoringElementContentWhitespace(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(in);
    }
    catch (Throwable errParser)
      {throw new WinstoneException(resources.getString("FileRealm.WebXMLParseError"), errParser);}
  }

  /**
   * Authenticate the user - do we know them ? Return a principal once we know them
   */
  public AuthenticationPrincipal authenticateByUsernamePassword(String userName, String password)
  {
    if ((userName == null) || (password == null))
      return null;
      
    String realPassword = (String) this.passwords.get(userName);
    if (realPassword == null)
      return null;
    else if (!realPassword.equals(password))
      return null;
    else
      return new AuthenticationPrincipal(userName, password,
                      (String []) this.roles.get(userName));
  }  
}
