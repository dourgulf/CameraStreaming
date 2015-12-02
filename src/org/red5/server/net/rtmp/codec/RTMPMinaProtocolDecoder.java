package org.red5.server.net.rtmp.codec;

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

import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.red5.io.object.Deserializer;
import org.red5.server.Red5;
import org.red5.server.net.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Constants;

/**
 * RTMP protocol decoder.
 */
public class RTMPMinaProtocolDecoder extends ProtocolDecoderAdapter {

	private RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();
	
	/** {@inheritDoc} */
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws ProtocolCodecException {
    	//get our state
		final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);
		//get the connection from the session
		RTMPConnection conn = (RTMPConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
		conn.getWriteLock().lock();
		try {
			// Set thread local here so we have the connection during decoding of packets
			Red5.setConnectionLocal(conn);
			//create a buffer and store it on the session
			IoBuffer buf = (IoBuffer) session.getAttribute("buffer");
			if (buf == null) {
				buf = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
				buf.setAutoExpand(true);
				session.setAttribute("buffer", buf);
			}
			buf.put(in);
			buf.flip();
			//construct any objects from the decoded bugger
			List<?> objects = decoder.decodeBuffer(state, buf);
			if (objects != null) {
				for (Object object : objects) {
					out.write(object);
				}
			}
		} finally {
			conn.getWriteLock().unlock();
		}
	}

	/**
	 * Setter for deserializer.
	 * 
	 * @param deserializer Deserializer
	 */
	public void setDeserializer(Deserializer deserializer) {
		decoder.setDeserializer(deserializer);
	}    
    
	public RTMPProtocolDecoder getDecoder() {
		return decoder;
	}

}
