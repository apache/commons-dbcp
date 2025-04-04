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

package org.apache.commons.dbcp2.transaction;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * A TransactionSynchronizationRegistry adapter.
 */
public class TransactionSynchronizationRegistryAdapter implements TransactionSynchronizationRegistry {

    @Override
    public Object getResource(final Object arg0) {
        return null;
    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }

    @Override
    public Object getTransactionKey() {
        return null;
    }

    @Override
    public int getTransactionStatus() {
        return 0;
    }

    @Override
    public void putResource(final Object arg0, final Object arg1) {
        // Noop
    }

    @Override
    public void registerInterposedSynchronization(final Synchronization arg0) {
        // Noop
    }

    @Override
    public void setRollbackOnly() {
        // Noop
    }

}
