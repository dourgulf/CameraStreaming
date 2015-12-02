package org.red5.server.net.rtmp.event;

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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Client bandwidth event
 */
public class ClientBW extends BaseEvent {

	private static final long serialVersionUID = 5848656135751336839L;

	/**
	 * Bandwidth
	 */
	private int bandwidth;

	/**
	 * Enforcement level of the bandwidth value based on three values.
	TYPE_DYNAMIC 2
	TYPE_HARD 0
	TYPE_SOFT 1
	 */
	private byte value2;

	public ClientBW() {
		super(Type.STREAM_CONTROL);
	}

	public ClientBW(int bandwidth, byte value2) {
		this();
		this.bandwidth = bandwidth;
		this.value2 = value2;
	}

	/** {@inheritDoc} */
	@Override
	public byte getDataType() {
		return TYPE_CLIENT_BANDWIDTH;
	}

	/**
	 * Getter for property 'bandwidth'.
	 *
	 * @return Value for property 'bandwidth'.
	 */
	public int getBandwidth() {
		return bandwidth;
	}

	/**
	 * Setter for bandwidth
	 *
	 * @param bandwidth  New bandwidth
	 */
	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/**
	 * Getter for value2
	 *
	 * @return Value for property 'value2'.
	 */
	public byte getValue2() {
		return value2;
	}

	/**
	 * Setter for property 'value2'.
	 *
	 * @param value2 Value to set for property 'value2'.
	 */
	public void setValue2(byte value2) {
		this.value2 = value2;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "ClientBW: " + bandwidth + " value2: " + value2;
	}

	/** {@inheritDoc} */
	@Override
	protected void releaseInternal() {

	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		bandwidth = in.readInt();
		value2 = in.readByte();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(bandwidth);
		out.writeByte(value2);
	}
}
