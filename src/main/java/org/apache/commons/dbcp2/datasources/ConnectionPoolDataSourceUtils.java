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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.Utils;

/** Shared physical connection creation with scoped provider configuration. */
final class ConnectionPoolDataSourceUtils {

    /** Weak identity keys avoid invoking mutable provider equals/hashCode implementations. */
    private static final class ProviderReference extends WeakReference<ConnectionPoolDataSource> {
        private final int hashCode;

        ProviderReference(final ConnectionPoolDataSource provider) {
            super(provider, STALE_PROVIDERS);
            hashCode = System.identityHashCode(provider);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            final ConnectionPoolDataSource provider = get();
            return provider != null && obj instanceof ProviderReference && provider == ((ProviderReference) obj).get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static final ReferenceQueue<ConnectionPoolDataSource> STALE_PROVIDERS = new ReferenceQueue<>();

    /** Neither the weak keys nor the lock values retain providers after their pools are closed. */
    private static final Map<ProviderReference, ReentrantReadWriteLock> LOCKS = new HashMap<>();

    private static PooledConnection connect(final ConnectionPoolDataSource cpds, final String userName, final String password) throws SQLException {
        return userName == null ? cpds.getPooledConnection() : cpds.getPooledConnection(userName, password);
    }

    /**
     * Opens a physical connection with an optional login timeout, restoring the provider's setting afterwards.
     * Timeout overrides exclude other connection attempts through these pools. Attempts without an override remain concurrent.
     */
    static PooledConnection getPooledConnection(final ConnectionPoolDataSource cpds, final String userName, final String password,
            final Duration loginTimeout) throws SQLException {
        Objects.requireNonNull(cpds, "cpds");
        final ReentrantReadWriteLock providerLock;
        synchronized (LOCKS) {
            Reference<? extends ConnectionPoolDataSource> stale;
            while ((stale = STALE_PROVIDERS.poll()) != null) {
                LOCKS.remove(stale);
            }
            providerLock = LOCKS.computeIfAbsent(new ProviderReference(cpds), key -> new ReentrantReadWriteLock());
        }
        final Lock lock = loginTimeout == null ? providerLock.readLock() : providerLock.writeLock();
        lock(lock, loginTimeout);
        try {
            if (loginTimeout == null) {
                return connect(cpds, userName, password);
            }
            final int previousTimeout = cpds.getLoginTimeout();
            final int timeoutSeconds = Utils.toJdbcSeconds(loginTimeout);
            if (previousTimeout == timeoutSeconds) {
                return connect(cpds, userName, password);
            }
            PooledConnection connection = null;
            Throwable failure = null;
            try {
                cpds.setLoginTimeout(timeoutSeconds);
                connection = connect(cpds, userName, password);
                return connection;
            } catch (final SQLException | RuntimeException | Error e) {
                failure = e;
                throw e;
            } finally {
                try {
                    cpds.setLoginTimeout(previousTimeout);
                } catch (final SQLException | RuntimeException | Error restoreException) {
                    if (failure != null) {
                        if (failure != restoreException) {
                            failure.addSuppressed(restoreException);
                        }
                    } else {
                        if (connection != null) {
                            try {
                                connection.close();
                            } catch (final SQLException | RuntimeException | Error closeException) {
                                if (closeException != restoreException) {
                                    restoreException.addSuppressed(closeException);
                                }
                            }
                        }
                        throw restoreException;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private static void lock(final Lock lock, final Duration timeout) throws SQLException {
        try {
            if (timeout != null && !timeout.isNegative() && !timeout.isZero()) {
                if (!lock.tryLock(Utils.toJdbcSeconds(timeout), TimeUnit.SECONDS)) {
                    throw new SQLTimeoutException("Timed out waiting to configure the provider's login timeout");
                }
            } else {
                lock.lockInterruptibly();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting to configure the provider's login timeout", e);
        }
    }

    private ConnectionPoolDataSourceUtils() {
        // Not instantiable.
    }
}
