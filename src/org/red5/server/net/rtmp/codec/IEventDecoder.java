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

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.FlexMessage;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.so.ISharedObjectMessage;

/**
 * Event decoder decodes event objects from incoming byte buffer.
 */
public interface IEventDecoder {
    /**
     * Decodes event of Unknown type.
	 *
     * @param dataType               Data type
     * @param in                     Byte buffer to decode
     * @return                       Unknown event
     */
	public abstract Unknown decodeUnknown(byte dataType, IoBuffer in);

    /**
     * Decodes chunk size event.
	 *
     * @param in                     Byte buffer to decode
     * @return                       ChunkSize event
     */
	public abstract ChunkSize decodeChunkSize(IoBuffer in);

    /**
     * Decodes shared object message event.
	 *
     * @param in                     Byte buffer to decode
     * @param rtmp					 RTMP protocol state
     * @return                       ISharedObjectMessage event
     */
	public abstract ISharedObjectMessage decodeSharedObject(IoBuffer in, RTMP rtmp);

    /**
     * Decodes shared object message event from AMF3 encoding.
	 *
     * @param in                     Byte buffer to decode
     * @param rtmp					 RTMP protocol state
     * @return                       ISharedObjectMessage event
     */
	public abstract ISharedObjectMessage decodeFlexSharedObject(IoBuffer in, RTMP rtmp);

    /**
     * Decodes notification event.
	 *
     * @param in                     Byte buffer to decode
     * @param rtmp					 RTMP protocol state
     * @return                       Notify event
     */
    public abstract Notify decodeNotify(IoBuffer in, RTMP rtmp);

    /**
     * Decodes invocation event.
	 *
     * @param in                     Byte buffer to decode
     * @param rtmp					 RTMP protocol state
     * @return                       Invoke event
     */
    public abstract Invoke decodeInvoke(IoBuffer in, RTMP rtmp);

    /**
     * Decodes ping event.
	 *
     * @param in                     Byte buffer to decode
     * @return                       Ping event
     */
    public abstract Ping decodePing(IoBuffer in);

    /**
     * Decodes BytesRead event.
	 *
     * @param in                     Byte buffer to decode
     * @return                       BytesRead event
     */
    public abstract BytesRead decodeBytesRead(IoBuffer in);

	/**
	 * Decodes the aggregated data.
	 * 
	 * @param in                     Byte buffer to decode
     * @return                       Aggregate event
	 */
	public abstract Aggregate decodeAggregate(IoBuffer in);
    
    /**
     * Decodes audio data event.
	 *
     * @param in                     Byte buffer to decode
     * @return                       AudioData event
     */
    public abstract AudioData decodeAudioData(IoBuffer in);

    /**
     * Decodes video data event.
	 *
     * @param in                     Byte buffer to decode
     * @return                       VideoData event
     */
    public abstract VideoData decodeVideoData(IoBuffer in);

    /**
     * Decodes Flex message event.
	 *
     * @param in                     Byte buffer to decode
     * @param rtmp					 RTMP protocol state
     * @return                       FlexMessage event
     */
    public abstract FlexMessage decodeFlexMessage(IoBuffer in, RTMP rtmp);
}
