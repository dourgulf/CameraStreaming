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

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.utils.HexDump;

/**
 * Unknown event
 */
public class Unknown extends BaseEvent {
	private static final long serialVersionUID = -1352770037962252975L;
    /**
     * Event data
     */
	protected IoBuffer data;
    /**
     * Type of data
     */
	protected byte dataType;

	public Unknown() {}
    /**
     * Create new unknown event with given data and data type
     * @param dataType             Data type
     * @param data                 Event data
     */
    public Unknown(byte dataType, IoBuffer data) {
		super(Type.SYSTEM);
		this.dataType = dataType;
		this.data = data;
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return dataType;
	}

	/**
     * Getter for data
     *
     * @return  Data
     */
    public IoBuffer getData() {
		return data;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		final IoBuffer buf = getData();
		StringBuffer sb = new StringBuffer();
		sb.append("Size: ");
		sb.append(buf.remaining());
		sb.append(" Data:\n\n");
		sb.append(HexDump.formatHexDump(buf.getHexDump()));
		return sb.toString();
	}

	/** {@inheritDoc} */
    @Override
	protected void releaseInternal() {
		if (data != null) {
			data.free();
			data = null;
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		dataType = in.readByte();
		byte[] byteBuf = (byte[]) in.readObject();
		if (byteBuf != null) {
			data = IoBuffer.allocate(0);
			data.setAutoExpand(true);
			SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeByte(dataType);
		if (data != null) {
			out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
		} else {
			out.writeObject(null);
		}
	}
}
