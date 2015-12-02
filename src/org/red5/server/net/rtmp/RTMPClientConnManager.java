package org.red5.server.net.rtmp;

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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.server.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTMPClientConnManager implements IRTMPConnManager {
	
	private static final Logger log = LoggerFactory.getLogger(RTMPClientConnManager.class);
	
	private static final RTMPClientConnManager instance = new RTMPClientConnManager();

	protected static CopyOnWriteArraySet<RTMPConnection> rtmpConnections = new CopyOnWriteArraySet<RTMPConnection>();
	
	private RTMPClientConnManager() {
	}
	
	public static RTMPClientConnManager getInstance() {
		return instance;
	}

	/**
	 * Returns a connection matching the given client id.
	 * 
	 * @param clientId
	 * @return connection
	 */
	public RTMPConnection getConnection(int clientId) {
		log.debug("Returning map entry for client id: {}", clientId);
		RTMPConnection connReturn = null;
		for (RTMPConnection conn : rtmpConnections) {
			if (conn.getId() == clientId) {
				connReturn = conn;
				break;
			}
		}
		return connReturn;
	}

	/**
	 * Removes a connection matching the client id specified. If found, the connection
	 * will be returned.
	 * 
	 * @param clientId
	 * @return connection
	 */
	public RTMPConnection removeConnection(int clientId) {
		RTMPConnection connReturn = null;
		for (RTMPConnection conn : rtmpConnections) {
			if (conn.getId() == clientId) {
				connReturn = conn;
				break;
			}
		}
		if (connReturn != null) {
			rtmpConnections.remove(connReturn);
		}
		return connReturn;
	}

	/**
	 * Removes all the connections from the set.
	 */
	public Collection<RTMPConnection> removeConnections() {
		rtmpConnections.clear();
		return null;
	}

	public RTMPConnection createConnection(Class<?> connCls) {
		log.debug("Creating connection, class: {}", connCls.getName());
		if (!RTMPConnection.class.isAssignableFrom(connCls)) {
			throw new IllegalArgumentException("Class was not assignable");
		}
		try {
			RTMPConnection conn = (RTMPConnection) connCls.newInstance();
			// the id may become confused with the Object/String client id
			conn.setId(BaseConnection.getNextClientId());
			log.debug("Connection id set {}", conn.getId());
			rtmpConnections.add(conn);
			log.debug("Connection added to the map");
			return conn;
		} catch (Exception e) {
			log.error("RTMPConnection creation failed", e);
			throw new RuntimeException(e);
		}
	}
}
