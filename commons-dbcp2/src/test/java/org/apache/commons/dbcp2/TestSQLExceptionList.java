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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.SQLTransientException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TestSQLExceptionList {

    @Test
    void testCause() {
        final SQLTransientException cause = new SQLTransientException();
        final List<SQLTransientException> list = Collections.singletonList(cause);
        final SQLExceptionList sqlExceptionList = new SQLExceptionList(list);
        assertEquals(cause, sqlExceptionList.getCause());
        assertEquals(list, sqlExceptionList.getCauseList());
        sqlExceptionList.printStackTrace();
    }

    @Test
    void testNullCause() {
        final SQLExceptionList sqlExceptionList = new SQLExceptionList(null);
        assertNull(sqlExceptionList.getCause());
        assertNull(sqlExceptionList.getCauseList());
    }
}
