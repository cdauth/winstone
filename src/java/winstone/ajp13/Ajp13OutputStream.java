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
package winstone.ajp13;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.Cookie;

import winstone.Logger;
import winstone.WinstoneException;
import winstone.WinstoneOutputStream;
import winstone.WinstoneResourceBundle;

/**
 * Extends the winstone output stream, so that the ajp13 protocol requirements
 * can be fulfilled.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id$
 */
public class Ajp13OutputStream extends WinstoneOutputStream {
    // Container originated packet types
    byte CONTAINER_SEND_BODY_CHUNK = 0x03;
    byte CONTAINER_SEND_HEADERS = 0x04;
    byte CONTAINER_END_RESPONSE = 0x05;

    // byte CONTAINER_GET_BODY_CHUNK = 0x06;
    // byte CONTAINER_CPONG_REPLY = 0x09;

    static Map headerCodes = null;

    static {
        headerCodes = new Hashtable();
        headerCodes.put("content-type", new byte[] { (byte) 0xA0, 0x01 });
        headerCodes.put("content-language", new byte[] { (byte) 0xA0, 0x02 });
        headerCodes.put("content-length", new byte[] { (byte) 0xA0, 0x03 });
        headerCodes.put("date", new byte[] { (byte) 0xA0, 0x04 });
        headerCodes.put("last-modified", new byte[] { (byte) 0xA0, 0x05 });
        headerCodes.put("location", new byte[] { (byte) 0xA0, 0x06 });
        headerCodes.put("set-cookie", new byte[] { (byte) 0xA0, 0x07 });
        headerCodes.put("set-cookie2", new byte[] { (byte) 0xA0, 0x08 });
        headerCodes.put("servlet-engine", new byte[] { (byte) 0xA0, 0x09 });
        headerCodes.put("server", new byte[] { (byte) 0xA0, 0x09 });
        headerCodes.put("status", new byte[] { (byte) 0xA0, 0x0A });
        headerCodes.put("www-authenticate", new byte[] { (byte) 0xA0, 0x0B });
    }

    private String headerEncoding;

    private WinstoneResourceBundle localResources;

    public Ajp13OutputStream(OutputStream outStream,
            WinstoneResourceBundle mainResources,
            WinstoneResourceBundle localResources, String headerEncoding) {
        super(outStream, false, mainResources);
        this.localResources = localResources;
        this.headerEncoding = headerEncoding;
    }

    public void commit() throws IOException {
        Logger.log(Logger.FULL_DEBUG, resources,
                "WinstoneOutputStream.CommittedBytes", "" + this.bytesWritten);
        this.buffer.flush();

        // If we haven't written the headers yet, write them out
        if (!this.committed) {
            this.owner.validateHeaders();

            ByteArrayOutputStream headerArrayStream = new ByteArrayOutputStream();
            for (Iterator i = this.owner.getHeaders().iterator(); i.hasNext();) {
                String header = (String) i.next();
                int colonPos = header.indexOf(':');
                if (colonPos == -1)
                    throw new WinstoneException(localResources.getString(
                            "Ajp13OutputStream.NoColonHeader", header));
                String headerName = header.substring(0, colonPos).trim();
                String headerValue = header.substring(colonPos + 1).trim();
                byte headerCode[] = (byte[]) headerCodes.get(headerName
                        .toLowerCase());
                headerArrayStream
                        .write(headerCode == null ? getStringBlock(headerName)
                                : headerCode);
                headerArrayStream.write(getStringBlock(headerValue));
            }

            for (Iterator i = this.owner.getCookies().iterator(); i.hasNext();) {
                Cookie cookie = (Cookie) i.next();
                String cookieText = this.owner.writeCookie(cookie);
                int colonPos = cookieText.indexOf(':');
                if (colonPos == -1)
                    throw new WinstoneException(localResources.getString(
                            "Ajp13OutputStream.NoColonHeader", cookieText));
                String headerName = cookieText.substring(0, colonPos).trim();
                String headerValue = cookieText.substring(colonPos + 1).trim();
                byte headerCode[] = (byte[]) headerCodes.get(headerName
                        .toLowerCase());
                headerArrayStream
                        .write(headerCode == null ? getStringBlock(headerName)
                                : headerCode);
                headerArrayStream.write(getStringBlock(headerValue));
            }

            // Write packet header + prefix + status code + status msg + header
            // count
            byte headerArray[] = headerArrayStream.toByteArray();
            byte headerPacket[] = new byte[12];
            headerPacket[0] = (byte) 0x41;
            headerPacket[1] = (byte) 0x42;
            setIntBlock(headerArray.length + 8, headerPacket, 2);
            headerPacket[4] = CONTAINER_SEND_HEADERS;
            setIntBlock(this.owner.getStatus(), headerPacket, 5);
            setIntBlock(0, headerPacket, 7); // empty msg
            headerPacket[9] = (byte) 0x00;
            setIntBlock(this.owner.getHeaders().size()
                    + this.owner.getCookies().size(), headerPacket, 10);

            // Ajp13Listener.packetDump(headerPacket, headerPacket.length);
            // Ajp13Listener.packetDump(headerArray, headerArray.length);

            this.outStream.write(headerPacket);
            this.outStream.write(headerArray);
        }

        // Write out the contents of the buffer in max 8k chunks
        byte bufferContents[] = this.buffer.toByteArray();
        int position = 0;
        while (position < bufferContents.length) {
            int packetLength = Math.min(bufferContents.length - position, 8184);
            byte responsePacket[] = new byte[packetLength + 8];
            responsePacket[0] = 0x41;
            responsePacket[1] = 0x42;
            setIntBlock(packetLength + 4, responsePacket, 2);
            responsePacket[4] = CONTAINER_SEND_BODY_CHUNK;
            setIntBlock(packetLength, responsePacket, 5);
            System.arraycopy(bufferContents, position, responsePacket, 7,
                    packetLength);
            responsePacket[packetLength + 7] = 0x00;
            position += packetLength;

            // Ajp13Listener.packetDump(responsePacket, responsePacket.length);
            this.outStream.write(responsePacket);
        }

        this.committed = true;
        this.buffer.reset();
        this.bufferPosition = 0;
    }

    public void finishResponse() throws IOException {
        // Send end response packet
        byte endResponse[] = new byte[] { 0x41, 0x42, 0x00, 0x02,
                CONTAINER_END_RESPONSE, 1 };
        // Ajp13Listener.packetDump(endResponse, endResponse.length);
        this.outStream.write(endResponse);
    }

    /**
     * Useful generic method for getting ajp13 format integers in a packet.
     */
    public byte[] getIntBlock(int integer) {
        byte hi = (byte) (0xFF & (integer >> 8));
        byte lo = (byte) (0xFF & (integer - (hi << 8)));
        return new byte[] { hi, lo };
    }

    /**
     * Useful generic method for setting ajp13 format integers in a packet.
     */
    public static void setIntBlock(int integer, byte packet[], int offset) {
        byte hi = (byte) (0xFF & (integer >> 8));
        byte lo = (byte) (0xFF & (integer - (hi << 8)));
        packet[offset] = hi;
        packet[offset + 1] = lo;
    }

    /**
     * Useful generic method for getting ajp13 format strings in a packet.
     */
    public byte[] getStringBlock(String text)
            throws UnsupportedEncodingException {
        byte textBytes[] = text.getBytes(headerEncoding);
        byte outArray[] = new byte[textBytes.length + 3];
        System.arraycopy(getIntBlock(textBytes.length), 0, outArray, 0, 2);
        System.arraycopy(textBytes, 0, outArray, 2, textBytes.length);
        outArray[textBytes.length + 2] = 0x00;
        return outArray;
    }

}
