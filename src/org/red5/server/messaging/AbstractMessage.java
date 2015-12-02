package org.red5.server.messaging;

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

import java.util.Map;

/**
 * Abstract base for all messages
 *
 * @see org.red5.server.messaging.IMessage
 */
public class AbstractMessage implements IMessage {
    protected String messageID;
	protected String correlationID;
	protected String messageType;
	protected Map<?, ?> extraHeaders = null;

	/** {@inheritDoc} */
    public String getMessageID() {
		return messageID;
	}

	/** {@inheritDoc} */
    public void setMessageID(String id) {
		this.messageID = id;
	}

	/** {@inheritDoc} */
    public String getCorrelationID() {
		return correlationID;
	}

	/** {@inheritDoc} */
    public void setCorrelationID(String id) {
		this.correlationID = id;
	}

	/** {@inheritDoc} */
    public String getMessageType() {
		return messageType;
	}

	/** {@inheritDoc} */
    public void setMessageType(String type) {
		this.messageType = type;
	}

	/** {@inheritDoc} */
    public boolean getBooleanProperty(String name) {
		return false;
	}

	/** {@inheritDoc} */
    public void setBooleanProperty(String name, boolean value) {
	}

	/** {@inheritDoc} */
    public byte getByteProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
    public void setByteProperty(String name, byte value) {
	}

	/** {@inheritDoc} */
    public double getDoubleProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
    public void setDoubleProperty(String name, double value) {
	}

	/** {@inheritDoc} */
    public float getFloatProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
    public void setFloatProperty(String name, float value) {
	}

	/** {@inheritDoc} */
    public int getIntProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
    public void setIntProperty(String name, int value) {
	}

	/** {@inheritDoc} */
    public long getLongProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
    public void setLongProperty(String name, long value) {
	}

	/** {@inheritDoc} */
    public short getShortProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
    public void setShortProperty(String name, short value) {
	}

	/** {@inheritDoc} */
    public String getStringProperty(String name) {
		return null;
	}

	/** {@inheritDoc} */
    public void setStringProperty(String name, String value) {
	}

	/** {@inheritDoc} */
    public Object getObjectProperty(String name) {
		return null;
	}

	/** {@inheritDoc} */
    public void setObjectProperty(String name, Object value) {
	}

}
