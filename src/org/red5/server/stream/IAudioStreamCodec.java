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

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Represents an Audio codec and its associated decoder configuration.
 * 
 * @author Paul Gregoire (mondain@gmail.com) 
 */
public interface IAudioStreamCodec {
	
	/**
	 * @return the name of the audio codec.
     */
	public String getName();

	/**
	 * Reset the codec to its initial state.
	 */
	public void reset();

	/**
	 * Returns true if the codec knows how to handle the passed
	 * stream data.
     * @param data some sample data to see if this codec can handle it.
     * @return can this code handle the data.
     */
	public boolean canHandleData(IoBuffer data);

	/**
	 * Update the state of the codec with the passed data.
     * @param data data to tell the codec we're adding
     * @return true for success. false for error.
     */
	public boolean addData(IoBuffer data);
	
	/**
	 * Returns information used to configure the decoder.
	 * 
	 * @return the data for decoder setup.
     */
	public IoBuffer getDecoderConfiguration();
	
}
