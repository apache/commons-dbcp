/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.time.Duration;
import java.util.function.Supplier;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * A {@link KeyedPooledObjectFactory} that creates {@link PoolableConnection}s.
 *
 * @since 2.0
 */
final class KeyedCPDSConnectionFactory extends AbstractConnectionFactory
        implements KeyedPooledObjectFactory<UserPassKey, PooledConnectionAndInfo>, ConnectionEventListener, PooledConnectionManager {

    private static final String NO_KEY_MESSAGE = "close() was called on a Connection, but I have no record of the underlying PooledConnection.";
    private KeyedObjectPool<UserPassKey, PooledConnectionAndInfo> pool;

    /**
     * Constructs a factory for pooled connections keyed by user credentials.
     *
     * @param cpds                           the provider from which to obtain pooled connections.
     * @param validationQuery                the validation query, or {@code null} to use {@link Connection#isValid(int)}.
     * @param validationQueryTimeoutDuration the timeout for connection validation.
     * @param rollbackAfterValidation        whether to roll back after executing the validation query.
     * @param maxConnDuration                the maximum connection lifetime, not null; zero or a negative duration disables the limit.
     * @param loginTimeoutSupplier           the supplier of the current login timeout, not null; a supplied {@code null} preserves the provider setting.
     * @throws NullPointerException Thrown if maxConnDuration or loginTimeoutSupplier is null.
     */
    KeyedCPDSConnectionFactory(final ConnectionPoolDataSource cpds, final String validationQuery, final Duration validationQueryTimeoutDuration,
            final boolean rollbackAfterValidation, final Duration maxConnDuration, Supplier<Duration> loginTimeoutSupplier) {
        super(cpds, validationQuery, validationQueryTimeoutDuration, rollbackAfterValidation, maxConnDuration, loginTimeoutSupplier);
    }

    @Override
    public void activateObject(final UserPassKey ignored, final PooledObject<PooledConnectionAndInfo> pooledObject) throws SQLException {
        validateLifetime(pooledObject);
    }

    /**
     * This implementation does not fully close the KeyedObjectPool, as this would affect all users. Instead, it clears
     * the pool associated with the given user. This method is not currently used.
     */
    @Override
    public void closePool(final String userName) throws SQLException {
        try {
            pool.clear(new UserPassKey(userName));
        } catch (final Exception ex) {
            throw new SQLException("Error closing connection pool", ex);
        }
    }

    /**
     * This will be called if the Connection returned by the getConnection method came from a PooledConnection, and the
     * user calls the close() method of this connection object. What we need to do here is to release this
     * PooledConnection from our pool...
     */
    @Override
    public void connectionClosed(final ConnectionEvent event) {
        final PooledConnection pc = (PooledConnection) event.getSource();
        // if this event occurred because we were validating, or if this
        // connection has been marked for removal, ignore it
        // otherwise return the connection to the pool.
        if (!validatingSet.contains(pc)) {
            final PooledConnectionAndInfo pci = pcMap.get(pc);
            if (pci == null) {
                throw new IllegalStateException(NO_KEY_MESSAGE);
            }
            try {
                pool.returnObject(pci.getUserPassKey(), pci);
            } catch (final Exception e) {
                System.err.println("CLOSING DOWN CONNECTION AS IT COULD NOT BE RETURNED TO THE POOL");
                pc.removeConnectionEventListener(this);
                try {
                    pool.invalidateObject(pci.getUserPassKey(), pci);
                } catch (final Exception e3) {
                    System.err.println("EXCEPTION WHILE DESTROYING OBJECT " + pci);
                    e3.printStackTrace();
                }
            }
        }
    }

    /**
     * If a fatal error occurs, close the underlying physical connection so as not to be returned in the future
     */
    @Override
    public void connectionErrorOccurred(final ConnectionEvent event) {
        final PooledConnection pc = (PooledConnection) event.getSource();
        if (event.getSQLException() != null) {
            System.err.println("CLOSING DOWN CONNECTION DUE TO INTERNAL ERROR (" + event.getSQLException() + ")");
        }
        pc.removeConnectionEventListener(this);

        final PooledConnectionAndInfo info = pcMap.get(pc);
        if (info == null) {
            throw new IllegalStateException(NO_KEY_MESSAGE);
        }
        try {
            pool.invalidateObject(info.getUserPassKey(), info);
        } catch (final Exception e) {
            System.err.println("EXCEPTION WHILE DESTROYING OBJECT " + info);
            e.printStackTrace();
        }
    }

    /**
     * Closes the PooledConnection and stops listening for events from it.
     */
    @Override
    public void destroyObject(final UserPassKey ignored, final PooledObject<PooledConnectionAndInfo> pooledObject) throws SQLException {
        final PooledConnection pooledConnection = pooledObject.getObject().getPooledConnection();
        pooledConnection.removeConnectionEventListener(this);
        pcMap.remove(pooledConnection);
        pooledConnection.close();
    }

    /**
     * Gets the keyed object pool used to pool connections created by this factory.
     *
     * @return KeyedObjectPool managing pooled connections
     */
    public KeyedObjectPool<UserPassKey, PooledConnectionAndInfo> getPool() {
        return pool;
    }

    /**
     * Invalidates the PooledConnection in the pool. The KeyedCPDSConnectionFactory closes the connection and pool
     * counters are updated appropriately. Also clears any idle instances associated with the user name that was used to
     * create the PooledConnection. Connections associated with this user are not affected, and they will not be
     * automatically closed on return to the pool.
     */
    @Override
    public void invalidate(final PooledConnection pc) throws SQLException {
        final PooledConnectionAndInfo info = pcMap.get(pc);
        if (info == null) {
            throw new IllegalStateException(NO_KEY_MESSAGE);
        }
        final UserPassKey key = info.getUserPassKey();
        try {
            pool.invalidateObject(key, info); // Destroy and update pool counters
            pool.clear(key); // Remove any idle instances with this key
        } catch (final Exception ex) {
            throw new SQLException("Error invalidating connection", ex);
        }
    }

    /**
     * Creates a new {@code PooledConnectionAndInfo} from the given {@code UserPassKey}.
     *
     * @param userPassKey
     *            {@code UserPassKey} containing user credentials
     * @throws SQLException Thrown if the connection could not be created.
     * @see org.apache.commons.pool2.KeyedPooledObjectFactory#makeObject(Object)
     */
    @Override
    public synchronized PooledObject<PooledConnectionAndInfo> makeObject(final UserPassKey userPassKey) throws SQLException {
        final PooledConnection pooledConnection = getPooledConnection(userPassKey);
        if (pooledConnection == null) {
            throw new IllegalStateException("Connection pool data source returned null from getPooledConnection");
        }
        // should we add this object as a listener or the pool.
        // consider the validateObject method in decision
        pooledConnection.addConnectionEventListener(this);
        final PooledConnectionAndInfo pci = new PooledConnectionAndInfo(pooledConnection, userPassKey);
        pcMap.put(pooledConnection, pci);
        return new DefaultPooledObject<>(pci);
    }

    @Override
    public void passivateObject(final UserPassKey ignored, final PooledObject<PooledConnectionAndInfo> pooledObject) throws SQLException {
        validateLifetime(pooledObject);
    }

    /**
     * Sets the password, which this implementation ignores because it does not cache user credentials.
     */
    @Override
    public void setPassword(final char[] password) {
        // Does nothing. This factory does not cache user credentials.
    }

    /**
     * Sets the password, which this implementation ignores because it does not cache user credentials.
     */
    @Override
    public void setPassword(final String password) {
        // Does nothing. This factory does not cache user credentials.
    }

    public void setPool(final KeyedObjectPool<UserPassKey, PooledConnectionAndInfo> pool) {
        this.pool = pool;
    }

    /**
     * Validates a pooled connection.
     *
     * <p>
     * A query validation timeout greater than 0 and less than 1 second is converted to 1 second.
     * </p>
     *
     * @param ignored
     *            ignored
     * @param pooledObject
     *            wrapped {@code PooledConnectionAndInfo} containing the connection to validate
     * @return true if validation succeeds
     * @throws ArithmeticException Thrown if the query validation timeout does not fit as seconds in an int.
     */
    @Override
    public boolean validateObject(final UserPassKey ignored, final PooledObject<PooledConnectionAndInfo> pooledObject) {
        return super.validateObject(pooledObject);
    }
}
