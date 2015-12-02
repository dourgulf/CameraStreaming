package org.red5.server.net.rtmp;

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

//import static org.red5.server.api.ScopeUtils.getScopeService;

//import java.beans.ConstructorProperties;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.BaseConnection;
import org.red5.server.IScheduledJob;
import org.red5.server.ISchedulingService;
import org.red5.server.Red5;
//import org.red5.server.api.IScope;
//import org.red5.server.api.stream.IClientBroadcastStream;
//import org.red5.server.api.stream.IClientStream;
//import org.red5.server.api.stream.IPlaylistSubscriberStream;
//import org.red5.server.api.stream.ISingleItemSubscriberStream;
//import org.red5.server.api.stream.IStreamCapableConnection;
//import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.service.Call;
import org.red5.server.service.IPendingServiceCall;
import org.red5.server.service.IPendingServiceCallback;
import org.red5.server.service.IServiceCall;
import org.red5.server.service.IServiceCapableConnection;
import org.red5.server.service.PendingCall;
//import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IClientBroadcastStream;
import org.red5.server.stream.IClientStream;
import org.red5.server.stream.IStreamCapableConnection;
//import org.red5.server.stream.IStreamService;
import org.red5.server.stream.OutputStream;
//import org.red5.server.stream.PlaylistSubscriberStream;
//import org.red5.server.stream.SingleItemSubscriberStream;
//import org.red5.server.stream.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP connection. Stores information about client streams, data transfer
 * channels, pending RPC calls, bandwidth configuration, used encoding
 * (AMF0/AMF3), connection state (is alive, last ping time and ping result) and
 * session.
 */
public abstract class RTMPConnection extends BaseConnection implements IStreamCapableConnection,IServiceCapableConnection {

	private static Logger log = LoggerFactory.getLogger(RTMPConnection.class);

	public static final String RTMP_CONNECTION_KEY = "rtmp.conn";

	public static final String RTMP_HANDSHAKE = "rtmp.handshake";

	/**
	 * Marker byte for standard or non-encrypted RTMP data.
	 */
	public static final byte RTMP_NON_ENCRYPTED = (byte) 0x03;

	/**
	 * Marker byte for encrypted RTMP data.
	 */
	public static final byte RTMP_ENCRYPTED = (byte) 0x06;

	/**
	 * Cipher for RTMPE input
	 */
	public static final String RTMPE_CIPHER_IN = "rtmpe.cipher.in";

	/**
	 * Cipher for RTMPE output
	 */
	public static final String RTMPE_CIPHER_OUT = "rtmpe.cipher.out";

	/**
	 * Connection channels
	 * 
	 * @see org.red5.server.net.rtmp.Channel
	 */
	private ConcurrentMap<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();

	/**
	 * Client streams
	 * 
	 * @see org.red5.server.stream.IClientStream
	 */
	private ConcurrentMap<Integer, IClientStream> streams = new ConcurrentHashMap<Integer, IClientStream>();

	private final BitSet reservedStreams = new BitSet();

	/**
	 * Identifier for remote calls.
	 */
	private AtomicInteger invokeId = new AtomicInteger(1);

	/**
	 * Hash map that stores pending calls and ids as pairs.
	 */
	private ConcurrentMap<Integer, IPendingServiceCall> pendingCalls = new ConcurrentHashMap<Integer, IPendingServiceCall>();

	/**
	 * Deferred results set.
	 * 
	 * @see org.red5.server.net.rtmp.DeferredResult
	 */
	private final HashSet<DeferredResult> deferredResults = new HashSet<DeferredResult>();

	/**
	 * Last ping round trip time
	 */
	private AtomicInteger lastPingTime = new AtomicInteger(-1);

	/**
	 * Timestamp when last ping command was sent.
	 */
	private AtomicLong lastPingSent = new AtomicLong(0);

	/**
	 * Timestamp when last ping result was received.
	 */
	private AtomicLong lastPongReceived = new AtomicLong(0);

	/**
	 * Name of quartz job that keeps connection alive.
	 */
	private String keepAliveJobName;

	/**
	 * Ping interval in ms to detect dead clients.
	 */
	private volatile int pingInterval = 5000;

	/**
	 * Maximum time in ms after which a client is disconnected because of inactivity.
	 */
	private volatile int maxInactivity = 60000;

	/**
	 * Data read interval
	 */
	protected int bytesReadInterval = 120 * 1024;

	/**
	 * Number of bytes to read next.
	 */
	protected int nextBytesRead = 120 * 1024;

	/**
	 * Number of bytes the client reported to have received.
	 */
	private long clientBytesRead = 0;

	/**
	 * Map for pending video packets and stream IDs.
	 */
	private ConcurrentMap<Integer, AtomicInteger> pendingVideos = new ConcurrentHashMap<Integer, AtomicInteger>();

	/**
	 * Number of streams used.
	 */
	private AtomicInteger usedStreams = new AtomicInteger(0);

	/**
	 * AMF version, AMF0 by default.
	 */
	private volatile Encoding encoding = Encoding.AMF0;

	/**
	 * Remembered stream buffer durations.
	 */
	private ConcurrentMap<Integer, Integer> streamBuffers = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * Name of job that is waiting for a valid handshake.
	 */
	private String waitForHandshakeJob;

	/**
	 * Maximum time in milliseconds to wait for a valid handshake.
	 */
	private volatile int maxHandshakeTimeout = 5000;

	protected volatile int clientId;

	/**
	 * protocol state
	 */
	protected volatile RTMP state;

	private ISchedulingService schedulingService;

	/**
	 * Creates anonymous RTMP connection without scope.
	 * 
	 * @param type Connection type
	 */
	//@ConstructorProperties({ "type" })
	public RTMPConnection(String type) {
		// We start with an anonymous connection without a scope.
		// These parameters will be set during the call of "connect" later.
		super(type);
	}

	public int getId() {
		return clientId;
	}

	public void setId(int clientId) {
		this.clientId = clientId;
	}

	public RTMP getState() {
		return state;
	}

	public byte getStateCode() {
		return state.getState();
	}

	public void setStateCode(byte code) {
		state.setState(code);
	}

	public void setState(RTMP state) {
		log.debug("Set state: {}", state);
		this.state = state;
	}

	@Override
	public boolean connect(Object[] params) {
//		log.debug("Connect scope: {}", newScope);
		try {
			boolean success = super.connect( params);
			if (success) {
				unscheduleWaitForHandshakeJob();
			}
			return success;
		} catch (Exception e) {
			log.warn("Client rejected, unscheduling waitForHandshakeJob", e);
			unscheduleWaitForHandshakeJob();
//			throw e;
		}
		return false;
	}

	private void unscheduleWaitForHandshakeJob() {
		getWriteLock().lock();
		try {
			if (waitForHandshakeJob != null) {
				schedulingService.removeScheduledJob(waitForHandshakeJob);
				waitForHandshakeJob = null;
				log.debug("Removed waitForHandshakeJob for: {}", getId());
			}
		} finally {
			getWriteLock().unlock();
		}
	}

	/**
	 * Initialize connection.
	 * 
	 * @param host Connection host
	 * @param path Connection path
	 * @param sessionId Connection session id
	 * @param params Params passed from client
	 */
	public void setup(String host, String path, String sessionId, Map<String, Object> params) {
		this.host = host;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
		if (params.get("objectEncoding") == Integer.valueOf(3)) {
			log.info("Setting object encoding to AMF3");
			encoding = Encoding.AMF3;
		}
	}

	/**
	 * Return AMF protocol encoding used by this connection.
	 * 
	 * @return AMF encoding used by connection
	 */
	public Encoding getEncoding() {
		return encoding;
	}

	/**
	 * Getter for next available channel id.
	 * 
	 * @return Next available channel id
	 */
	public int getNextAvailableChannelId() {
		int result = 4;
		while (isChannelUsed(result)) {
			result++;
		}
		return result;
	}

	/**
	 * Checks whether channel is used.
	 * 
	 * @param channelId Channel id
	 * @return <code>true</code> if channel is in use, <code>false</code>
	 *         otherwise
	 */
	public boolean isChannelUsed(int channelId) {
		return channels.get(channelId) != null;
	}

	/**
	 * Return channel by id.
	 * 
	 * @param channelId Channel id
	 * @return Channel by id
	 */
	public Channel getChannel(int channelId) {
		final Channel value = new Channel(this, channelId);
		Channel result = channels.putIfAbsent(channelId, value);
		if (result == null) {
			result = value;
		}
		return result;
	}

	/**
	 * Closes channel.
	 * 
	 * @param channelId Channel id
	 */
	public void closeChannel(int channelId) {
		channels.remove(channelId);
	}

	/**
	 * Getter for client streams.
	 * 
	 * @return Client streams as array
	 */
	protected Collection<IClientStream> getStreams() {
		return streams.values();
	}

	/** {@inheritDoc} */
	public int reserveStreamId() {
		int result = -1;
		getWriteLock().lock();
		try {
			for (int i = 0; true; i++) {
				if (!reservedStreams.get(i)) {
					reservedStreams.set(i);
					result = i;
					break;
				}
			}
		} finally {
			getWriteLock().unlock();
		}
		return result + 1;
	}

	/**
	 * Creates output stream object from stream id. Output stream consists of
	 * audio, data and video channels.
	 * 
	 * @see org.red5.server.stream.OutputStream
	 * 
	 * @param streamId Stream id
	 * @return Output stream object
	 */
	public OutputStream createOutputStream(int streamId) {
		int channelId = (4 + ((streamId - 1) * 5));
		final Channel data = getChannel(channelId++);
		final Channel video = getChannel(channelId++);
		final Channel audio = getChannel(channelId++);
		// final Channel unknown = getChannel(channelId++);
		// final Channel ctrl = getChannel(channelId++);
		return new OutputStream(video, audio, data);
	}

	/** {@inheritDoc} */
	public IClientBroadcastStream newBroadcastStream(int streamId) {
//		getReadLock().lock();
//		try {
//			int index = streamId - 1;
//			if (index < 0 || !reservedStreams.get(index)) {
//				// StreamId has not been reserved before
//				return null;
//			}
//		} finally {
//			getReadLock().unlock();
//		}
//
//		if (streams.get(streamId - 1) != null) {
//			// Another stream already exists with this id
//			return null;
//		}
//		/**
//		 * Picking up the ClientBroadcastStream defined as a spring
//		 * prototype in red5-common.xml
//		 */
//		ClientBroadcastStream cbs = new clientBroadcastStream();
//		Integer buffer = streamBuffers.get(streamId - 1);
//		if (buffer != null) {
//			cbs.setClientBufferDuration(buffer);
//		}
//		cbs.setStreamId(streamId);
//		cbs.setConnection(this);
//		cbs.setName(createStreamName());
////		cbs.setScope(this.getScope());
//
//		registerStream(cbs);
//		usedStreams.incrementAndGet();
//		return cbs;
		return null;
	}

	/** {@inheritDoc} */
//	public ISingleItemSubscriberStream newSingleItemSubscriberStream(int streamId) {
//		getReadLock().lock();
//		try {
//			int index = streamId - 1;
//			if (index < 0 || !reservedStreams.get(streamId - 1)) {
//				// StreamId has not been reserved before
//				return null;
//			}
//		} finally {
//			getReadLock().unlock();
//		}
//
//		if (streams.get(streamId - 1) != null) {
//			// Another stream already exists with this id
//			return null;
//		}
//		/**
//		 * Picking up the SingleItemSubscriberStream defined as a Spring
//		 * prototype in red5-common.xml
//		 */
////		SingleItemSubscriberStream siss = new SingleItemSubscriberStream();
////		Integer buffer = streamBuffers.get(streamId - 1);
////		if (buffer != null) {
////			siss.setClientBufferDuration(buffer);
////		}
////		siss.setName(createStreamName());
////		siss.setConnection(this);
////		siss.setScope(this.getScope());
////		siss.setStreamId(streamId);
////		registerStream(siss);
////		usedStreams.incrementAndGet();
////		return siss;
//		return null;
//	}

	/** {@inheritDoc} */
//	public IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId) {
//		getReadLock().lock();
//		try {
//			int index = streamId - 1;
//			if (index < 0 || !reservedStreams.get(streamId - 1)) {
//				// StreamId has not been reserved before
//				return null;
//			}
//		} finally {
//			getReadLock().unlock();
//		}
//
//		if (streams.get(streamId - 1) != null) {
//			// Another stream already exists with this id
//			return null;
//		}
//		/**
//		 * Picking up the PlaylistSubscriberStream defined as a Spring
//		 * prototype in red5-common.xml
//		 */
//		PlaylistSubscriberStream pss = new PlaylistSubscriberStream() ;
//		Integer buffer = streamBuffers.get(streamId - 1);
//		if (buffer != null) {
//			pss.setClientBufferDuration(buffer);
//		}
//		pss.setName(createStreamName());
//		pss.setConnection(this);
//		pss.setScope(this.getScope());
//		pss.setStreamId(streamId);
//		registerStream(pss);
//		usedStreams.incrementAndGet();
//		return pss;
//		return null;
//	}
//
	public void addClientStream(IClientStream stream) {
		int streamId = stream.getStreamId();
		getWriteLock().lock();
		try {
			if (reservedStreams.get(streamId - 1)) {
				return;
			}
			reservedStreams.set(streamId - 1);
		} finally {
			getWriteLock().unlock();
		}
		streams.put(streamId - 1, stream);
		usedStreams.incrementAndGet();
	}

	public void removeClientStream(int streamId) {
		unreserveStreamId(streamId);
	}

	/**
	 * Getter for used stream count.
	 * 
	 * @return Value for property 'usedStreamCount'.
	 */
	protected int getUsedStreamCount() {
		return usedStreams.get();
	}

	/** {@inheritDoc} */
	public IClientStream getStreamById(int id) {
		if (id <= 0) {
			return null;
		}
		return streams.get(id - 1);
	}

	/**
	 * Return stream id for given channel id.
	 * 
	 * @param channelId Channel id
	 * @return ID of stream that channel belongs to
	 */
	public int getStreamIdForChannel(int channelId) {
		if (channelId < 4) {
			return 0;
		}
		return ((channelId - 4) / 5) + 1;
	}

	/**
	 * Return stream by given channel id.
	 * 
	 * @param channelId Channel id
	 * @return Stream that channel belongs to
	 */
	public IClientStream getStreamByChannelId(int channelId) {
		if (channelId < 4) {
			return null;
		}
		return streams.get(getStreamIdForChannel(channelId) - 1);
	}

	/**
	 * Store a stream in the connection.
	 * 
	 * @param stream
	 */
	@SuppressWarnings("unused")
	private void registerStream(IClientStream stream) {
		streams.put(stream.getStreamId() - 1, stream);
	}

	/**
	 * Remove a stream from the connection.
	 * 
	 * @param stream
	 */
	@SuppressWarnings("unused")
	private void unregisterStream(IClientStream stream) {
		streams.remove(stream.getStreamId());
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		getWriteLock().lock();
		try {
			if (keepAliveJobName != null) {
				schedulingService.removeScheduledJob(keepAliveJobName);
				keepAliveJobName = null;
			}
		} finally {
			getWriteLock().unlock();
		}
		Red5.setConnectionLocal(this);
//		IStreamService streamService = (IStreamService) getScopeService(scope, IStreamService.class, StreamService.class);
//		if (streamService != null) {
//			for (Map.Entry<Integer, IClientStream> entry : streams.entrySet()) {
//				IClientStream stream = entry.getValue();
//				if (stream != null) {
//					log.debug("Closing stream: {}", stream.getStreamId());
//					streamService.deleteStream(this, stream.getStreamId());
//					usedStreams.decrementAndGet();
//				}
//			}
//			streams.clear();
//		}
		channels.clear();
		super.close();
	}

	/**
	 * When the connection has been closed, notify any remaining pending service calls that they have failed because
	 * the connection is broken. Implementors of IPendingServiceCallback may only deduce from this notification that
	 * it was not possible to read a result for this service call. It is possible that (1) the service call was never
	 * written to the service, or (2) the service call was written to the service and although the remote method was
	 * invoked, the connection failed before the result could be read, or (3) although the remote method was invoked
	 * on the service, the service implementor detected the failure of the connection and performed only partial
	 * processing. The caller only knows that it cannot be confirmed that the callee has invoked the service call
	 * and returned a result.
	 */
	public void sendPendingServiceCallsCloseError() {
		if (pendingCalls != null && !pendingCalls.isEmpty()) {
			for (IPendingServiceCall call : pendingCalls.values()) {
				call.setStatus(Call.STATUS_NOT_CONNECTED);
				for (IPendingServiceCallback callback : call.getCallbacks()) {
					callback.resultReceived(call);
				}
			}
		}
	}

	/** {@inheritDoc} */
	public void unreserveStreamId(int streamId) {
		getWriteLock().lock();
		try {
			deleteStreamById(streamId);
			if (streamId > 0) {
				reservedStreams.clear(streamId - 1);
			}
		} finally {
			getWriteLock().unlock();
		}
	}

	/** {@inheritDoc} */
	public void deleteStreamById(int streamId) {
		if (streamId > 0) {
			if (streams.get(streamId - 1) != null) {
				pendingVideos.remove(streamId);
				usedStreams.decrementAndGet();
				streams.remove(streamId - 1);
				streamBuffers.remove(streamId - 1);
			}
		}
	}

	/**
	 * Handler for ping event.
	 * 
	 * @param ping Ping event context
	 */
	public void ping(Ping ping) {
		getChannel(2).write(ping);
	}

	/**
	 * Write raw byte buffer.
	 * 
	 * @param out IoBuffer
	 */
	public abstract void rawWrite(IoBuffer out);

	/**
	 * Write packet.
	 * 
	 * @param out Packet
	 */
	public abstract void write(Packet out);

	/**
	 * Update number of bytes to read next value.
	 */
	protected void updateBytesRead() {
		getWriteLock().lock();
		try {
			long bytesRead = getReadBytes();
			if (bytesRead >= nextBytesRead) {
				BytesRead sbr = new BytesRead((int) bytesRead);
				getChannel(2).write(sbr);
				// @todo: what do we want to see printed here?
				// log.info(sbr);
				nextBytesRead += bytesReadInterval;
			}
		} finally {
			getWriteLock().unlock();
		}
	}

	/**
	 * Read number of received bytes.
	 * 
	 * @param bytes Number of bytes
	 */
	public void receivedBytesRead(int bytes) {
		getWriteLock().lock();
		try {
//			log.debug("Client received {} bytes, written {} bytes, {} messages pending", new Object[] { bytes, getWrittenBytes(), getPendingMessages() });
			clientBytesRead = bytes;
		} finally {
			getWriteLock().unlock();
		}
	}

	/**
	 * Get number of bytes the client reported to have received.
	 * 
	 * @return Number of bytes
	 */
	public long getClientBytesRead() {
		getReadLock().lock();
		try {
			return clientBytesRead;
		} finally {
			getReadLock().unlock();
		}
	}

	/** {@inheritDoc} */
	public void invoke(IServiceCall call) {
		invoke(call, 3);
	}

	/**
	 * Generate next invoke id.
	 * 
	 * @return Next invoke id for RPC
	 */
	public int getInvokeId() {
		return invokeId.incrementAndGet();
	}

	/**
	 * Register pending call (remote function call that is yet to finish).
	 * 
	 * @param invokeId Deferred operation id
	 * @param call Call service
	 */
	public void registerPendingCall(int invokeId, IPendingServiceCall call) {
		pendingCalls.put(invokeId, call);
	}

	/** {@inheritDoc} */
	public void invoke(IServiceCall call, int channel) {
		// We need to use Invoke for all calls to the client
		Invoke invoke = new Invoke();
		invoke.setCall(call);
		invoke.setInvokeId(getInvokeId());
		if (call instanceof IPendingServiceCall) {
			registerPendingCall(invoke.getInvokeId(), (IPendingServiceCall) call);
		}
		getChannel(channel).write(invoke);
	}

	/** {@inheritDoc} */
	public void invoke(String method) {
		invoke(method, null, null);
	}

	/** {@inheritDoc} */
	public void invoke(String method, Object[] params) {
		invoke(method, params, null);
	}

	/** {@inheritDoc} */
	public void invoke(String method, IPendingServiceCallback callback) {
		invoke(method, null, callback);
	}

	/** {@inheritDoc} */
	public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
		IPendingServiceCall call = new PendingCall(method, params);
		if (callback != null) {
			call.registerCallback(callback);
		}
		invoke(call);
	}

	/** {@inheritDoc} */
	public void notify(IServiceCall call) {
		notify(call, 3);
	}

	/** {@inheritDoc} */
	public void notify(IServiceCall call, int channel) {
		Notify notify = new Notify();
		notify.setCall(call);
		getChannel(channel).write(notify);
	}

	/** {@inheritDoc} */
	public void notify(String method) {
		notify(method, null);
	}

	/** {@inheritDoc} */
	public void notify(String method, Object[] params) {
		IServiceCall call = new Call(method, params);
		notify(call);
	}

	/** {@inheritDoc} */
	@Override
	public long getReadBytes() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public long getWrittenBytes() {
		return 0;
	}

	/**
	 * Get pending call service by id.
	 * 
	 * @param invokeId
	 *            Pending call service id
	 * @return Pending call service object
	 */
	protected IPendingServiceCall getPendingCall(int invokeId) {
		return pendingCalls.get(invokeId);
	}

	/**
	 * Retrieve pending call service by id. The call will be removed afterwards.
	 * 
	 * @param invokeId
	 *            Pending call service id
	 * @return Pending call service object
	 */
	protected IPendingServiceCall retrievePendingCall(int invokeId) {
		return pendingCalls.remove(invokeId);
	}

	/**
	 * Generates new stream name.
	 * 
	 * @return New stream name
	 */
	protected String createStreamName() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Mark message as being written.
	 * 
	 * @param message
	 *            Message to mark
	 */
	protected void writingMessage(Packet message) {
		if (message.getMessage() instanceof VideoData) {
			int streamId = message.getHeader().getStreamId();
			final AtomicInteger value = new AtomicInteger();
			AtomicInteger old = pendingVideos.putIfAbsent(streamId, value);
			if (old == null) {
				old = value;
			}
			old.incrementAndGet();
		}
	}

	/**
	 * Increases number of read messages by one. Updates number of bytes read.
	 */
	public void messageReceived() {
		readMessages.incrementAndGet();
		// Trigger generation of BytesRead messages
		updateBytesRead();
	}

	/**
	 * Mark message as sent.
	 * 
	 * @param message
	 *            Message to mark
	 */
	public void messageSent(Packet message) {
		if (message.getMessage() instanceof VideoData) {
			int streamId = message.getHeader().getStreamId();
			AtomicInteger pending = pendingVideos.get(streamId);
			if (pending != null) {
				pending.decrementAndGet();
			}
		}
		writtenMessages.incrementAndGet();
	}

	/**
	 * Increases number of dropped messages.
	 */
	protected void messageDropped() {
		droppedMessages.incrementAndGet();
	}

	/** {@inheritDoc} */
	@Override
	public long getPendingVideoMessages(int streamId) {
		AtomicInteger count = pendingVideos.get(streamId);
		long result = (count != null ? count.intValue() - getUsedStreamCount() : 0);
		return (result > 0 ? result : 0);
	}

	/** {@inheritDoc} */
	public void ping() {
		long newPingTime = System.currentTimeMillis();
		log.debug("Pinging client with id {} at {}, last ping sent at {}", new Object[] { getId(), newPingTime, lastPingSent.get() });
		if (lastPingSent.get() == 0) {
			lastPongReceived.set(newPingTime);
		}
		Ping pingRequest = new Ping();
		pingRequest.setEventType(Ping.PING_CLIENT);
		lastPingSent.set(newPingTime);
		int now = (int) (newPingTime & 0xffffffff);
		pingRequest.setValue2(now);
		ping(pingRequest);
	}

	/**
	 * Marks that ping back was received.
	 * 
	 * @param pong
	 *            Ping object
	 */
	public void pingReceived(Ping pong) {
		long now = System.currentTimeMillis();
		long previousReceived = (int) (lastPingSent.get() & 0xffffffff);
		log.debug("Pong from client id {} at {} with value {}, previous received at {}", new Object[] { getId(), now, pong.getValue2(), previousReceived });
		if (pong.getValue2() == previousReceived) {
			lastPingTime.set((int) (now & 0xffffffff) - pong.getValue2());
		}
		lastPongReceived.set(now);
	}

	/** {@inheritDoc} */
	public int getLastPingTime() {
		return lastPingTime.get();
	}

	/**
	 * Setter for ping interval.
	 * 
	 * @param pingInterval Interval in ms to ping clients. Set to <code>0</code> to
	 *            disable ghost detection code.
	 */
	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * Setter for maximum inactivity.
	 * 
	 * @param maxInactivity Maximum time in ms after which a client is disconnected in
	 *            case of inactivity.
	 */
	public void setMaxInactivity(int maxInactivity) {
		this.maxInactivity = maxInactivity;
	}

	/**
	 * Starts measurement.
	 */
	public void startRoundTripMeasurement() {
		if (pingInterval > 0 && keepAliveJobName == null) {
			keepAliveJobName = schedulingService.addScheduledJob(pingInterval, new KeepAliveJob());
			log.debug("Keep alive job name {} for client id {}", keepAliveJobName, getId());
		}
	}

	/**
	 * Sets the scheduling service.
	 * 
	 * @param schedulingService scheduling service
	 */
	public void setSchedulingService(ISchedulingService schedulingService) {
		this.schedulingService = schedulingService;
	}

	/**
	 * Inactive state event handler.
	 */
	protected abstract void onInactive();

	/** {@inheritDoc} */
	@Override
	public String toString() {
		Object[] args = new Object[] { getClass().getSimpleName(), getRemoteAddress(), getRemotePort(), getHost(), getReadBytes(), getWrittenBytes() };
		return String.format("%1$s from %2$s : %3$s to %4$s (in: %5$s out %6$s )", args);
	}

	/**
	 * Registers deferred result.
	 * 
	 * @param result Result to register
	 */
	protected void registerDeferredResult(DeferredResult result) {
		getWriteLock().lock();
		try {
			deferredResults.add(result);
		} finally {
			getWriteLock().unlock();
		}
	}

	/**
	 * Unregister deferred result
	 * 
	 * @param result
	 *            Result to unregister
	 */
	protected void unregisterDeferredResult(DeferredResult result) {
		getWriteLock().lock();
		try {
			deferredResults.remove(result);
		} finally {
			getWriteLock().unlock();
		}
	}

	protected void rememberStreamBufferDuration(int streamId, int bufferDuration) {
		streamBuffers.put(streamId - 1, bufferDuration);
	}

	/**
	 * Set maximum time to wait for valid handshake in milliseconds.
	 * 
	 * @param maxHandshakeTimeout Maximum time in milliseconds
	 */
	public void setMaxHandshakeTimeout(int maxHandshakeTimeout) {
		this.maxHandshakeTimeout = maxHandshakeTimeout;
	}

	/**
	 * Start waiting for a valid handshake.
	 * 
	 * @param service
	 *            The scheduling service to use
	 */
	protected void startWaitForHandshake(ISchedulingService service) {
		waitForHandshakeJob = service.addScheduledOnceJob(maxHandshakeTimeout, new WaitForHandshakeJob());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + clientId;
		if (host != null) {
			result = result + host.hashCode();
		}
		if (remoteAddress != null) {
			result = result + remoteAddress.hashCode();
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RTMPConnection other = (RTMPConnection) obj;
		if (clientId != other.clientId) {
			return false;
		}
		if (host != null && !host.equals(other.getHost())) {
			return false;
		}
		if (remoteAddress != null && !remoteAddress.equals(other.getRemoteAddress())) {
			return false;
		}
		return true;
	}

	/**
	 * Quartz job that keeps connection alive and disconnects if client is dead.
	 */
	private class KeepAliveJob implements IScheduledJob {

		private final AtomicLong lastBytesRead = new AtomicLong(0);

		private volatile long lastBytesReadTime = 0;

		/** {@inheritDoc} */
		public void execute(ISchedulingService service) {
			long thisRead = getReadBytes();
			long previousReadBytes = lastBytesRead.get();
			if (thisRead > previousReadBytes) {
				// Client sent data since last check and thus is not dead. No need to ping
				if (lastBytesRead.compareAndSet(previousReadBytes, thisRead)) {
					lastBytesReadTime = System.currentTimeMillis();
				}
				return;
			}
			// Client didn't send response to ping command and didn't sent data for too long, disconnect
			if (lastPongReceived.get() > 0 && (lastPingSent.get() - lastPongReceived.get() > maxInactivity) && !(System.currentTimeMillis() - lastBytesReadTime < maxInactivity)) {
				log.debug("Keep alive job name {}", keepAliveJobName);
				if (log.isDebugEnabled()) {
					log.debug("Scheduled job list");
					for (String jobName : service.getScheduledJobNames()) {
						log.debug("Job: {}", jobName);
					}
				}
				service.removeScheduledJob(keepAliveJobName);
				keepAliveJobName = null;
				log.warn("Closing {}, with id {}, due to too much inactivity ({}ms), last ping sent {}ms ago", new Object[] { RTMPConnection.this, getId(),
						(lastPingSent.get() - lastPongReceived.get()), (System.currentTimeMillis() - lastPingSent.get()) });
				// Add the following line to (hopefully) deal with a very common support request
				// on the Red5 list
				log.warn("This often happens if YOUR Red5 application generated an exception on start-up. Check earlier in the log for that exception first!");
				onInactive();
				return;
			}
			// Send ping command to client to trigger sending of data
			ping();
		}
	}

	/**
	 * Quartz job that waits for a valid handshake and disconnects the client if
	 * none is received.
	 */
	private class WaitForHandshakeJob implements IScheduledJob {

		/** {@inheritDoc} */
		public void execute(ISchedulingService service) {
			waitForHandshakeJob = null;
			// Client didn't send a valid handshake, disconnect
			log.warn("Closing {}, with id {} due to long handshake", RTMPConnection.this, getId());
			onInactive();
		}
	}

}
