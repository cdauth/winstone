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
package winstone.ssl;

import winstone.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.*;
import javax.net.ssl.*;

/**
 * Implements the main listener daemon thread. This is the class that
 * gets launched by the command line, and owns the server socket, etc.
 *
 * @author  <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class HttpsListener extends HttpListener
{
  private static final String LOCAL_RESOURCE_FILE    = "winstone.ssl.LocalStrings";

  private String keystore;
  private String password;
  private WinstoneResourceBundle localResources;

  /**
   * Constructor
   */
  public HttpsListener(Map args, WinstoneResourceBundle resources, ObjectPool objectPool)
    throws IOException
  {
    super(args, resources, objectPool);
    this.localResources = new WinstoneResourceBundle(LOCAL_RESOURCE_FILE);
    this.keystore = WebAppConfiguration.stringArg(args, getConnectorName() + "KeyStore", "winstone.ks");
    this.password = WebAppConfiguration.stringArg(args, getConnectorName() + "KeyStorePassword", null);
  }

  /**
   * The default port to use - this is just so that we can override for the SSL
   * connector.
   */
  protected int getDefaultPort() {return -1;}

  /**
   * The name to use when getting properties - this is just so that we can 
   * override for the SSL connector.
   */
  protected String getConnectorName() {return "https";}
  
  /**
   * Gets a server socket - this is mostly for the purpose of allowing an 
   * override in the SSL connector.
   */
  protected ServerSocket getServerSocket() throws IOException
  {
    SSLContext context = getSSLContext(this.keystore, this.password);
    SSLServerSocketFactory factory = context.getServerSocketFactory();
    SSLServerSocket ss = (SSLServerSocket) (this.listenAddress == null 
              ? factory.createServerSocket(this.listenPort, BACKLOG_COUNT)
              : factory.createServerSocket(this.listenPort, BACKLOG_COUNT, 
                                  InetAddress.getByName(this.listenAddress)));
    ss.setSoTimeout(LISTENER_TIMEOUT);
    ss.setEnableSessionCreation(true);
    Logger.log(Logger.INFO, localResources, "HttpsListener.StartupOK",
                                      this.listenPort + "");
    return ss;
  }
  
  /**
   * Extracts the relevant socket stuff and adds it to the request object.
   * This method relies on the base class for everything other than SSL 
   * related attributes
   */
  protected void parseSocketInfo(Socket socket, WinstoneRequest req)
    throws IOException
  {
    super.parseSocketInfo(socket, req);
    if (socket instanceof SSLSocket)
    {
      SSLSocket s = (SSLSocket) socket;
      SSLSession ss = s.getSession();
      if (ss != null)
      {
        Certificate certChain[] = null;
        try 
          {certChain = ss.getPeerCertificates();}
        catch (Throwable err)
          {/* do nothing */}

        if (certChain != null)
        {
          req.setAttribute("javax.servlet.request.X509Certificate", certChain);
          req.setAttribute("javax.servlet.request.cipher_suite", ss.getCipherSuite());
          req.setAttribute("javax.servlet.request.ssl_session", new String(ss.getId()));
          req.setAttribute("javax.servlet.request.key_size", getKeySize(ss.getCipherSuite()));
        }
      }
      req.setIsSecure(true);
    }
  }
  
  /**
   * Just a mapping of key sizes for cipher types. Taken indirectly 
   * from the TLS specs.
   */
  private Integer getKeySize(String cipherSuite)
  {
    if (cipherSuite.indexOf("_WITH_NULL_") != -1)
      return new Integer(0);
    else if (cipherSuite.indexOf("_WITH_IDEA_CBC_") != -1)
      return new Integer(128);
    else if (cipherSuite.indexOf("_WITH_RC2_CBC_40_") != -1)
      return new Integer(40);
    else if (cipherSuite.indexOf("_WITH_RC4_40_") != -1)
      return new Integer(40);
    else if (cipherSuite.indexOf("_WITH_RC4_128_") != -1)
      return new Integer(128);
    else if (cipherSuite.indexOf("_WITH_DES40_CBC_") != -1)
      return new Integer(40);
    else if (cipherSuite.indexOf("_WITH_DES_CBC_") != -1)
      return new Integer(56);
    else if (cipherSuite.indexOf("_WITH_3DES_EDE_CBC_") != -1)
      return new Integer(168);
    else
      return null;
  }
  
  /**
   * Used to get the base ssl context in which to create the server socket. 
   * This is basically just so we can have a custom location for key stores.
   */
  public SSLContext getSSLContext(String keyStoreName, String password) 
  {
    try
    {
      InputStream in = new FileInputStream(keyStoreName);
      char[] passwordChars = password == null ? null : password.toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(in, passwordChars);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, passwordChars);
      Logger.log(Logger.FULL_DEBUG, localResources, "HttpsListener.KeyCount", ks.size() + "");
      for (Enumeration e = ks.aliases(); e.hasMoreElements(); )
      {
        String alias = (String) e.nextElement();
        Logger.log(Logger.FULL_DEBUG, localResources, "HttpsListener.KeyFound", new String[] {alias, ks.getCertificate(alias) + ""});
      }
      
      SSLContext context = SSLContext.getInstance("SSL");
      context.init(kmf.getKeyManagers(), null, null);
      Arrays.fill(passwordChars, (char)'x');
      return context;
    }
    catch (Throwable err)
      {throw new WinstoneException(localResources.getString("HttpsListener.ErrorGettingContext"), err);}
  }
}

