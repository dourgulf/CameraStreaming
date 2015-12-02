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

//import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.BufferUtils;
import org.red5.server.IConnection;
import org.red5.server.Red5;
import org.red5.server.IConnection.Encoding;
//import org.red5.server.api.stream.IClientStream;
//import org.red5.server.exception.ClientDetailsException;
import org.red5.server.net.ProtocolState;
//import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.codec.RTMP.LiveTimestampMapping;
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
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.message.SharedObjectTypeMapping;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.service.Call;
import org.red5.server.service.IPendingServiceCall;
import org.red5.server.service.IServiceCall;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.ISharedObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP protocol encoder encodes RTMP messages and packets to byte buffers.
 */
public class RTMPProtocolEncoder implements Constants, IEventEncoder {

	/**
	 * Logger.
	 */
	protected static Logger log = LoggerFactory.getLogger(RTMPProtocolEncoder.class);

	/**
	 * Serializer object.
	 */
	private Serializer serializer;

	/**
	 * Tolerance (in milliseconds) for late media on streams. A set of levels based on this
	 * value will be determined. 
	 */
	private long baseTolerance = 15000;

	/**
	 * Middle tardiness level, between base and this value disposable frames
	 * will be dropped. Between this and highest value regular interframes
	 * will be dropped.
	 */
	private long midTolerance = baseTolerance + (long) (baseTolerance * 0.3);
	
	/**
	 * Highest tardiness level before dropping key frames
	 */
	private long highestTolerance = baseTolerance + (long) (baseTolerance * 0.6);

	
	/**
	 * Indicates if we should drop live packets with future timestamp 
	 * (i.e, when publisher bandwidth is limited) - EXPERIMENTAL
	 * */
	private boolean dropLiveFuture = false;
	
	/**
	 * Encodes object with given protocol state to byte buffer
	 * 
	 * @param state			Protocol state
	 * @param message		Object to encode
	 * @return				IoBuffer with encoded data
	 * @throws Exception    Any decoding exception
	 */
	public IoBuffer encode(ProtocolState state, Object message) throws Exception {
		try {
			final RTMP rtmp = (RTMP) state;
			if (message instanceof IoBuffer) {
				return (IoBuffer) message;
			} else {
				return encodePacket(rtmp, (Packet) message);
			}
		} catch (RuntimeException e) {
			log.error("Error encoding object: ", e);
		}
		return null;
	}

	/**
	 * Encode packet.
	 *
	 * @param rtmp        RTMP protocol state
	 * @param packet      RTMP packet
	 * @return            Encoded data
	 */
	public IoBuffer encodePacket(RTMP rtmp, Packet packet) {

		IoBuffer out = null;
		IoBuffer data = null;

		final Header header = packet.getHeader();
		final int channelId = header.getChannelId();
//		log.debug("Channel id: {}", channelId);
		final IRTMPEvent message = packet.getMessage();

		if (message instanceof ChunkSize) {
			ChunkSize chunkSizeMsg = (ChunkSize) message;
			rtmp.setWriteChunkSize(chunkSizeMsg.getSize());
		}

		//normally the message is expected not to be dropped
		if (!dropMessage(rtmp, channelId, message)) {
			data = encodeMessage(rtmp, header, message);
			if (data != null) {
				if (data.position() != 0) {
					data.flip();
				} else {
					data.rewind();
				}
				header.setSize(data.limit());

				Header lastHeader = rtmp.getLastWriteHeader(channelId);
				int headerSize = calculateHeaderSize(rtmp, header, lastHeader);

				rtmp.setLastWriteHeader(channelId, header);
				rtmp.setLastWritePacket(channelId, packet);

				int chunkSize = rtmp.getWriteChunkSize();
				int chunkHeaderSize = 1;
				if (header.getChannelId() > 320) {
					chunkHeaderSize = 3;
				} else if (header.getChannelId() > 63) {
					chunkHeaderSize = 2;
				}
				int numChunks = (int) Math.ceil(header.getSize() / (float) chunkSize);
				int bufSize = header.getSize() + headerSize + (numChunks > 0 ? (numChunks - 1) * chunkHeaderSize : 0);
				out = IoBuffer.allocate(bufSize, false);

				encodeHeader(rtmp, header, lastHeader, out);

				if (numChunks == 1) {
					// we can do it with a single copy
					BufferUtils.put(out, data, out.remaining());
				} else {
					for (int i = 0; i < numChunks - 1; i++) {
						BufferUtils.put(out, data, chunkSize);
						RTMPUtils.encodeHeaderByte(out, HEADER_CONTINUE, header.getChannelId());
					}
					BufferUtils.put(out, data, out.remaining());
				}

				data.free();
				out.flip();
				data = null;
			}
		}
		message.release();

		return out;
	}

	/**
	 * Determine if this message should be dropped for lateness. Live publish data
	 * does not come through this section, only outgoing data does.
	 * 
	 * - determine latency between server and client using ping
	 * - ping timestamp is unsigned int (4 bytes) and is set from value on sender
	 * 
	 * 1st drop disposable frames - lowest mark
	 * 2nd drop interframes - middle
	 * 3rd drop key frames - high mark
	 * 
	 * @param rtmp the protocol state
	 * @param channelId the channel ID
	 * @param message the message
	 * @return true to drop; false to send
	 */
	protected boolean dropMessage(RTMP rtmp, int channelId, IRTMPEvent message) {
		//whether or not the packet will be dropped
		boolean drop = false;
		// we only drop in server mode
		if (rtmp.getMode() == RTMP.MODE_SERVER) {
			//whether or not the packet is video data
			boolean isVideo = false;
			if (message instanceof Ping) {
				final Ping pingMessage = (Ping) message;
				if (pingMessage.getEventType() == Ping.STREAM_PLAYBUFFER_CLEAR) {
					// client buffer cleared, make sure to reset timestamps for this stream
					final int channel = (4 + ((pingMessage.getValue2() - 1) * 5));
					rtmp.setLastTimestampMapping(channel, null);
					rtmp.setLastTimestampMapping(channel+1, null);
					rtmp.setLastTimestampMapping(channel+2, null);
				}
				// never drop pings
				return false;
			}
			
			//we only drop audio or video data
			if ((isVideo = message instanceof VideoData) || message instanceof AudioData) {
				if (message.getTimestamp() == 0) {
					// never drop initial packages, also this could be the first packet after
					// MP4 seeking and therefore mess with the timestamp mapping
					return false;
				}
				
				//determine working type
				boolean isLive = message.getSourceType() == Constants.SOURCE_TYPE_LIVE;
//				log.trace("Connection type: {}", (isLive ? "Live" : "VOD"));

				long timestamp = (message.getTimestamp() & 0xFFFFFFFFL);
				LiveTimestampMapping mapping = rtmp.getLastTimestampMapping(channelId);
				// just get the current time ONCE per packet
				long now = System.currentTimeMillis();
				if (mapping == null || timestamp < mapping.getLastStreamTime()) {
					log.debug("Resetting clock time ({}) to stream time ({})", now, timestamp);
					// either first time through, or time stamps were reset
					mapping = new LiveTimestampMapping(now, timestamp);
					rtmp.setLastTimestampMapping(channelId, mapping);
				}
				mapping.setLastStreamTime(timestamp);

				long clockTimeOfMessage = mapping.getClockStartTime() + timestamp - mapping.getStreamStartTime();

				//determine tardiness / how late it is
				long tardiness = clockTimeOfMessage - now;
				//TDJ: EXPERIMENTAL dropping for LIVE packets in future (default false)
				if (isLive && dropLiveFuture) {
					tardiness = Math.abs(tardiness);
				}
				//subtract the ping time / latency from the tardiness value
				IConnection conn = Red5.getConnectionLocal();
//				log.debug("Connection: {}", conn);
				if (conn != null) {
//					log.debug("Last ping time for connection: {}", conn.getLastPingTime());
					tardiness -= conn.getLastPingTime();
					//subtract the buffer time
//					RTMPConnection rtmpConn = (RTMPConnection) conn;
//					int streamId = rtmpConn.getStreamIdForChannel(channelId);
//					IClientStream stream = rtmpConn.getStreamById(streamId);
//					if (stream != null) {
//						int clientBufferDuration = stream.getClientBufferDuration();
//						if (clientBufferDuration > 0) {
//							//two times the buffer duration seems to work best with vod
//							if (!isLive) {
//								tardiness -= clientBufferDuration * 2;
//							} else {
//								tardiness -= clientBufferDuration;
//							}
//						}
////						log.debug("Client buffer duration: {}", clientBufferDuration);
//					}
				} else {
					log.debug("Connection is null");
				}

				//TODO: how should we differ handling based on live or vod?

				//TODO: if we are VOD do we "pause" the provider when we are consistently late?

//				log.debug("Packet timestamp: {}; tardiness: {}; now: {}; message clock time: {}, dropLiveFuture: {}", new Object[] { timestamp, tardiness, now, clockTimeOfMessage,
//						dropLiveFuture });

				//anything coming in less than the base will be allowed to pass, it will not be
				//dropped or manipulated
				if (tardiness < baseTolerance) {
					//frame is below lowest bounds, let it go

				} else if (tardiness > highestTolerance) {
					//frame is really late, drop it no matter what type
					log.debug("Dropping late message: {}", message);
					//if we're working with video, indicate that we will need a key frame to proceed
					if (isVideo) {
						mapping.setKeyFrameNeeded(true);
					}
					//drop it
					drop = true;
				} else {
					if (isVideo) {
						VideoData video = (VideoData) message;
						if (video.getFrameType() == FrameType.KEYFRAME) {
							//if its a key frame the inter and disposible checks can be skipped
//							log.debug("Resuming stream with key frame; message: {}", message);
							mapping.setKeyFrameNeeded(false);
						} else if (tardiness >= baseTolerance && tardiness < midTolerance) {
							//drop disposable frames
							if (video.getFrameType() == FrameType.DISPOSABLE_INTERFRAME) {
								log.debug("Dropping disposible frame; message: {}", message);
								drop = true;
							}
						} else if (tardiness >= midTolerance && tardiness <= highestTolerance) {
							//drop inter-frames and disposable frames
							log.debug("Dropping disposible or inter frame; message: {}", message);
							drop = true;
						}
					}
				}
			}
			log.debug("Drop data: {}", drop);
		}
		return drop;
	}

	/**
	 * Determine type of header to use.
	 * 
	 * @param rtmp        The protocol state
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @return            Header type to use.
	 */
	private byte getHeaderType(final RTMP rtmp, final Header header, final Header lastHeader) {
		if (lastHeader == null) {
			return HEADER_NEW;
		}
		final Integer lastFullTs = rtmp.getLastFullTimestampWritten(header.getChannelId());
		if (lastFullTs == null) {
			return HEADER_NEW;
		}
		final byte headerType;
		final long diff = RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
		final long timeSinceFullTs = RTMPUtils.diffTimestamps(header.getTimer(), lastFullTs);
		if (header.getStreamId() != lastHeader.getStreamId() || diff < 0 || timeSinceFullTs >= 250) {
			// New header mark if header for another stream
			headerType = HEADER_NEW;
		} else if (header.getSize() != lastHeader.getSize() || header.getDataType() != lastHeader.getDataType()) {
			// Same source header if last header data type or size differ
			headerType = HEADER_SAME_SOURCE;
		} else if (header.getTimer() != lastHeader.getTimer() + lastHeader.getTimerDelta()) {
			// Timer change marker if there's time gap between header time stamps
			headerType = HEADER_TIMER_CHANGE;
		} else {
			// Continue encoding
			headerType = HEADER_CONTINUE;
		}
		return headerType;
	}

	/**
	 * Calculate number of bytes necessary to encode the header.
	 * 
	 * @param rtmp        The protocol state
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @return            Calculated size
	 */
	private int calculateHeaderSize(final RTMP rtmp, final Header header, final Header lastHeader) {
		final byte headerType = getHeaderType(rtmp, header, lastHeader);
		int channelIdAdd = 0;
		int channelId = header.getChannelId();
		if (channelId > 320) {
			channelIdAdd = 2;
		} else if (channelId > 63) {
			channelIdAdd = 1;
		}
		return RTMPUtils.getHeaderLength(headerType) + channelIdAdd;
	}

	/**
	 * Encode RTMP header.
	 * @param rtmp        The protocol state
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @return            Encoded header data
	 */
	public IoBuffer encodeHeader(final RTMP rtmp, final Header header, final Header lastHeader) {
		final IoBuffer result = IoBuffer.allocate(calculateHeaderSize(rtmp, header, lastHeader));
		encodeHeader(rtmp, header, lastHeader, result);
		return result;
	}

	/**
	 * Encode RTMP header into given IoBuffer.
	 *
	 * @param rtmp        The protocol state
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @param buf         Buffer to write encoded header to
	 */
	public void encodeHeader(final RTMP rtmp, final Header header, final Header lastHeader, final IoBuffer buf) {
		final byte headerType = getHeaderType(rtmp, header, lastHeader);
		RTMPUtils.encodeHeaderByte(buf, headerType, header.getChannelId());

		final int timer;
		switch (headerType) {
			case HEADER_NEW:
				timer = header.getTimer();
				if (timer < 0 || timer >= 0xffffff) {
					RTMPUtils.writeMediumInt(buf, 0xffffff);
				} else {
					RTMPUtils.writeMediumInt(buf, timer);
				}
				RTMPUtils.writeMediumInt(buf, header.getSize());
				buf.put(header.getDataType());
				RTMPUtils.writeReverseInt(buf, header.getStreamId());
				if (timer < 0 || timer >= 0xffffff) {
					buf.putInt(timer);
				}
				header.setTimerBase(timer);
				header.setTimerDelta(0);
				rtmp.setLastFullTimestampWritten(header.getChannelId(), timer);
				break;
			case HEADER_SAME_SOURCE:
				timer = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
				if (timer < 0 || timer >= 0xffffff) {
					RTMPUtils.writeMediumInt(buf, 0xffffff);
				} else {
					RTMPUtils.writeMediumInt(buf, timer);
				}
				RTMPUtils.writeMediumInt(buf, header.getSize());
				buf.put(header.getDataType());
				if (timer < 0 || timer >= 0xffffff) {
					buf.putInt(timer);
				}
				header.setTimerBase(header.getTimer() - timer);
				header.setTimerDelta(timer);
				break;
			case HEADER_TIMER_CHANGE:
				timer = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
				if (timer < 0 || timer >= 0xffffff) {
					RTMPUtils.writeMediumInt(buf, 0xffffff);
					buf.putInt(timer);
				} else {
					RTMPUtils.writeMediumInt(buf, timer);
				}
				header.setTimerBase(header.getTimer() - timer);
				header.setTimerDelta(timer);
				break;
			case HEADER_CONTINUE:
				timer = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
				header.setTimerBase(header.getTimer() - timer);
				header.setTimerDelta(timer);
				break;
			default:
				break;
		}
//		log.trace("CHUNK, E, {}, {}", header, headerType);
	}

	/**
	 * Encode message.
	 *
	 * @param rtmp        RTMP protocol state
	 * @param header      RTMP message header
	 * @param message     RTMP message (event)
	 * @return            Encoded message data
	 */
	public IoBuffer encodeMessage(RTMP rtmp, Header header, IRTMPEvent message) {
		IServiceCall call = null;
		switch (header.getDataType()) {
			case TYPE_CHUNK_SIZE:
				return encodeChunkSize((ChunkSize) message);
			case TYPE_INVOKE:
//				log.trace("Invoke {}", message);
				call = ((Invoke) message).getCall();
				if (call != null) {
//					log.debug("{}", call.toString());
					Object[] args = call.getArguments();
					if (args != null && args.length > 0) {
						Object a0 = args[0];
						if (a0 instanceof Status) {
							Status status = (Status) a0;
							//code: NetStream.Seek.Notify
							if (StatusCodes.NS_SEEK_NOTIFY.equals(status.getCode())) {
								//desc: Seeking 25000 (stream ID: 1).
								int seekTime = Integer.valueOf(status.getDescription().split(" ")[1]);
//								log.trace("Seek to time: {}", seekTime);
								//audio and video channels
								int[] channels = new int[] { 5, 6 };
								//if its a seek notification, reset the "mapping" for audio (5) and video (6)
								for (int channelId : channels) {
									LiveTimestampMapping mapping = rtmp.getLastTimestampMapping(channelId);
									if (mapping != null) {
										long timestamp = mapping.getClockStartTime() + (seekTime & 0xFFFFFFFFL);
//										log.trace("Setting last stream time to: {}", timestamp);
										mapping.setLastStreamTime(timestamp);
									} else {
										log.debug("No ts mapping for channel id: {}", channelId);
									}
								}
							}
						}
					}
				}
				return encodeInvoke((Invoke) message, rtmp);
			case TYPE_NOTIFY:
//				log.trace("Notify {}", message);
				call = ((Notify) message).getCall();
				if (call == null) {
					return encodeStreamMetadata((Notify) message);
				} else {				
					return encodeNotify((Notify) message, rtmp);
				}
			case TYPE_PING:
				return encodePing((Ping) message);
			case TYPE_BYTES_READ:
				return encodeBytesRead((BytesRead) message);
			case TYPE_AGGREGATE:
//				log.trace("Encode aggregate message");
				return encodeAggregate((Aggregate) message);
			case TYPE_AUDIO_DATA:
//				log.trace("Encode audio message");

				return encodeAudioData((AudioData) message);
			case TYPE_VIDEO_DATA:
//				log.trace("Encode video message");

				return encodeVideoData((VideoData) message);
			case TYPE_FLEX_SHARED_OBJECT:
				return encodeFlexSharedObject((ISharedObjectMessage) message, rtmp);
			case TYPE_SHARED_OBJECT:
				return encodeSharedObject((ISharedObjectMessage) message, rtmp);
			case TYPE_SERVER_BANDWIDTH:
				return encodeServerBW((ServerBW) message);
			case TYPE_CLIENT_BANDWIDTH:
				return encodeClientBW((ClientBW) message);
			case TYPE_FLEX_MESSAGE:
				return encodeFlexMessage((FlexMessage) message, rtmp);
			case TYPE_FLEX_STREAM_SEND:
				return encodeFlexStreamSend((FlexStreamSend) message);
			default:
				log.warn("Unknown object type: {}", header.getDataType());
		}
		return null;
	}

	/**
	 * Encode server-side bandwidth event.
	 *
	 * @param serverBW    Server-side bandwidth event
	 * @return            Encoded event data
	 */
	private IoBuffer encodeServerBW(ServerBW serverBW) {
		final IoBuffer out = IoBuffer.allocate(4);
		out.putInt(serverBW.getBandwidth());
		return out;
	}

	/**
	 * Encode client-side bandwidth event.
	 *
	 * @param clientBW    Client-side bandwidth event
	 * @return            Encoded event data
	 */
	private IoBuffer encodeClientBW(ClientBW clientBW) {
		final IoBuffer out = IoBuffer.allocate(5);
		out.putInt(clientBW.getBandwidth());
		out.put(clientBW.getValue2());
		return out;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeChunkSize(ChunkSize chunkSize) {
		final IoBuffer out = IoBuffer.allocate(4);
		out.putInt(chunkSize.getSize());
		return out;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeFlexSharedObject(ISharedObjectMessage so, RTMP rtmp) {
		final IoBuffer out = IoBuffer.allocate(128);
		out.setAutoExpand(true);
		// TODO: also support sending of AMF3 encoded data
		out.put((byte) 0x00);
		doEncodeSharedObject(so, rtmp, out);
		return out;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeSharedObject(ISharedObjectMessage so, RTMP rtmp) {
		final IoBuffer out = IoBuffer.allocate(128);
		out.setAutoExpand(true);
		doEncodeSharedObject(so, rtmp, out);
		return out;
	}

	/**
	 * Perform the actual encoding of the shared object contents.
	 *
	 * @param so shared object
	 * @param rtmp rtmp
	 * @param out output buffer
	 */
	public void doEncodeSharedObject(ISharedObjectMessage so, RTMP rtmp, IoBuffer out) {
		final Output output = new org.red5.io.amf.Output(out);

		output.putString(so.getName());
		// SO version
		out.putInt(so.getVersion());
		// Encoding (this always seems to be 2 for persistent shared objects)
		out.putInt(so.isPersistent() ? 2 : 0);
		// unknown field
		out.putInt(0);

		int mark, len;

		for (ISharedObjectEvent event : so.getEvents()) {
			byte type = SharedObjectTypeMapping.toByte(event.getType());

			switch (event.getType()) {
				case SERVER_CONNECT:
				case CLIENT_INITIAL_DATA:
				case CLIENT_CLEAR_DATA:
					out.put(type);
					out.putInt(0);
					break;

				case SERVER_DELETE_ATTRIBUTE:
				case CLIENT_DELETE_DATA:
				case CLIENT_UPDATE_ATTRIBUTE:
					out.put(type);
					mark = out.position();
					out.skip(4); // we will be back
					output.putString(event.getKey());
					len = out.position() - mark - 4;
					out.putInt(mark, len);
					break;

				case SERVER_SET_ATTRIBUTE:
				case CLIENT_UPDATE_DATA:
					if (event.getKey() == null) {
						// Update multiple attributes in one request
						Map<?, ?> initialData = (Map<?, ?>) event.getValue();
						for (Object o : initialData.keySet()) {

							out.put(type);
							mark = out.position();
							out.skip(4); // we will be back

							String key = (String) o;
							output.putString(key);
							serializer.serialize(output, initialData.get(key));

							len = out.position() - mark - 4;
							out.putInt(mark, len);
						}
					} else {
						out.put(type);
						mark = out.position();
						out.skip(4); // we will be back

						output.putString(event.getKey());
						serializer.serialize(output, event.getValue());

						len = out.position() - mark - 4;
						out.putInt(mark, len);
					}
					break;

				case CLIENT_SEND_MESSAGE:
				case SERVER_SEND_MESSAGE:
					// Send method name and value
					out.put(type);
					mark = out.position();
					out.skip(4);
					// Serialize name of the handler to call...
					serializer.serialize(output, event.getKey());
					// ...and the arguments
					for (Object arg : (List<?>) event.getValue()) {
						serializer.serialize(output, arg);
					}
					len = out.position() - mark - 4;
					//log.debug(len);
					out.putInt(mark, len);
					//log.info(out.getHexDump());
					break;

				case CLIENT_STATUS:
					out.put(type);
					final String status = event.getKey();
					final String message = (String) event.getValue();
					out.putInt(message.length() + status.length() + 4);
					output.putString(message);
					output.putString(status);
					break;

				default:
					//log.error("Unknown event " + event.getType());
					// XXX: come back here, need to make this work in server or client mode
					// talk to joachim about this part.
					out.put(type);
					mark = out.position();
					//out.putInt(0);
					out.skip(4); // we will be back
					output.putString(event.getKey());
					serializer.serialize(output, event.getValue());
					len = out.position() - mark - 4;
					out.putInt(mark, len);
					break;

			}
		}
	}

	/** {@inheritDoc} */
	public IoBuffer encodeNotify(Notify notify, RTMP rtmp) {
		return encodeNotifyOrInvoke(notify, rtmp);
	}

	/** {@inheritDoc} */
	public IoBuffer encodeInvoke(Invoke invoke, RTMP rtmp) {
		return encodeNotifyOrInvoke(invoke, rtmp);
	}

	/**
	 * Encode notification event.
	 *
	 * @param invoke            Notification event
	 * @return                  Encoded event data
	 */
	protected IoBuffer encodeNotifyOrInvoke(Notify invoke, RTMP rtmp) {
		IoBuffer out = IoBuffer.allocate(1024);
		out.setAutoExpand(true);
		encodeNotifyOrInvoke(out, invoke, rtmp);
		return out;
	}

	/**
	 * Encode notification event and fill given byte buffer.
	 *
	 * @param out               Byte buffer to fill
	 * @param invoke            Notification event
	 */
	protected void encodeNotifyOrInvoke(IoBuffer out, Notify invoke, RTMP rtmp) {
		// TODO: tidy up here
		// log.debug("Encode invoke");
		Output output = new org.red5.io.amf.Output(out);
		final IServiceCall call = invoke.getCall();
		final boolean isPending = (call.getStatus() == Call.STATUS_PENDING);
//		log.debug("Call: {} pending: {}", call, isPending);
		if (!isPending) {
			log.debug("Call has been executed, send result");
			serializer.serialize(output, call.isSuccess() ? "_result" : "_error"); // seems right
		} else {
			log.debug("This is a pending call, send request");
			// for request we need to use AMF3 for client mode
			// if the connection is AMF3
			if (rtmp.getEncoding() == Encoding.AMF3 && rtmp.getMode() == RTMP.MODE_CLIENT) {
				output = new org.red5.io.amf3.Output(out);
			}
			final String action = (call.getServiceName() == null) ? call.getServiceMethodName() : call.getServiceName() + '.' + call.getServiceMethodName();
			serializer.serialize(output, action); // seems right
		}
		if (invoke instanceof Invoke) {
			serializer.serialize(output, Integer.valueOf(invoke.getInvokeId()));
			serializer.serialize(output, invoke.getConnectionParams());
		}

		if (call.getServiceName() == null && "connect".equals(call.getServiceMethodName())) {
			// Response to initial connect, always use AMF0
			output = new org.red5.io.amf.Output(out);
		} else {
			if (rtmp.getEncoding() == Encoding.AMF3) {
				output = new org.red5.io.amf3.Output(out);
			} else {
				output = new org.red5.io.amf.Output(out);
			}
		}

		if (!isPending && (invoke instanceof Invoke)) {
			IPendingServiceCall pendingCall = (IPendingServiceCall) call;
			if (!call.isSuccess()) {
				log.debug("Call was not successful");
				StatusObject status = generateErrorResult(StatusCodes.NC_CALL_FAILED, call.getException());
				pendingCall.setResult(status);
			}
			Object res = pendingCall.getResult();
//			log.debug("Writing result: {}", res);
			serializer.serialize(output, res);
		} else {
//			log.debug("Writing params");
			final Object[] args = call.getArguments();
			if (args != null) {
				for (Object element : args) {
					serializer.serialize(output, element);
				}
			}
		}

		if (invoke.getData() != null) {
			out.setAutoExpand(true);
			out.put(invoke.getData());
		}

	}

	/** {@inheritDoc} */
	public IoBuffer encodePing(Ping ping) {
		int len = 6;
		if (ping.getValue3() != Ping.UNDEFINED) {
			len += 4;
		}
		if (ping.getValue4() != Ping.UNDEFINED) {
			len += 4;
		}
		final IoBuffer out = IoBuffer.allocate(len);
		out.putShort(ping.getEventType());
		out.putInt(ping.getValue2());
		if (ping.getValue3() != Ping.UNDEFINED) {
			out.putInt(ping.getValue3());
		}
		if (ping.getValue4() != Ping.UNDEFINED) {
			out.putInt(ping.getValue4());
		}
		return out;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeBytesRead(BytesRead bytesRead) {
		final IoBuffer out = IoBuffer.allocate(4);
		out.putInt(bytesRead.getBytesRead());
		return out;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeAggregate(Aggregate aggregate) {
		final IoBuffer result = aggregate.getData();
		return result;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeAudioData(AudioData audioData) {
		final IoBuffer result = audioData.getData();
		return result;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeVideoData(VideoData videoData) {
		final IoBuffer result = videoData.getData();
		return result;
	}

	/** {@inheritDoc} */
	public IoBuffer encodeUnknown(Unknown unknown) {
		final IoBuffer result = unknown.getData();
		return result;
	}

	public IoBuffer encodeStreamMetadata(Notify metaData) {
		final IoBuffer result = metaData.getData();
		return result;
	}

	/**
	 * Generate error object to return for given exception.
	 * 
	 * @param code call
	 * @param error error
	 * @return status object
	 */
	protected StatusObject generateErrorResult(String code, Throwable error) {
		// Construct error object to return
		String message = "";
		while (error != null && error.getCause() != null) {
			error = error.getCause();
		}
		if (error != null && error.getMessage() != null) {
			message = error.getMessage();
		}
		StatusObject status = new StatusObject(code, "error", message);
//		if (error instanceof ClientDetailsException) {
//			// Return exception details to client
//			status.setApplication(((ClientDetailsException) error).getParameters());
//			if (((ClientDetailsException) error).includeStacktrace()) {
//				List<String> stack = new ArrayList<String>();
//				for (StackTraceElement element: error.getStackTrace()) {
//					stack.add(element.toString());
//				}
//				status.setAdditional("stacktrace", stack);
//			}
//		} else 
		if (error != null) {
			status.setApplication(error.getClass().getCanonicalName());
		}
		return status;
	}	

	/**
	 * Encodes Flex message event.
	 *
	 * @param msg                Flex message event
	 * @param rtmp RTMP
	 * @return                   Encoded data
	 */
	public IoBuffer encodeFlexMessage(FlexMessage msg, RTMP rtmp) {
		IoBuffer out = IoBuffer.allocate(1024);
		out.setAutoExpand(true);
		// Unknown byte, always 0?
		out.put((byte) 0);
		encodeNotifyOrInvoke(out, msg, rtmp);
		return out;
	}

	public IoBuffer encodeFlexStreamSend(FlexStreamSend msg) {
		final IoBuffer result = msg.getData();
		return result;
	}

	private void updateTolerance() {
		midTolerance = baseTolerance + (long) (baseTolerance * 0.3);
		highestTolerance = baseTolerance + (long) (baseTolerance * 0.6);
	}

	/**
	 * Setter for serializer.
	 *
	 * @param serializer Serializer
	 */
	public void setSerializer(org.red5.io.object.Serializer serializer) {
		this.serializer = serializer;
	}	
	
	public void setBaseTolerance(long baseTolerance) {
		this.baseTolerance = baseTolerance;
		//update high and low tolerance
		updateTolerance();
	}
	
	/**
	 *   Setter for dropLiveFuture
	 * */
	public void setDropLiveFuture(boolean dropLiveFuture) {
		this.dropLiveFuture = dropLiveFuture;
	}

	public long getBaseTolerance() {
		return baseTolerance;
	}

}
