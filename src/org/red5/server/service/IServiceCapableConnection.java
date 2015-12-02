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

import org.red5.server.IConnection;

/**
 * Connection that has options to invoke and handle remote calls
 */
// TODO: this should really extend IServiceInvoker
public interface IServiceCapableConnection extends IConnection {
    /**
     * Invokes service using remoting call object
     * @param call       Service call object
     */
    void invoke(IServiceCall call);

    /**
     * Invoke service using call and channel
     * @param call       Service call
     * @param channel    Channel used
     */
    void invoke(IServiceCall call, int channel);

    /**
     * Invoke method by name
     * @param method     Called method name
     */
    void invoke(String method);

    /**
     * Invoke method by name with callback
     * @param method     Called method name
     * @param callback   Callback
     */
    void invoke(String method, IPendingServiceCallback callback);

    /**
     * Invoke method with parameters
     * @param method     Method name
     * @param params     Invocation parameters passed to method
     */
    void invoke(String method, Object[] params);

    /**
     *
     * @param method
     * @param params
     * @param callback
     */
    void invoke(String method, Object[] params,
			IPendingServiceCallback callback);

    /**
     *
     * @param call
     */
    void notify(IServiceCall call);

    /**
     *
     * @param call
     * @param channel
     */
    void notify(IServiceCall call, int channel);

    /**
     *
     * @param method
     */
    void notify(String method);

    /**
     * 
     * @param method
     * @param params
     */
    void notify(String method, Object[] params);

}
