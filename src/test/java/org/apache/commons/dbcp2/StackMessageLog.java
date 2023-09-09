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

package org.apache.commons.dbcp2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.impl.SimpleLog;

/**
 * A logger that pushes log messages onto a stack. The stack itself is static.
 * To get a log exclusive to a test case, use explicit lock / unlock and clear.
 */
public class StackMessageLog extends SimpleLog {

    private static final long serialVersionUID = 1L;
    private static final Stack<String> MESSAGE_STACK = new Stack<>();
    private static final Lock LOCK = new ReentrantLock();

    public static void clear() {
        LOCK.lock();
        try {
            MESSAGE_STACK.clear();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Note: iterator is fail-fast, lock the stack first.
     */
    public static List<String> getAll() {
        final List<String> messages = new ArrayList<>();
        for (String element : MESSAGE_STACK) {
            messages.add(element);
        }
        return messages;
    }

    public static boolean isEmpty() {
        return MESSAGE_STACK.isEmpty();
    }

    /**
     * Obtains an exclusive lock on the log.
     */
    public static void lock() {
        LOCK.lock();
    }

    /**
     * @return the most recent log message, or null if the log is empty
     */
    public static String popMessage() {
        LOCK.lock();
        try {
            return MESSAGE_STACK.pop();
        } catch (final EmptyStackException ex) {
            // ignore, return null
            return null;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Relinquishes exclusive lock on the log.
     */
    public static void unLock() {
        try {
            LOCK.unlock();
        } catch (final IllegalMonitorStateException ex) {
            // ignore
        }
    }

    public StackMessageLog(final String name) {
        super(name);
    }

    /**
     * Ignores type.  Pushes message followed by stack trace of t onto the stack.
     */
    @Override
    protected void log(final int type, final Object message, final Throwable t) {
        LOCK.lock();
        try {
            final StringBuilder buf = new StringBuilder();
            buf.append(message.toString());
            if (t != null) {
                buf.append(" <");
                buf.append(t.toString());
                buf.append(">");
                final java.io.StringWriter sw = new StringWriter(1024);
                try (final java.io.PrintWriter pw = new PrintWriter(sw)) {
                    t.printStackTrace(pw);
                }
                buf.append(sw.toString());
            }
            MESSAGE_STACK.push(buf.toString());
        } finally {
            LOCK.unlock();
        }
    }
}
