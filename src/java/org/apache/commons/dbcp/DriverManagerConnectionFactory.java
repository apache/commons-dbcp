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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A {@link DriverManager}-based implementation of {@link ConnectionFactory}.
 *
 * @author Rodney Waldhoff
 * @author Ignacio J. Ortega
 * @version $Revision: 1.8 $ $Date: 2004/02/28 12:18:17 $
 */
public class DriverManagerConnectionFactory implements ConnectionFactory {

    public DriverManagerConnectionFactory(String connectUri, Properties props) {
        _connectUri = connectUri;
        _props = props;
    }

    public DriverManagerConnectionFactory(String connectUri, String uname, String passwd) {
        _connectUri = connectUri;
        _uname = uname;
        _passwd = passwd;
    }

    public Connection createConnection() throws SQLException {
        if(null == _props) {
            if((_uname == null) || (_passwd == null)) {
                return DriverManager.getConnection(_connectUri);
            } else {
                return DriverManager.getConnection(_connectUri,_uname,_passwd);
            }
        } else {
            return DriverManager.getConnection(_connectUri,_props);
        }
    }

    protected String _connectUri = null;
    protected String _uname = null;
    protected String _passwd = null;
    protected Properties _props = null;
}
