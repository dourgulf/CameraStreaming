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


/**
 * Audio codecs that Red5 supports.
 * 
 * @author Art Clarke
 * @author Paul Gregoire (mondain@gmail.com)
 */
public enum AudioCodec {
	
	PCM((byte) 0), ADPCM((byte) 0x01), MP3((byte) 0x02), PCM_LE((byte) 0x03), 
	NELLY_MOSER_16K((byte) 0x04), NELLY_MOSER_8K((byte) 0x05), NELLY_MOSER((byte) 0x06), 
	PCM_ALAW((byte) 0x07), PCM_MULAW((byte) 0x08), RESERVED((byte) 0x09), 
	AAC((byte) 0x0a), SPEEX((byte) 0x0b), MP3_8K((byte) 0x0e), 
	DEVICE_SPECIFIC((byte) 0x0f);

	private byte id;

	private AudioCodec(byte id) {
		this.id = id;
	}

	/**
	 * Returns back a numeric id for this codec, that happens to correspond to the 
	 * numeric identifier that FLV will use for this codec.
	 * 
	 * @return the codec id
	 */
	public byte getId() {
		return id;
	}
	
}