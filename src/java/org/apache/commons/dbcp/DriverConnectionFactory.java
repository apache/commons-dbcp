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

package org.apache.commons.dbcp;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A {@link Driver}-based implementation of {@link ConnectionFactory}.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.7 $ $Date: 2004/02/28 12:18:17 $
 */
public class DriverConnectionFactory implements ConnectionFactory {
    public DriverConnectionFactory(Driver driver, String connectUri, Properties props) {
        _driver = driver;
        _connectUri = connectUri;
        _props = props;
    }

    public Connection createConnection() throws SQLException {
        return _driver.connect(_connectUri,_props);
    }

    protected Driver _driver = null;
    protected String _connectUri = null;
    protected Properties _props = null;

    public String toString() {
        return this.getClass().getName() + " [" + String.valueOf(_driver) + ";" + String.valueOf(_connectUri) + ";"  + String.valueOf(_props) + "]";
    }
}
