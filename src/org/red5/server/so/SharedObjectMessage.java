package org.red5.server.so;

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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.red5.server.event.IEventListener;
import org.red5.server.net.rtmp.event.BaseEvent;

/**
 * Shared object event
 */
public class SharedObjectMessage extends BaseEvent implements ISharedObjectMessage {

	private static final long serialVersionUID = -8128704039659990049L;

	/**
	 * SO event name
	 */
	private String name;

	/**
	 * SO events chain
	 */
	private ConcurrentLinkedQueue<ISharedObjectEvent> events = new ConcurrentLinkedQueue<ISharedObjectEvent>();

	/**
	 * SO version, used for synchronization purposes
	 */
	private int version;

	/**
	 * Whether SO persistent
	 */
	private boolean persistent;

	public SharedObjectMessage() {
	}

	/**
	 * Creates Shared Object event with given name, version and persistence flag
	 * 
	 * @param name Event name
	 * @param version SO version
	 * @param persistent SO persistence flag
	 */
	public SharedObjectMessage(String name, int version, boolean persistent) {
		this(null, name, version, persistent);
	}

	/**
	 * Creates Shared Object event with given listener, name, SO version and
	 * persistence flag
	 * 
	 * @param source Event listener
	 * @param name Event name
	 * @param version SO version
	 * @param persistent SO persistence flag
	 */
	public SharedObjectMessage(IEventListener source, String name, int version, boolean persistent) {
		super(Type.SHARED_OBJECT, source);
		this.name = name;
		this.version = version;
		this.persistent = persistent;
	}

	/** {@inheritDoc} */
	@Override
	public byte getDataType() {
		return TYPE_SHARED_OBJECT;
	}

	/** {@inheritDoc} */
	public int getVersion() {
		return version;
	}

	/**
	 * Setter for version
	 * 
	 * @param version
	 *            New version
	 */
	protected void setVersion(int version) {
		this.version = version;
	}

	/** {@inheritDoc} */
	public String getName() {
		return name;
	}

	/**
	 * Setter for name
	 * 
	 * @param name
	 *            Event name
	 */
	protected void setName(String name) {
		this.name = name;
	}

	/** {@inheritDoc} */
	public boolean isPersistent() {
		return persistent;
	}

	/**
	 * Setter for persistence flag
	 * 
	 * @param persistent
	 *            Persistence flag
	 */
	protected void setIsPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/** {@inheritDoc} */
	public void addEvent(ISharedObjectEvent event) {
		events.add(event);
	}

	public void addEvents(List<ISharedObjectEvent> events) {
		this.events.addAll(events);
	}

	public void addEvents(Queue<ISharedObjectEvent> events) {
		this.events.addAll(events);
	}

	/** {@inheritDoc} */
	public ConcurrentLinkedQueue<ISharedObjectEvent> getEvents() {
		return events;
	}

	/** {@inheritDoc} */
	public void addEvent(ISharedObjectEvent.Type type, String key, Object value) {
		events.add(new SharedObjectEvent(type, key, value));
	}

	/** {@inheritDoc} */
	public void clear() {
		events.clear();
	}

	/** {@inheritDoc} */
	public boolean isEmpty() {
		return events.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public Type getType() {
		return Type.SHARED_OBJECT;
	}

	/** {@inheritDoc} */
	@Override
	public Object getObject() {
		return getEvents();
	}

	/** {@inheritDoc} */
	@Override
	protected void releaseInternal() {

	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("SharedObjectMessage: ").append(name).append(" { ");
		final Iterator<ISharedObjectEvent> it = events.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(" , ");
			}
		}
		sb.append(" } ");
		return sb.toString();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		name = (String) in.readObject();
		version = in.readInt();
		persistent = in.readBoolean();
		Object o = in.readObject();
		if (o != null && o instanceof ConcurrentLinkedQueue) {
			events = (ConcurrentLinkedQueue<ISharedObjectEvent>) o;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(name);
		out.writeInt(version);
		out.writeBoolean(persistent);
		out.writeObject(events);
	}
}
