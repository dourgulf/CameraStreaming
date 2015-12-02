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

//import org.red5.server.api.IScope;
//import org.red5.server.api.IScopeHandler;
//import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.net.rtmp.event.Notify;

/**
 * Abstract base implementation of IStream. Contains codec information, stream name, scope, event handling
 * meand, provides stream start and stop operations.
 *
 * @see  org.red5.server.stream.IStream
 */
public abstract class AbstractStream implements IStream {
    
    /**
     * Current state
     */
    protected StreamState state = StreamState.UNINIT;
    
	/**
     *  Stream name
     */
    private String name;
    
    /**
     *  Stream audio and video codec information
     */
	private IStreamCodecInfo codecInfo;
    
	/**
	 * Stores the streams metadata
	 */
	protected Notify metaData;
	
	/**
     *  Stream scope
     */
//	private IScope scope;
	
	/**
	 * Timestamp the stream was created.
	 */
	protected long creationTime;
	
    /**
     *  Return stream name
     *  @return     Stream name
     */
	public String getName() {
		return name;
	}

    /**
     * Return codec information
     * @return              Stream codec information
     */
    public IStreamCodecInfo getCodecInfo() {
		return codecInfo;
	}

	/**
	 * Returns the metadata for the associated stream, if it exists.
	 * 
	 * @return stream meta data
	 */
	public Notify getMetaData() {
		return metaData;
	}    
    
    /**
     * Return scope
     * @return         Scope
     */
//    public IScope getScope() {
//		return scope;
//	}
    
	/**
	 * Returns timestamp at which the stream was created.
	 * 
	 * @return creation timestamp
	 */
	public long getCreationTime() {
		return creationTime;
	}

    /**
     * Setter for name
     * @param name     Stream name
     */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * Setter for codec info
     * @param codecInfo     Codec info
     */
    public void setCodecInfo(IStreamCodecInfo codecInfo) {
		this.codecInfo = codecInfo;
	}

    /**
     * Setter for scope
     * @param scope         Scope
     */
//	public void setScope(IScope scope) {
//		this.scope = scope;
//	}

    /**
     * Return stream aware scope handler or null if scope is null
     * @return      IStreamAwareScopeHandler implementation
     */
//	protected IStreamAwareScopeHandler getStreamAwareHandler() {
////		if (scope != null) {
////			IScopeHandler handler = scope.getHandler();
////			if (handler instanceof IStreamAwareScopeHandler) {
////				return (IStreamAwareScopeHandler) handler;
////			}
////		}
//		return null;
//	}
}
