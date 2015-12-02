package org.red5.server.service;

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
 * Exception class than contains additional parameters to return to the client.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ClientDetailsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1908769505547253205L;

	/**
	 * Parameters to return to the client.
	 */
	private Object parameters;
	
	/**
	 * Also return stacktrace to client?
	 */
	private boolean stacktrace;
	
	/**
	 * Create new exception object from message and parameters. By default, no
	 * stacktrace is returned to the client.
	 * 
	 * @param message message
	 * @param params parameters for message
	 */
	public ClientDetailsException(String message, Object params) {
		this(message, params, false);
	}

	/**
	 * Create new exception object from message and parameters with optional stacktrace.
	 * 
	 * @param message message
	 * @param params parameters
	 * @param includeStacktrace whether or not to include a stack trace
	 */
	public ClientDetailsException(String message, Object params, boolean includeStacktrace) {
		super(message);
		this.parameters = params;
		this.stacktrace = includeStacktrace;
	}

	/**
	 * Get parameters to return to the client.
	 * 
	 * @return parameters
	 */
	public Object getParameters() {
		return parameters;
	}
	
	/**
	 * Should the stacktrace returned to the client?
	 * 
	 * @return stacktrace
	 */
	public boolean includeStacktrace() {
		return stacktrace;
	}
	
}
