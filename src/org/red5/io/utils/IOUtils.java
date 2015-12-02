package org.red5.io.utils;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;

/**
 * Miscellaneous I/O utility methods
 * 
 * @see <a href="http://www.cs.utsa.edu/~wagner/laws/Abytes.html">Unsigned bytes in Java</a>
 */
public class IOUtils {

	/**
	 * UTF-8 is used
	 */
	public static final Charset CHARSET = Charset.forName("UTF-8");

	/**
	 * Writes integer in reverse order
	 * @param out         Data buffer to fill
	 * @param value       Integer
	 */
	public static void writeReverseInt(IoBuffer out, int value) {
		byte[] bytes = new byte[4];
		IoBuffer rev = IoBuffer.allocate(4);
		rev.putInt(value);
		rev.flip();
		bytes[3] = rev.get();
		bytes[2] = rev.get();
		bytes[1] = rev.get();
		bytes[0] = rev.get();
		out.put(bytes);
		rev.free();
		rev = null;
	}

	/**
	 * Writes medium integer
	 * @param out           Output buffer
	 * @param value         Integer to write
	 */
	public static void writeMediumInt(IoBuffer out, int value) {
		byte[] bytes = new byte[3];
		bytes[0] = (byte) ((value >>> 16) & 0xff);
		bytes[1] = (byte) ((value >>> 8) & 0xff);
		bytes[2] = (byte) (value & 0xff);
		out.put(bytes);
	}
	
	/**
	 * Writes medium integer
	 * @param out           Output buffer
	 * @param value         Integer to write
	 */
	public static void writeMediumInt(ByteBuffer out, int value) {
		out.put((byte) ((value >>> 16) & 0xff));
		out.put((byte) ((value >>> 8) & 0xff));
		out.put((byte) (value & 0xff));
	}	

	/**
	 * Writes extended medium integer (equivalent to a regular integer whose
	 * most significant byte has been moved to its end, past its least significant
	 * byte)
	 * @param out           Output buffer
	 * @param value         Integer to write
	 */
	public static void writeExtendedMediumInt(IoBuffer out, int value) {
		value = ((value & 0xff000000) >> 24) | (value << 8);
		out.putInt(value);
		/*
		byte[] bytes = new byte[4];
		bytes[0] = (byte) ((value >>> 16) & 0xff);
		bytes[1] = (byte) ((value >>> 8) & 0xff);
		bytes[2] = (byte) (value & 0xff); // least significant byte
		bytes[3] = (byte) ((value >>> 24) & 0xff); // most significant byte	
		out.put(bytes);
		*/
	}
	
	/**
	 * Writes extended medium integer (equivalent to a regular integer whose
	 * most significant byte has been moved to its end, past its least significant
	 * byte)
	 * @param out           Output buffer
	 * @param value         Integer to write
	 */
	public static void writeExtendedMediumInt(ByteBuffer out, int value) {
		value = ((value & 0xff000000) >> 24) | (value << 8);
		out.putInt(value);
	}	

	/**
	 * Reads unsigned medium integer
	 * @param in              Unsigned medium int source
	 * @return                int value
	 */
	public static int readUnsignedMediumInt(IoBuffer in) {
		return ((in.get() & 0xff) << 16) + ((in.get() & 0xff) << 8) + ((in.get() & 0xff));
	}

	/**
	 * Reads medium int
	 * @param in       Source
	 * @return         int value
	 */
	public static int readMediumInt(IoBuffer in) {
		return ((in.get() & 0x000000ff) << 16) + ((in.get() & 0x000000ff) << 8) + ((in.get() & 0x000000ff));
	}

	/**
	 * Reads extended medium int
	 * @param in       Source
	 * @return         int value
	 */
	public static int readExtendedMediumInt(IoBuffer in) {
		int result = in.getInt();
		result = (result >>> 8) | ((result & 0x000000ff) << 24);
		return result;
	}	
	
	/**
	 * Reads extended medium int
	 * @param in       Source
	 * @return         int value
	 */
	public static int readExtendedMediumInt(ByteBuffer in) {
		int result = in.getInt();
		result = (result >>> 8) | ((result & 0x000000ff) << 24);
		return result;
	}		
	
	/**
	 * Reads reverse int
	 * @param in       Source
	 * @return         int
	 */
	public static int readReverseInt(IoBuffer in) {
		byte[] bytes = new byte[4];
		in.get(bytes);
		int val = 0;
		val += bytes[3] * 256 * 256 * 256;
		val += bytes[2] * 256 * 256;
		val += bytes[1] * 256;
		val += bytes[0];
		return val;
	}

	/**
	 * Format debug message
	 * @param log          Logger
	 * @param msg          Message
	 * @param buf          Byte buffer to debug
	 */
	public static void debug(Logger log, String msg, IoBuffer buf) {
//		if (log.isDebugEnabled()) {
//			log.debug(msg);
//			log.debug("Size: {}", buf.remaining());
//			log.debug("Data:\n{}", HexDump.formatHexDump(buf.getHexDump()));
//			log.debug("\n{}\n", toString(buf));
//		}
	}

	/**
	 * String representation of byte buffer
	 * @param buf           Byte buffer
	 * @return              String representation
	 */
	public static String toString(IoBuffer buf) {
		int pos = buf.position();
		int limit = buf.limit();
		final java.nio.ByteBuffer strBuf = buf.buf();
		final String string = CHARSET.decode(strBuf).toString();
		buf.position(pos);
		buf.limit(limit);
		return string;
	}

	public static void main(String[] args) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		IOUtils.writeExtendedMediumInt(buf, 1234567890);
		buf.flip();
		System.out.println("Result: " + IOUtils.readExtendedMediumInt(buf));
	}
	
}
