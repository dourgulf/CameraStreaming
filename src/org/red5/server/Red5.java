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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

//import javax.management.openmbean.CompositeData;

/**
 * Utility class for accessing Red5 API objects.
 *
 * This class uses a thread local, and will be setup by the service invoker.
 *  
 * The code below shows various uses. 
 * <br />
 * <pre> 
 * IConnection conn = Red5.getConnectionLocal();
 * Red5 r5 = new Red5(); 
 * IScope scope = r5.getScope();
 * conn = r5.getConnection();
 * r5 = new Red5(conn);
 * IClient client = r5.getClient();
 * </pre>
 *   
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class Red5 {

	/**
	 * Current connection thread. Each connection of Red5 application runs in a
	 * separate thread. This is thread object associated with the current connection.
	 */
	private static ThreadLocal<WeakReference<IConnection>> connThreadLocal = new ThreadLocal<WeakReference<IConnection>>();

	/**
	 * Connection local to the current thread 
	 */
	public IConnection conn;

	/**
	 * Current server version with revision
	 */
	public static final String VERSION = "Red5 Server 1.0.0 RC1 $Rev: 4204 $";

	/**
	 * Current server version for fmsVer requests 
	 */
	public static final String FMS_VERSION = "RED5/1,0,0,0";

	/**
	 * Data version for NetStatusEvents
	 */
	@SuppressWarnings("serial")
	public static final Map<String, Object> DATA_VERSION = new HashMap<String, Object>(2) {
		{
			put("version", "4,0,0,1121");
			put("type", "red5");
		}
	};

	/**
	 * Server start time
	 */
	private static final long START_TIME = System.currentTimeMillis();

	/**
	 * Create a new Red5 object using given connection.
	 * 
	 * @param conn Connection object.
	 */
	public Red5(IConnection conn) {
		this.conn = conn;
	}

	/**
	 * Create a new Red5 object using the connection local to the current thread
	 * A bit of magic that lets you access the red5 scope from anywhere
	 */
	public Red5() {
		conn = Red5.getConnectionLocal();
	}

	/**
	 * Setter for connection
	 *
	 * @param connection     Thread local connection
	 */
	public static void setConnectionLocal(IConnection connection) {
		if (connection != null) {
    		connThreadLocal.set(new WeakReference<IConnection>(connection));
//    		IScope scope = connection.getScope();
//    		if (scope != null) {
//    			Thread.currentThread().setContextClassLoader(scope.getClassLoader());
//    		}
		} else {
			// use null to clear the value
			connThreadLocal.remove();
		}
	}

	/**
	 * Get the connection associated with the current thread. This method allows
	 * you to get connection object local to current thread. When you need to
	 * get a connection associated with event handler and so forth, this method
	 * provides you with it.
	 * 
	 * @return Connection object
	 */
	public static IConnection getConnectionLocal() {
		WeakReference<IConnection> ref = connThreadLocal.get();
		if (ref != null) {
			return ref.get();
		} else {
			return null;
		}
	}

	/**
	 * Get the connection object.
	 * 
	 * @return Connection object
	 */
	public IConnection getConnection() {
		return conn;
	}

	/**
	 * Get the scope
	 * 
	 * @return Scope object
	 */
//	public IScope getScope() {
//		return conn.getScope();
//	}

	/**
	 * Get the client
	 * 
	 * @return Client object
	 */
	public IClient getClient() {
		return conn.getClient();
	}

	/**
	 * Get the spring application context
	 * 
	 * @return Application context
	 */
//	public IContext getContext() {
//		return conn.getScope().getContext();
//	}

	/**
	 * Returns the current version with revision number
	 * 
	 * @return String version
	 */
	public static String getVersion() {
		return VERSION;
	}

	/**
	 * Returns the current version for fmsVer requests
	 *
	 * @return String fms version
	 */
	public static String getFMSVersion() {
		return FMS_VERSION;
	}

	public static Object getDataVersion() {
		return DATA_VERSION;
	}

	/**
	 * Returns server uptime in milliseconds.
	 *
	 * @return String version
	 */
	public static long getUpTime() {
		return System.currentTimeMillis() - START_TIME;
	}

	/**
	 * Allows for reconstruction via CompositeData.
	 * 
	 * @param cd composite data
	 * @return Red5 class instance
	 */
//	public static Red5 from(CompositeData cd) {
//		Red5 instance = null;
//		if (cd.containsKey("connection")) {
//			Object cn = cd.get("connection");
//			if (cn != null && cn instanceof IConnection) {
//				instance = new Red5((IConnection) cn);
//			} else {
//				instance = new Red5();
//			}
//		} else {
//			instance = new Red5();
//		}
//		return instance;
//	}

}
