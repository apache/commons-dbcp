/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.dbcp.managed;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;

import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import java.sql.SQLException;

/**
 * BasicManagedDataSource is an extension of BasicDataSource which
 * creates ManagedConnections.  This data source can create either
 * create full two-phase-commit XA connections or one-phase-commit
 * local connection.  Both types of connections are committed or
 * rolled back as part of the global transaction (a.k.a. XA
 * transaction or JTA Transaction), but only XA connections can be
 * recovered in the case of a system crash.
 * </p>
 * BasicManagedDataSource adds the TransactionManager and XADataSource
 * properties.  The TransactionManager property is required and is
 * used to elist connections in global transactions.  The XADataSource
 * is optional and if set is the class name of the XADataSource class
 * for a two-phase-commit JDBC driver.  If the XADataSource property
 * is set, the driverClassName is ignored and a DataSourceXAConnectionFactory
 * is created. Otherwise, a standard DriverConnectionFactory is created
 * and wrapped with a LocalXAConnectionFactory.
 * </p>
 * This is not the only way to combine the <em>commons-dbcp</em> and
 * <em>commons-pool</em> packages, but provides a "one stop shopping"
 * solution for basic requirements.</p>
 *
 * @see BasicDataSource
 * @see ManagedConnection
 * @version $Revision$
 */
public class BasicManagedDataSource extends BasicDataSource {
    protected TransactionRegistry transactionRegistry;
    protected TransactionManager transactionManager;
    protected String xaDataSource;

    /**
     * Gets the required transaction manager property.
     * @return the transaction manager used to enlist connections
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Sets the required transaction manager property.
     * @param transactionManager the transaction manager used to enlist connections
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Gets the optional XADataSource class name.
     * @return the optional XADataSource class name
     */
    public String getXADataSource() {
        return xaDataSource;
    }

    /**
     * Sets the optional XADataSource class name.
     * @param xaDataSource the optional XADataSource class name
     */
    public void setXADataSource(String xaDataSource) {
        this.xaDataSource = xaDataSource;
    }

    protected ConnectionFactory createConnectionFactory() throws SQLException {
        if (transactionManager == null) {
            throw new SQLException("Transaction manager must be set before a connection can be created");
        }

        // If xa data source is not specified a DriverConnectionFactory is created and wrapped with a LocalXAConnectionFactory
        if (xaDataSource == null) {
            ConnectionFactory connectionFactory = super.createConnectionFactory();
            XAConnectionFactory xaConnectionFactory = new LocalXAConnectionFactory(getTransactionManager(), connectionFactory);
            transactionRegistry = xaConnectionFactory.getTransactionRegistry();
            return xaConnectionFactory;
        }

        // Load the XA data source class
        Class xaDataSourceClass = null;
        try {
            xaDataSourceClass = Class.forName(xaDataSource);
        } catch (Throwable t) {
            String message = "Cannot load XA data source class '" + xaDataSource + "'";
            logWriter.println(message);
            t.printStackTrace(logWriter);
            throw (SQLException) new SQLException(message).initCause(t);
        }

        // Create the xa data source instance
        XADataSource xaDataSource = null;
        try {
            xaDataSource = (XADataSource) xaDataSourceClass.newInstance();
        } catch (Throwable t) {
            String message = "Cannot create XA data source of class '" + xaDataSource + "'";
            logWriter.println(message);
            t.printStackTrace(logWriter);
            throw (SQLException) new SQLException(message).initCause(t);
        }

        // finally, create the XAConectionFactory using the XA data source
        XAConnectionFactory xaConnectionFactory = new DataSourceXAConnectionFactory(getTransactionManager(), xaDataSource, username, password);
        transactionRegistry = xaConnectionFactory.getTransactionRegistry();
        return xaConnectionFactory;
    }

    protected void createDataSourceInstance() throws SQLException {
        dataSource = new ManagedDataSource(connectionPool, transactionRegistry);
        ((PoolingDataSource) dataSource).setAccessToUnderlyingConnectionAllowed(isAccessToUnderlyingConnectionAllowed());
        dataSource.setLogWriter(logWriter);
    }
}
