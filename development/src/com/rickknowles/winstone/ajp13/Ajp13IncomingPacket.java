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
package com.rickknowles.winstone.ajp13;

import java.io.*;
import java.util.*;
import com.rickknowles.winstone.*;

/**
 * Models a single incoming ajp13 packet.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Ajp13IncomingPacket
{
  // Server originated packet types
  byte SERVER_FORWARD_REQUEST = 0x02;
  //public static byte SERVER_SHUTDOWN        = 0x07; //not implemented
  //public static byte SERVER_PING            = 0x08; //not implemented
  //public static byte SERVER_CPING           = 0x10; //not implemented

  private WinstoneResourceBundle resources;
  private int packetLength;
  private byte packetBytes[];

  private byte packetType;
  private String method;
  private String protocol;
  private String uri;
  private String remoteAddr;
  private String remoteHost;
  private String serverName;
  private int serverPort;
  private boolean isSSL;
  private String headers[];
  private Map attributes;

  /**
   * Constructor
   */
  public Ajp13IncomingPacket(InputStream in, WinstoneResourceBundle resources)
    throws IOException
  {
    this.resources = resources;

    // Get the incoming packet flag
    byte headerBuffer[] = new byte[4];
    int headerBytesRead = in.read(headerBuffer);
    if (headerBytesRead != 4)
      throw new WinstoneException(this.resources.getString("Ajp13IncomingPacket.InvalidHeader"));
    else if ((headerBuffer[0] != 0x12) || (headerBuffer[1] != 0x34))
      throw new WinstoneException(this.resources.getString("Ajp13IncomingPacket.InvalidHeader"));

    // Read in the whole packet
    packetLength = ((headerBuffer[2] & 0xFF) << 8) + (headerBuffer[3] & 0xFF);
    packetBytes = new byte[packetLength];
    int packetBytesRead = in.read(packetBytes);

    if (packetBytesRead < packetLength)
      throw new WinstoneException(this.resources.getString("Ajp13IncomingPacket.ShortPacket"));
    //Ajp13Listener.packetDump(packetBytes, packetBytesRead);
  }

  public byte parsePacket(String encoding) throws IOException
  {
    int position = 0;
    this.packetType = packetBytes[position++];

    if (this.packetType != SERVER_FORWARD_REQUEST)
      throw new WinstoneException(this.resources.getString(
            "Ajp13IncomingPacket.UnknownPacketType",
            "[#packetType]", this.packetType + ""));

    // Check for terminator
    if ((packetBytes[packetLength - 2] != (byte) 0) ||
        (packetBytes[packetLength - 1] != (byte) 255))
      throw new WinstoneException(this.resources.getString(
            "Ajp13IncomingPacket.InvalidTerminator"));

    this.method = decodeMethodType(packetBytes[position++]);
    Logger.log(Logger.FULL_DEBUG, "Method: " + method);

    // Protocol
    int protocolLength = readInteger(position, packetBytes, true);
    position += 2;
    this.protocol = readString(position, packetBytes, encoding, protocolLength);
    position += (protocolLength == 0 ? 0 : protocolLength + 1);
    Logger.log(Logger.FULL_DEBUG, "Protocol: " + protocol);

    // URI
    int uriLength = readInteger(position, packetBytes, true);
    position += 2;
    this.uri = readString(position, packetBytes, encoding, uriLength);
    position += (uriLength == 0 ? 0 : uriLength + 1);
    Logger.log(Logger.FULL_DEBUG, "URI: " + uri);

    // Remote addr
    int remoteAddrLength = readInteger(position, packetBytes, true);
    position += 2;
    this.remoteAddr = readString(position, packetBytes, encoding, remoteAddrLength);
    position += (remoteAddrLength == 0 ? 0 : remoteAddrLength + 1);
    Logger.log(Logger.FULL_DEBUG, "Remote address: " + remoteAddr);

    // Remote host
    int remoteHostLength = readInteger(position, packetBytes, true);
    position += 2;
    this.remoteHost = readString(position, packetBytes, encoding, remoteHostLength);
    position += (remoteHostLength == 0 ? 0 : remoteHostLength + 1);
    Logger.log(Logger.FULL_DEBUG, "Remote host: " + remoteHost);

    // Server name
    int serverNameLength = readInteger(position, packetBytes, true);
    position += 2;
    this.serverName = readString(position, packetBytes, encoding, serverNameLength);
    position += (serverNameLength == 0 ? 0 : serverNameLength + 1);
    Logger.log(Logger.FULL_DEBUG, "Server name: " + serverName);

    this.serverPort = readInteger(position, packetBytes, false);
    position += 2;
    Logger.log(Logger.FULL_DEBUG, "Server port: " + serverPort);

    this.isSSL = readBoolean(position++, packetBytes);
    Logger.log(Logger.FULL_DEBUG, "SSL: " + isSSL);

    // Read headers
    int headerCount = readInteger(position, packetBytes, false);
    Logger.log(Logger.FULL_DEBUG, "Header count: " + headerCount);
    position += 2;
    this.headers = new String[headerCount];
    for (int n = 0; n < headerCount; n++)
    {
      // Header name
      int headerTypeOrLength = readInteger(position, packetBytes, false);
      position += 2;
      String headerName = null;
      if (packetBytes[position - 2] == (byte) 0xA0)
        headerName = decodeHeaderType(headerTypeOrLength);
      else
      {
        headerName = readString(position, packetBytes, encoding, headerTypeOrLength);
        position += (headerTypeOrLength == 0 ? 0 : headerTypeOrLength + 1);
      }

      // Header value
      int headerValueLength = readInteger(position, packetBytes, true);
      position += 2;
      this.headers[n] = headerName + ": " +
                        readString(position, packetBytes, encoding, headerValueLength);
      position += (headerValueLength == 0 ? 0 : headerValueLength + 1);
      Logger.log(Logger.FULL_DEBUG, "Header: " + this.headers[n]);
    }

    // Attribute parsing
    this.attributes = new Hashtable();
    while (position < packetLength - 2)
    {
      String attName = decodeAttributeType(packetBytes[position++]);
      int attValueLength = readInteger(position, packetBytes, true);
      position += 2;
      String attValue = readString(position, packetBytes, encoding, attValueLength);
      position += (attValueLength == 0 ? 0 : attValueLength + 1);

      this.attributes.put(attName, attValue);
      Logger.log(Logger.FULL_DEBUG, "Attribute: " + attName + " = " + attValue);
    }
    Logger.log(Logger.FULL_DEBUG, this.resources.getString(
            "Ajp13IncomingPacket.SuccessfullyReadRequest", "[#packetLength]", "" + packetLength));
    return this.packetType;
  }

  public int getPacketLength() {return this.packetLength;}
  public String getMethod() {return this.method;}
  public String getProtocol() {return this.protocol;}
  public String getURI() {return this.uri;}
  public String getRemoteAddress() {return this.remoteAddr;}
  public String getRemoteHost() {return this.remoteHost;}
  public String getServerName() {return this.serverName;}
  public int getServerPort() {return this.serverPort;}
  public boolean isSSL() {return this.isSSL;}
  public String[] getHeaders() {return this.headers;}
  public Map getAttributes() {return this.attributes;}

  /**
   * Read a single integer from the stream
   */
  private int readInteger(int position, byte packet[], boolean forStringLength)
  {
    if (forStringLength &&
        (packet[position] == (byte) 0xFF) &&
        (packet[position + 1] == (byte) 0xFF))
      return 0;
    else
      return ((packet[position] & 0xFF) << 8) + (packet[position + 1] & 0xFF);
  }

  /**
   * Read a single boolean from the stream
   */
  private boolean readBoolean(int position, byte packet[])
    {return (packet[position] == (byte) 1);}

  /**
   * Read a single string from the stream
   */
  private String readString(int position, byte packet[], String encoding, int length)
    throws UnsupportedEncodingException
  {
    //System.out.println("Reading string length: " + length);
    return length == 0 ? "" : new String(packet, position, length, encoding);
  }

  /**
   * Decodes the method types into Winstone HTTP method strings
   */
  private String decodeMethodType(byte methodType)
  {
    switch (methodType)
    {
      case 1: return "OPTIONS";
      case 2: return "GET";
      case 3: return "HEAD";
      case 4: return "POST";
      case 5: return "PUT";
      case 6: return "DELETE";
      case 7: return "TRACE";
      case 8: return "PROPFIND";
      case 9: return "PROPPATCH";
      case 10: return "MKCOL";
      case 11: return "COPY";
      case 12: return "MOVE";
      case 13: return "LOCK";
      case 14: return "UNLOCK";
      case 15: return "ACL";
      case 16: return "REPORT";
      case 17: return "VERSION-CONTROL";
      case 18: return "CHECKIN";
      case 19: return "CHECKOUT";
      case 20: return "UNCHECKOUT";
      case 21: return "SEARCH";
      case 22: return "MKWORKSPACE";
      case 23: return "UPDATE";
      case 24: return "LABEL";
      case 25: return "MERGE";
      case 26: return "BASELINE_CONTROL";
      case 27: return "MKACTIVITY";
      default: return "UNKNOWN";
    }
  }

  /**
   * Decodes the header types into Winstone HTTP header strings
   */
  private String decodeHeaderType(int headerType)
  {
    switch (headerType)
    {
      case 0xA001: return "Accept";
      case 0xA002: return "Accept-Charset";
      case 0xA003: return "Accept-Encoding";
      case 0xA004: return "Accept-Language";
      case 0xA005: return "Authorization";
      case 0xA006: return "Connection";
      case 0xA007: return "Content-Type";
      case 0xA008: return "Content-Length";
      case 0xA009: return "Cookie";
      case 0xA00A: return "Cookie2";
      case 0xA00B: return "Host";
      case 0xA00C: return "Pragma";
      case 0xA00D: return "Referer";
      case 0xA00E: return "User-Agent";
      default: return null;
    }
  }

  /**
   * Decodes the header types into Winstone HTTP header strings
   */
  private String decodeAttributeType(byte attributeType)
  {
    switch (attributeType)
    {
      case 0x01: return "context";
      case 0x02: return "servlet_path";
      case 0x03: return "remote_user";
      case 0x04: return "auth_type";
      case 0x05: return "query_string";
      case 0x06: return "jvm_route";
      case 0x07: return "ssl_cert";
      case 0x08: return "ssl_cipher";
      case 0x09: return "ssl_session";
      case 0x0A: return "req_attribute";
      default: return null;
    }
  }
}

