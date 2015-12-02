package org.red5.server.net.rtmp.message;

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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.event.IRTMPEvent;

/**
 * RTMP packet. Consists of packet header, data and event context.
 */
public class Packet implements Externalizable {
	
	private static final long serialVersionUID = -6415050845346626950L;
    
	/**
     * Header
     */
	private Header header;
    
	/**
     * RTMP event
     */
	private IRTMPEvent message;
    
	/**
     * Packet data
     */
	private IoBuffer data;

	public Packet() {
		data = null;
		header = null;
	}
    /**
     * Create packet with given header
     * @param header       Packet header
     */
    public Packet(Header header) {
		this.header = header;
		data = IoBuffer.allocate(header.getSize(), false);
		// Workaround for SN-19: BufferOverflowException
		// Size is checked in RTMPProtocolDecoder
		data.setAutoExpand(true);
	}

    /**
     * Create packet with given header and event context
     * @param header     RTMP header
     * @param event      RTMP message
     */
    public Packet(Header header, IRTMPEvent event) {
		this.header = header;
		this.message = event;
	}

	/**
     * Getter for header
     *
     * @return  Packet header
     */
    public Header getHeader() {
		return header;
	}

	/**
     * Setter for event context
     *
     * @param message  RTMP event context
     */
    public void setMessage(IRTMPEvent message) {
		this.message = message;
	}

	/**
     * Getter for event context
     *
     * @return RTMP event context
     */
    public IRTMPEvent getMessage() {
		return message;
	}

	/**
     * Setter for data
     *
     * @param data Packet data
     */
    public void setData(IoBuffer data) {
		this.data = data;
	}

	/**
     * Getter for data
     *
     * @return Packet data
     */
    public IoBuffer getData() {
		return data;
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		header = (Header) in.readObject();
		message = (IRTMPEvent) in.readObject();
		message.setHeader(header);
		message.setTimestamp(header.getTimer());
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(header);
		out.writeObject(message);
	}
}
