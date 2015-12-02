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

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Stream data packet
 */
public interface IStreamData<T> {

	/**
     * Getter for property 'data'.
     *
     * @return Value for property 'data'.
     */
    public IoBuffer getData();
    
    /**
     * Creates a byte accurate copy.
     * 
     * @return duplicate of the current data item
     * @throws IOException
     * @throws ClassNotFoundException
     */
	public IStreamData<T> duplicate() throws IOException, ClassNotFoundException;
	
}
