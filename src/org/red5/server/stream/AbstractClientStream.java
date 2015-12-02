package org.red5.server.stream;

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

import java.lang.ref.WeakReference;


//import org.red5.server.api.stream.IClientStream;
//import org.red5.server.api.stream.IStreamCapableConnection;

/**
 * Abstract base for client streams
 */
public abstract class AbstractClientStream extends AbstractStream implements IClientStream{

	/**
	 *  Stream identifier. Unique across server.
	 */
	private int streamId;
	
	/**
	 * Stream name of the broadcasting stream.
	 */
	private String broadcastStreamPublishName;

	/**
	 *  Connection that works with streams
	 */
	private WeakReference<IStreamCapableConnection> conn;

	/**
	 * Buffer duration in ms as requested by the client
	 */
	private int clientBufferDuration;

	/**
	 * Return stream id
	 * @return           Stream id
	 */
	public int getStreamId() {
		return streamId;
	}

	/**
	 * Return connection associated with stream
	 * @return           Stream capable connection object
	 */
	public IStreamCapableConnection getConnection() {
		return conn.get();
	}

	/**
	 * Setter for stream id
	 * @param streamId       Stream id
	 */
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	/**
	 * Setter for stream capable connection
	 * @param conn           IStreamCapableConnection object
	 */
	public void setConnection(IStreamCapableConnection conn) {
		this.conn = new WeakReference<IStreamCapableConnection>(conn);
	}

	/** {@inheritDoc} */
	public void setClientBufferDuration(int duration) {
		clientBufferDuration = duration;
	}

	/**
	 * Get duration in ms as requested by the client.
	 *
	 * @return value
	 */
	public int getClientBufferDuration() {
		return clientBufferDuration;
	}

	/**
	 * Sets the broadcasting streams name.
	 * 
	 * @param broadcastStreamPublishName name of the broadcasting stream
	 */
	public void setBroadcastStreamPublishName(String broadcastStreamPublishName) {
		this.broadcastStreamPublishName = broadcastStreamPublishName;
	}

	/** {@inheritDoc} */
	public String getBroadcastStreamPublishName() {
		return broadcastStreamPublishName;
	}
		
}
