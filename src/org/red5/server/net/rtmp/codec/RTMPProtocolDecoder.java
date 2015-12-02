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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.amf.Output;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Serializer;
import org.red5.io.utils.BufferUtils;
import org.red5.server.IConnection;
import org.red5.server.Red5;
import org.red5.server.IConnection.Encoding;
import org.red5.server.net.ProtocolException;
import org.red5.server.net.ProtocolState;
import org.red5.server.net.rtmp.HandshakeFailedException;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.event.Abort;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.FlexMessage;
import org.red5.server.net.rtmp.event.FlexStreamSend;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.message.SharedObjectTypeMapping;
import org.red5.server.net.rtmp.message.StreamAction;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.so.FlexSharedObjectMessage;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.ISharedObjectMessage;
import org.red5.server.so.SharedObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP protocol decoder.
 */
public class RTMPProtocolDecoder implements Constants, IEventDecoder {

	/**
	 * Logger.
	 */
	protected static Logger log = LoggerFactory.getLogger(RTMPProtocolDecoder.class);

	/**
	 * Deserializer.
	 */
	private Deserializer deserializer;

	/** Constructs a new RTMPProtocolDecoder. */
	public RTMPProtocolDecoder() {
	}

	/**
	 * Setter for deserializer.
	 * 
	 * @param deserializer Deserializer
	 */
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 * Decode all available objects in buffer.
	 * 
	 * @param state Stores state for the protocol
	 * @param buffer IoBuffer of data to be decoded
	 * @return a list of decoded objects, may be empty if nothing could be
	 *         decoded
	 */
	public List<Object> decodeBuffer(ProtocolState state, IoBuffer buffer) {
		final List<Object> result = new LinkedList<Object>();
		try {
			while (true) {
				final int remaining = buffer.remaining();
				if (state.canStartDecoding(remaining)) {
					state.startDecoding();
				} else {
					break;
				}
				final Object decodedObject = decode(state, buffer);
				if (state.hasDecodedObject()) {
					if (decodedObject != null) {
						result.add(decodedObject);
					}
				} else if (state.canContinueDecoding()) {
					continue;
				} else {
					break;
				}
				if (!buffer.hasRemaining()) {
					break;
				}
			}
		} catch (HandshakeFailedException hfe) {
			// patched by Victor to clear buffer if something is wrong in
			// protocol decoding.
			buffer.clear();
			// get the connection and close it
			IConnection conn = Red5.getConnectionLocal();
			if (conn != null) {
				conn.close();
			} else {
				log.error("Handshake validation failed but no current connection!?");
			}
			return null;
		} catch (Exception ex) {
			// Exception handling is patched by Victor - we catch any exception in the decoding
			// Then clear the buffer to eliminate memory leaks when we can't parse protocol
			// Also close Connection because we can't parse data from it
			log.error("Error decoding buffer", ex);
			buffer.clear();
			// get the connection and close it
			IConnection conn = Red5.getConnectionLocal();
			if (conn != null) {
				log.warn("Closing connection because decoding failed: {}", conn);
				conn.close();
			} else {
				log.error("Decoding buffer failed but no current connection!?");
			}
			return null;
		} finally {
			buffer.compact();
		}
		return result;
	}

	/**
	 * Decodes the buffer data
	 * 
	 * @param state Stores state for the protocol, ProtocolState is just a marker
	 *            interface
	 * @param in IoBuffer of data to be decoded
	 * @return one of three possible values. null : the object could not be
	 *         decoded, or some data was skipped, just continue. ProtocolState :
	 *         the decoder was unable to decode the whole object, refer to the
	 *         protocol state Object : something was decoded, continue
	 * @throws Exception on error
	 */
	public Object decode(ProtocolState state, IoBuffer in) throws ProtocolException {
//		int start = in.position();
//		log.debug("Start: {}", start);
		try {
			final RTMP rtmp = (RTMP) state;
			switch (rtmp.getState()) {
				case RTMP.STATE_CONNECTED:
					return decodePacket(rtmp, in);
				case RTMP.STATE_CONNECT:
				case RTMP.STATE_HANDSHAKE:
					return decodeHandshake(rtmp, in);
				case RTMP.STATE_ERROR:
					// attempt to correct error
				default:
					return null;
			}
		} catch (ProtocolException pe) {
			// Raise to caller unmodified
			throw pe;
		} catch (RuntimeException e) {
			throw new ProtocolException("Error during decoding", e);
		}
	}

	/**
	 * Decodes handshake message.
	 * 
	 * @param rtmp RTMP protocol state
	 * @param in IoBuffer
	 * @return IoBuffer
	 */
	public IoBuffer decodeHandshake(RTMP rtmp, IoBuffer in) {
//		log.debug("decodeHandshake - rtmp: {} buffer: {}", rtmp, in);
		final int remaining = in.remaining();
		if (rtmp.getMode() == RTMP.MODE_SERVER) {
			if (rtmp.getState() == RTMP.STATE_CONNECT) {
				if (remaining < HANDSHAKE_SIZE + 1) {
//					log.debug("Handshake init too small, buffering. remaining: {}", remaining);
					rtmp.bufferDecoding(HANDSHAKE_SIZE + 1);
				} else {
					final IoBuffer hs = IoBuffer.allocate(HANDSHAKE_SIZE);
					in.get(); // skip the header byte
					BufferUtils.put(hs, in, HANDSHAKE_SIZE);
					hs.flip();
					rtmp.setState(RTMP.STATE_HANDSHAKE);
					return hs;
				}
			} else if (rtmp.getState() == RTMP.STATE_HANDSHAKE) {
				log.debug("Handshake reply");
				if (remaining < HANDSHAKE_SIZE) {
					log.debug("Handshake reply too small, buffering. remaining: {}", remaining);
					rtmp.bufferDecoding(HANDSHAKE_SIZE);
				} else {
					in.skip(HANDSHAKE_SIZE);
					rtmp.setState(RTMP.STATE_CONNECTED);
					rtmp.continueDecoding();
				}
			}
		} else {
			// else, this is client mode.
			if (rtmp.getState() == RTMP.STATE_CONNECT) {
				final int size = (2 * HANDSHAKE_SIZE) + 1;
				if (remaining < size) {
					log.debug("Handshake init too small, buffering. remaining: {}", remaining);
					rtmp.bufferDecoding(size);
				} else {
					final IoBuffer hs = IoBuffer.allocate(size);
					BufferUtils.put(hs, in, size);
					hs.flip();
					rtmp.setState(RTMP.STATE_CONNECTED);
					return hs;
				}
			}
		}
		return null;
	}

	/**
	 * Decodes packet.
	 * 
	 * @param rtmp RTMP protocol state
	 * @param in IoBuffer
	 * @return IoBuffer
	 */
	public Packet decodePacket(RTMP rtmp, IoBuffer in) {
//		log.debug("decodePacket - rtmp: {} buffer: {}", rtmp, in);
		final int remaining = in.remaining();

		// We need at least one byte
		if (remaining < 1) {
			rtmp.bufferDecoding(1);
			return null;
		}

		final int position = in.position();
		byte headerByte = in.get();
		int headerValue;
		int byteCount;
		if ((headerByte & 0x3f) == 0) {
			// Two byte header
			if (remaining < 2) {
				in.position(position);
				rtmp.bufferDecoding(2);
				return null;
			}
			headerValue = (headerByte & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 2;
		} else if ((headerByte & 0x3f) == 1) {
			// Three byte header
			if (remaining < 3) {
				in.position(position);
				rtmp.bufferDecoding(3);
				return null;
			}
			headerValue = (headerByte & 0xff) << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 3;
		} else {
			// Single byte header
			headerValue = headerByte & 0xff;
			byteCount = 1;
		}
		final int channelId = RTMPUtils.decodeChannelId(headerValue, byteCount);
		if (channelId < 0) {
			throw new ProtocolException("Bad channel id: " + channelId);
		}

		// Get the header size and length
		int headerLength = RTMPUtils.getHeaderLength(RTMPUtils.decodeHeaderSize(headerValue, byteCount));
		headerLength += byteCount - 1;

		if (headerLength + byteCount - 1 > remaining) {
			log.debug("Header too small, buffering. remaining: {}", remaining);
			in.position(position);
			rtmp.bufferDecoding(headerLength + byteCount - 1);
			return null;
		}

		// Move the position back to the start
		in.position(position);

		Header lastHeader = rtmp.getLastReadHeader(channelId);
		final Header header = decodeHeader(in, lastHeader);
		if (header == null) {
			throw new ProtocolException("Header is null, check for error");
		}
		rtmp.setLastReadHeader(channelId, header);

		// Check to see if this is a new packets or continue decoding an
		// existing one.
		Packet packet = rtmp.getLastReadPacket(channelId);
		if (packet == null) {
			packet = new Packet(header.clone());
			rtmp.setLastReadPacket(channelId, packet);
		}

		final IoBuffer buf = packet.getData();
		final int readRemaining = header.getSize() - buf.position();
		final int chunkSize = rtmp.getReadChunkSize();
		final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
		if (in.remaining() < readAmount) {
//			log.debug("Chunk too small, buffering ({},{})", in.remaining(), readAmount);
			// skip the position back to the start
			in.position(position);
			rtmp.bufferDecoding(headerLength + readAmount);
			return null;
		}

		BufferUtils.put(buf, in, readAmount);
		if (buf.position() < header.getSize()) {
			rtmp.continueDecoding();
			return null;
		}

		// Check workaround for SN-19 to find cause for BufferOverflowException
		if (buf.position() > header.getSize()) {
			log.warn("Packet size expanded from {} to {} ({})", new Object[] { (header.getSize()), buf.position(), header });
		}

		buf.flip();

		try {
			final IRTMPEvent message = decodeMessage(rtmp, packet.getHeader(), buf);
			message.setHeader(packet.getHeader());
			// Unfortunately flash will, especially when resetting a video stream with a new key frame, sometime 
			// send an earlier time stamp.  To avoid dropping it, we just give it the minimal increment since the 
			// last message.  But to avoid relative time stamps being mis-computed, we don't reset the header we stored.
			final Header lastReadHeader = rtmp.getLastReadPacketHeader(channelId);
			if (lastReadHeader != null && (message instanceof AudioData || message instanceof VideoData)
					&& RTMPUtils.compareTimestamps(lastReadHeader.getTimer(), packet.getHeader().getTimer()) >= 0) {
//				log.trace("Non-monotonically increasing timestamps; type: {}; adjusting to {}; ts: {}; last: {}", new Object[] { header.getDataType(),
//						lastReadHeader.getTimer() + 1, header.getTimer(), lastReadHeader.getTimer() });
				message.setTimestamp(lastReadHeader.getTimer() + 1);
			} else {
				message.setTimestamp(header.getTimer());
			}
			rtmp.setLastReadPacketHeader(channelId, packet.getHeader());

			packet.setMessage(message);

			if (message instanceof ChunkSize) {
				ChunkSize chunkSizeMsg = (ChunkSize) message;
				rtmp.setReadChunkSize(chunkSizeMsg.getSize());
			} else if (message instanceof Abort) {
				log.debug("Abort packet detected");
				// The client is aborting a message; reset the packet
				// because the next chunk on that stream will start a new packet.
				Abort abort = (Abort) message;
				rtmp.setLastReadPacket(abort.getChannelId(), null);
				packet = null;
			}
			if (packet != null && packet.getHeader().isGarbage()) {
				// discard this packet; this gets rid of the garbage
				// audio data FP inserts
//				log.trace("Dropping garbage packet: {}, {}", packet, packet.getHeader());
				packet = null;
			} else {
				// collapse the time stamps on the last packet so that it works
				// right for chunk type 3 later
				lastHeader = rtmp.getLastReadHeader(channelId);
				lastHeader.setTimerBase(header.getTimer());
			}
		} finally {
			rtmp.setLastReadPacket(channelId, null);
		}
		return packet;

	}

	/**
	 * Decodes packet header.
	 * 
	 * @param in Input IoBuffer
	 * @param lastHeader Previous header
	 * @return Decoded header
	 */
	public Header decodeHeader(IoBuffer in, Header lastHeader) {
//		log.debug("decodeHeader - lastHeader: {} buffer: {}", lastHeader, in);
		byte headerByte = in.get();
		int headerValue;
		int byteCount = 1;
		if ((headerByte & 0x3f) == 0) {
			// Two byte header
			headerValue = (headerByte & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 2;
		} else if ((headerByte & 0x3f) == 1) {
			// Three byte header
			headerValue = (headerByte & 0xff) << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 3;
		} else {
			// Single byte header
			headerValue = headerByte & 0xff;
			byteCount = 1;
		}
		final int channelId = RTMPUtils.decodeChannelId(headerValue, byteCount);
		final int headerSize = RTMPUtils.decodeHeaderSize(headerValue, byteCount);
		Header header = new Header();
		header.setChannelId(channelId);
		header.setIsGarbage(false);
		if (headerSize != HEADER_NEW && lastHeader == null) {
			log.error("Last header null not new, headerSize: {}, channelId {}", headerSize, channelId);
			//this will trigger an error status, which in turn will disconnect the "offending" flash player
			//preventing a memory leak and bringing the whole server to its knees
			return null;
		}
		int timeValue;
		switch (headerSize) {
			case HEADER_NEW:
				// an absolute time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(RTMPUtils.readUnsignedMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(RTMPUtils.readReverseInt(in));
				if (timeValue == 0xffffff) {
					timeValue = in.getInt();
				}
				header.setTimerBase(timeValue);
				header.setTimerDelta(0);
				break;

			case HEADER_SAME_SOURCE:
				// a delta time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(RTMPUtils.readUnsignedMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(lastHeader.getStreamId());
				if (timeValue == 0xffffff) {
					timeValue = in.getInt();
				} else if (timeValue == 0 && header.getDataType() == TYPE_AUDIO_DATA) {
					header.setIsGarbage(true);
//					log.trace("Audio with zero delta; setting to garbage; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(),
//							headerSize });
				}
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(timeValue);
				break;

			case HEADER_TIMER_CHANGE:
				// a delta time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				if (timeValue == 0xffffff) {
					timeValue = in.getInt();
				} else if (timeValue == 0 && header.getDataType() == TYPE_AUDIO_DATA) {
					header.setIsGarbage(true);
//					log.trace("Audio with zero delta; setting to garbage; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(),
//							headerSize });
				}
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(timeValue);
				break;

			case HEADER_CONTINUE:
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(lastHeader.getTimerDelta());
				break;

			default:
				log.error("Unexpected header size: {}", headerSize);
				return null;
		}
//		log.trace("CHUNK, D, {}, {}", header, headerSize);
		return header;
	}

	/**
	 * Decodes RTMP message event.
	 * 
	 * @param rtmp RTMP protocol state
	 * @param header RTMP header
	 * @param in Input IoBuffer
	 * @return RTMP event
	 */
	public IRTMPEvent decodeMessage(RTMP rtmp, Header header, IoBuffer in) {
		IRTMPEvent message;
		byte dataType = header.getDataType();
		switch (dataType) {
			case TYPE_CHUNK_SIZE:
				message = decodeChunkSize(in);
				break;
			case TYPE_ABORT:
				message = decodeAbort(in);
				break;
			case TYPE_INVOKE:
				message = decodeInvoke(in, rtmp);
				break;
			case TYPE_NOTIFY:
				if (header.getStreamId() == 0) {
					message = decodeNotify(in, header, rtmp);
				} else {
					message = decodeStreamMetadata(in, rtmp);
				}
				break;
			case TYPE_PING:
				message = decodePing(in);
				break;
			case TYPE_BYTES_READ:
				message = decodeBytesRead(in);
				break;
			case TYPE_AUDIO_DATA:
				message = decodeAudioData(in);
				break;
			case TYPE_VIDEO_DATA:
				message = decodeVideoData(in);
				break;
			case TYPE_FLEX_SHARED_OBJECT:
				message = decodeFlexSharedObject(in, rtmp);
				break;
			case TYPE_SHARED_OBJECT:
				message = decodeSharedObject(in, rtmp);
				break;
			case TYPE_SERVER_BANDWIDTH:
				message = decodeServerBW(in);
				break;
			case TYPE_CLIENT_BANDWIDTH:
				message = decodeClientBW(in);
				break;
			case TYPE_FLEX_MESSAGE:
				message = decodeFlexMessage(in, rtmp);
				break;
			case TYPE_FLEX_STREAM_SEND:
				message = decodeFlexStreamSend(in);
				break;
			case TYPE_AGGREGATE:
				message = decodeAggregate(in);
				break;
			default:
				log.warn("Unknown object type: {}", dataType);
				message = decodeUnknown(dataType, in);
				break;
		}
		return message;
	}

	public IRTMPEvent decodeAbort(IoBuffer in) {
		return new Abort(in.getInt());
	}

	/**
	 * Decodes server bandwidth.
	 * 
	 * @param in IoBuffer
	 * @return RTMP event
	 */
	private IRTMPEvent decodeServerBW(IoBuffer in) {
		return new ServerBW(in.getInt());
	}

	/**
	 * Decodes client bandwidth.
	 * 
	 * @param in
	 *            Byte buffer
	 * @return RTMP event
	 */
	private IRTMPEvent decodeClientBW(IoBuffer in) {
		return new ClientBW(in.getInt(), in.get());
	}

	/** {@inheritDoc} */
	public Unknown decodeUnknown(byte dataType, IoBuffer in) {
		return new Unknown(dataType, in);
	}

	/** {@inheritDoc} */
	public Aggregate decodeAggregate(IoBuffer in) {
		return new Aggregate(in);
	}

	/** {@inheritDoc} */
	public ChunkSize decodeChunkSize(IoBuffer in) {
		return new ChunkSize(in.getInt());
	}

	/** {@inheritDoc} */
	public ISharedObjectMessage decodeFlexSharedObject(IoBuffer in, RTMP rtmp) {
		byte encoding = in.get();
		Input input;
		if (encoding == 0) {
			input = new org.red5.io.amf.Input(in);
		} else if (encoding == 3) {
			input = new org.red5.io.amf3.Input(in);
		} else {
			throw new RuntimeException("Unknown SO encoding: " + encoding);
		}
		String name = input.getString();
		// Read version of SO to modify
		int version = in.getInt();
		// Read persistence informations
		boolean persistent = in.getInt() == 2;
		// Skip unknown bytes
		in.skip(4);

		final SharedObjectMessage so = new FlexSharedObjectMessage(null, name, version, persistent);
		doDecodeSharedObject(so, in, input);
		return so;
	}

	/** {@inheritDoc} */
	public ISharedObjectMessage decodeSharedObject(IoBuffer in, RTMP rtmp) {
		final Input input = new org.red5.io.amf.Input(in);
		String name = input.getString();
		// Read version of SO to modify
		int version = in.getInt();
		// Read persistence informations
		boolean persistent = in.getInt() == 2;
		// Skip unknown bytes
		in.skip(4);

		final SharedObjectMessage so = new FlexSharedObjectMessage(null, name, version, persistent);
		doDecodeSharedObject(so, in, input);
		return so;
	}

	/**
	 * Perform the actual decoding of the shared object contents.
	 * 
	 * @param so
	 * @param in
	 * @param input
	 */
	protected void doDecodeSharedObject(SharedObjectMessage so, IoBuffer in, Input input) {
		// Parse request body
		Input amf3Input = new org.red5.io.amf3.Input(in);
		while (in.hasRemaining()) {
			final ISharedObjectEvent.Type type = SharedObjectTypeMapping.toType(in.get());
			if (type == null) {
				in.skip(in.remaining());
				return;
			}
			String key = null;
			Object value = null;

			// if(log.isDebugEnabled())
			// log.debug("type: "+SharedObjectTypeMapping.toString(type));

			// SharedObjectEvent event = new SharedObjectEvent(,null,null);
			final int length = in.getInt();
			if (type == ISharedObjectEvent.Type.CLIENT_STATUS) {
				// Status code
				key = input.getString();
				// Status level
				value = input.getString();
			} else if (type == ISharedObjectEvent.Type.CLIENT_UPDATE_DATA) {
				key = null;
				// Map containing new attribute values
				final Map<String, Object> map = new HashMap<String, Object>();
				final int start = in.position();
				while (in.position() - start < length) {
					String tmp = input.getString();
					map.put(tmp, deserializer.deserialize(input, Object.class));
				}
				value = map;
			} else if (type != ISharedObjectEvent.Type.SERVER_SEND_MESSAGE && type != ISharedObjectEvent.Type.CLIENT_SEND_MESSAGE) {
				if (length > 0) {
					key = input.getString();
					if (length > key.length() + 2) {
						// FIXME workaround for player version >= 9.0.115.0
						byte objType = in.get();
						in.position(in.position() - 1);
						Input propertyInput;
						if (objType == AMF.TYPE_AMF3_OBJECT && !(input instanceof org.red5.io.amf3.Input)) {
							// The next parameter is encoded using AMF3
							propertyInput = amf3Input;
						} else {
							// The next parameter is encoded using AMF0
							propertyInput = input;
						}
						value = deserializer.deserialize(propertyInput, Object.class);
					}
				}
			} else {
				final int start = in.position();
				// the "send" event seems to encode the handler name
				// as complete AMF string including the string type byte
				key = deserializer.deserialize(input, String.class);

				// read parameters
				final List<Object> list = new LinkedList<Object>();
				// while loop changed for JIRA CODECS-9
				while (in.position() - start < length) {
					byte objType = in.get();
					in.position(in.position() - 1);
					// FIXME workaround for player version >= 9.0.115.0
					Input propertyInput;
					if (objType == AMF.TYPE_AMF3_OBJECT && !(input instanceof org.red5.io.amf3.Input)) {
						// The next parameter is encoded using AMF3
						propertyInput = amf3Input;
					} else {
						// The next parameter is encoded using AMF0
						propertyInput = input;
					}
					Object tmp = deserializer.deserialize(propertyInput, Object.class);
					list.add(tmp);
				}
				value = list;
			}
			so.addEvent(type, key, value);
		}
	}

	/** {@inheritDoc} */
	public Notify decodeNotify(IoBuffer in, RTMP rtmp) {
		return decodeNotify(in, null, rtmp);
	}

	public Notify decodeNotify(IoBuffer in, Header header, RTMP rtmp) {
		return decodeNotifyOrInvoke(new Notify(), in, header, rtmp);
	}

	/** {@inheritDoc} */
	public Invoke decodeInvoke(IoBuffer in, RTMP rtmp) {
		return (Invoke) decodeNotifyOrInvoke(new Invoke(), in, null, rtmp);
	}

	/**
	 * Checks if the passed action is a reserved stream method.
	 * 
	 * @param action
	 *            Action to check
	 * @return <code>true</code> if passed action is a reserved stream method,
	 *         <code>false</code> otherwise
	 */
	private boolean isStreamCommand(String action) {
		switch (StreamAction.getEnum(action)) {
			case CREATE_STREAM:
			case DELETE_STREAM:
			case RELEASE_STREAM:
			case PUBLISH:
			case PLAY:
			case PLAY2:
			case SEEK:
			case PAUSE:
			case PAUSE_RAW:
			case CLOSE_STREAM:
			case RECEIVE_VIDEO:
			case RECEIVE_AUDIO:
				return true;
			default:
				log.debug("Stream action {} is not a recognized command", action);
				return false;
		}
	}

	/**
	 * Decodes notification event.
	 * 
	 * @param notify
	 *            Notify event
	 * @param in
	 *            Byte buffer
	 * @param header
	 *            Header
	 * @param rtmp
	 *            RTMP protocol state
	 * @return Notification event
	 */
	@SuppressWarnings({ "unchecked" })
	protected Notify decodeNotifyOrInvoke(Notify notify, IoBuffer in, Header header, RTMP rtmp) {
		// TODO: we should use different code depending on server or client mode
		int start = in.position();
		Input input;
		// for response, the action string and invokeId is always encoded as AMF0
		// we use the first byte to decide which encoding to use.
		byte tmp = in.get();
		in.position(start);
		if (rtmp.getEncoding() == Encoding.AMF3 && tmp == AMF.TYPE_AMF3_OBJECT) {
			input = new org.red5.io.amf3.Input(in);
		} else {
			input = new org.red5.io.amf.Input(in);
		}
		String action = deserializer.deserialize(input, String.class);
//		log.info("Action {}", action);
		//throw a runtime exception if there is no action
		if (action == null) {
			//TODO replace this with something better as time permits
			throw new RuntimeException("Action was null");
		}

		//TODO Handle NetStream.send? Where and how?

		if (!(notify instanceof Invoke) && rtmp != null && rtmp.getMode() == RTMP.MODE_SERVER && header != null && header.getStreamId() != 0 && !isStreamCommand(action)) {
			// Don't decode "NetStream.send" requests
			in.position(start);
			notify.setData(in.asReadOnlyBuffer());
			return notify;
		}

		if (header == null || header.getStreamId() == 0) {
			int invokeId = deserializer.<Number> deserialize(input, Number.class).intValue();
			notify.setInvokeId(invokeId);
		}

		// now go back to the actual encoding to decode parameters
		if (rtmp.getEncoding() == Encoding.AMF3) {
			input = new org.red5.io.amf3.Input(in);
		} else {
			input = new org.red5.io.amf.Input(in);
		}

		Object[] params = new Object[] {};

		if (in.hasRemaining()) {
			List<Object> paramList = new ArrayList<Object>();

			final Object obj = deserializer.deserialize(input, Object.class);

			if (obj instanceof Map) {
				// Before the actual parameters we sometimes (connect) get a map
				// of parameters, this is usually null, but if set should be
				// passed to the connection object.
				final Map<String, Object> connParams = (Map<String, Object>) obj;
				notify.setConnectionParams(connParams);
			} else if (obj != null) {
				paramList.add(obj);
			}

			while (in.hasRemaining()) {
				paramList.add(deserializer.deserialize(input, Object.class));
			}
			params = paramList.toArray();
//			if (log.isDebugEnabled()) {
//				log.debug("Num params: {}", paramList.size());
//				for (int i = 0; i < params.length; i++) {
//					log.info(" > {}: {}", i, params[i]);
//				}
//			}
		}

		final int dotIndex = action.lastIndexOf('.');
		String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
		//pull off the prefixes since java doesnt allow this on a method name
		if (serviceName != null && (serviceName.startsWith("@") || serviceName.startsWith("|"))) {
			serviceName = serviceName.substring(1);
		}
		String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
		//pull off the prefixes since java doesnt allow this on a method name
		if (serviceMethod.startsWith("@") || serviceMethod.startsWith("|")) {
			serviceMethod = serviceMethod.substring(1);
		}

		if (notify instanceof Invoke) {
			PendingCall call = new PendingCall(serviceName, serviceMethod, params);
			((Invoke) notify).setCall(call);
		} else {
			Call call = new Call(serviceName, serviceMethod, params);
			notify.setCall(call);
		}

		return notify;
	}

	/**
	 * Decodes ping event.
	 * 
	 * @param in IoBuffer
	 * @return Ping event
	 */
	public Ping decodePing(IoBuffer in) {
		final Ping ping = new Ping();
		ping.setDebug(in.getHexDump());
		ping.setEventType(in.getShort());
		ping.setValue2(in.getInt());
		if (in.hasRemaining()) {
			ping.setValue3(in.getInt());
		}
		if (in.hasRemaining()) {
			ping.setValue4(in.getInt());
		}
		return ping;
	}

	/** {@inheritDoc} */
	public BytesRead decodeBytesRead(IoBuffer in) {
		return new BytesRead(in.getInt());
	}

	/** {@inheritDoc} */
	public AudioData decodeAudioData(IoBuffer in) {
		return new AudioData(in.asReadOnlyBuffer());
	}

	/** {@inheritDoc} */
	public VideoData decodeVideoData(IoBuffer in) {
		return new VideoData(in.asReadOnlyBuffer());
	}

	@SuppressWarnings("unchecked")
	public Notify decodeStreamMetadata(IoBuffer in, RTMP rtmp) {
		Input input;
		//we will make a pre-emptive copy of the incoming buffer here to
		//prevent issues that seem to occur fairly often
		IoBuffer copy = in.duplicate();
		log.error("metadata {}", copy.buf().toString());
		if (rtmp.getEncoding() == Encoding.AMF0) {
			input = new org.red5.io.amf.Input(copy);
		} else {
			org.red5.io.amf3.Input.RefStorage refStorage = new org.red5.io.amf3.Input.RefStorage();
			input = new org.red5.io.amf3.Input(copy, refStorage);
		}
		//get the first datatype
		byte dataType = input.readDataType();
		if (dataType == DataTypes.CORE_STRING) {
			String setData = input.readString(String.class);
			if (setData.equals("@setDataFrame")) {
				//get the second datatype
				@SuppressWarnings("unused")
				byte dataType2 = input.readDataType();
//				log.debug("Dataframe method type: {}", dataType2);
				String onCueOrOnMeta = input.readString(String.class);
				//get the params datatype
				byte object = input.readDataType();
//				log.debug("Dataframe params type: {}", object);
				Map<Object, Object> params;
				if (object == DataTypes.CORE_MAP) {
					// The params are sent as a Mixed-Array.  This is needed
					// to support the RTMP publish provided by ffmpeg/xuggler
					params = (Map<Object, Object>) input.readMap(deserializer, null);
				} else {
					// Read the params as a standard object
					params = (Map<Object, Object>) input.readObject(deserializer, Object.class);
				}
//				log.debug("Dataframe: {} params: {}", onCueOrOnMeta, params.toString());

				IoBuffer buf = IoBuffer.allocate(1024);
				buf.setAutoExpand(true);
				Output out = new Output(buf);
				out.writeString(onCueOrOnMeta);
				out.writeMap(params, new Serializer());

				buf.flip();
				return new Notify(buf);
			} else {
				log.info("Unhandled request: {}", setData);
			}
		}

		return new Notify(in.asReadOnlyBuffer());
	}

	/**
	 * Decodes FlexMessage event.
	 * 
	 * @param in IoBuffer
	 * @param rtmp RTMP protocol state
	 * @return FlexMessage event
	 */
	public FlexMessage decodeFlexMessage(IoBuffer in, RTMP rtmp) {
		// TODO: Unknown byte, probably encoding as with Flex SOs?
		in.skip(1);
		// Encoding of message params can be mixed - some params may be in AMF0, others in AMF3,
		// but according to AMF3 spec, we should collect AMF3 references
		// for the whole message body (through all params)
		org.red5.io.amf3.Input.RefStorage refStorage = new org.red5.io.amf3.Input.RefStorage();

		Input input = new org.red5.io.amf.Input(in);
		String action = deserializer.deserialize(input, String.class);
		int invokeId = deserializer.<Number> deserialize(input, Number.class).intValue();
		FlexMessage msg = new FlexMessage();
		msg.setInvokeId(invokeId);
		Object[] params = new Object[] {};

		if (in.hasRemaining()) {
			ArrayList<Object> paramList = new ArrayList<Object>();

			final Object obj = deserializer.deserialize(input, Object.class);
			if (obj != null) {
				paramList.add(obj);
			}

			while (in.hasRemaining()) {
				// Check for AMF3 encoding of parameters
				byte tmp = in.get();
				in.position(in.position() - 1);
				if (tmp == AMF.TYPE_AMF3_OBJECT) {
					// The next parameter is encoded using AMF3
					input = new org.red5.io.amf3.Input(in, refStorage);
				} else {
					// The next parameter is encoded using AMF0
					input = new org.red5.io.amf.Input(in);
				}
				paramList.add(deserializer.deserialize(input, Object.class));
			}
			params = paramList.toArray();
//			if (log.isDebugEnabled()) {
//				log.debug("Num params: {}", paramList.size());
//				for (int i = 0; i < params.length; i++) {
//					log.info(" > {}: {}", i, params[i]);
//				}
//			}
		}

		final int dotIndex = action.lastIndexOf('.');
		String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
		String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());

		PendingCall call = new PendingCall(serviceName, serviceMethod, params);
		msg.setCall(call);
		return msg;
	}

	public FlexStreamSend decodeFlexStreamSend(IoBuffer in) {
		return new FlexStreamSend(in.asReadOnlyBuffer());
	}

}
