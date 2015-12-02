package org.red5.server.stream.codec;

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

/**
 * Red5 video codec for the sorenson video format.
 *
 * VERY simple implementation, just stores last keyframe.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com) 
 */
public class SorensonVideo implements IVideoStreamCodec {

    /**
     * Sorenson video codec constant
     */
	static final String CODEC_NAME = "SorensonVideo";

    /**
     * Block of data
     */
	private byte[] blockData;
    /**
     * Number of data blocks
     */
	private int dataCount;
    /**
     * Data block size
     */
	private int blockSize;

	/** Constructs a new SorensonVideo. */
    public SorensonVideo() {
		this.reset();
	}

	/** {@inheritDoc} */
    public String getName() {
		return CODEC_NAME;
	}

	/** {@inheritDoc} */
    public boolean canDropFrames() {
		return true;
	}

	/** {@inheritDoc} */
    public void reset() {
		this.blockData = null;
		this.blockSize = 0;
		this.dataCount = 0;
	}

	/** {@inheritDoc} */
    public boolean canHandleData(IoBuffer data) {
		if (data.limit() == 0) {
			// Empty buffer
			return false;
		}

		byte first = data.get();
		boolean result = ((first & 0x0f) == VideoCodec.H263.getId());
		data.rewind();
		return result;
	}

	/** {@inheritDoc} */
    public boolean addData(IoBuffer data) {
		if (data.limit() == 0) {
			// Empty buffer
			return true;
		}

		if (!this.canHandleData(data)) {
			return false;
		}

		byte first = data.get();
		data.rewind();
		if ((first & 0xf0) != FLV_FRAME_KEY) {
			// Not a keyframe
			return true;
		}

		// Store last keyframe
		this.dataCount = data.limit();
		if (this.blockSize < this.dataCount) {
			this.blockSize = this.dataCount;
			this.blockData = new byte[this.blockSize];
		}

		data.get(this.blockData, 0, this.dataCount);
		data.rewind();
		return true;
	}

	/** {@inheritDoc} */
    public IoBuffer getKeyframe() {
		if (this.dataCount == 0) {
			return null;
		}

		IoBuffer result = IoBuffer.allocate(this.dataCount);
		result.put(this.blockData, 0, this.dataCount);
		result.rewind();
		return result;
	}
    
	public IoBuffer getDecoderConfiguration() {
		return null;
	}    
    
}
