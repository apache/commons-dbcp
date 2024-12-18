/*

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

/**
 * Basic XAConnection. getConnection() returns a handle on a physical
 * Connection. Closing the handle does not close the physical connection, you
 * have to close the XAConnection for that (PooledConnection behavior).
 * XA behavior is implemented through a LocalXAResource.
 */
public class TesterBasicXAConnection implements XAConnection {

    /**
     * Delegates everything to a Connection, except for close() which just
     * notifies the parent XAConnection.
     */
    public static final class ConnectionHandle implements InvocationHandler {

        public Connection conn;

        public final TesterBasicXAConnection xaconn;

        public ConnectionHandle(final Connection conn, final TesterBasicXAConnection xaconn) {
            this.conn = conn;
            this.xaconn = xaconn;
        }

        protected Object close() throws SQLException {
            if (conn != null) {
                conn.clearWarnings();
                conn = null;
                xaconn.handle = null;
                xaconn.notifyConnectionClosed();
            }
            return null;
        }

        public void closeHandle() {
            conn = null;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args)
                throws Throwable {
            final String methodName = method.getName();
            switch (methodName) {
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == args[0];
            case "isClosed":
                return conn == null;
            case "close":
                return close();
            default:
                break;
            }
            if (conn == null) {
                throw new SQLException("Connection closed");
            }
            try {
                return method.invoke(conn, args);
            } catch (final InvocationTargetException e) {
                final Throwable te = e.getTargetException();
                if (te instanceof SQLException) {
                    xaconn.notifyConnectionErrorOccurred((SQLException) te);
                }
                throw te;
            }
        }
    }

    public Connection conn;

    public ConnectionHandle handle;

    public final List<ConnectionEventListener> listeners = new LinkedList<>();

    public final AtomicInteger closeCounter;

    public TesterBasicXAConnection(final Connection conn) {
        this(conn, null);
    }

    public TesterBasicXAConnection(final Connection conn, final AtomicInteger closeCounter) {
        this.conn = conn;
        this.closeCounter = closeCounter;
    }

    @Override
    public void addConnectionEventListener(
            final ConnectionEventListener connectionEventListener) {
        listeners.add(connectionEventListener);
    }

    @Override
    public void addStatementEventListener(final StatementEventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws SQLException {
        if (handle != null) {
            closeHandle();
        }
        try {
            conn.close();
            if (closeCounter != null) {
                closeCounter.incrementAndGet();
            }
        } finally {
            conn = null;
        }
    }

    protected void closeHandle() throws SQLException {
        handle.closeHandle();
        if (!conn.getAutoCommit()) {
            try {
                conn.rollback();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        handle = null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (conn == null) {
            final SQLException e = new SQLException("XAConnection closed");
            notifyConnectionErrorOccurred(e);
            throw e;
        }
        try {
            if (handle != null) {
                // only one handle at a time on the XAConnection
                closeHandle();
                conn.clearWarnings();
            }
        } catch (final SQLException e) {
            notifyConnectionErrorOccurred(e);
            throw e;
        }
        handle = new ConnectionHandle(conn, this);
        return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[] { Connection.class },
                handle);
    }

    @Override
    public XAResource getXAResource() throws SQLException {
        return new LocalXAConnectionFactory.LocalXAResource(conn);
    }

    protected void notifyConnectionClosed() {
        final ConnectionEvent event = new ConnectionEvent(this);
        final List<ConnectionEventListener> copy = new ArrayList<>(
                listeners);
        for (final ConnectionEventListener listener : copy) {
            listener.connectionClosed(event);
        }
    }

    protected void notifyConnectionErrorOccurred(final SQLException e) {
        final ConnectionEvent event = new ConnectionEvent(this, e);
        final List<ConnectionEventListener> copy = new ArrayList<>(
                listeners);
        for (final ConnectionEventListener listener : copy) {
            listener.connectionErrorOccurred(event);
        }
    }

    @Override
    public void removeConnectionEventListener(
            final ConnectionEventListener connectionEventListener) {
        listeners.remove(connectionEventListener);
    }

    @Override
    public void removeStatementEventListener(final StatementEventListener listener) {
        throw new UnsupportedOperationException();
    }
}

