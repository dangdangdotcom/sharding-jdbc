/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *//*


package org.apache.shardingsphere.infra.optimize.execute;

import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

*/
/**
 * Calcite executor.
 *//*

public interface CalciteExecutor {
    
    */
/**
     * Execute query.
     *
     * @param sql sql
     * @param parameters parameters
     * @return execute result
     * @throws SQLException SQL exception
     *//*

    List<QueryResult> executeQuery(String sql, List<Object> parameters) throws SQLException;
    
    */
/**
     * Close.
     * 
     * @throws SQLException SQL exception
     *//*

    void close() throws SQLException;
    
    */
/**
     * Get result set.
     *
     * @return result set
     * @throws SQLException sql exception
     *//*

    ResultSet getResultSet() throws SQLException;
}
*/
