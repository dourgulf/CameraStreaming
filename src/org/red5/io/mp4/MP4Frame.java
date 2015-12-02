package org.red5.io.mp4;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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
 * Represents an MP4 frame / chunk sample
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class MP4Frame implements Comparable<MP4Frame> {

	private byte type;

	private long offset;

	private int size;

	private double time;

	//this value originates from the ctts atom
	private int timeOffset;
	
	private boolean keyFrame;

	/**
	 * Returns the data type, being audio or video.
	 * 
	 * @return the data type
	 */
	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * Returns the offset of the data chunk in the media source.
	 * 
	 * @return the offset in bytes
	 */
	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	/**
	 * Returns the size of the data chunk.
	 * 
	 * @return the size in bytes
	 */
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Returns the timestamp.
	 * 
	 * @return the timestamp
	 */
	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	/**
	 * @return the timeOffset
	 */
	public int getTimeOffset() {
		return timeOffset;
	}

	/**
	 * @param timeOffset the timeOffset to set
	 */
	public void setTimeOffset(int timeOffset) {
		this.timeOffset = timeOffset;
	}

	/**
	 * Returns whether or not this chunk represents a key frame.
	 * 
	 * @return true if a key frame
	 */
	public boolean isKeyFrame() {
		return keyFrame;
	}

	public void setKeyFrame(boolean keyFrame) {
		this.keyFrame = keyFrame;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (offset ^ (offset >>> 32));
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MP4Frame other = (MP4Frame) obj;
		if (offset != other.offset)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MP4Frame type=");
		sb.append(type);
		sb.append(", time=");
		sb.append(time);
		sb.append(", timeOffset=");
		sb.append(timeOffset);		
		sb.append(", size=");
		sb.append(size);
		sb.append(", offset=");
		sb.append(offset);
		sb.append(", keyframe=");
		sb.append(keyFrame);
		return sb.toString();
	}

	/**
	 * The frames are expected to be sorted by their timestamp
	 */
	public int compareTo(MP4Frame that) {
		int ret = 0;
		if (this.time > that.getTime()) {
			ret = 1;
		} else if (this.time < that.getTime()) {
			ret = -1;
		} else if (Double.doubleToLongBits(time) == Double.doubleToLongBits(that.getTime()) && this.offset > that.getOffset()) {
			ret = 1;
		} else if (Double.doubleToLongBits(time) == Double.doubleToLongBits(that.getTime()) && this.offset < that.getOffset()) {
			ret = -1;
		}
		return ret;
	}

}
