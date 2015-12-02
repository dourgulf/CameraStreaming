package org.red5.server.net.rtmp.event;

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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.server.event.IEventListener;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;

/**
 * Base abstract class for all RTMP events
 */
public abstract class BaseEvent implements Constants, IRTMPEvent, Externalizable {

	// XXX we need a better way to inject allocation debugging
	// (1) make it configurable in xml
	// (2) make it aspect oriented
	private static final boolean allocationDebugging = false;

	/**
	 * Event type
	 */
	private Type type;

	/**
	 * Source type
	 */
	private byte sourceType;

	/**
	 * Event target object
	 */
	protected Object object;

	/**
	 * Event listener
	 */
	protected IEventListener source;

	/**
	 * Event timestamp
	 */
	protected int timestamp;

	/**
	 * Event RTMP packet header
	 */
	protected Header header = null;

	/**
	 * Event references count
	 */
	protected AtomicInteger refcount = new AtomicInteger(1);

	public BaseEvent() {
		// set a default type
		this(Type.SERVER, null);
	}

	/**
	 * Create new event of given type
	 * @param type             Event type
	 */
	public BaseEvent(Type type) {
		this(type, null);
	}

	/**
	 * Create new event of given type
	 * @param type             Event type
	 * @param source           Event source
	 */
	public BaseEvent(Type type, IEventListener source) {
		this.type = type;
		this.source = source;
		if (allocationDebugging) {
			AllocationDebugger.getInstance().create(this);
		}
	}

	/** {@inheritDoc} */
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public byte getSourceType() {
		return sourceType;
	}

	public void setSourceType(byte sourceType) {
		this.sourceType = sourceType;
	}

	/** {@inheritDoc} */
	public Object getObject() {
		return object;
	}

	/** {@inheritDoc} */
	public Header getHeader() {
		return header;
	}

	/** {@inheritDoc} */
	public void setHeader(Header header) {
		this.header = header;
	}

	/** {@inheritDoc} */
	public boolean hasSource() {
		return source != null;
	}

	/** {@inheritDoc} */
	public IEventListener getSource() {
		return source;
	}

	/** {@inheritDoc} */
	public void setSource(IEventListener source) {
		this.source = source;
	}

	/** {@inheritDoc} */
	public abstract byte getDataType();

	/** {@inheritDoc} */
	public int getTimestamp() {
		return timestamp;
	}

	/** {@inheritDoc} */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("all")
	public void retain() {
		if (allocationDebugging) {
			AllocationDebugger.getInstance().retain(this);
		}
		final int baseCount = refcount.getAndIncrement();
		if (allocationDebugging && baseCount < 1) {
			throw new RuntimeException("attempt to retain object with invalid ref count");
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("all")
	public void release() {
		if (allocationDebugging) {
			AllocationDebugger.getInstance().release(this);
		}
		final int baseCount = refcount.decrementAndGet();
		if (baseCount == 0) {
			releaseInternal();
		} else if (allocationDebugging && baseCount < 0) {
			throw new RuntimeException("attempt to retain object with invalid ref count");
		}
	}

	/**
	 * Release event
	 */
	protected abstract void releaseInternal();

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		type = (Type) in.readObject();
		sourceType = in.readByte();
		timestamp = in.readInt();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(type);
		out.writeByte(sourceType);
		out.writeInt(timestamp);
	}
	
}
