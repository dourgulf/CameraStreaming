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


/**
 * A stream that is bound to a client.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IClientStream extends IStream {

	public static final String MODE_READ = "read";

	public static final String MODE_RECORD = "record";

	public static final String MODE_APPEND = "append";

	public static final String MODE_LIVE = "live";
	
	public static final String MODE_PUBLISH = "publish";

	/**
	 * Get stream id allocated in a connection.
	 * 
	 * @return the stream id
	 */
	int getStreamId();

	/**
	 * Get connection containing the stream.
	 * 
	 * @return the connection object or <code>null</code> if the connection is no longer active
	 */
	IStreamCapableConnection getConnection();

	/**
	 * Set the buffer duration for this stream as requested by the client.
	 * 
	 * @param bufferTime duration in ms the client wants to buffer
	 */
	void setClientBufferDuration(int bufferTime);
	
	/**
	 * Get the buffer duration for this stream as requested by the client.
	 * 
	 * @return bufferTime duration in ms the client wants to buffer
	 */
	int getClientBufferDuration();	

	/**
	 * Returns the published stream name that this client is consuming.
	 * 
	 * @return stream name of stream being consumed
	 */
	String getBroadcastStreamPublishName();
	
}
