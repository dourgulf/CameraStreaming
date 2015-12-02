package org.red5.server.net.rtmp.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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
 * RTMP packet header
 */
public class Header implements Constants, Cloneable, Externalizable {

	private static final long serialVersionUID = 8982665579411495024L;

	/**
	 * Channel
	 */
	private int channelId;

	/**
	 * Timer
	 */
	private int timerBase;

	/**
	 * Delta
	 */
	private int timerDelta;

	/**
	 * Header size
	 */
	private int size;

	/**
	 * Type of data
	 */
	private byte dataType;

	/**
	 * Stream id
	 */
	private int streamId;

	/**
	 * Flash player occasionally sends garbage audio that
	 * as far as I can tell exists only to make folks who
	 * don't know about it slowly get out-of-sync audio
	 * and video.  We now detect that.
	 */
	private boolean isGarbage = false;

	/**
	 * Getter for channel id
	 *
	 * @return  Channel id
	 */
	public int getChannelId() {
		return channelId;
	}

	/**
	 * Setter for channel id
	 *
	 * @param channelId  Header channel id
	 */
	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	/**
	 * Getter for data type
	 *
	 * @return  Data type
	 */
	public byte getDataType() {
		return dataType;
	}

	/**
	 * Setter for data type
	 *
	 * @param dataType  Data type
	 */
	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	/**
	 * Getter for size.
	 *
	 * @return  Header size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Setter for size
	 *
	 * @param size  Header size
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Getter for stream id
	 *
	 * @return  Stream id
	 */
	public int getStreamId() {
		return streamId;
	}

	/**
	 * Setter for stream id
	 *
	 * @param streamId  Stream id
	 */
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	/**
	 * Getter for timer
	 *
	 * @return  Timer
	 */
	public int getTimer() {
		return timerBase + timerDelta;
	}

	/**
	 * Setter for timer
	 *
	 * @param timer  Timer
	 */
	public void setTimer(int timer) {
		this.timerBase = timer;
		this.timerDelta = 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Header)) {
			return false;
		}
		final Header header = (Header) other;
		return (header.getChannelId() == channelId && header.getDataType() == dataType && header.getSize() == size
				&& header.getTimer() == this.getTimer() && header.getStreamId() == streamId);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ChannelId: ").append(channelId).append(", ");
		sb.append("Timer: ").append(getTimer()).append(", ");
		sb.append("TimerBase: ").append(timerBase).append(", ");
		sb.append("TimerDelta: ").append(timerDelta).append(", ");
		sb.append("Size: ").append(size).append(", ");
		sb.append("DataType: ").append(dataType).append(", ");
		sb.append("Garbage: ").append(isGarbage).append(", ");
		sb.append("StreamId: ").append(streamId);
		return sb.toString();
	}

	/** {@inheritDoc} */
	@Override
	public Header clone() {
		final Header header = new Header();
		header.setChannelId(channelId);
		header.setTimerBase(timerBase);
		header.setTimerDelta(timerDelta);
		header.setSize(size);
		header.setDataType(dataType);
		header.setStreamId(streamId);
		header.setIsGarbage(isGarbage);
		return header;
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		dataType = in.readByte();
		channelId = in.readInt();
		size = in.readInt();
		streamId = in.readInt();
		timerBase = in.readInt();
		timerDelta = in.readInt();
		isGarbage = in.readBoolean();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(dataType);
		out.writeInt(channelId);
		out.writeInt(size);
		out.writeInt(streamId);
		out.writeInt(timerBase);
		out.writeInt(timerDelta);
		out.writeBoolean(isGarbage);
	}

	public void setTimerDelta(int timerDelta) {
		this.timerDelta = timerDelta;
	}

	public int getTimerDelta() {
		return timerDelta;
	}

	public void setTimerBase(int timerBase) {
		this.timerBase = timerBase;
	}

	public int getTimerBase() {
		return timerBase;
	}

	public void setIsGarbage(boolean isGarbage) {
		this.isGarbage = isGarbage;
	}

	public boolean isGarbage() {
		return isGarbage;
	}
}
