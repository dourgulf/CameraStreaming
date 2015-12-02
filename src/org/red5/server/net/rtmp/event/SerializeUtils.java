package org.red5.server.net.rtmp.event;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;

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

/**
 * The utility class provides conversion methods to ease the use of
 * byte arrays, Mina IoBuffers, and NIO ByteBuffers.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SerializeUtils {

	public static byte[] ByteBufferToByteArray(IoBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.rewind();
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}
	
	public static byte[] NioByteBufferToByteArray(ByteBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.position(0);
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}	
	
	public static void ByteArrayToByteBuffer(byte[] byteBuf, IoBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
	
	public static void ByteArrayToNioByteBuffer(byte[] byteBuf, ByteBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
	
}