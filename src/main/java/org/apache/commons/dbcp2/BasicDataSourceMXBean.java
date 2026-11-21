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

import javax.management.StandardMBean;

/**
 * Legacy data source interface retained for Java API compatibility.
 *
 * <p>
 * As of 2.9.0, this interface extends {@link DataSourceMXBean}.
 * </p>
 *
 * <p>
 * Since 2.15.0, {@link BasicDataSource} implements {@link javax.management.DynamicMBean} to expose {@link DataSourceMXBean} on direct registration,
 * without a {@code Password} attribute. This legacy interface remains an MXBean interface so references to it from other MXBeans retain their
 * {@link javax.management.ObjectName} mapping.
 * </p>
 *
 * <p>
 * A data source supports one direct JMX registration at a time. Unregister it before registering it again, or use separate
 * {@link StandardMBean#StandardMBean(Object, Class) StandardMBean} wrappers with {@link DataSourceMXBean} for multiple registrations.
 * </p>
 *
 * <p>
 * When constructing a {@link StandardMBean}, explicitly select {@link DataSourceMXBean}. Selecting this legacy interface, including through automatic
 * MXBean interface discovery in a wrapper, still exposes {@link #getPassword()}. Other attributes, such as the JDBC URL, are returned unchanged.
 * </p>
 *
 * @since 2.0
 */
public interface BasicDataSourceMXBean extends DataSourceMXBean {

    /**
     * Gets the password passed to the JDBC driver to establish connections.
     *
     * @return The connection password.
     * @deprecated Retained for Java API compatibility. Use {@link DataSourceMXBean} for JMX management to avoid exposing this accessor.
     */
    @Deprecated
    String getPassword();
}
