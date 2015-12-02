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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Red5 video codec for the AVC (h264) video format.
 *
 * Store DecoderConfigurationRecord and last keyframe (for now! we're cooking a very exciting new!)
 *
 * @author Tiago Jacobs (tiago@imdt.com.br)
 * @author Paul Gregoire (mondain@gmail.com) 
 */
public class AVCVideo implements IVideoStreamCodec {

	private static Logger log = LoggerFactory.getLogger(AVCVideo.class);

	/**
	 * AVC video codec constant
	 */
	static final String CODEC_NAME = "AVC";

	/**
	 * Block of data (AVC DecoderConfigurationRecord)
	 */
	private byte[] blockDataAVCDCR;

	/**
	 * Data block size (AVC DecoderConfigurationRecord)
	 */
	private int blockSizeAVCDCR;

	/**
	 * Block of data (Last KeyFrame)
	 */
	private byte[] blockDataLKF;

	/**
	 * Data block size (Last KeyFrame)
	 */
	private int blockSizeLKF;

	/**
	 * Number of data blocks (last key frame)
	 */
	private int dataCountLKF;

	/**
	 * Number of data blocks (Decoder Configuration Record)
	 */
	private int dataCountAVCDCR;

	/** Constructs a new AVCVideo. */
	public AVCVideo() {
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
		this.blockDataLKF = null;
		this.blockSizeLKF = 0;
		this.blockSizeAVCDCR = 0;
		this.blockDataAVCDCR = null;
		this.dataCountLKF = 0;
		this.dataCountAVCDCR = 0;
	}

	/** {@inheritDoc} */
	public boolean canHandleData(IoBuffer data) {
		if (data.limit() == 0) {
			// Empty buffer
			return false;
		}

		byte first = data.get();
		boolean result = ((first & 0x0f) == VideoCodec.AVC.getId());
		data.rewind();
		return result;
	}

	/** {@inheritDoc} */
	public boolean addData(IoBuffer data) {
		if (data.limit() > 0) {
			
			//ensure that we can "handle" the data
    		if (!canHandleData(data)) {
    			return false;
    		}
    
    		byte frameType = data.get();
    
    		//check for keyframe
    		if ((frameType & 0xf0) == FLV_FRAME_KEY) {
    			log.trace("Key frame found");
    			//If we don't have the AVCDecoderConfigurationRecord stored...
    			if (blockDataAVCDCR == null) {
    				//data.get();//Frame Type - already read above
    				data.get();//CODECID
    
    				byte AVCPacketType = data.get();
    
    				//Sequence Header / here comes a AVCDecoderConfigurationRecord
    				log.debug("AVCPacketType: {}", AVCPacketType);
    				if (AVCPacketType == 0) {
    					log.trace("Decoder configuration found");
    					data.rewind();
    
    					// Store AVCDecoderConfigurationRecord data
    					this.dataCountAVCDCR = data.limit();
    
    					if (this.blockSizeAVCDCR < this.dataCountAVCDCR) {
    						this.blockSizeAVCDCR = this.dataCountAVCDCR;
    						this.blockDataAVCDCR = new byte[this.blockSizeAVCDCR];
    					}
    
    					data.get(this.blockDataAVCDCR, 0, this.dataCountAVCDCR);
    				}
    			}
    
    			//rewind data prior to reading the keyframe
    			data.rewind();
    
    			// Store last keyframe
    			this.dataCountLKF = data.limit();
    			if (this.blockSizeLKF < this.dataCountLKF) {
    				this.blockSizeLKF = this.dataCountLKF;
    				this.blockDataLKF = new byte[this.blockSizeLKF];
    			}
    
    			data.get(this.blockDataLKF, 0, this.dataCountLKF);
    		}
    
    		//finished with the data, rewind one last time
    		data.rewind();
		}
		
		return true;
	}

	/** {@inheritDoc} */
	public IoBuffer getKeyframe() {
		if (this.dataCountLKF == 0) {
			return null;
		}

		IoBuffer result = IoBuffer.allocate(dataCountLKF);
		result.put(blockDataLKF, 0, dataCountLKF);
		result.flip();
		return result;
	}

	/** {@inheritDoc} */
	public IoBuffer getDecoderConfiguration() {
		if (dataCountAVCDCR == 0) {
			return null;
		}

		IoBuffer result = IoBuffer.allocate(dataCountAVCDCR);
		result.put(blockDataAVCDCR, 0, dataCountAVCDCR);
		result.flip();
		return result;
	}
}
