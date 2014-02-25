/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp2.datasources;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EventListener;
import java.util.Vector;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

/**
 * PooledConnection implementation that wraps a driver-supplied
 * PooledConnection and proxies events, allowing behavior to be
 * modified to simulate behavior of different implementations.
 * 
 * @version $Revision$ $Date$
 */
public class PooledConnectionProxy implements PooledConnection,
    ConnectionEventListener {

    protected PooledConnection delegate = null;
    
    /**
     * ConnectionEventListeners
     */
    private final Vector<EventListener> eventListeners = new Vector<>();
    
    /** 
     * True means we will (dubiously) notify listeners with a
     * ConnectionClosed event when this (i.e. the PooledConnection itself)
     * is closed
     */
    private boolean notifyOnClose = false;
    
    public PooledConnectionProxy(PooledConnection pooledConnection) {
        this.delegate = pooledConnection;
        pooledConnection.addConnectionEventListener(this);
    }

    /** 
     * If notifyOnClose is on, notify listeners
     */
    @Override
    public void close() throws SQLException {
        delegate.close();
        if (isNotifyOnClose()) {
           notifyListeners();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    /**
     * Remove event listeners.
     */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        eventListeners.remove(listener);
    }

    /* JDBC_4_ANT_KEY_BEGIN */
    @Override
    public void removeStatementEventListener(StatementEventListener listener) {
        eventListeners.remove(listener);
    }
    /* JDBC_4_ANT_KEY_END */

    public boolean isNotifyOnClose() {
        return notifyOnClose;
    }

    public void setNotifyOnClose(boolean notifyOnClose) {
        this.notifyOnClose = notifyOnClose;
    }
    
    /**
     * sends a connectionClosed event to listeners.
     */
    void notifyListeners() {
        ConnectionEvent event = new ConnectionEvent(this);
        Object[] listeners = eventListeners.toArray();
        for (Object listener : listeners) {
            ((ConnectionEventListener) listener).connectionClosed(event);
        }
    }
    
    /**
     * Add event listeners.
     */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /* JDBC_4_ANT_KEY_BEGIN */
    @Override
    public void addStatementEventListener(StatementEventListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }
    /* JDBC_4_ANT_KEY_END */
    
    /**
     * Pass closed events on to listeners
     */
    @Override
    public void connectionClosed(ConnectionEvent event) {
        notifyListeners();    
    }

    /**
     * Pass error events on to listeners
     */ 
    @Override
    public void connectionErrorOccurred(ConnectionEvent event) {
        Object[] listeners = eventListeners.toArray();
        for (Object listener : listeners) {
            ((ConnectionEventListener) listener).connectionErrorOccurred(event);
        } 
    }
    
    /**
     * Generate a connection error event
     */
    public void throwConnectionError() {
        ConnectionEvent event = new ConnectionEvent(this);
        connectionErrorOccurred(event);
    }
    
    /**
     * Expose listeners
     */
    public Collection<EventListener> getListeners() {
        return eventListeners;
    }

}
