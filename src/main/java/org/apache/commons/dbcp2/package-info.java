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

/**
 * Database Connection Pool API.
 *
 * <strong>FAQ</strong>
 *
 * <p>
 * Q: How do I use the DBCP package?
 * </p>
 *
 * <p>
 * A: There are two primary ways to access the DBCP pool, as a {@link java.sql.Driver Driver}, or as a
 * {@link javax.sql.DataSource DataSource}. You'll want to create an instance of
 * {@link org.apache.commons.dbcp2.PoolingDriver} or {@link org.apache.commons.dbcp2.PoolingDataSource}. When using one
 * of these interfaces, you can just use your JDBC objects the way you normally would. Closing a
 * {@link java.sql.Connection} will simply return it to its pool.
 * </p>
 *
 * <p>
 * Q: But {@link org.apache.commons.dbcp2.PoolingDriver PoolingDriver} and
 * {@link org.apache.commons.dbcp2.PoolingDataSource PoolingDataSource} both expect an
 * {@link org.apache.commons.pool2.ObjectPool ObjectPool} as an input. Where do I get one of those?
 * </p>
 *
 * <p>
 * A: The {@link org.apache.commons.pool2.ObjectPool ObjectPool} interface is defined in Commons Pool. You can use one
 * of the provided implementations such as {@link org.apache.commons.pool2.impl.GenericObjectPool GenericObjectPool},
 * {@link org.apache.commons.pool2.proxy.ProxiedObjectPool ProxiedObjectPool} or
 * {@link org.apache.commons.pool2.impl.SoftReferenceObjectPool SoftReferenceObjectPool} or you can create your own.
 * </p>
 *
 * <p>
 * Q: Ok, I've found an {@link org.apache.commons.pool2.ObjectPool ObjectPool} implementation that I think suits my
 * connection pooling needs. But it wants a {@link org.apache.commons.pool2.PooledObjectFactory PooledObjectFactory}.
 * What should I use for that?
 * </p>
 *
 * <p>
 * A: The DBCP package provides a class for this purpose. It's called
 * {@link org.apache.commons.dbcp2.PoolableConnectionFactory}. It implements the factory and lifecycle methods of
 * {@link org.apache.commons.pool2.PooledObjectFactory} for {@link java.sql.Connection}s. But it doesn't create the
 * actual database {@link java.sql.Connection}s itself, it uses a {@link org.apache.commons.dbcp2.ConnectionFactory} for
 * that. The {@link org.apache.commons.dbcp2.PoolableConnectionFactory} will take {@link java.sql.Connection}s created
 * by the {@link org.apache.commons.dbcp2.ConnectionFactory} and wrap them with classes that implement the pooling
 * behavior.
 * </p>
 *
 * <p>
 * Several implementations of {@link org.apache.commons.dbcp2.ConnectionFactory} are provided--one that uses
 * {@link java.sql.DriverManager} to create connections
 * ({@link org.apache.commons.dbcp2.DriverManagerConnectionFactory}), one that uses a {@link java.sql.Driver} to create
 * connections ({@link org.apache.commons.dbcp2.DriverConnectionFactory}), one that uses a {@link javax.sql.DataSource}
 * to create connections ({@link org.apache.commons.dbcp2.DataSourceConnectionFactory}).
 * </p>
 *
 * <p>
 * Q: I think I'm starting to get it, but can you walk me though it again?
 * </p>
 *
 * <p>
 * A: Sure. Let's assume you want to create a {@link javax.sql.DataSource} that pools {@link java.sql.Connection}s.
 * Let's also assume that those pooled {@link java.sql.Connection}s should be obtained from the
 * {@link java.sql.DriverManager}. You'll want to create a {@link org.apache.commons.dbcp2.PoolingDataSource}.
 * </p>
 *
 * <p>
 * The {@link org.apache.commons.dbcp2.PoolingDataSource} uses an underlying {@link org.apache.commons.pool2.ObjectPool}
 * to create and store its {@link java.sql.Connection}.
 * </p>
 *
 * <p>
 * To create a {@link org.apache.commons.pool2.ObjectPool}, you'll need a
 * {@link org.apache.commons.pool2.PooledObjectFactory} that creates the actual {@link java.sql.Connection}s. That's
 * what {@link org.apache.commons.dbcp2.PoolableConnectionFactory} is for.
 * </p>
 *
 * <p>
 * To create the {@link org.apache.commons.dbcp2.PoolableConnectionFactory}, you'll need at least two things:
 * </p>
 * <ol>
 * <li>A {@link org.apache.commons.dbcp2.ConnectionFactory} from which the actual database {@link java.sql.Connection}s
 * will be obtained.</li>
 * <li>An empty and factory-less {@link org.apache.commons.pool2.ObjectPool} in which the {@link java.sql.Connection}s
 * will be stored. <br>
 * When you pass an {@link org.apache.commons.pool2.ObjectPool} into the
 * {@link org.apache.commons.dbcp2.PoolableConnectionFactory}, it will automatically register itself as the
 * {@link org.apache.commons.pool2.PooledObjectFactory} for that pool.</li>
 * </ol>
 *
 * <p>
 * In code, that might look like this:
 * </p>
 * <pre>
 * GenericObjectPool connectionPool = new GenericObjectPool(null);
 * ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string", "userName",
 *         "password");
 * ObjectName oName = new ObjectName("MyTests:DataSource=test"); // or null to not use JMX
 * PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, oName);
 * PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
 * </pre>
 *
 * <p>
 * To create a {@link org.apache.commons.dbcp2.PoolingDriver}, we do the same thing, except that instead of creating a
 * {@link javax.sql.DataSource} on the last line, we create a {@link org.apache.commons.dbcp2.PoolingDriver}, and
 * register the {@code connectionPool} with it. For example:
 * </p>
 * <pre>
 * GenericObjectPool connectionPool = new GenericObjectPool(null);
 * ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string", "userName",
 *         "password");
 * ObjectName oName = new ObjectName("MyTests:DataSource=test"); // or null to not use JMX
 * PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, oName);
 * PoolingDriver driver = new PoolingDriver();
 * driver.registerPool("example", connectionPool);
 * </pre>
 *
 * <p>
 * Since the {@link org.apache.commons.dbcp2.PoolingDriver} registers itself with the {@link java.sql.DriverManager}
 * when it is created, now you can just go to the {@link java.sql.DriverManager} to create your
 * {@link java.sql.Connection}s, like you normally would:
 * </p>
 * <pre>
 * Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:example");
 * </pre>
 *
 * <p>
 * Q: Do I need to specify or explicitly load a JDBC driver class?
 * </p>
 *
 * <p>
 * A: Usually not with JDBC 4.0 or later when using {@link java.sql.DriverManager}. JDBC 4.0, introduced in Java SE 6,
 * added automatic driver discovery through Java's service-provider mechanism. A driver JAR on the classpath declares
 * its driver implementations in {@code META-INF/services/java.sql.Driver}. Provided the service metadata and driver
 * classes are visible to the relevant class loaders, the application normally supplies the JDBC URL and any required
 * credentials or connection properties without explicitly naming or loading the driver class.
 * </p>
 *
 * <p>
 * Before JDBC 4.0, applications using {@link java.sql.DriverManager} generally arranged for drivers to be loaded
 * explicitly, commonly through {@link java.lang.Class#forName(String)} or the {@code jdbc.drivers} system property.
 * Drivers then registered themselves with {@link java.sql.DriverManager}. This was the usual model for JDBC 1.x,
 * 2.x, and 3.0. JDBC 3.0 was introduced in J2SE 1.4 and remained the JDBC version in Java 5.
 * </p>
 *
 * <p>
 * JDBC 4.1 (Java 7), 4.2 (Java 8), and 4.3 (Java 9) retained automatic discovery while adding other JDBC capabilities.
 * Explicit loading remains supported for compatibility. Tools and deployment environments may still require a driver
 * class name, including with modern JDBC drivers. See the
 * <a href="https://docs.oracle.com/javase/9/docs/api/java/sql/package-summary.html">JDBC version history and features</a>.
 * </p>
 *
 * <p>
 * Q: How does DBCP select a JDBC driver?
 * </p>
 *
 * <p>
 * A: When using {@link org.apache.commons.dbcp2.BasicDataSource}, a driver instance supplied through
 * {@link org.apache.commons.dbcp2.BasicDataSource#setDriver(java.sql.Driver)} takes precedence over a class name
 * supplied through {@link org.apache.commons.dbcp2.BasicDataSource#setDriverClassName(String)}. If neither is
 * configured, DBCP uses {@link java.sql.DriverManager#getDriver(String)} to select an available driver for the
 * configured JDBC URL. See {@link org.apache.commons.dbcp2.BasicDataSource#createConnectionFactory()} for details
 * of class loading and driver validation.
 * </p>
 *
 * <p>
 * URL-based selection predates JDBC 4.0. Automatic discovery changes how drivers become available to
 * {@link java.sql.DriverManager}; selection uses drivers that are registered and accessible to the caller. A driver
 * JAR can contain multiple implementations for compatibility or specialized use cases. If several drivers accept
 * the same JDBC URL, that URL alone does not uniquely identify the intended implementation.
 * </p>
 *
 * <p>
 * Q: Does DBCP make an untrusted JDBC driver safe to use?
 * </p>
 *
 * <p>
 * A: No. Whether a driver JAR is supplied through a user interface, obtained as a Maven dependency, or made available
 * through another build or deployment process, it contains executable code chosen for the application to load.
 * Automatic discovery neither downloads the driver nor establishes its trustworthiness. Developers and
 * administrators are responsible for selecting trusted dependencies and controlling who can change driver
 * configuration.
 * </p>
 *
 * <p>
 * When {@link org.apache.commons.dbcp2.BasicDataSource} loads a driver class by name, it checks that the class
 * implements {@link java.sql.Driver} before initializing it or invoking its constructor. This hardening prevents
 * unrelated classes from being initialized or instantiated through that driver-creation path. A malicious
 * implementation of {@link java.sql.Driver} can still pass the check and execute code with the privileges available
 * to it. Type validation does not establish that a driver is safe, and DBCP does not provide a sandbox for driver
 * implementations. The driver JAR must therefore be trusted.
 * </p>
 */
package org.apache.commons.dbcp2;
