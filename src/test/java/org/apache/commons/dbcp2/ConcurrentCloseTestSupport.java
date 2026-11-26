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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.function.Executable;

/**
 * Bounded coordination for close regression tests.
 */
final class ConcurrentCloseTestSupport {

    /**
     * Runs an operation and retains any failure for the test thread.
     */
    static final class Task implements AutoCloseable {
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final Thread thread;

        /**
         * Starts a daemon worker, so a failed test cannot keep the test JVM alive.
         */
        Task(final Executable operation) {
            thread = new Thread(() -> {
                try {
                    operation.execute();
                } catch (final Throwable t) {
                    failure.set(t);
                }
            }, "close-regression");
            thread.setDaemon(true);
            thread.start();
        }

        /**
         * Waits for lock contention, failing if the operation finishes without waiting.
         */
        void awaitBlocked() throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (thread.getState() != Thread.State.BLOCKED && thread.getState() != Thread.State.WAITING) {
                assertTrue(thread.isAlive(), () -> "Operation finished instead of waiting: " + failure.get());
                assertTrue(System.nanoTime() < deadline, "Operation did not wait for the other caller");
                Thread.sleep(1);
            }
        }

        /**
         * Gets the operation's failure after waiting for completion.
         */
        Throwable awaitFailure() throws InterruptedException {
            thread.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(thread.isAlive(), "Operation did not finish");
            return failure.get();
        }

        /**
         * Waits for successful completion.
         */
        void awaitSuccess() throws InterruptedException {
            assertNull(awaitFailure());
        }

        @Override
        public void close() throws InterruptedException {
            thread.interrupt();
            thread.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(thread.isAlive(), "Worker did not terminate");
        }
    }

    /**
     * Waits for a test checkpoint without allowing a broken test to hang.
     */
    static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Checkpoint was not reached");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private ConcurrentCloseTestSupport() {
    }
}
