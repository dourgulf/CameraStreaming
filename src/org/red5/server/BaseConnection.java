package org.red5.server;

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

//import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Collections;
//import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//import org.red5.server.api.IBasicScope;
//import org.red5.server.api.IScope;
import org.red5.server.event.IEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base abstract class for connections. Adds connection specific functionality like work with clients
 * to AttributeStore.
 */
public abstract class BaseConnection extends AttributeStore implements IConnection {

	/**
	 *  Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(BaseConnection.class);

	/**
	 *  Connection type
	 */
	protected final String type;

	/**
	 *  Connection host
	 */
	protected volatile String host;

	/**
	 *  Connection remote address
	 */
	protected volatile String remoteAddress;

	/**
	 *  Connection remote addresses
	 */
	protected volatile List<String> remoteAddresses;

	/**
	 *  Remote port
	 */
	protected volatile int remotePort;

	/**
	 *  Path of scope client connected to
	 */
	protected volatile String path;

	/**
	 *  Connection session identifier
	 */
	protected volatile String sessionId;

	/**
	 *  Number of read messages
	 */
	protected AtomicLong readMessages = new AtomicLong(0);

	/**
	 *  Number of written messages
	 */
	protected AtomicLong writtenMessages = new AtomicLong(0);

	/**
	 *  Number of dropped messages
	 */
	protected AtomicLong droppedMessages = new AtomicLong(0);

	/**
	 * Connection params passed from client with NetConnection.connect call
	 *
	 * @see <a href='http://livedocs.adobe.com/fms/2/docs/00000570.html'>NetConnection in Flash Media Server docs (external)</a>
	 */
	@SuppressWarnings("all")
	protected volatile Map<String, Object> params = null;

	/**
	 * Client bound to connection
	 */
	protected volatile IClient client;

	/**
	 * Scope that connection belongs to
	 */
	//protected volatile Scope scope;

	/**
	 * Set of basic scopes.
	 */
//	protected Set<IBasicScope> basicScopes = new CopyOnWriteArraySet<IBasicScope>();

	/**
	 * Is the connection closed?
	 */
	protected volatile boolean closed;

	/**
	 * Lock used on in the connections to protect read and write operations
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Used for generation of client ids that may be shared across the server
	 */
	private final static AtomicInteger clientIdGenerator = new AtomicInteger(0);

	/**
	 * Creates a new persistent base connection
	 */
//	@ConstructorProperties(value = { "persistent" })
	public BaseConnection() {
		log.debug("New BaseConnection");
		this.type = PERSISTENT;
	}

	/**
	 * Creates a new base connection with the given type.
	 * 
	 * @param type                Connection type
	 */
//	@ConstructorProperties({ "type" })
	public BaseConnection(String type) {
		log.debug("New BaseConnection - type: {}", type);
		this.type = type;
	}

	/**
	 * Creates a new base connection with the given parameters.
	 * 
	 * @param type                Connection type
	 * @param host                Host
	 * @param remoteAddress       Remote address
	 * @param remotePort          Remote port
	 * @param path                Scope path on server
	 * @param sessionId           Session id
	 * @param params              Params passed from client
	 */
//	@ConstructorProperties({ "type", "host", "remoteAddress", "remotePort", "path", "sessionId" })
	public BaseConnection(String type, String host, String remoteAddress, int remotePort, String path, String sessionId, Map<String, Object> params) {
		log.debug("New BaseConnection - type: {} host: {} remoteAddress: {} remotePort: {} path: {} sessionId: {}", new Object[] { type, host, remoteAddress, remotePort, path,
				sessionId });
		log.debug("Params: {}", params);
		this.type = type;
		this.host = host;
		this.remoteAddress = remoteAddress;
		this.remoteAddresses = new ArrayList<String>(1);
		this.remoteAddresses.add(remoteAddress);
		this.remoteAddresses = Collections.unmodifiableList(this.remoteAddresses);
		this.remotePort = remotePort;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
	}

	/**
	 * Returns the next available client id.
	 * 
	 * @return new client id
	 */
	public static int getNextClientId() {
		return clientIdGenerator.incrementAndGet();
	}

	/**
	 * @return lock for read only operations
	 */
	public Lock getReadLock() {
		return lock.readLock();
	}

	/**
	 * @return lock for changing state operations
	 */
	public Lock getWriteLock() {
		return lock.writeLock();
	}

	/**
	 * Initializes client
	 * @param client        Client bound to connection
	 */
	public void initialize(IClient client) {
//		if (this.client != null && this.client instanceof Client) {
//			// Unregister old client
//			((Client) this.client).unregister(this);
//		}
//		this.client = client;
//		if (this.client instanceof Client) {
//			// Register new client
//			((Client) this.client).register(this);
//		}
	}

	/**
	 *
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 *
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 *
	 * @return remote address
	 */
	public String getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * @return remote address
	 */
	public List<String> getRemoteAddresses() {
		return remoteAddresses;
	}

	/**
	 *
	 * @return remote port
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 *
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 *
	 * @return session id
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Return connection parameters
	 * @return connection parameters
	 */
	public Map<String, Object> getConnectParams() {
		return Collections.unmodifiableMap(params);
	}

	/**
	 *
	 * @return client
	 */
	public IClient getClient() {
		return client;
	}

	/**
	 * Check whether connection is alive
	 * @return       true if connection is bound to scope, false otherwise
	 */
	/*ryong21
	public boolean isConnected() {
		return scope != null;
	}
*/
	/**
	 * Connect to another scope on server
	 * @param newScope     New scope
	 * @return             true on success, false otherwise
	 */
	public boolean connect() {
		return connect(new Object[]{});
	}

	/**
	 * Connect to another scope on server with given parameters
	 * @param newScope        New scope
	 * @param params          Parameters to connect with
	 * @return                true on success, false otherwise
	 */
	public boolean connect(Object[] params) {
		/*
		if (log.isDebugEnabled()) {
			log.debug("Connect Params: {}", params);
			for (Object e : params) {
				log.debug("Param: {}", e);
			}
		}
		Lock lock = getWriteLock();
		lock.lock();
		try {
			final Scope oldScope = scope;
			scope = (Scope) newScope;
			if (scope.connect(this, params)) {
				if (oldScope != null) {
					oldScope.disconnect(this);
				}
				return true;
			} else {
				scope = oldScope;
				return false;
			}
		} finally {
			lock.unlock();
		}*/
		return true;
	}

	/**
	 *
	 * @return scope
	 */
	/*
	public IScope getScope() {
		return scope;
	}
*/
	/**
	 *  Closes connection
	 */
	public void close() {
		getWriteLock().lock();
		try {
			/*
			 * if (closed || scope == null) {

				log.debug("Close, not connected nothing to do.");
				return;
			}
		     */
			closed = true;
		} finally {
			getWriteLock().unlock();
		}
		log.debug("Close, disconnect from scope, and children");
		try {
			// Unregister all child scopes first
//			for (IBasicScope basicScope : basicScopes) {
//				unregisterBasicScope(basicScope);
//			}
		} catch (Exception err) {
			log.error("Error while unregistering basic scopes.", err);
		}
		// Disconnect
		try {
			//scope.disconnect(this);
		} catch (Exception err) {
			//log.error("Error while disconnecting from scope: {}. {}", scope, err);
		}
		// Unregister client
//		if (client != null && client instanceof Client) {
//			((Client) client).unregister(this);
//			client = null;
//		}
		//scope = null;
	}

	/**
	 * Notified on event
	 * @param event       Event
	 */
	public void notifyEvent(IEvent event) {
		log.debug("Event notify was not handled: {}", event);
	}

	/**
	 * Dispatches event
	 * @param event       Event
	 */
	public void dispatchEvent(IEvent event) {
		log.debug("Event notify was not dispatched: {}", event);
	}

	/**
	 * Handles event
	 * @param event        Event
	 * @return             true if associated scope was able to handle event, false otherwise
	 */
	public boolean handleEvent(IEvent event) {
//		return getScope().handleEvent(event);
		return true;
	}

	/**
	 *
	 * @return basic scopes
	 */
//	public Iterator<IBasicScope> getBasicScopes() {
//		return basicScopes.iterator();
//	}

	/**
	 * Registers basic scope
	 * @param basicScope      Basic scope to register
	 */
//	public void registerBasicScope(IBasicScope basicScope) {
//		basicScopes.add(basicScope);
//		basicScope.addEventListener(this);
//	}

	/**
	 * Unregister basic scope
	 *
	 * @param basicScope      Unregister basic scope
	 */
//	public void unregisterBasicScope(IBasicScope basicScope) {
//		basicScopes.remove(basicScope);
//		basicScope.removeEventListener(this);
//	}

	/**
	 *
	 * @return bytes read
	 */
	public abstract long getReadBytes();

	/**
	 *
	 * @return bytes written
	 */
	public abstract long getWrittenBytes();

	/**
	 *
	 * @return messages read
	 */
	public long getReadMessages() {
		return readMessages.get();
	}

	/**
	 *
	 * @return messages written
	 */
	public long getWrittenMessages() {
		return writtenMessages.get();
	}

	/**
	 *
	 * @return dropped messages
	 */
	public long getDroppedMessages() {
		return droppedMessages.get();
	}

	/**
	 *
	 * @return pending messages
	 */
	public long getPendingMessages() {
		return 0;
	}

	/**
	 *
	 * @param streamId the id you want to know about
	 * @return pending messages for this streamId
	 */
	public long getPendingVideoMessages(int streamId) {
		return 0;
	}

	/** {@inheritDoc} */
	public long getClientBytesRead() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if (host != null) {
			result = prime * result + host.hashCode();
		}
		if (remoteAddress != null) {
			result = prime * result + remoteAddress.hashCode();
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BaseConnection other = (BaseConnection) obj;
		if (host != null && !host.equals(other.getHost())) {
			return false;
		}
		if (remoteAddress != null && !remoteAddress.equals(other.getRemoteAddress())) {
			return false;
		}
		return true;
	}

}
