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

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Iterator;
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
    private static final Stack<String> messageStack = new Stack<>();
    private static final Lock lock = new ReentrantLock();

    public StackMessageLog(final String name) {
        super(name);
    }

    /**
     * Ignores type.  Pushes message followed by stack trace of t onto the stack.
     */
    @Override
    protected void log(final int type, final Object message, final Throwable t) {
        lock.lock();
        try {
            final StringBuilder buf = new StringBuilder();
            buf.append(message.toString());
            if(t != null) {
                buf.append(" <");
                buf.append(t.toString());
                buf.append(">");
                final java.io.StringWriter sw = new java.io.StringWriter(1024);
                final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                t.printStackTrace(pw);
                pw.close();
                buf.append(sw.toString());
            }
            messageStack.push(buf.toString());
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the most recent log message, or null if the log is empty
     */
    public static String popMessage() {
        String ret = null;
        lock.lock();
        try {
            ret = messageStack.pop();
        } catch (final EmptyStackException ex) {
            // ignore, return null
        } finally {
            lock.unlock();
        }
        return ret;
    }

    /**
     * Note: iterator is fail-fast, lock the stack first.
     */
    public static List<String> getAll() {
        final Iterator<String> iterator = messageStack.iterator();
        final List<String> messages = new ArrayList<>();
        while (iterator.hasNext()) {
            messages.add(iterator.next());
        }
        return messages;
    }

    public static void clear() {
        lock.lock();
        try {
            messageStack.clear();
        } finally {
            lock.unlock();
        }
    }

    public static boolean isEmpty() {
        return messageStack.isEmpty();
    }

    /**
     * Obtains an exclusive lock on the log.
     */
    public static void lock() {
        lock.lock();
    }

    /**
     * Relinquishes exclusive lock on the log.
     */
    public static void unLock() {
        try {
            lock.unlock();
        } catch (final IllegalMonitorStateException ex) {
            // ignore
        }
    }
}
