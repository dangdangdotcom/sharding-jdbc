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
 */

package org.apache.shardingsphere.shardingjdbc.orchestration.temp.internal.circuit.datasource;

import org.apache.shardingsphere.shardingjdbc.jdbc.unsupported.AbstractUnsupportedOperationDataSource;
import org.apache.shardingsphere.shardingjdbc.orchestration.temp.internal.circuit.connection.CircuitBreakerConnection;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Circuit breaker datasource.
 * 
 * @author caohao
 */
public final class CircuitBreakerDataSource extends AbstractUnsupportedOperationDataSource implements AutoCloseable {
    
    @Override
    public void close() {
    }
    
    @Override
    public Connection getConnection() {
        return new CircuitBreakerConnection();
    }
    
    @Override
    public Connection getConnection(final String username, final String password) {
        return new CircuitBreakerConnection();
    }
    
    @Override
    public PrintWriter getLogWriter() {
        return null;
    }
    
    @Override
    public void setLogWriter(final PrintWriter out) {
    }
    
    @Override
    public Logger getParentLogger() {
        return null;
    }
}
