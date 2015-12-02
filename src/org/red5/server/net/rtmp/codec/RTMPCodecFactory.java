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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * RTMP codec factory creates RTMP encoders/decoders.
 */
public class RTMPCodecFactory implements ProtocolCodecFactory {

    /**
     * Deserializer.
     */
    protected Deserializer deserializer;

    /**
     * Serializer.
     */
    protected Serializer serializer;

    /**
     * Mina protocol decoder for RTMP.
     */
    private RTMPMinaProtocolDecoder decoder;

    /**
     * Mina protocol encoder for RTMP.
     */
    private RTMPMinaProtocolEncoder encoder;

    /**
     * Initialization
     */
    public void init() {
		decoder = new RTMPMinaProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RTMPMinaProtocolEncoder();
		encoder.setSerializer(serializer);
	}

	/**
     * Setter for deserializer.
     *
     * @param deserializer  Deserializer
     */
    public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	/**
     * Setter for serializer.
     *
     * @param serializer  Serializer
     */
    public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	/** {@inheritDoc} */
    public ProtocolDecoder getDecoder(IoSession session) {
		return decoder;
	}

	/** {@inheritDoc} */
    public ProtocolEncoder getEncoder(IoSession session) {
		return encoder;
	}

    /**
     * Returns the RTMP decoder.
     * 
     * @return decoder
     */
    public RTMPProtocolDecoder getRTMPDecoder() {
		return decoder.getDecoder();
	}

	/**
	 * Returns the RTMP encoder.
	 * 
	 * @return encoder
	 */
    public RTMPProtocolEncoder getRTMPEncoder() {
		return encoder.getEncoder();
	}    
    
}
