package org.red5.server.net.rtmp.event;

import org.apache.mina.core.buffer.IoBuffer;

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
 * AMF3 stream send message.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class FlexStreamSend extends Notify {

	private static final long serialVersionUID = -4226252245996614504L;

	public FlexStreamSend() {}
	/**
	 * Create new stream send object.
	 * 
	 * @param data data
	 */
	public FlexStreamSend(IoBuffer data) {
		super(data);
	}
	
	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_FLEX_STREAM_SEND;
	}

}
