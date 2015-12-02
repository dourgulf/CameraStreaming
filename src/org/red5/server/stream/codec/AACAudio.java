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
 * Red5 audio codec for the AAC audio format.
 *
 * Stores the decoder configuration
 * 
 * @author Paul Gregoire (mondain@gmail.com) 
 * @author Wittawas Nakkasem (vittee@hotmail.com)
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
public class AACAudio implements IAudioStreamCodec {

	private static Logger log = LoggerFactory.getLogger(AACAudio.class);

	public static final int[] AAC_SAMPLERATES = { 96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350 };

	/**
	 * AAC audio codec constant
	 */
	static final String CODEC_NAME = "AAC";

	/**
	 * Block of data (AAC DecoderConfigurationRecord)
	 */
	private byte[] blockDataAACDCR;

	/** Constructs a new AVCVideo. */
	public AACAudio() {
		this.reset();
	}

	/** {@inheritDoc} */
	public String getName() {
		return CODEC_NAME;
	}

	/** {@inheritDoc} */
	public void reset() {
		blockDataAACDCR = null;
	}

	/** {@inheritDoc} */
	public boolean canHandleData(IoBuffer data) {
		if (data.limit() == 0) {
			// Empty buffer
			return false;
		}
		byte first = data.get();
		boolean result = (((first & 0xf0) >> 4) == AudioCodec.AAC.getId());
		data.rewind();
		return result;
	}

	/** {@inheritDoc} */
	public boolean addData(IoBuffer data) {
		int dataLength = data.limit();
		if (dataLength > 1) {
			//ensure we are at the beginning
			data.rewind();
			byte frameType = data.get();
			log.trace("Frame type: {}", frameType);
			byte header = data.get();
			//go back to beginning
			data.rewind();
			//If we don't have the AACDecoderConfigurationRecord stored...
			if (blockDataAACDCR == null) {
				if ((((frameType & 0xF0) >> 4) == AudioCodec.AAC.getId()) && (header == 0)) {
					//go back to beginning
					data.rewind();
					blockDataAACDCR = new byte[dataLength];
					data.get(blockDataAACDCR);
					//go back to beginning
					data.rewind();
				}
			}
		}
		return true;
	}

	/** {@inheritDoc} */
	public IoBuffer getDecoderConfiguration() {
		if (blockDataAACDCR == null) {
			return null;
		}
		IoBuffer result = IoBuffer.allocate(4);
		result.setAutoExpand(true);
		result.put(blockDataAACDCR);
		result.rewind();
		return result;
	}

	@SuppressWarnings("unused")
	private long sample2TC(long time, int sampleRate) {
		return (time * 1000L / sampleRate);
	}

	//private final byte[] getAACSpecificConfig() {		
	//	byte[] b = new byte[] { 
	//			(byte) (0x10 | /*((profile > 2) ? 2 : profile << 3) | */((sampleRateIndex >> 1) & 0x03)),
	//			(byte) (((sampleRateIndex & 0x01) << 7) | ((channels & 0x0F) << 3))
	//		};
	//	log.debug("SpecificAudioConfig {}", HexDump.toHexString(b));
	//	return b;	
	//}    
}
