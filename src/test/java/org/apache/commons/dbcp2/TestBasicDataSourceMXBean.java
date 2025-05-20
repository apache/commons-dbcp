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

package org.apache.commons.dbcp2;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.OperationsException;

import org.junit.jupiter.api.Test;

/**
 * Tests for BasicDataSourceMXBean.
 */
public class TestBasicDataSourceMXBean {

    public static void testMXBeanCompliance(final Class<?> clazz) throws OperationsException {
        assertTrue(JMX.isMXBeanInterface(clazz));
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = ObjectName.getInstance("com.sun.management:type=DiagnosticCommand");
        JMX.newMBeanProxy(server, objectName, clazz, true);
    }

    private final BasicDataSourceMXBean bean = new BasicDataSourceMXBean() {

        @Override
        public boolean getAbandonedUsageTracking() {
            return false;
        }

        @Override
        public boolean getCacheState() {
            return false;
        }

        @Override
        public String[] getConnectionInitSqlsAsArray() {
            return null;
        }

        @Override
        public Boolean getDefaultAutoCommit() {
            return null;
        }

        @Override
        public String getDefaultCatalog() {
            return null;
        }

        @Override
        public Boolean getDefaultReadOnly() {
            return null;
        }

        @Override
        public int getDefaultTransactionIsolation() {
            return 0;
        }

        @Override
        public String[] getDisconnectionIgnoreSqlCodesAsArray() {
            return null;
        }

        @Override
        public String[] getDisconnectionSqlCodesAsArray() {
            return null;
        }

        @Override
        public String getDriverClassName() {
            return null;
        }

        @Override
        public boolean getFastFailValidation() {
            return false;
        }

        @Override
        public int getInitialSize() {
            return 0;
        }

        @Override
        public boolean getLifo() {
            return false;
        }

        @Override
        public boolean getLogAbandoned() {
            return false;
        }

        @Override
        public boolean getLogExpiredConnections() {
            return false;
        }

        @Override
        public long getMaxConnLifetimeMillis() {
            return 0;
        }

        @Override
        public int getMaxIdle() {
            return 0;
        }

        @Override
        public int getMaxOpenPreparedStatements() {
            return 0;
        }

        @Override
        public int getMaxTotal() {
            return 0;
        }

        @Override
        public long getMaxWaitMillis() {
            return 0;
        }

        @Override
        public long getMinEvictableIdleTimeMillis() {
            return 0;
        }

        @Override
        public int getMinIdle() {
            return 0;
        }

        @Override
        public int getNumActive() {
            return 0;
        }

        @Override
        public int getNumIdle() {
            return 0;
        }

        @Override
        public int getNumTestsPerEvictionRun() {
            return 0;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean getRemoveAbandonedOnBorrow() {
            return false;
        }

        @Override
        public boolean getRemoveAbandonedOnMaintenance() {
            return false;
        }

        @Override
        public int getRemoveAbandonedTimeout() {
            return 0;
        }

        @Override
        public long getSoftMinEvictableIdleTimeMillis() {
            return 0;
        }

        @Override
        public boolean getTestOnBorrow() {
            return false;
        }

        @Override
        public boolean getTestOnCreate() {
            return false;
        }

        @Override
        public boolean getTestWhileIdle() {
            return false;
        }

        @Override
        public long getTimeBetweenEvictionRunsMillis() {
            return 0;
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUsername() {
            return null;
        }

        @Override
        public String getValidationQuery() {
            return null;
        }

        @Override
        public int getValidationQueryTimeout() {
            return 0;
        }

        @Override
        public boolean isAccessToUnderlyingConnectionAllowed() {
            return false;
        }

        @Override
        public boolean isClearStatementPoolOnReturn() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public boolean isPoolPreparedStatements() {
            return false;
        }
    };

    /**
     * Tests the interface defined default method.
     */
    @Test
    public void testDefaultSchema() {
        assertNull(bean.getDefaultSchema());
    }

    /**
     * Tests if the {@link BasicDataSourceMXBean} interface is a valid MXBean interface.
     */
    @Test
    public void testMXBeanCompliance() throws OperationsException {
        testMXBeanCompliance(BasicDataSourceMXBean.class);
    }
}
