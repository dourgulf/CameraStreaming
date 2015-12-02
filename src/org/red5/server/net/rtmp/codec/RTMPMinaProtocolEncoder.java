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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.server.IConnection;
import org.red5.server.Red5;
import org.red5.server.net.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mina protocol encoder for RTMP.
 */
public class RTMPMinaProtocolEncoder extends ProtocolEncoderAdapter {

	protected static Logger log = LoggerFactory.getLogger(RTMPMinaProtocolEncoder.class);
	
	private RTMPProtocolEncoder encoder = new RTMPProtocolEncoder();
	
	/** {@inheritDoc} */
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws ProtocolCodecException {

    	final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);

		RTMPConnection conn = (RTMPConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
		final IConnection prevConn = Red5.getConnectionLocal();
		conn.getWriteLock().lock();
		try {
			// Set thread local here so we have the connection during decoding of packets
			Red5.setConnectionLocal(conn);

			// We need to synchronize on the output and flush the
			// generated data to prevent two packages to the same channel
			// to be sent in different order thus resulting in wrong
			// headers being generated.
			final IoBuffer buf = encoder.encode(state, message);
			if (buf != null) {
				out.write(buf);
				out.mergeAll();
				out.flush();
			} else {
//				log.trace("Response buffer was null after encoding");
			}
		} catch (Exception ex) {
			log.error("", ex);
		} finally {
			conn.getWriteLock().unlock();
			Red5.setConnectionLocal(prevConn);
		}
	}

    public RTMPProtocolEncoder getEncoder() {
		return encoder;
	}
    
	/**
	 * Setter for serializer.
	 *
	 * @param serializer Serializer
	 */
	public void setSerializer(org.red5.io.object.Serializer serializer) {
		encoder.setSerializer(serializer);
	}
    
	/**
	 * Setter for baseTolerance
	 * */
	public void setBaseTolerance(long baseTolerance) {
		encoder.setBaseTolerance(baseTolerance);
	}
	
	/**
	 * Setter for dropLiveFuture
	 * */
	public void setDropLiveFuture (boolean dropLiveFuture) {
		encoder.setDropLiveFuture(dropLiveFuture);
	}    
}
