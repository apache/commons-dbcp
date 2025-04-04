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

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * A Transaction adapter.
 */
public class TransactionAdapter implements Transaction {

    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException,
        SecurityException, SystemException {
        // Noop
    }

    @Override
    public boolean delistResource(final XAResource arg0, final int arg1) throws IllegalStateException, SystemException {
        return false;
    }

    @Override
    public boolean enlistResource(final XAResource arg0) throws IllegalStateException, RollbackException, SystemException {
        return false;
    }

    @Override
    public int getStatus() throws SystemException {
        return 0;
    }

    @Override
    public void registerSynchronization(final Synchronization arg0)
        throws IllegalStateException, RollbackException, SystemException {
        // Noop
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        // Noop
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        // Noop
    }

}
