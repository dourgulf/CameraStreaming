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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * RTMP Abort event.
 * 
 * @author aclarke@xuggle.com
 *
 */
public class Abort extends BaseEvent {

	private int channelId=0;
	public Abort()
	{
		super(Type.SYSTEM);
	}
	public Abort(int channelId) {
		this.channelId = channelId;
	}
	public byte getDataType() {
		return TYPE_ABORT;
	}

	protected void releaseInternal() {
	
	}
	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}
	public int getChannelId() {
		return channelId;
	}
	/** {@inheritDoc} */
    @Override
	public String toString() {
		return "Abort Channel: " + channelId;
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		channelId= in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(channelId);
	}

}