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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.sql.SQLException;

import javax.sql.PooledConnection;

import org.junit.jupiter.api.Test;

/*
 * Tests {@link PooledConnectionManager}.
 */
class PooledConnectionManagerTest {

    static class Fixture implements PooledConnectionManager {

        private char[] password;

        @Override
        public void closePool(final String userName) throws SQLException {
            // empty
        }

        char[] getPassword() {
            return password;
        }

        @Override
        public void invalidate(final PooledConnection pc) throws SQLException {
            // empty
        }

        @Override
        public void setPassword(final String password) {
            this.password = password.toCharArray();
        }
    }

    @Test
    void testSetPasswordCharArray() {
        final Fixture fixture = new Fixture();
        fixture.setPassword("p".toCharArray());
        assertArrayEquals("p".toCharArray(), fixture.getPassword());
    }

    @Test
    void testSetPasswordString() {
        final Fixture fixture = new Fixture();
        fixture.setPassword("p");
        assertArrayEquals("p".toCharArray(), fixture.getPassword());
    }
}
