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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.service.IServiceCall;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.IStreamPacket;

/**
 * Stream notification event
 * @author Red5 team
 * @author Tiago Daniel Jacobs (tiago@imdt.com.br)
 */
public class Notify extends BaseEvent implements IStreamData<Notify>, IStreamPacket {

	private static final long serialVersionUID = -6085848257275156569L;
    
	/**
	 * Service call
	 */
	protected IServiceCall call;

	/**
	 * Event data
	 */
	protected IoBuffer data;

	/**
	 * Invoke id
	 */
	private int invokeId = 0;

    /**
     * Connection parameters
     */
    private Map<String, Object> connectionParams;

	/** Constructs a new Notify. */
    public Notify() {
		super(Type.SERVICE_CALL);
	}

    /**
     * Create new notification event with given byte buffer
     * @param data       Byte buffer
     */
    public Notify(IoBuffer data) {
		super(Type.STREAM_DATA);
		this.data = data;
	}

    /**
     * Create new notification event with given service call
     * @param call        Service call
     */
	public Notify(IServiceCall call) {
		super(Type.SERVICE_CALL);
		this.call = call;
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_NOTIFY;
	}

	/**
     * Setter for data
     *
     * @param data  Data
     */
    public void setData(IoBuffer data) {
		this.data = data;
	}

	/**
     * Setter for call
     *
     * @param call Service call
     */
    public void setCall(IServiceCall call) {
		this.call = call;
	}

	/**
     * Getter for service call
     *
     * @return  Service call
     */
    public IServiceCall getCall() {
		return this.call;
	}

	/** {@inheritDoc} */
    public IoBuffer getData() {
		return data;
	}

	/**
     * Getter for invoke id
     *
     * @return  Invoke id
     */
    public int getInvokeId() {
		return invokeId;
	}

	/**
     * Setter for invoke id
     *
     * @param invokeId  Invoke id
     */
    public void setInvokeId(int invokeId) {
		this.invokeId = invokeId;
	}

    /**
     * Release event (nullify call object)
     */
    protected void doRelease() {
		call = null;
	}

	/**
     * Getter for connection parameters
     *
     * @return Connection parameters
     */
    public Map<String, Object> getConnectionParams() {
		return connectionParams;
	}

	/**
     * Setter for connection parameters
     *
     * @param connectionParams  Connection parameters
     */
    public void setConnectionParams(Map<String, Object> connectionParams) {
		this.connectionParams = connectionParams;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Notify: ").append(call);
		return sb.toString();
	}

	/** {@inheritDoc} */
    @Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Notify)) {
			return false;
		}
		Notify other = (Notify) obj;
		if (getConnectionParams() == null
				&& other.getConnectionParams() != null) {
			return false;
		}
		if (getConnectionParams() != null
				&& other.getConnectionParams() == null) {
			return false;
		}
		if (getConnectionParams() != null
				&& !getConnectionParams().equals(other.getConnectionParams())) {
			return false;
		}
		if (getInvokeId() != other.getInvokeId()) {
			return false;
		}
		if (getCall() == null && other.getCall() != null) {
			return false;
		}
		if (getCall() != null && other.getCall() == null) {
			return false;
		}
		if (getCall() != null && !getCall().equals(other.getCall())) {
			return false;
		}
		return true;
	}

	/** {@inheritDoc} */
    @Override
	protected void releaseInternal() {
		if (data != null) {
			data.free();
			data = null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		call = (IServiceCall) in.readObject();
		connectionParams = (Map<String, Object>) in.readObject();
		invokeId = in.readInt();
		byte[] byteBuf = (byte[]) in.readObject();
		if (byteBuf != null) {
			data = IoBuffer.allocate(0);
			data.setAutoExpand(true);
			SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(call);
		out.writeObject(connectionParams);
		out.writeInt(invokeId);
		if (data != null) {
			out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
		} else {
			out.writeObject(null);
		}
	}
	
	/**
     * Duplicate this Notify message to future injection
     * Serialize to memory and deserialize, safe way.
     * 
     * @return  duplicated Notify event
     */
	public Notify duplicate() throws IOException, ClassNotFoundException {
		Notify result = new Notify();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);		
		writeExternal(oos);
		oos.close();
		
		byte[] buf = baos.toByteArray();
		baos.close();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		ObjectInputStream ois = new ObjectInputStream(bais);
		
		result.readExternal(ois);
		ois.close();
		bais.close();
		
		return result;
	}
	
}
