/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp.cpdsadapter;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.dbcp.PoolablePreparedStatement;

/**
 * A {@link PoolablePreparedStatement} stub since activate and passivate
 * are declared protected and we need to be able to call them within this
 * package.
 *
 * @author John D. McNally
 * @version $Revision: 1.8 $ $Date: 2004/02/28 12:18:17 $
 */
class PoolablePreparedStatementStub extends PoolablePreparedStatement {

    /**
     * Constructor
     * @param stmt my underlying {@link PreparedStatement}
     * @param key my key" as used by {@link KeyedObjectPool}
     * @param pool the {@link KeyedObjectPool} from which I was obtained.
     * @param conn the {@link Connection} from which I was created
     */
    public PoolablePreparedStatementStub(PreparedStatement stmt, Object key, 
            KeyedObjectPool pool, Connection conn) {
        super(stmt, key, pool, conn);
    }

    protected void activate() throws SQLException {
        super.activate();
    }

    protected void passivate() throws SQLException {
        super.passivate();
    }
}
